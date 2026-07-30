package com.murex.mxorbit.squadorchestrator.core.squad.agent;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDefinition {

	@NonNull
	private String agentKey;

	@NonNull
	private String name;

	@Builder.Default
	@NonNull
	private List<String> inputs = new ArrayList<>();

	@Builder.Default
	@NonNull
	private List<String> outputs = new ArrayList<>();

	@Builder.Default
	@NonNull
	private String serviceUrl = "http://localhost:8000";
}
