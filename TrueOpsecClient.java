package com.trueopsec;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class TrueOpsecClient implements ClientModInitializer {
    public static final Identifier LOGO = Identifier.fromNamespaceAndPath("trueopsec", "textures/gui/logo.png");

    /** Set by JoinMultiplayerScreenMixin each time the server list screen is (re)built. */
    public static Button logoButton;

    @Override
    public void onInitializeClient() {
        Spoof.load();
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof JoinMultiplayerScreen) {
                ScreenEvents.afterRender(screen).register((s, g, mx, my, dt) -> drawLogo(g));
            }
        });
    }

    private static void drawLogo(GuiGraphics g) {
        Button b = logoButton;
        if (b == null || !b.visible) return;
        int size = 16; // logo is 32x32 so it stays crisp at GUI scale 2+
        int x = b.getX() + (b.getWidth() - size) / 2;
        int y = b.getY() + (b.getHeight() - size) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, x, y, 0f, 0f, size, size, 32, 32, 32, 32);
    }
}
