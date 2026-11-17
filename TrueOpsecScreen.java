package com.trueopsec;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrueOpsecScreen extends Screen {
    private static final int ANIM_MS = 520;   // main button glide
    private static final int STAGGER_MS = 90; // delay between option rows appearing
    private static final int FADE_MS = 220;
    private static final int W = 220;

    private final Screen parent;
    private Button main;
    private long clickedAt = -1;
    private boolean boardOpen = false;

    private Button invisBtn, boardBtn, boardToggle;
    private EditBox nameBox, bName, bMoney, bKills, bShards;

    private final List<AbstractWidget> options = new ArrayList<>();
    private final List<AbstractWidget> boardWidgets = new ArrayList<>();
    private final Map<AbstractWidget, Integer> order = new HashMap<>();

    public TrueOpsecScreen(Screen parent) {
        super(Component.literal("True opsec"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        options.clear();
        boardWidgets.clear();
        order.clear();

        int x = this.width / 2 - W / 2;
        int y = 54;

        main = Button.builder(Component.literal("True opsec"), b -> start())
                .bounds(this.width / 2 - 60, this.height / 2 - 10, 120, 20).build();
        addRenderableWidget(main);

        // 0: username changer (live, client-side)
        nameBox = new EditBox(this.font, x, y, W, 20, Component.literal("Username"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.literal("Username changer (any name)"));
        nameBox.setValue(Spoof.username);
        nameBox.setResponder(v -> Spoof.username = v.trim());
        add(nameBox, 0);

        // 1: invisibility
        invisBtn = Button.builder(invisLabel(), b -> {
            Spoof.invisible = !Spoof.invisible;
            b.setMessage(invisLabel());
        }).bounds(x, y + 26, W, 20).build();
        add(invisBtn, 1);

        // 2: dropdown
        boardBtn = Button.builder(boardLabel(), b -> {
            boardOpen = !boardOpen;
            b.setMessage(boardLabel());
        }).bounds(x, y + 52, W, 20).build();
        add(boardBtn, 2);

        // 3: board fields (shown only when dropdown is open)
        int by = y + 78;
        int half = (W - 6) / 2;
        bName = field(x, by, W, "Name", Spoof.boardName, v -> Spoof.boardName = v.trim());
        bMoney = field(x, by + 24, half, "Money", Spoof.boardMoney, v -> Spoof.boardMoney = v.trim());
        bKills = field(x + half + 6, by + 24, half, "Kills", Spoof.boardKills, v -> Spoof.boardKills = v.trim());
        bShards = field(x, by + 48, half, "Shards", Spoof.boardShards, v -> Spoof.boardShards = v.trim());
        boardToggle = Button.builder(toggleLabel(), b -> {
            Spoof.boardOn = !Spoof.boardOn;
            b.setMessage(toggleLabel());
        }).bounds(x + half + 6, by + 48, half, 20).build();
        for (AbstractWidget w : new AbstractWidget[]{bName, bMoney, bKills, bShards, boardToggle}) {
            boardWidgets.add(w);
            add(w, 3);
        }

        // On resize while already open, skip the intro.
        if (clickedAt > 0) clickedAt = System.currentTimeMillis() - 100_000;
    }

    private EditBox field(int x, int y, int w, String hint, String value, java.util.function.Consumer<String> onChange) {
        EditBox e = new EditBox(this.font, x, y, w, 20, Component.literal(hint));
        e.setMaxLength(32);
        e.setHint(Component.literal(hint));
        e.setValue(value);
        e.setResponder(onChange);
        return e;
    }

    private void add(AbstractWidget w, int idx) {
        w.visible = false;
        w.active = false;
        addRenderableWidget(w);
        options.add(w);
        order.put(w, idx);
    }

    private void start() {
        if (clickedAt < 0) clickedAt = System.currentTimeMillis();
    }

    private Component invisLabel() { return Component.literal("Character inv: " + (Spoof.invisible ? "ON" : "OFF")); }
    private Component boardLabel() { return Component.literal("Donut board  " + (boardOpen ? "^" : "v")); }
    private Component toggleLabel() { return Component.literal("Spoof: " + (Spoof.boardOn ? "ON" : "OFF")); }

    private static float clamp01(float v) { return v < 0 ? 0 : Math.min(v, 1); }
    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long since = clickedAt < 0 ? -1 : System.currentTimeMillis() - clickedAt;

        // main button glides from the centre to the top
        float p = since < 0 ? 0 : easeInOutCubic(clamp01(since / (float) ANIM_MS));
        int startY = this.height / 2 - 10;
        int endY = 24;
        main.setY(Math.round(startY + (endY - startY) * p));

        // option rows fade in one after another once the glide is done
        for (AbstractWidget w : options) {
            float a = since < 0 ? 0 : clamp01((since - ANIM_MS - order.get(w) * STAGGER_MS) / (float) FADE_MS);
            boolean show = a > 0 && (!boardWidgets.contains(w) || boardOpen);
            w.visible = show;
            w.active = show && a >= 1f;
            w.setAlpha(a);
        }

        super.render(g, mouseX, mouseY, partialTick);

        if (since > ANIM_MS + FADE_MS) {
            g.drawCenteredString(this.font, "Client-side only - other players can't see any of this",
                    this.width / 2, this.height - 16, 0xFFA0A0A0);
        }
    }

    @Override
    public void removed() {
        Spoof.save();
        super.removed();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
