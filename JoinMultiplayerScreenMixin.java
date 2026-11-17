package com.trueopsec.mixin;

import com.trueopsec.TrueOpsecClient;
import com.trueopsec.TrueOpsecScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    protected JoinMultiplayerScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void trueopsec$addButton(CallbackInfo ci) {
        String editText = Component.translatable("selectServer.edit").getString();
        AbstractWidget edit = null;
        for (GuiEventListener l : this.children()) {
            if (l instanceof AbstractWidget w && w.getMessage().getString().equals(editText)) { edit = w; break; }
        }
        int size = 20;
        int x = edit != null ? edit.getX() - size - 4 : this.width / 2 - 154 - size - 4;
        int y = edit != null ? edit.getY() + (edit.getHeight() - size) / 2 : this.height - 52;

        Button b = Button.builder(Component.empty(),
                        btn -> this.minecraft.setScreen(new TrueOpsecScreen((Screen) (Object) this)))
                .bounds(x, y, size, size)
                .tooltip(Tooltip.create(Component.literal("True opsec")))
                .build();
        this.addRenderableWidget(b);
        TrueOpsecClient.logoButton = b;
    }
}
