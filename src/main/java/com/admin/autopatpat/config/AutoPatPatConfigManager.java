package com.admin.autopatpat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AutoPatPatConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("autopatpat");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;
    private AutoPatPatConfig current = new AutoPatPatConfig();

    public AutoPatPatConfigManager(Path configPath) {
        this.configPath = configPath.toAbsolutePath();
    }

    public AutoPatPatConfig get() {
        return current;
    }

    public void load() {
        if (!Files.exists(configPath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            AutoPatPatConfig loaded = GSON.fromJson(reader, AutoPatPatConfig.class);
            if (loaded == null) {
                throw new JsonParseException("Configuration must be a JSON object");
            }
            loaded.normalize();
            current = loaded;
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            LOGGER.error("Could not load Auto PatPat configuration from {}", configPath, exception);
        }
    }

    public boolean save(AutoPatPatConfig config) {
        AutoPatPatConfig saved = config.copy();
        saved.normalize();
        try {
            Files.createDirectories(configPath.getParent());
            Path temporary = Files.createTempFile(configPath.getParent(), "autopatpat-", ".tmp");
            try {
                Files.writeString(temporary, GSON.toJson(saved), StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, configPath, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    LOGGER.debug("Atomic configuration replacement is unavailable for {}", configPath);
                    Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            current = saved;
            return true;
        } catch (IOException exception) {
            LOGGER.error("Could not save Auto PatPat configuration to {}", configPath, exception);
            return false;
        }
    }
}
