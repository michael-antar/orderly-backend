package com.orderly.orderly_backend.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ItemTagId implements Serializable {

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;
}
