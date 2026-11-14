package com.leclowndu93150.thaumaturge.api.aspect;

/**
 * Supplies per-instance aspects for an entity whose composition cannot be expressed by the static entity aspect table.
 * Implement on the entity class; it is consulted before the table and fully replaces the type-level entry.
 */
public interface IEntityAspectSource {
    AspectList entityAspects();
}
