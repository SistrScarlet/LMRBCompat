package net.sistr.lmrbcompat.forge.gvclib.client;

import gvclib.client.event.RenderTypeGun;
import gvclib.item.ItemGunBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModel;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMMatrixStack;

public class LMGunBaseFeatureRenderer<T extends LivingEntity & IHasMultiModel, M extends MultiModel<T>> extends FeatureRenderer<T, M> {

    public LMGunBaseFeatureRenderer(FeatureRendererContext<T, M> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        boolean mainRight = entity.getMainArm() == Arm.RIGHT;
        ItemStack leftStack = mainRight ? entity.getOffHandStack() : entity.getMainHandStack();
        ItemStack rightStack = mainRight ? entity.getMainHandStack() : entity.getOffHandStack();
        if (!leftStack.isEmpty() || !rightStack.isEmpty()) {
            matrices.push();
            if (this.getContextModel().child) {
                float scale = 0.5F;
                matrices.translate(0.0F, 0.75F, 0.0F);
                matrices.scale(scale, scale, scale);
            }

            this.renderArmWithItem(entity, rightStack, Arm.RIGHT, matrices, vertexConsumers, light);
            this.renderArmWithItem(entity, leftStack, Arm.LEFT, matrices, vertexConsumers, light);
            matrices.pop();
        }
    }

    protected void renderArmWithItem(T entity, ItemStack stack, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!stack.isEmpty()) {
            matrices.push();
            if (entity.isInSneakingPose()) {
                matrices.translate(0.0F, 0.2F, 0.0F);
            }

            boolean isLeft = arm == Arm.LEFT;
            entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                    .ifPresent(model -> model.adjustHandItem(new MMMatrixStack(matrices), isLeft));

            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
            matrices.scale(0.1875F, 0.1875F, 0.1875F);

            // matrices.translate((float) (isLeft ? -1 : 1) / 16.0F * -5.33F, 0.16625F, 2.8125F);
            matrices.translate((float) (isLeft ? -1 : 1) / 16.0F * 2.0F, -0.5F, 0.15F);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemGunBase gun) {
                gun.ModelLoad();
                if (gun.obj_model != null) {
                    boolean isReload = false;
                    MinecraftClient.getInstance().getEntityRenderDispatcher().textureManager.getTexture(Identifier.parse(gun.obj_tex));
                    VertexConsumer vertexconsumer = vertexConsumers.getBuffer(RenderTypeGun.gunrender(Identifier.parse(gun.obj_tex)));
                    gun.obj_model.renderPartColor(vertexconsumer, matrices, "mat1", 255, 255, 255, light);
                    gun.obj_model.renderPartColor(vertexconsumer, matrices, "mat100", 255, 255, 255, light);
                    gun.obj_model.renderPartColor(vertexconsumer, matrices, "mat2", 255, 255, 255, light);
                    if (!isReload)gun.obj_model.renderPartColor(vertexconsumer, matrices, "mat3", 255, 255, 255, light);
                    gun.obj_model.renderPartColor(vertexconsumer, matrices, "mat20", 255, 255, 255, light);
                    gun.obj_model.renderPartColor(vertexconsumer, matrices, "mat21", 255, 255, 255, light);
                    gun.obj_model.renderPartColor(vertexconsumer, matrices, "mat22", 255, 255, 255, light);
                    gun.obj_model.renderPartColor(vertexconsumer, matrices, "mat25", 255, 255, 255, light);
                    gun.obj_model.renderPartColor(vertexconsumer, matrices, "mat31", 255, 255, 255, light);
                    gun.obj_model.renderPartColor(vertexconsumer, matrices, "mat32", 255, 255, 255, light);
                }
            }

            matrices.pop();
        }

    }
}
