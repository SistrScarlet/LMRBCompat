package net.sistr.lmrbcompat.forge.slashblade.mode;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.lmrbcompat.forge.slashblade.SlashBladeCompat;
import net.sistr.lmrbcompat.mode.AbstractFencerMode;

import java.util.Optional;

public class SlashBladeMode extends AbstractFencerMode<ItemStack> {
    private boolean enableStep = false;
    private int attackCool;
    private Combo combo;

    public SlashBladeMode(MobEntity mob, ModeType<? extends Mode> modeType, String name) {
        super(mob, modeType, name);
    }

    @Override
    protected Optional<ItemStack> getWeaponInstance(ItemStack stack) {
        return stack.getItem() instanceof ItemSlashBlade
                ? Optional.of(stack)
                : Optional.empty();
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        if (this.mob.getStepHeight() < 1.0f) {
            this.mob.setStepHeight(1.0f);
            enableStep = true;
        }
        this.combo = this.mob.getRandom().nextBoolean() ? Combo.R : Combo.L;
    }

    @Override
    public void resetTask() {
        super.resetTask();
        if (enableStep) {
            this.mob.setStepHeight(0.5f);
            enableStep = false;
        }
    }

    @Override
    protected void tickToMove() {
        super.tickToMove();
    }

    @Override
    protected void preTryAttackTick() {
        super.preTryAttackTick();
        // 距離が近すぎたら後退
        var distanceSq = getBoundingDistance(this.target);
        var minRange = getMinAttackRange();
        if (distanceSq < minRange) {
            // 後退量を相手との相対距離で決める
            /*var maxRange = getMaxAttackRange();
            var distance = Math.sqrt(distanceSq);
            var percent = 1 - Math.max(0, distance - minRange) / (maxRange - minRange);
            var speed = (float) (percent * percent);*/
            // 速度変わらん
            this.mob.getMoveControl().strafeTo(-1, 0.0F);
        } else {
            this.mob.getMoveControl().strafeTo(0.0F, 0.0F);
        }
    }

    @Override
    protected boolean canAttack() {
        if (attackCool-- > 0) {
            return false;
        }
        return super.canAttack();
    }

    @Override
    protected void attack() {
        this.mob.lookAtEntity(target, 30.0F, 30.0F);
        attackCool = getMaxAttackCool();
        boolean flag = SlashBladeCompat.INSTANCE.slashInput(weaponStack, mob, combo == Combo.R);
        if (flag) {
            this.mob.swingHand(Hand.MAIN_HAND);
            if (this.mob instanceof SoundPlayable soundPlayable) {
                soundPlayable.play(LMSounds.ATTACK);
            }
        }
    }

    @Override
    protected boolean isClose(double distance) {
        var range = getMaxAttackRange();
        return distance < range;
    }

    @Override
    public void writeModeData(NbtCompound nbt) {
        super.writeModeData(nbt);
        nbt.putBoolean("enableStep", enableStep);
    }

    @Override
    public void readModeData(NbtCompound nbt) {
        super.readModeData(nbt);
        enableStep = nbt.getBoolean("enableStep");
        if (enableStep) {
            this.mob.setStepHeight(1.0f);
        }
    }

    protected float getMaxAttackRange() {
        return 3.5f;
    }

    protected float getMinAttackRange() {
        return 3.0f;
    }

    protected int getMaxAttackCool() {
        return 4;
    }

    public enum Combo {
        R,
        L
    }
}
