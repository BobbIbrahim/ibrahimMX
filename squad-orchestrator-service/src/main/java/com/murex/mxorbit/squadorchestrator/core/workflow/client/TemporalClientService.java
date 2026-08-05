package com.murex.mxorbit.squadorchestrator.core.workflow.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import io.temporal.api.common.v1.Memo;
import io.temporal.api.common.v1.Payload;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.spring.boot.autoconfigure.properties.TemporalProperties;
import io.temporal.workflow.Functions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TemporalClientService implements TemporalClient {

	private static final int LIST_PAGE_SIZE = 100;

	private final WorkflowClient client;
	private final TemporalProperties temporalProperties;
	private final WorkflowServiceStubs workflowServiceStubs;

	@Override
	public <T> T createWorkflowExecutionStub(Class<T> workflowClass, WorkflowOptions workflowOptions) {
		return client.newWorkflowStub(workflowClass, workflowOptions);
	}

	@Override
	public <T> void startWorkflow(Functions.Proc1<T> workflowMethod, T input) {
		WorkflowClient.start(workflowMethod, input);
	}

	@Override
	public boolean isWorkflowRunning(String workflowId, String workflowType) {
		return getWorkflowStatus(workflowId, workflowType) == WorkflowRunStatus.RUNNING;
	}

	@Override
	public WorkflowRunStatus getWorkflowStatus(String workflowId, String workflowType) {
		try {
			WorkflowExecutionInfo info = describeWorkflowExecution(workflowId);
			if (!info.getType().getName().equals(workflowType)) {
				return WorkflowRunStatus.FAILED;
			}
			return mapStatus(info.getStatus());
		} catch (StatusRuntimeException e) {
			return WorkflowRunStatus.FAILED;
		}
	}

	@Override
	public List<WorkflowExecutionSummary> listWorkflowExecutions(String workflowType) {
		return queryWorkflowExecutions("WorkflowType = '" + workflowType + "'");
	}

	@Override
	public List<WorkflowExecutionSummary> listRunningWorkflowExecutions(String workflowType) {
		return queryWorkflowExecutions("WorkflowType = '" + workflowType + "' AND ExecutionStatus = 'Running'");
	}

	private List<WorkflowExecutionSummary> queryWorkflowExecutions(String query) {
		List<WorkflowExecutionSummary> summaries = new ArrayList<>();
		ByteString nextPageToken = ByteString.EMPTY;
		do {
			ListWorkflowExecutionsRequest request = ListWorkflowExecutionsRequest.newBuilder()
					.setNamespace(temporalProperties.getNamespace()).setPageSize(LIST_PAGE_SIZE).setQuery(query)
					.setNextPageToken(nextPageToken).build();

			ListWorkflowExecutionsResponse response = workflowServiceStubs.blockingStub()
					.listWorkflowExecutions(request);

			for (WorkflowExecutionInfo info : response.getExecutionsList()) {
				summaries.add(toWorkflowExecutionSummary(info));
			}
			nextPageToken = response.getNextPageToken();
		} while (!nextPageToken.isEmpty());
		summaries.sort(Comparator.comparing(WorkflowExecutionSummary::getStartTime).reversed());
		return summaries;
	}

	@Override
	public <T> T getWorkflowExecutionStub(Class<T> workflowClass, String workflowId) {
		return client.newWorkflowStub(workflowClass, workflowId);
	}

	@Override
	public WorkflowStub getWorkflowStub(String workflowId) {
		return client.newUntypedWorkflowStub(workflowId);
	}

	private WorkflowExecutionInfo describeWorkflowExecution(String workflowId) {
		DescribeWorkflowExecutionRequest request = DescribeWorkflowExecutionRequest.newBuilder()
				.setNamespace(temporalProperties.getNamespace())
				.setExecution(WorkflowExecution.newBuilder().setWorkflowId(workflowId).build()).build();

		DescribeWorkflowExecutionResponse response = workflowServiceStubs.blockingStub()
				.describeWorkflowExecution(request);
		return response.getWorkflowExecutionInfo();
	}

	private WorkflowExecutionSummary toWorkflowExecutionSummary(WorkflowExecutionInfo info) {
		WorkflowExecutionSummary.WorkflowExecutionSummaryBuilder builder = WorkflowExecutionSummary.builder()
				.workflowId(info.getExecution().getWorkflowId()).startTime(toInstant(info.getStartTime()))
				.memo(decodeMemo(info.getMemo())).status(mapStatus(info.getStatus()));
		if (info.hasCloseTime() && info.getCloseTime().getSeconds() > 0) {
			builder.closeTime(toInstant(info.getCloseTime()));
		}
		return builder.build();
	}

	private Instant toInstant(Timestamp timestamp) {
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
	}

	private Map<String, String> decodeMemo(Memo memo) {
		Map<String, String> decoded = new LinkedHashMap<>();
		for (Map.Entry<String, Payload> field : memo.getFieldsMap().entrySet()) {
			decoded.put(field.getKey(),
					client.getOptions().getDataConverter().fromPayload(field.getValue(), String.class, String.class));
		}
		return decoded;
	}

	private WorkflowRunStatus mapStatus(WorkflowExecutionStatus status) {
		if (status == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING) {
			return WorkflowRunStatus.RUNNING;
		}
		if (status == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED) {
			return WorkflowRunStatus.COMPLETED;
		}
		if (status == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_CANCELED
				|| status == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_TERMINATED) {
			return WorkflowRunStatus.CANCELLED;
		}
		return WorkflowRunStatus.FAILED;
	}
}
