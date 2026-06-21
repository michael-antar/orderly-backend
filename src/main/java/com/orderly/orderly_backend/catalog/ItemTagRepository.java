package com.orderly.orderly_backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface ItemTagRepository extends JpaRepository<ItemTag, ItemTagId> {

    List<ItemTag> findByIdItemId(UUID itemId);

    void deleteByIdItemId(UUID itemId);

    void deleteByIdTagId(Long tagId);

    /** Used during tag merge to find all items linked to the source tag. */
    @Query("SELECT it.id.itemId FROM ItemTag it WHERE it.id.tagId = :tagId")
    List<UUID> findItemIdsByTagId(@Param("tagId") Long tagId);

    /** Usage count for a tag — drives TagDetailDto.usageCount and unused-tag filtering. */
    long countByIdTagId(Long tagId);

    /** Used during tag merge to check if an item is already linked to the target tag. */
    boolean existsByIdItemIdAndIdTagId(UUID itemId, Long tagId);
}
