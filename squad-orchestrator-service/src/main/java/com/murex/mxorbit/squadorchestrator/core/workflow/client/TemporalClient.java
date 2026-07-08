package com.murex.mxorbit.squadorchestrator.core.workflow.client;

import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.workflow.Functions;

public interface TemporalClient {

	<T> T createWorkflowExecutionStub(Class<T> workflowClass, WorkflowOptions workflowOptions);

	<T> void startWorkflow(Functions.Proc1<T> workflowMethod, T input);

	boolean isWorkflowRunning(String workflowId, String workflowType);

	WorkflowRunStatus getWorkflowStatus(String workflowId, String workflowType);

	<T> T getWorkflowExecutionStub(Class<T> workflowClass, String workflowId);

	WorkflowStub getWorkflowStub(String workflowId);
}
