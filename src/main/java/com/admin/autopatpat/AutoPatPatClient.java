package com.admin.autopatpat;

import com.admin.autopatpat.config.AutoPatPatConfig;
import com.admin.autopatpat.config.AutoPatPatConfigManager;
import com.admin.autopatpat.friend.FriendProtection;
import com.admin.autopatpat.screen.AutoPatPatConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.lopymine.patpat.client.config.PatPatClientConfig;
import net.lopymine.patpat.client.config.resourcepack.PlayerConfig;
import net.lopymine.patpat.client.manager.PatPatClientManager;
import net.lopymine.patpat.client.render.PatPatClientRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class AutoPatPatClient implements ClientModInitializer {
    private static final String MOD_ID = "autopatpat";
    // Matches the manual pat cooldown in PatPat 1.3.1.
    private static final int PAT_COOLDOWN_TICKS = 4;
    private static final AutoPatPatConfigManager CONFIG_MANAGER = new AutoPatPatConfigManager(
            FabricLoader.getInstance().getConfigDir().resolve("autopatpat.json"));

    private KeyBinding toggleKey;
    private KeyBinding settingsKey;
    private boolean enabled;
    private boolean toggleWasPressed;
    private boolean settingsWasPressed;

    @Override
    public void onInitializeClient() {
        CONFIG_MANAGER.load();
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "general"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autopatpat.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, category));
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autopatpat.open_settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH, category));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        FriendProtection.register();
    }

    public static AutoPatPatConfigManager getConfigManager() {
        return CONFIG_MANAGER;
    }

    private void tick(MinecraftClient client) {
        boolean togglePressed = toggleKey.isPressed();
        boolean freshPress = togglePressed && !toggleWasPressed;
        toggleWasPressed = togglePressed;
        boolean settingsPressed = settingsKey.isPressed();
        boolean freshSettingsPress = settingsPressed && !settingsWasPressed;
        settingsWasPressed = settingsPressed;

        if (client.player == null || client.world == null || !client.player.isAlive()) {
            enabled = false;
            return;
        }
        if (client.currentScreen != null || !client.isWindowFocused() || client.isPaused()) {
            return;
        }
        if (freshSettingsPress) {
            client.setScreen(new AutoPatPatConfigScreen(null));
            return;
        }

        boolean shiftPressed = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        AutoPatPatConfig config = CONFIG_MANAGER.get();
        if (!config.isAutoPatAllowed()) {
            enabled = false;
            if (freshPress && shiftPressed) {
                client.player.sendMessage(Text.translatable("message.autopatpat.settings_disabled"), true);
            }
            return;
        }
        if (freshPress && shiftPressed) {
            enabled = !enabled;
            client.player.sendMessage(Text.translatable(
                    enabled ? "message.autopatpat.enabled" : "message.autopatpat.disabled"), true);
        }

        if (!enabled || client.player.isSpectator() || client.world.getTickManager().isFrozen()
                || !PatPatClientConfig.getInstance().getMainConfig().isModEnabled()
                || PatPatClientManager.getPatCooldown() > 0) {
            return;
        }

        LivingEntity target = PatPatClientManager.getPatEntityFromHitResult();
        if (!(target instanceof PlayerEntity targetPlayer) || targetPlayer == client.player || !targetPlayer.isAlive()) {
            return;
        }
        if (!config.mayAutoPat(targetPlayer.getGameProfile().name())) {
            return;
        }

        PatPatClientRenderer.registerClientPacket(new PatPatClientRenderer.PacketPat(
                target, PlayerConfig.currentSession(), client.player, false));
        PatPatClientManager.setPatCooldown(PAT_COOLDOWN_TICKS);
    }
}
