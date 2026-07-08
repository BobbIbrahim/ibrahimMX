package com.murex.mxorbit.squadorchestrator.core.workflow.client;

import io.grpc.StatusRuntimeException;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.spring.boot.autoconfigure.properties.TemporalProperties;
import io.temporal.workflow.Functions;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TemporalClientService implements TemporalClient {

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

	private WorkflowRunStatus mapStatus(WorkflowExecutionStatus status) {
		if (status == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING) {
			return WorkflowRunStatus.RUNNING;
		}
		if (status == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED) {
			return WorkflowRunStatus.COMPLETED;
		}
		return WorkflowRunStatus.FAILED;
	}
}
