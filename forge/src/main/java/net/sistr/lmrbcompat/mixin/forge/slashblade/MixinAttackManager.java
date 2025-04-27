package net.sistr.lmrbcompat.mixin.forge.slashblade;

import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.lmrbcompat.forge.slashblade.SlashBladeCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AttackManager.class)
public class MixinAttackManager {

    @Inject(method = "doMeleeAttack", at = @At("HEAD"), cancellable = true)
    private static void onDoMeleeAttack(LivingEntity attacker, Entity target, boolean forceHit, boolean resetHit, CallbackInfo ci) {
        if (!(attacker instanceof LittleMaidEntity maid)) {
            return;
        }
        if (target instanceof EntityAbstractSummonedSword) {
            return;
        }
        ci.cancel();

        SlashBladeCompat.littlemaidDoMelee(maid, target, forceHit, resetHit);
    }

}
