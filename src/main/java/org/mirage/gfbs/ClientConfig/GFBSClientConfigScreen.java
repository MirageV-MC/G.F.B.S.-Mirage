/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.mirage.gfbs.ClientConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;

public class GFBSClientConfigScreen extends OptionsSubScreen {

    private static final Component TITLE = Component.literal("G.F.B.S. 客户端设置");
    private static final Component BACK_TO_CATEGORIES = Component.literal("← 返回分类");

    private CategoryList categoryList;
    private ConfigList configList;

    private boolean showingCategories = true;
    private String currentCategory = "";

    public GFBSClientConfigScreen(net.minecraft.client.gui.screens.Screen lastScreen, net.minecraft.client.Options options) {
        super(lastScreen, options, TITLE);
    }

    @Override
    protected void init() {
        super.init();
        ClientConfigRegistry.loadIfNeeded();

        int w = this.width;
        int h = this.height;

        this.clearWidgets();

        if (showingCategories) {
            showCategorySelection();
        } else {
            showConfigPage();
        }
    }

    private void showCategorySelection() {
        int w = this.width;
        int h = this.height;

        categoryList = new CategoryList(this.minecraft, w, h, 32, h - 32, 36);
        this.addRenderableWidget(categoryList);

        List<String> categories = ClientConfigRegistry.categories();
        if (categories.isEmpty()) categories = List.of("general");

        for (String category : categories) {
            categoryList.addEntryPublic(new CategoryEntry(category));
        }

        this.addRenderableWidget(Button.builder(Component.literal("完成"), (b) -> onClose())
                .bounds(w / 2 - 100, h - 27, 200, 20).build());
    }

    private void showConfigPage() {
        int w = this.width;
        int h = this.height;

        this.addRenderableWidget(Button.builder(BACK_TO_CATEGORIES, (b) -> {
                    showingCategories = true;
                    rebuildWidgets();
                })
                .bounds(10, 10, 100, 20)
                .build());

        this.addRenderableWidget(Button.builder(TITLE, (b) -> {})
                .bounds(w / 2 - 75, 4, 150, 20)
                .build());

        configList = new ConfigList(this.minecraft, w, h, 40, h - 32, 28);
        this.addRenderableWidget(configList);

        List<ClientConfigKey<?>> keys = ClientConfigRegistry.allKeys().stream()
                .filter(k -> Objects.equals(k.category(), currentCategory))
                .sorted(Comparator.comparing(ClientConfigKey::id))
                .collect(Collectors.toList());

        for (ClientConfigKey<?> key : keys) {
            configList.addEntryPublic(new ConfigEntryRow(key));
        }

        this.addRenderableWidget(Button.builder(Component.literal("完成"), (b) -> onClose())
                .bounds(w / 2 - 100, h - 27, 200, 20).build());
    }

    protected void rebuildWidgets() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        String copyright = "GFBS - 全局F.A.A.S广播系统";
        g.drawString(this.font, copyright, 10, this.height - 10, 0xCCCCCC);

        if (showingCategories) {
            g.drawCenteredString(this.font, TITLE, this.width / 2, 10, 0xFFFFFF);
            g.drawCenteredString(this.font, Component.literal("选择配置分类"), this.width / 2, 22, 0xCCCCCC);
        } else {
            g.drawCenteredString(this.font, Component.literal(currentCategory + " 配置"), this.width / 2, 22, 0xCCCCCC);
        }

        if (showingCategories) {
            var hoveredCategory = categoryList.getHoveredEntryPublic();
            if (hoveredCategory != null) {
                String desc = getCategoryDescription(hoveredCategory.category);
                if (desc != null && !desc.isBlank()) {
                    g.renderTooltip(this.font, Component.literal(desc), mouseX, mouseY);
                }
            }
        } else {
            var hoveredConfig = configList.getHoveredEntryPublic();
            if (hoveredConfig != null) {
                String comment = hoveredConfig.key.comment();
                if (comment != null && !comment.isBlank()) {
                    g.renderTooltip(this.font, Component.literal(comment), mouseX, mouseY);
                }
            }
        }
    }

    private String getCategoryDescription(String category) {
        return category + " 相关配置";
    }

    private final class CategoryList extends ObjectSelectionList<CategoryEntry> {
        CategoryList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 145;
        }

        public void addEntryPublic(CategoryEntry entry) {
            super.addEntry(entry);
        }

        public CategoryEntry getHoveredEntryPublic() {
            return super.getHovered();
        }
    }

    private final class CategoryEntry extends ObjectSelectionList.Entry<CategoryEntry> {
        final String category;
        private final Button button;

        CategoryEntry(String category) {
            this.category = category;

            long configCount = ClientConfigRegistry.allKeys().stream()
                    .filter(k -> Objects.equals(k.category(), category))
                    .count();

            Component buttonText = Component.literal(category + " (" + configCount + ")");

            this.button = Button.builder(buttonText, b -> {
                        showingCategories = false;
                        currentCategory = category;
                        rebuildWidgets();
                    })
                    .bounds(0, 0, 280, 30)
                    .build();
        }

        @Override
        public void render(GuiGraphics g, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTick) {
            button.setX(x + 10);
            button.setY(y);
            button.setWidth(rowWidth - 20);
            button.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.button.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return Component.literal("分类: " + category);
        }
    }

    private final class ConfigList extends ObjectSelectionList<ConfigEntryRow> {
        ConfigList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return 310;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 155;
        }

        public void addEntryPublic(ConfigEntryRow entry) {
            super.addEntry(entry);
        }

        public ConfigEntryRow getHoveredEntryPublic() {
            return super.getHovered();
        }
    }

    private final class ConfigEntryRow extends ObjectSelectionList.Entry<ConfigEntryRow> {
        final ClientConfigKey<?> key;
        private final net.minecraft.client.gui.components.AbstractWidget widget;

        ConfigEntryRow(ClientConfigKey<?> key) {
            this.key = key;
            this.widget = createWidgetForKey(key);
        }

        @Override
        public void render(GuiGraphics g, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTick) {
            g.drawString(GFBSClientConfigScreen.this.font, Component.literal(key.displayName()), x + 10, y + 9, 0xFFFFFF, false);

            widget.setX(x + rowWidth - 150);
            widget.setY(y);
            widget.setWidth(140);
            widget.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            GFBSClientConfigScreen.this.setFocused(this);
            if (this.widget.mouseClicked(mouseX, mouseY, button)) {
                GFBSClientConfigScreen.this.setFocused(this.widget);
                return false;
            }
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.key.displayName());
        }
    }

    @SuppressWarnings("unchecked")
    private net.minecraft.client.gui.components.AbstractWidget createWidgetForKey(ClientConfigKey<?> k) {
        return switch (k.type()) {
            case BOOLEAN -> {
                ClientConfigKey<Boolean> key = (ClientConfigKey<Boolean>) k;
                boolean cur = GFBSClientConfigAPI.get(key);
                yield net.minecraft.client.gui.components.CycleButton.onOffBuilder(cur)
                        .create(0, 0, 140, 20, Component.empty(),
                                (btn, value) -> GFBSClientConfigAPI.set(key, value));
            }
            case INT -> {
                ClientConfigKey<Integer> key = (ClientConfigKey<Integer>) k;
                int cur = GFBSClientConfigAPI.get(key);
                int min = key.min() == null ? Integer.MIN_VALUE : key.min().intValue();
                int max = key.max() == null ? Integer.MAX_VALUE : key.max().intValue();

                net.minecraft.client.gui.components.EditBox box = new net.minecraft.client.gui.components.EditBox(
                        this.font, 0, 0, 140, 20, Component.empty());
                box.setValue(String.valueOf(cur));
                box.setResponder(text -> {
                    try {
                        int v = Integer.parseInt(text.trim());
                        if (v < min) v = min;
                        if (v > max) v = max;
                        GFBSClientConfigAPI.set(key, v);
                    } catch (Exception ignored) {
                    }
                });
                yield box;
            }
            case DOUBLE -> {
                ClientConfigKey<Double> key = (ClientConfigKey<Double>) k;
                double cur = GFBSClientConfigAPI.get(key);
                double min = key.min() == null ? -Double.MAX_VALUE : key.min();
                double max = key.max() == null ? Double.MAX_VALUE : key.max();

                net.minecraft.client.gui.components.EditBox box = new net.minecraft.client.gui.components.EditBox(
                        this.font, 0, 0, 140, 20, Component.empty());
                box.setValue(String.valueOf(cur));
                box.setResponder(text -> {
                    try {
                        double v = Double.parseDouble(text.trim());
                        if (v < min) v = min;
                        if (v > max) v = max;
                        GFBSClientConfigAPI.set(key, v);
                    } catch (Exception ignored) {
                    }
                });
                yield box;
            }
            case STRING -> {
                ClientConfigKey<String> key = (ClientConfigKey<String>) k;
                String cur = GFBSClientConfigAPI.get(key);
                int maxLen = key.max() == null ? 256 : key.max().intValue();

                net.minecraft.client.gui.components.EditBox box = new net.minecraft.client.gui.components.EditBox(
                        this.font, 0, 0, 140, 20, Component.empty());
                box.setMaxLength(maxLen);
                box.setValue(cur);
                box.setResponder(text -> GFBSClientConfigAPI.set(key, text));
                yield box;
            }
            case ENUM -> {
                ClientConfigKey<Enum<?>> key = (ClientConfigKey<Enum<?>>) k;
                Enum<?> cur = (Enum<?>) GFBSClientConfigAPI.get((ClientConfigKey) key);
                Class<? extends Enum<?>> cls = k.enumClass();

                List<Enum<?>> values = cls == null ? List.of() : List.of(cls.getEnumConstants());
                if (values.isEmpty()) {
                    yield Button.builder(Component.literal("N/A"), b -> {}).bounds(0, 0, 140, 20).build();
                }

                yield net.minecraft.client.gui.components.CycleButton.<Enum<?>>builder(e -> Component.literal(e.name()))
                        .withValues(values)
                        .withInitialValue(cur)
                        .create(0, 0, 140, 20, Component.empty(),
                                (btn, value) -> GFBSClientConfigAPI.set((ClientConfigKey) key, value));
            }
        };
    }
}