package net.sistr.lmrbcompat.forge.slashblade.client;

import mods.flammpfeil.slashblade.client.renderer.layers.LayerMainBlade;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModel;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.lmrbcompat.mixin.forge.slashblade.MixinMaidModelRenderer;

/**
 * Called from {@link MixinMaidModelRenderer#onInit}
 */
@OnlyIn(Dist.CLIENT)
public class LMSlashBladeFeatureRenderer<T extends LivingEntity & IHasMultiModel, M extends MultiModel<T>> extends LayerMainBlade<T, M> {
    public LMSlashBladeFeatureRenderer(FeatureRendererContext<T, M> entityRendererIn) {
        super(entityRendererIn);
    }
}
