package com.orderly.orderly_backend.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    /**
     * Integer identity column — {@code Long}, not {@code UUID}.
     * The schema uses {@code integer GENERATED ALWAYS AS IDENTITY}, which is an intentional
     * design choice. Mixing this up with UUID generation causes JPA mapping errors.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private UUID categoryDefId;

    @Column(nullable = false)
    private UUID userId;
}
