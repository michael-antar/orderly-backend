package com.orderly.orderly_backend.catalog;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private UUID categoryDefId;

    /**
     * Free-form property values keyed by {@link FieldDefinition#key()} values from the parent category.
     * Stored as JSONB. The server does not validate keys or value types against the category's
     * field definitions — that is a client-side concern.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> properties;

    /** Null when status is {@code BACKLOG}. Assigned 1000.0 on creation or promotion to {@code RANKED}. */
    private BigDecimal rating;

    /**
     * Glicko-1 ratings deviation — measures uncertainty in the rating.
     * 350 = maximum uncertainty (new or reset item). Approaches ~30 after many comparisons.
     * Never written directly by the client; updated server-side on each comparison.
     */
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal rd = new BigDecimal("350");

    @Column(nullable = false)
    @Builder.Default
    private int comparisonCount = 0;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    private Instant lastComparedAt;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "item_status", nullable = false)
    private ItemStatus status;

    @Column(nullable = false)
    private UUID userId;
}
