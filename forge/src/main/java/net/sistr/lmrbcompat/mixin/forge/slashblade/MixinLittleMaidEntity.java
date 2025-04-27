package net.sistr.lmrbcompat.mixin.forge.slashblade;

import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.lmrbcompat.compat.CompatUtil;
import net.sistr.lmrbcompat.reflection.ReflectionUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LittleMaidEntity.class)
public abstract class MixinLittleMaidEntity {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (CompatUtil.isModLoaded("slashblade")) {
            ReflectionUtil.invokeStatic(
                    "net.sistr.lmrbcompat.forge.slashblade.SlashBladeCompat",
                    "tickLittleMaid",
                    this
            );
        }
    }

}
