package com.kaptaitourist.kaptaitourist.user.domain;

public enum Gender {
    MALE,
    FEMALE;

    public static boolean isValid(String value) {
        if (value == null) return false;
        for (Gender g : values()) {
            if (g.name().equals(value)) return true;
        }
        return false;
    }
}
