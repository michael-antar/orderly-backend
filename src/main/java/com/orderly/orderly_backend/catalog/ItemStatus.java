package com.orderly.orderly_backend.catalog;

/**
 * Maps to the {@code item_status} Postgres enum ({@code 'ranked'} | {@code 'backlog'}).
 *
 * <p>Persisted via {@code @Enumerated(EnumType.STRING)} with
 * {@code @Column(columnDefinition = "item_status")} to prevent Hibernate from attempting
 * to create its own enum type and to use the existing Postgres enum directly.
 *
 * <p>Status transitions on {@code PUT /items/{id}} have rating side-effects:
 * <ul>
 *   <li>{@code BACKLOG → RANKED}: rating reset to 1000.0, rd reset to 350.0, comparisonCount reset to 0</li>
 *   <li>{@code RANKED → BACKLOG}: rating set to null</li>
 *   <li>Same status: no rating change</li>
 * </ul>
 */
public enum ItemStatus {
    RANKED,
    BACKLOG
}
