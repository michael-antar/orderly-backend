package com.orderly.orderly_backend.catalog;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "category_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    /** Mapped and rendered by frontend, we just store it as given */
    private String icon;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<FieldDefinition> fieldDefinitions;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}
