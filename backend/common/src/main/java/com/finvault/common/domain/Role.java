package com.finvault.common.domain;

public enum Role {
    USER,
    ADMIN,
    SUPER_ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}

