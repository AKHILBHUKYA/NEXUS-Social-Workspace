package com.akhil.social.enums;

public enum Platform {
    WHATSAPP, INSTAGRAM, FACEBOOK, X, REELS, GENERAL;

    public static Platform from(String value) {
        if (value == null || value.isBlank()) return GENERAL;
        return Platform.valueOf(value.trim().toUpperCase());
    }
}
