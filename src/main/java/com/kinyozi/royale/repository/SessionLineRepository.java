package com.kinyozi.royale.repository;

import com.kinyozi.royale.model.SessionLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SessionLineRepository extends JpaRepository<SessionLine, UUID> {
}
