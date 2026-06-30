package com.mxorbit.backend.squad.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SquadRepository extends JpaRepository<SquadEntity, UUID> {
}