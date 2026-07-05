package com.myledger.entity;

/**
 * Application roles. Stored as the string value (matching the DB CHECK constraint
 * and Spring Security authority naming, which expects the {@code ROLE_} prefix).
 */
public enum Role {
    ROLE_OWNER,
    ROLE_CONTRACTOR
}
