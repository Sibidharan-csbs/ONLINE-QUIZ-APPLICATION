package com.quizapp.entity;

/**
 * Application roles used for role-based access control (RBAC).
 * Spring Security expects authorities prefixed with "ROLE_",
 * this is handled inside CustomUserDetailsService.
 */
public enum Role {
    ADMIN,
    STUDENT
}
