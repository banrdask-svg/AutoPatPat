package com.admin.autopatpat.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class AutoPatPatConfig {
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private boolean autoPatAllowed = true;
    private boolean whitelistEnabled;
    private boolean protectFriends = true;
    private Set<String> whitelist = new LinkedHashSet<>();
    private Set<String> blacklist = new LinkedHashSet<>();
    private Set<String> friends = new LinkedHashSet<>();

    public boolean isAutoPatAllowed() {
        return autoPatAllowed;
    }

    public void setAutoPatAllowed(boolean autoPatAllowed) {
        this.autoPatAllowed = autoPatAllowed;
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public void setWhitelistEnabled(boolean whitelistEnabled) {
        this.whitelistEnabled = whitelistEnabled;
    }

    public boolean isProtectFriends() {
        return protectFriends;
    }

    public void setProtectFriends(boolean protectFriends) {
        this.protectFriends = protectFriends;
    }

    public boolean mayAutoPat(String nickname) {
        if (!isValidNickname(nickname)) {
            return false;
        }
        String normalized = normalizeNickname(nickname);
        return autoPatAllowed && !blacklist.contains(normalized)
                && (!whitelistEnabled || whitelist.contains(normalized));
    }

    public boolean isProtectedFriend(String nickname) {
        return protectFriends && isValidNickname(nickname) && friends.contains(normalizeNickname(nickname));
    }

    public Set<String> getNames(PlayerList list) {
        return Collections.unmodifiableSet(names(list));
    }

    public boolean addName(PlayerList list, String nickname) {
        return names(list).add(normalizeNickname(nickname));
    }

    public boolean removeName(PlayerList list, String nickname) {
        return names(list).remove(normalizeNickname(nickname));
    }

    public AutoPatPatConfig copy() {
        AutoPatPatConfig copy = new AutoPatPatConfig();
        copy.autoPatAllowed = autoPatAllowed;
        copy.whitelistEnabled = whitelistEnabled;
        copy.protectFriends = protectFriends;
        copy.whitelist = new LinkedHashSet<>(whitelist);
        copy.blacklist = new LinkedHashSet<>(blacklist);
        copy.friends = new LinkedHashSet<>(friends);
        return copy;
    }

    public void normalize() {
        whitelist = normalizedNames(whitelist);
        blacklist = normalizedNames(blacklist);
        friends = normalizedNames(friends);
    }

    public static boolean isValidNickname(String nickname) {
        return nickname != null && NICKNAME_PATTERN.matcher(nickname.strip()).matches();
    }

    private Set<String> names(PlayerList list) {
        return switch (list) {
            case WHITELIST -> whitelist;
            case BLACKLIST -> blacklist;
            case FRIENDS -> friends;
        };
    }

    private static String normalizeNickname(String nickname) {
        if (!isValidNickname(nickname)) {
            throw new IllegalArgumentException("Invalid Minecraft nickname: " + nickname);
        }
        return nickname.strip().toLowerCase(Locale.ROOT);
    }

    private static Set<String> normalizedNames(Set<String> names) {
        Set<String> normalized = new LinkedHashSet<>();
        if (names != null) {
            for (String nickname : names) {
                normalized.add(normalizeNickname(nickname));
            }
        }
        return normalized;
    }
}
