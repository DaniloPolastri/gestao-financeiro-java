package com.findash.repository;

import com.findash.entity.Recurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RecurrenceRepository extends JpaRepository<Recurrence, UUID> {
}
