package net.sistr.lmrbcompat.forge.slashblade;

import mods.flammpfeil.slashblade.ability.ArrowReflector;
import mods.flammpfeil.slashblade.ability.TNTExtinguisher;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidrebirth.api.mode.ItemMatcher;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.lmrbcompat.compat.AbstractCompat;
import net.sistr.lmrbcompat.forge.slashblade.mode.SlashBladeMode;
import net.sistr.lmrbcompat.mixin.forge.slashblade.MixinLittleMaidEntity;
import org.jetbrains.annotations.NotNull;

import static mods.flammpfeil.slashblade.item.ItemSlashBlade.BLADESTATE;

public class SlashBladeCompat extends AbstractCompat<SlashBladeConfig> {
    public static SlashBladeCompat INSTANCE;

    public SlashBladeCompat() {
        super("slashblade", SlashBladeConfig.class);
    }

    public void init() {
        super.init();
        INSTANCE = this;
        register("samurai", ModeType
                .<SlashBladeMode>builder((type, entity) ->
                        new SlashBladeMode(entity, type, "Samurai"))
                .addItemMatcher(
                        (stack) -> stack.getItem() instanceof ItemSlashBlade
                                && !isBroken(stack),
                        ItemMatcher.Priority.HIGH)
                .build());
    }

    private boolean isBroken(ItemStack stack) {
        return stack.getCapability(BLADESTATE)
                .map(ISlashBladeState::isBroken)
                .orElse(false);
    }

    @Override
    public String getName() {
        return "SlashBlade";
    }

    /**
     * Called from {@link MixinLittleMaidEntity#onTick}
     */
    public static void tickLittleMaid(LittleMaidEntity maid) {
        var stack = maid.getMainHandStack();
        if (stack.getItem() instanceof ItemSlashBlade) {
            stack.getCapability(BLADESTATE).ifPresent((state) -> {
                maid.getCapability(ItemSlashBlade.INPUT_STATE)
                        .ifPresent((mInput) -> mInput.getScheduler().onTick(maid));
                state.resolvCurrentComboState(maid).tickAction(maid);
                state.sendChanges(maid);
            });
        }
    }

    private static @NotNull EntityAttributeModifier getEntityAttributeModifier(LittleMaidEntity attacker, ISlashBladeState state, IConcentrationRank.ConcentrationRanks rankBonus) {
        float modifiedRatio = (float) rankBonus.level / 2.0F;
        if (IConcentrationRank.ConcentrationRanks.S.level <= rankBonus.level
                && attacker.getOwner() instanceof PlayerEntity player) {
            // 本来はアタッカー本人だが、あえてプレイヤー経験値を参照する
            int level = player.experienceLevel;
            modifiedRatio = Math.max(modifiedRatio, (float) Math.min(level, state.getRefine()));
        }

        return new EntityAttributeModifier("RankDamageBonus", modifiedRatio, EntityAttributeModifier.Operation.ADDITION);
    }

    public static void littlemaidDoMelee(LittleMaidEntity attacker, Entity target, boolean forceHit, boolean resetHit) {
        attacker.getMainHandStack().getCapability(BLADESTATE).ifPresent((state) -> {
            IConcentrationRank.ConcentrationRanks rankBonus = attacker.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                    .map((rp) -> rp.getRank(attacker.getEntityWorld().getTime()))
                    .orElse(IConcentrationRank.ConcentrationRanks.NONE);
            EntityAttributeModifier am = getEntityAttributeModifier(attacker, state, rankBonus);

            try {
                state.setOnClick(true);
                attacker.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).addTemporaryModifier(am);
                // アタック部分をプレイヤーのものから置き換え
                doManagedAttack(attacker, target, forceHit, resetHit);
            } finally {
                attacker.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).removeModifier(am);
                state.setOnClick(false);
            }
        });

        ArrowReflector.doReflect(target, attacker);
        TNTExtinguisher.doExtinguishing(target, attacker);
    }

    private static void doManagedAttack(LittleMaidEntity attacker, Entity target, boolean forceHit, boolean resetHit) {
        if (forceHit) {
            target.timeUntilRegen = 0;
        }
        attack(attacker, target);
        if (resetHit) {
            target.timeUntilRegen = 0;
        }
    }

    private static void attack(LittleMaidEntity attacker, Entity target) {
        float baseAmount = (float) attacker.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getValue();
        if (target.damage(attacker.getDamageSources().mobAttack(attacker), baseAmount)
                && target instanceof LivingEntity livingTarget) {
            ItemStack stack = attacker.getMainHandStack();
            stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent((state) -> {
                state.resolvCurrentComboState(attacker).hitEffect(livingTarget, attacker);
                state.damageBlade(stack, 1, attacker, ItemSlashBlade.getOnBroken(stack));
            });
        }
    }

}
