package com.murex.mxorbit.squadorchestrator.config.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.murex.mxorbit.squadorchestrator.api.squad.SquadController;
import com.murex.mxorbit.squadorchestrator.api.squad.mapper.SquadApiMapper;
import com.murex.mxorbit.squadorchestrator.core.squad.facade.SquadFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Confirms that a representative API endpoint remains reachable while the Task
 * 6 security foundation is disabled (the default), and that it is not
 * short-circuited with 401/403 by Spring Security now that the security
 * starters are on the classpath.
 */
@WebMvcTest(controllers = SquadController.class)
@Import({SecurityConfig.class, MxorbitSecurityPropertiesConfiguration.class})
@TestPropertySource(properties = "mxorbit.security.enabled=false")
class SecurityDisabledEndpointAccessibilityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SquadFacade squadFacade;

	@MockitoBean
	private SquadApiMapper squadApiMapper;

	@Test
	void getSquadsRemainsAccessibleWithoutAuthenticationWhileSecurityIsDisabled() throws Exception {
		when(squadFacade.getSquads()).thenReturn(List.of());
		when(squadApiMapper.toSquadApiResponses(List.of())).thenReturn(List.of());

		mockMvc.perform(get("/squads")).andExpect(status().isOk());
	}
}
