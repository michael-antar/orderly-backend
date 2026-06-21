package com.orderly.orderly_backend.catalog;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "item_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemTag {

    @EmbeddedId
    private ItemTagId id;
}
