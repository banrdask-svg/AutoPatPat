package com.admin.autopatpat.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class AutoPatPatConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoPatPatConfigTest.class);
    private static int checks;

    @AfterAll
    static void reportChecks() {
        LOGGER.info("Passed {} Auto PatPat configuration checks", checks);
    }

    @Test
    void verifyPermissions() {
        AutoPatPatConfig config = new AutoPatPatConfig();
        check(config.mayAutoPat("Alex"), "Default settings preserve auto-patting of other players");
        check(!config.isProtectedFriend("Alex"), "Empty friends list does not block attacks");
        config.setWhitelistEnabled(true);
        check(!config.mayAutoPat("Alex"), "An enabled empty whitelist denies everyone");
        config.addName(PlayerList.WHITELIST, "Alex");
        check(config.mayAutoPat("aLEX"), "Whitelisted names ignore case");
        check(!config.mayAutoPat("Steve"), "Unlisted names are denied in whitelist mode");
        config.addName(PlayerList.BLACKLIST, "ALEX");
        check(!config.mayAutoPat("Alex"), "Blacklist takes priority over whitelist");
        config.setWhitelistEnabled(false);
        check(!config.mayAutoPat("Alex"), "Blacklist also applies when whitelist mode is disabled");
        check(config.mayAutoPat("Steve"), "Other players remain allowed outside whitelist mode");
        config.removeName(PlayerList.BLACKLIST, "alex");
        check(config.mayAutoPat("Alex"), "Removing a blacklist entry restores access");
        config.setAutoPatAllowed(false);
        check(!config.mayAutoPat("Alex") && !config.mayAutoPat("Steve"), "Master setting disables all auto-patting");
        config.addName(PlayerList.FRIENDS, "Steve");
        check(config.isProtectedFriend("sTEVE"), "Friends are protected independently of auto-patting");
        check(!config.isProtectedFriend("Alex"), "Other players are not protected");
        config.setProtectFriends(false);
        check(!config.isProtectedFriend("Steve"), "Protection can be disabled");
        config.setProtectFriends(true);
        config.removeName(PlayerList.FRIENDS, "STEVE");
        check(!config.isProtectedFriend("Steve"), "Removing a friend restores attacks");
        check(!config.mayAutoPat(null) && !config.mayAutoPat("bad name"), "Invalid target names do not crash auto-patting");
        check(!config.isProtectedFriend(null), "Invalid target names do not crash attack handling");
    }

    @Test
    void verifyNamesAndCopies() {
        AutoPatPatConfig config = new AutoPatPatConfig();
        check(config.addName(PlayerList.WHITELIST, "  Player_1  "), "Nickname whitespace is trimmed");
        check(!config.addName(PlayerList.WHITELIST, "PLAYER_1"), "Case-only duplicates are rejected");
        check(config.getNames(PlayerList.WHITELIST).size() == 1, "Duplicate insertion leaves one entry");
        check(!config.removeName(PlayerList.WHITELIST, "Nobody"), "Removing an absent name is harmless");
        for (String invalid : new String[]{"", "  ", "two names", "a/b", "Name!", "abcdefghijklmnopq"}) {
            check(!AutoPatPatConfig.isValidNickname(invalid), "Invalid nickname is rejected: " + invalid);
            try {
                config.addName(PlayerList.FRIENDS, invalid);
                throw new AssertionError("Invalid nickname was accepted");
            } catch (IllegalArgumentException expected) {
                check(config.getNames(PlayerList.FRIENDS).isEmpty(), "Invalid insertion does not change the list");
            }
        }
        check(AutoPatPatConfig.isValidNickname("abcdefghijklmnop"), "Sixteen-character names are accepted");
        AutoPatPatConfig copy = config.copy();
        copy.removeName(PlayerList.WHITELIST, "player_1");
        copy.addName(PlayerList.FRIENDS, "Friend");
        copy.setAutoPatAllowed(false);
        check(config.getNames(PlayerList.WHITELIST).contains("player_1"), "Editing a draft leaves the original list unchanged");
        check(!config.isProtectedFriend("Friend") && config.isAutoPatAllowed(), "Editing a draft leaves original settings unchanged");
        try {
            config.getNames(PlayerList.WHITELIST).clear();
            throw new AssertionError("Exposed list was mutable");
        } catch (UnsupportedOperationException expected) {
            check(config.getNames(PlayerList.WHITELIST).size() == 1, "Lists cannot be changed through their public view");
        }
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            config.addName(PlayerList.FRIENDS, "INDIGO");
            check(config.isProtectedFriend("indigo"), "Name matching is independent of system locale");
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void verifySerialization() {
        Gson gson = new Gson();
        AutoPatPatConfig legacy = gson.fromJson("{}", AutoPatPatConfig.class);
        legacy.normalize();
        check(legacy.isAutoPatAllowed() && legacy.isProtectFriends() && !legacy.isWhitelistEnabled(),
                "Missing settings use backwards-compatible defaults");
        AutoPatPatConfig loaded = gson.fromJson(
                "{\"whitelistEnabled\":true,\"whitelist\":[\"Alex\",\"ALEX\"],"
                        + "\"blacklist\":[\"Steve\"],\"friends\":[\" Player_1 \"]}", AutoPatPatConfig.class);
        loaded.normalize();
        check(loaded.getNames(PlayerList.WHITELIST).size() == 1 && loaded.mayAutoPat("aLEX"),
                "Loaded entries are normalized and deduplicated");
        check(!loaded.mayAutoPat("Steve") && loaded.isProtectedFriend("player_1"), "All loaded lists apply");
        AutoPatPatConfig roundTrip = gson.fromJson(gson.toJson(loaded), AutoPatPatConfig.class);
        roundTrip.normalize();
        for (PlayerList list : PlayerList.values()) {
            check(roundTrip.getNames(list).equals(loaded.getNames(list)), "JSON round trip preserves " + list);
        }
        AutoPatPatConfig nullLists = gson.fromJson("{\"whitelist\":null,\"blacklist\":null,\"friends\":null}",
                AutoPatPatConfig.class);
        nullLists.normalize();
        check(nullLists.mayAutoPat("Alex") && !nullLists.isProtectedFriend("Alex"), "Null lists normalize to empty lists");
    }

    @Test
    void verifyPersistence() throws Exception {
        Path directory = Files.createTempDirectory("autopatpat-config-tests-");
        Path path = directory.resolve("autopatpat.json");
        Path occupied = directory.resolve("occupied");
        try {
            AutoPatPatConfigManager manager = new AutoPatPatConfigManager(path);
            manager.load();
            check(!Files.exists(path) && manager.get().mayAutoPat("Alex"), "Missing config uses defaults without creating a file");
            AutoPatPatConfig draft = manager.get().copy();
            draft.setWhitelistEnabled(true);
            draft.addName(PlayerList.WHITELIST, "Alex");
            draft.addName(PlayerList.BLACKLIST, "Steve");
            draft.addName(PlayerList.FRIENDS, "Friend");
            check(manager.save(draft), "Configuration saves successfully");
            draft.removeName(PlayerList.FRIENDS, "Friend");
            check(manager.get().isProtectedFriend("Friend"), "Saved settings are isolated from the screen draft");
            AutoPatPatConfigManager reloaded = new AutoPatPatConfigManager(path);
            reloaded.load();
            check(reloaded.get().isWhitelistEnabled() && reloaded.get().mayAutoPat("Alex"), "Whitelist survives restart");
            check(!reloaded.get().mayAutoPat("Steve") && reloaded.get().isProtectedFriend("FRIEND"),
                    "Blacklist and friends survive restart");
            try (var files = Files.list(directory)) {
                check(files.noneMatch(file -> file.getFileName().toString().endsWith(".tmp")), "Saving leaves no temporary files");
            }
            Files.writeString(path, "{");
            reloaded.load();
            check(reloaded.get().isProtectedFriend("Friend") && Files.readString(path).equals("{"),
                    "Malformed files preserve current settings and are not silently overwritten");
            Files.writeString(occupied, "occupied");
            AutoPatPatConfigManager unwritable = new AutoPatPatConfigManager(occupied.resolve("autopatpat.json"));
            check(!unwritable.save(manager.get()), "A save failure is reported to the screen");
            check(!unwritable.get().isProtectedFriend("Friend") && Files.readString(occupied).equals("occupied"),
                    "A save failure does not apply the draft or overwrite unrelated files");
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(occupied);
            Files.delete(directory);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
