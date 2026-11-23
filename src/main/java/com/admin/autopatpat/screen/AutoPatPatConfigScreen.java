package com.admin.autopatpat.screen;

import com.admin.autopatpat.AutoPatPatClient;
import com.admin.autopatpat.config.AutoPatPatConfig;
import com.admin.autopatpat.config.PlayerList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public final class AutoPatPatConfigScreen extends Screen {
    private final Screen parent;
    private final AutoPatPatConfig draft;
    private boolean saveFailed;

    public AutoPatPatConfigScreen(Screen parent) {
        super(Text.translatable("config.autopatpat.title"));
        this.parent = parent;
        this.draft = AutoPatPatClient.getConfigManager().get().copy();
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(420, width - 40);
        int x = (width - contentWidth) / 2;
        int buttonWidth = (contentWidth - 8) / 2;

        addDrawableChild(ButtonWidget.builder(option("config.autopatpat.auto_allowed", draft.isAutoPatAllowed()),
                button -> {
                    draft.setAutoPatAllowed(!draft.isAutoPatAllowed());
                    button.setMessage(option("config.autopatpat.auto_allowed", draft.isAutoPatAllowed()));
                }).dimensions(x, 48, contentWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("config.autopatpat.auto_allowed.tooltip"))).build());

        addDrawableChild(ButtonWidget.builder(option("config.autopatpat.whitelist_only", draft.isWhitelistEnabled()),
                button -> {
                    draft.setWhitelistEnabled(!draft.isWhitelistEnabled());
                    button.setMessage(option("config.autopatpat.whitelist_only", draft.isWhitelistEnabled()));
                }).dimensions(x, 72, contentWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("config.autopatpat.whitelist_only.tooltip"))).build());

        addDrawableChild(ButtonWidget.builder(option("config.autopatpat.protect_friends", draft.isProtectFriends()),
                button -> {
                    draft.setProtectFriends(!draft.isProtectFriends());
                    button.setMessage(option("config.autopatpat.protect_friends", draft.isProtectFriends()));
                }).dimensions(x, 96, contentWidth, 20)
                .tooltip(Tooltip.of(Text.translatable("config.autopatpat.protect_friends.tooltip"))).build());

        int y = 128;
        for (PlayerList list : PlayerList.values()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("config.autopatpat.list_count",
                    Text.translatable(list.translationKey()), draft.getNames(list).size()),
                    button -> client.setScreen(new PlayerListScreen(this, draft, list)))
                    .dimensions(x, y, list == PlayerList.FRIENDS ? buttonWidth : contentWidth, 20)
                    .tooltip(Tooltip.of(Text.translatable(list.translationKey() + ".tooltip"))).build());
            y += 24;
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("config.autopatpat.keybindings"),
                button -> client.setScreen(new KeybindsScreen(this, client.options)))
                .dimensions(x + buttonWidth + 8, y - 24, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("config.autopatpat.save"), button -> {
            if (AutoPatPatClient.getConfigManager().save(draft)) {
                close();
            } else {
                saveFailed = true;
            }
        }).dimensions(x, height - 28, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close())
                .dimensions(x + buttonWidth + 8, height - 28, buttonWidth, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFFFF);
        Text hint = Text.translatable(saveFailed ? "config.autopatpat.save_failed" : "config.autopatpat.shortcut");
        context.drawCenteredTextWithShadow(textRenderer, hint, width / 2, 34,
                saveFailed ? 0xFFFF5555 : 0xFFAAAAAA);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private static Text option(String translationKey, boolean enabled) {
        return Text.translatable(translationKey, Text.translatable(enabled ? "options.on" : "options.off"));
    }
}
