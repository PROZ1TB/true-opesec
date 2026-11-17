package com.trueopsec.mixin;

import com.trueopsec.Spoof;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    /** Client-side invisibility: we simply never draw our own player. */
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void trueopsec$hideSelf(E entity, Frustum frustum, double x, double y, double z,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (Spoof.invisible && entity == Minecraft.getInstance().player) cir.setReturnValue(false);
    }
}
