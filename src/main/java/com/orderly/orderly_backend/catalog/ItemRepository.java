package com.orderly.orderly_backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {

    boolean existsByIdAndUserId(UUID id, UUID userId);

    Optional<Item> findByIdAndUserId(UUID id, UUID userId);
}
