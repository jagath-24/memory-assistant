package com.memoryassistant.repository;

import com.memoryassistant.entity.FactCategory;
import com.memoryassistant.entity.MemoryFact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemoryFactRepository extends JpaRepository<MemoryFact, Long> {

    List<MemoryFact> findAllByOrderByUpdatedAtDesc();

    List<MemoryFact> findByCategoryOrderByUpdatedAtDesc(FactCategory category);

    Optional<MemoryFact> findFirstByCategoryAndContentIgnoreCase(FactCategory category, String content);
}
