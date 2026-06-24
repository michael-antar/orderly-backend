package com.orderly.orderly_backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface CategoryDefinitionRepository extends JpaRepository<CategoryDefinition, UUID> {

    List<CategoryDefinition> findByUserIdOrderBySortOrderAscCreatedAtAsc(UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    int countByUserId(UUID userId);

    @Query("SELECT COALESCE(MAX(c.sortOrder), -1) FROM CategoryDefinition c WHERE c.userId = :userId")
    int findMaxSortOrderByUserId(@Param("userId") UUID userId);
}
