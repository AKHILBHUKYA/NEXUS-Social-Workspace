package com.akhil.social.enums;

public enum MediaType {
    IMAGE, VIDEO, AUDIO, NONE;

    public static MediaType from(String value) {
        if (value == null || value.isBlank()) return NONE;
        return MediaType.valueOf(value.trim().toUpperCase());
    }
}
