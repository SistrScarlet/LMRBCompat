package net.sistr.lmrbcompat.mixin.forge.slashblade;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.sistr.littlemaidrebirth.client.LMMultiModel;
import net.sistr.littlemaidrebirth.client.MaidModelRenderer;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.lmrbcompat.compat.CompatUtil;
import net.sistr.lmrbcompat.reflection.ReflectionUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MaidModelRenderer.class)
public abstract class MixinMaidModelRenderer extends MobEntityRenderer<LittleMaidEntity, LMMultiModel<LittleMaidEntity>> {

    public MixinMaidModelRenderer(EntityRendererFactory.Context arg, LMMultiModel<LittleMaidEntity> arg2, float f) {
        super(arg, arg2, f);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityRendererFactory.Context ctx, CallbackInfo ci) {
        if (CompatUtil.isModLoaded("slashblade")) {
            ReflectionUtil.getConstructor(
                            "net.sistr.lmrbcompat.forge.slashblade.client.LMSlashBladeFeatureRenderer",
                            FeatureRendererContext.class
                    )
                    .map((constructor) -> constructor.newInstance(this))
                    .flatMap(o -> o)
                    .filter(o -> o instanceof FeatureRenderer)
                    .map(o -> (FeatureRenderer) o)
                    .ifPresent(this::addFeature);
        }
    }

}
