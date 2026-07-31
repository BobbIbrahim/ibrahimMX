package com.murex.mxorbit.squadorchestrator.api.squad.request;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSquadRunApiRequest {

	@Builder.Default
	private Map<String, Object> input = new LinkedHashMap<>();
}
