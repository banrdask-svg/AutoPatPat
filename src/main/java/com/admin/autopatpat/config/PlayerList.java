package com.admin.autopatpat.config;

public enum PlayerList {
    WHITELIST("whitelist"),
    BLACKLIST("blacklist"),
    FRIENDS("friends");

    private final String id;

    PlayerList(String id) {
        this.id = id;
    }

    public String translationKey() {
        return "config.autopatpat." + id;
    }
}
