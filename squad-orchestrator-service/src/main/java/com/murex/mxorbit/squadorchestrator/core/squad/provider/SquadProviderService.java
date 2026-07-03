package com.murex.mxorbit.squadorchestrator.core.squad.provider;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.store.SquadStore;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadProviderService implements SquadProvider {

	private final SquadStore squadStore;

	@Override
	public List<Squad> getSquads() {
		log.debug("Getting all squads");
		return squadStore.findAll();
	}
}
