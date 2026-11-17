package com.trueopsec.mixin;

import com.trueopsec.Spoof;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sidebar scoreboard lines, tab list names and name tags all go through here. */
@Mixin(PlayerTeam.class)
public class PlayerTeamMixin {
    @Inject(method = "formatNameForTeam", at = @At("RETURN"), cancellable = true)
    private static void trueopsec$rewrite(Team team, Component name, CallbackInfoReturnable<MutableComponent> cir) {
        MutableComponent original = cir.getReturnValue();
        Component out = Spoof.applyTeamLine(original);
        if (out != original) {
            cir.setReturnValue(out instanceof MutableComponent m ? m : out.copy());
        }
    }
}
