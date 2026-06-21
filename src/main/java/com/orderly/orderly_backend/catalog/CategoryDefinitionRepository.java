package com.orderly.orderly_backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CategoryDefinitionRepository extends JpaRepository<CategoryDefinition, UUID> {

    List<CategoryDefinition> findByUserIdOrderBySortOrderAscCreatedAtAsc(UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    int countByUserId(UUID userId);
}
