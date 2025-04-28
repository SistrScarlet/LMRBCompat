package net.sistr.lmrbcompat.mixin.forge.slashblade;

import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AttackManager.class)
public class MixinAttackManager {

    @Inject(method = "doAttackWith", at = @At("HEAD"), cancellable = true)
    private static void onDoAttackWith(DamageSource src, float amount, Entity target, boolean forceHit, boolean resetHit, CallbackInfo ci) {
        if (!(target instanceof EntityAbstractSummonedSword)) {
            AttackManager.doManagedAttack((t) -> {
                var attacker = (LivingEntity) src.getAttacker();
                assert attacker != null;
                var mainHandStack = attacker.getMainHandStack();
                var opt = mainHandStack.getCapability(ItemSlashBlade.BLADESTATE);
                if (!opt.isPresent()) {
                    return;
                }
                mainHandStack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent((state) -> {
                    IConcentrationRank.ConcentrationRanks rankBonus
                            = attacker.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                            .map((rp) -> rp.getRank(attacker.getEntityWorld().getTime()))
                            .orElse(IConcentrationRank.ConcentrationRanks.NONE);
                    EntityAttributeModifier am = LMRBCompat$getEntityAttributeModifier(rankBonus);

                    try {
                        state.setOnClick(true);
                        attacker.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).addTemporaryModifier(am);
                        // アタック部分をプレイヤーのものから置き換え
                        if (t.damage(src, amount)) {
                            if (target instanceof LivingEntity livingTarget) {
                                var slashBladeItem = (ItemSlashBlade) mainHandStack.getItem();
                                slashBladeItem.postHit(mainHandStack, livingTarget, attacker);
                            }
                        }
                    } finally {
                        attacker.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).removeModifier(am);
                        state.setOnClick(false);
                        ci.cancel();
                    }
                });
            }, target, forceHit, resetHit);
        }
    }

    @Unique
    private static EntityAttributeModifier LMRBCompat$getEntityAttributeModifier(IConcentrationRank.ConcentrationRanks rankBonus) {
        float modifiedRatio = (float) rankBonus.level / 2.0F;
        return new EntityAttributeModifier(
                "RankDamageBonus",
                modifiedRatio,
                EntityAttributeModifier.Operation.ADDITION
        );
    }

}
