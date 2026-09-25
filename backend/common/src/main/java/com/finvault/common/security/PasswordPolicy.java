package com.finvault.common.security;

import java.util.regex.Pattern;

public final class PasswordPolicy {
    private static final Pattern POLICY = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#-]).{8,}$");

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && POLICY.matcher(password).matches();
    }

    public static String message() {
        return "Password must be at least 8 characters and include uppercase, lowercase, number, and special character.";
    }
}

