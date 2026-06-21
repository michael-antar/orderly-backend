package com.orderly.orderly_backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByCategoryDefIdOrderByNameAsc(UUID categoryDefId);

    Optional<Tag> findByIdAndUserId(Long id, UUID userId);

    Optional<Tag> findByCategoryDefIdAndNameIgnoreCase(UUID categoryDefId, String name);

    boolean existsByIdAndUserId(Long id, UUID userId);
}
