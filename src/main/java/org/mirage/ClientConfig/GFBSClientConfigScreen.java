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

package org.mirage.ClientConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GFBS client config UI screen.
 *
 * Features:
 * - Categories (simple dropdown cycling)
 * - Scroll list of registered keys
 * - Live apply: changes are persisted and listeners are invoked immediately
 */
public class GFBSClientConfigScreen extends OptionsSubScreen {

    private static final Component TITLE = Component.literal("G.F.B.S. 客户端设置");

    private CycleButton<String> categoryButton;
    private ConfigList list;

    private String currentCategory = "general";

    public GFBSClientConfigScreen(net.minecraft.client.gui.screens.Screen lastScreen, net.minecraft.client.Options options) {
        super(lastScreen, options, TITLE);
    }

    @Override
    protected void init() {
        super.init();
        ClientConfigRegistry.loadIfNeeded();

        int w = this.width;
        int h = this.height;

        List<String> cats = ClientConfigRegistry.categories();
        if (cats.isEmpty()) cats = List.of("general");
        if (!cats.contains(currentCategory)) currentCategory = cats.get(0);

        categoryButton = CycleButton.<String>builder(s -> Component.literal("分类: " + s))
                .withValues(cats)
                .withInitialValue(currentCategory)
                .create(w / 2 - 155, 18, 310, 20, Component.empty(),
                        (btn, value) -> {
                            currentCategory = value;
                            rebuildList();
                        });

        this.addRenderableWidget(categoryButton);

        list = new ConfigList(this.minecraft, w, h, 46, h - 32, 28);
        this.addRenderableWidget(list);

        rebuildList();

        this.addRenderableWidget(Button.builder(Component.literal("完成"), (b) -> onClose())
                .bounds(w / 2 - 100, h - 27, 200, 20).build());
    }

    private void rebuildList() {
        list.clearEntriesPublic();

        List<ClientConfigKey<?>> keys = ClientConfigRegistry.allKeys().stream()
                .filter(k -> Objects.equals(k.category(), currentCategory))
                .collect(Collectors.toList());

        // stable ordering: id
        keys.sort(Comparator.comparing(ClientConfigKey::id));

        for (ClientConfigKey<?> k : keys) {
            list.addEntryPublic(new ConfigEntryRow(k));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        g.drawCenteredString(this.font, this.title, this.width / 2, 5, 0xFFFFFF);

        // tooltip: show comment when hovering a row widget
        var hovered = list.getHoveredEntryPublic();
        if (hovered != null) {
            String c = hovered.key.comment();
            if (c != null && !c.isBlank()) {
                g.renderTooltip(this.font, Component.literal(c), mouseX, mouseY);
            }
        }
    }

    // ---------------- list ----------------

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


        // Mojang mappings in 1.20.1 expose these as protected; provide public wrappers for our screen.
        public void clearEntriesPublic() {
            super.clearEntries();
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
        private final List<? extends GuiEventListener> children;
        private final List<? extends NarratableEntry> narratables;

        private final net.minecraft.client.gui.components.AbstractWidget widget;

        ConfigEntryRow(ClientConfigKey<?> key) {
            this.key = key;

            this.widget = createWidgetForKey(key);
            this.children = List.of(widget);
            this.narratables = List.of(widget);
        }

        @Override
        public void render(GuiGraphics g, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTick) {
            // label left
            g.drawString(GFBSClientConfigScreen.this.font, Component.literal(key.displayName()), x, y + 9, 0xFFFFFF, false);

            // widget right
            widget.setX(x + 170);
            widget.setY(y);
            widget.setWidth(140);
            widget.render(g, mouseX, mouseY, partialTick);
        }
        @Override
        public Component getNarration() {
            // Used by narrator / accessibility
            return Component.literal(this.key.displayName());
        }
    }

    // ---------------- widget factory ----------------

    @SuppressWarnings("unchecked")
    private net.minecraft.client.gui.components.AbstractWidget createWidgetForKey(ClientConfigKey<?> k) {
        return switch (k.type()) {
            case BOOLEAN -> {
                ClientConfigKey<Boolean> key = (ClientConfigKey<Boolean>) k;
                boolean cur = GFBSClientConfigAPI.get(key);
                yield CycleButton.onOffBuilder(cur)
                        .create(0, 0, 140, 20, Component.empty(),
                                (btn, value) -> GFBSClientConfigAPI.set(key, value));
            }
            case INT -> {
                ClientConfigKey<Integer> key = (ClientConfigKey<Integer>) k;
                int cur = GFBSClientConfigAPI.get(key);
                int min = key.min() == null ? Integer.MIN_VALUE : key.min().intValue();
                int max = key.max() == null ? Integer.MAX_VALUE : key.max().intValue();

                // use an EditBox for flexibility; clamp on commit
                EditBox box = new EditBox(this.font, 0, 0, 140, 20, Component.empty());
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

                EditBox box = new EditBox(this.font, 0, 0, 140, 20, Component.empty());
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

                EditBox box = new EditBox(this.font, 0, 0, 140, 20, Component.empty());
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

                yield CycleButton.<Enum<?>>builder(e -> Component.literal(e.name()))
                        .withValues(values)
                        .withInitialValue(cur)
                        .create(0, 0, 140, 20, Component.empty(),
                                (btn, value) -> GFBSClientConfigAPI.set((ClientConfigKey) key, value));
            }
        };
    }
}
