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
		DescribeWorkflowExecutionRequest request = DescribeWorkflowExecutionRequest.newBuilder()
				.setNamespace(temporalProperties.getNamespace())
				.setExecution(WorkflowExecution.newBuilder().setWorkflowId(workflowId).build()).build();
		try {
			DescribeWorkflowExecutionResponse response = workflowServiceStubs.blockingStub()
					.describeWorkflowExecution(request);
			WorkflowExecutionInfo info = response.getWorkflowExecutionInfo();
			return info.getType().getName().equals(workflowType)
					&& info.getStatus() == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING;
		} catch (StatusRuntimeException e) {
			return false;
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
}
