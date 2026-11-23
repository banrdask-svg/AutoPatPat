package com.admin.autopatpat.screen;

import com.admin.autopatpat.config.AutoPatPatConfig;
import com.admin.autopatpat.config.PlayerList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class PlayerListScreen extends Screen {
    private final Screen parent;
    private final AutoPatPatConfig draft;
    private final PlayerList list;
    private TextFieldWidget nicknameField;
    private ButtonWidget addButton;
    private List<String> names = List.of();
    private int page;
    private int rowsPerPage;
    private boolean duplicate;

    public PlayerListScreen(Screen parent, AutoPatPatConfig draft, PlayerList list) {
        super(Text.translatable(list.translationKey()));
        this.parent = parent;
        this.draft = draft;
        this.list = list;
    }

    @Override
    protected void init() {
        String input = nicknameField == null ? "" : nicknameField.getText();
        int contentWidth = Math.min(420, width - 40);
        int x = (width - contentWidth) / 2;
        names = List.copyOf(draft.getNames(list));
        rowsPerPage = Math.max(1, (height - 168) / 24);
        int pages = Math.max(1, (names.size() + rowsPerPage - 1) / rowsPerPage);
        page = Math.min(page, pages - 1);

        nicknameField = addDrawableChild(new TextFieldWidget(textRenderer, x, 44, contentWidth - 100, 20,
                Text.translatable("config.autopatpat.nickname")));
        nicknameField.setMaxLength(32);
        nicknameField.setPlaceholder(Text.translatable("config.autopatpat.nickname"));
        nicknameField.setText(input);

        addButton = addDrawableChild(ButtonWidget.builder(Text.translatable("config.autopatpat.add"),
                button -> addNickname()).dimensions(x + contentWidth - 92, 44, 92, 20).build());
        addButton.active = AutoPatPatConfig.isValidNickname(input);
        nicknameField.setChangedListener(value -> {
            addButton.active = AutoPatPatConfig.isValidNickname(value);
            duplicate = false;
        });

        int start = page * rowsPerPage;
        for (int row = 0; row < rowsPerPage && start + row < names.size(); row++) {
            String nickname = names.get(start + row);
            addDrawableChild(ButtonWidget.builder(Text.translatable("config.autopatpat.remove"), button -> {
                draft.removeName(list, nickname);
                duplicate = false;
                clearAndInit();
            }).dimensions(x + contentWidth - 92, 96 + row * 24, 92, 20).build());
        }

        ButtonWidget previous = addDrawableChild(ButtonWidget.builder(Text.translatable("config.autopatpat.previous"),
                button -> {
                    page--;
                    clearAndInit();
                }).dimensions(x, height - 64, 92, 20).build());
        previous.active = page > 0;
        ButtonWidget next = addDrawableChild(ButtonWidget.builder(Text.translatable("config.autopatpat.next"),
                button -> {
                    page++;
                    clearAndInit();
                }).dimensions(x + contentWidth - 92, height - 64, 92, 20).build());
        next.active = page + 1 < pages;

        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(x, height - 28, contentWidth, 20).build());
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(nicknameField);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (nicknameField.isFocused() && addButton.active
                && (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            addNickname();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable(duplicate ? "config.autopatpat.duplicate" : "config.autopatpat.nickname_hint"),
                width / 2, 74, duplicate ? 0xFFFF5555 : 0xFFAAAAAA);

        int x = (width - Math.min(420, width - 40)) / 2;
        int start = page * rowsPerPage;
        for (int row = 0; row < rowsPerPage && start + row < names.size(); row++) {
            context.drawTextWithShadow(textRenderer, names.get(start + row), x + 8, 102 + row * 24, 0xFFFFFFFF);
        }
        if (names.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.autopatpat.empty"),
                    width / 2, 102, 0xFFAAAAAA);
        }
        int pages = Math.max(1, (names.size() + rowsPerPage - 1) / rowsPerPage);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("config.autopatpat.page", page + 1, pages),
                width / 2, height - 58, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private void addNickname() {
        if (!addButton.active) {
            return;
        }
        if (!draft.addName(list, nicknameField.getText())) {
            duplicate = true;
            return;
        }
        nicknameField.setText("");
        page = (draft.getNames(list).size() - 1) / rowsPerPage;
        clearAndInit();
    }
}
