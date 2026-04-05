package net.sistr.lmrbcompat.actionarms.mode;

import java.util.Optional;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.sistr.actionarms.entity.util.AIGunController;
import net.sistr.actionarms.entity.util.AIGunController.GunGoal;
import net.sistr.actionarms.item.GunItem;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.mode.AbstractArcherMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

public class ShooterMode extends AbstractArcherMode<Item> {
    private AIGunController aiGun;
    private int inSightTime;
    private final double maxAimDegreesCos = Math.cos(Math.toRadians(15));
    private int zanshin;

    public ShooterMode(
            ModeType<? extends AbstractArcherMode> modeType, String name, LittleMaidEntity mob) {
        super(modeType, name, mob);
    }

    @Override
    public boolean shouldExecute() {
        zanshin = Math.max(0, zanshin - 1);

        var stack = this.mob.getMainHandStack();
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            return false;
        }

        // AIGunController の生成（初回 or 銃が変わった場合）
        if (aiGun == null) {
            aiGun =
                    gunItem.createAIController(
                            mob,
                            () -> mob.getMainHandStack(),
                            () -> Optional.of(mob.getInventory()));
            aiGun.setCooldownMultiplier(1.5f);
        }

        var status = aiGun.getStatus();
        if (!status.canAttack() && !status.canReload()) {
            return false;
        }

        boolean shouldExecute = super.shouldExecute();
        if (shouldExecute) {
            return true;
        }
        return zanshin > 0;
    }

    @Override
    public void tick() {
        var target = this.mob.getTarget();
        boolean targetAlive = target != null && target.isAlive();
        var status = aiGun.getStatus();

        // Goal 決定
        if (targetAlive && status.canAttack()) {
            aiGun.setGoal(GunGoal.ATTACK);
            zanshin = 10;
        } else if (status.canReload()) {
            aiGun.setGoal(GunGoal.RELOAD);
            zanshin = 10;
        } else {
            aiGun.setGoal(GunGoal.IDLE);
        }

        // ターゲットがいる場合は親の tick で視線追従
        if (targetAlive) {
            super.tick();
        }

        // 銃の tick
        var result = aiGun.tick(1f / 20f);

        // 射撃時の演出
        if (result.fired()) {
            this.mob.play(LMSounds.SHOOT);
            this.mob.swingHand(Hand.MAIN_HAND);
        }
    }

    @Override
    protected void tickRangedAttack(
            LivingEntity target,
            ItemStack stack,
            boolean canSee,
            double distanceSq,
            float maxRange) {
        if (canSee) {
            inSightTime++;
        } else {
            inSightTime = 0;
        }

        // 視界に入って間もない or 射程外 → READY に留める
        if (inSightTime < 10 || distanceSq >= maxRange * maxRange) {
            aiGun.setGoal(GunGoal.READY);
            return;
        }

        // 照準チェック
        var lookFor = this.mob.getRotationVec(1.0f);
        var targetFor = target.getEyePos().subtract(this.mob.getEyePos()).normalize();
        if (targetFor.dotProduct(lookFor) < maxAimDegreesCos) {
            aiGun.setGoal(GunGoal.READY);
            return;
        }

        // 射線チェック（味方への誤射防止）
        var rayResult =
                this.raycastShootLine(
                        target,
                        maxRange,
                        (e) ->
                                e instanceof LivingEntity living
                                        && TameableUtil.isFriend(this.mob, living));
        if (rayResult.isPresent() && rayResult.get().getType() != HitResult.Type.MISS) {
            aiGun.setGoal(GunGoal.READY);
            return;
        }

        // 射撃 OK → ATTACK
        aiGun.setGoal(GunGoal.ATTACK);
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.zanshin = 0;
        this.inSightTime = 0;
        if (aiGun != null) {
            aiGun.setGoal(GunGoal.IDLE);
        }
    }

    @Override
    protected float getMaxRange(ItemStack itemStack) {
        return 16;
    }

    @Override
    protected Optional<Item> getWeaponInstance(ItemStack itemStack) {
        if (itemStack.getItem() instanceof GunItem) {
            return Optional.of(itemStack.getItem());
        }
        return Optional.empty();
    }
}
