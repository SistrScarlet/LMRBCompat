package net.sistr.lmrbcompat.actionarms.mode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.sistr.actionarms.entity.util.GunController;
import net.sistr.actionarms.entity.util.IKeyInputManager;
import net.sistr.actionarms.entity.util.InventoryAmmoUtil;
import net.sistr.actionarms.entity.util.KeyInputManager;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.IItemComponent;
import net.sistr.actionarms.item.component.LeverActionGunComponent;
import net.sistr.actionarms.item.component.Reloadable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.mode.AbstractArcherMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

public class ShooterMode extends AbstractArcherMode<LeverActionGunItem> {
    private final GunController gunController;
    private final IKeyInputManager keyInputManager;
    private int inSightTime;
    private LeverActionGunComponent component;
    private final double maxAimDegreesCos = Math.cos(Math.toRadians(15));
    private int zanshin;

    public ShooterMode(
            ModeType<? extends AbstractArcherMode> modeType, String name, LittleMaidEntity mob) {
        super(modeType, name, mob);
        this.keyInputManager = new KeyInputManager();
        this.gunController =
                new GunController(mob, keyInputManager, () -> getItems(mob.getInventory())) {
                    @Override
                    protected Optional<Inventory> getInventory() {
                        return Optional.of(mob.getInventory());
                    }
                };
    }

    @Override
    public boolean shouldExecute() {
        zanshin = Math.max(0, zanshin - 1);
        boolean shouldExecute = super.shouldExecute();
        if (shouldExecute) {
            return true;
        }
        return zanshin > 0;
    }

    private List<ItemStack> getItems(Inventory inventory) {
        var items = new ArrayList<ItemStack>();

        items.add(this.mob.getMainHandStack());

        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            items.add(stack);
        }
        return items;
    }

    @Override
    public void tick() {
        this.component =
                IItemComponent.query(this.weapon.getGunComponent(), this.weaponStack, c -> c);

        Reloadable.ReloadStartContext reloadStartContext =
                (predicate) -> InventoryAmmoUtil.hasBullet(this.mob.getInventory(), predicate);

        // 弾が無くなったら終了
        if (!this.component.getChamber().canShoot()
                && this.component.getMagazine().isEmpty()
                && !reloadStartContext.hasBullet(
                        this.component.getMagazine().getMagazineType().allowBullet())) {
            resetTask();
        }

        // サイクルすべきじゃないならサイクル状態を解除
        boolean cycling = this.component.shouldCycle();

        // リロードすべきじゃないならリロード状態を解除
        var target = this.mob.getTarget();
        boolean targetAlive = target != null && target.isAlive();
        boolean required =
                !this.component.getChamber().canShoot() && this.component.getMagazine().isEmpty();
        boolean shouldReload = this.component.shouldReload();
        boolean reloading = !cycling && shouldReload && (!targetAlive || required);

        // 動作中はザンシンする
        if (cycling || reloading || targetAlive) {
            zanshin = 10;
        }

        this.keyInputManager.tick();

        boolean shouldInput = this.mob.age % 5 == 0;

        // サイクルすべきならサイクル操作
        boolean cycle =
                shouldInput
                        && cycling
                        && component.canCycle()
                        && !this.keyInputManager.isPressPrev(KeyInputManager.Key.COCK, 1);
        this.keyInputManager.input(KeyInputManager.Key.COCK, cycle);

        // リロードすべきならリロード操作
        boolean reload =
                shouldInput
                        && reloading
                        && this.component.canReload(reloadStartContext)
                        && !this.keyInputManager.isPressPrev(KeyInputManager.Key.RELOAD, 1);
        this.keyInputManager.input(KeyInputManager.Key.RELOAD, reload);

        // 射撃操作は後で上書きする
        this.keyInputManager.input(KeyInputManager.Key.FIRE, false);

        if (target != null && target.isAlive()) {
            super.tick();
        }

        boolean canShoot = this.component.getChamber().canShoot();

        this.gunController.tick();

        this.component =
                IItemComponent.query(this.weapon.getGunComponent(), this.weaponStack, c -> c);

        if (canShoot && !this.component.getChamber().canShoot()) {
            this.mob.play(LMSounds.SHOOT);
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

        if (inSightTime < 10) {
            return;
        }

        if (this.keyInputManager.isPressPrev(KeyInputManager.Key.FIRE, 1)) {
            return;
        }

        // 銃状態チェック
        if (!component.canTrigger()) {
            return;
        }

        if (distanceSq >= maxRange * maxRange) {
            return;
        }

        // 照準チェック
        var lookFor = this.mob.getRotationVec(1.0f);
        var targetFor = target.getEyePos().subtract(this.mob.getEyePos()).normalize();
        double dot = targetFor.dotProduct(lookFor);
        if (dot < maxAimDegreesCos) {
            return;
        }

        // 射線チェック
        var result =
                this.raycastShootLine(
                        target,
                        maxRange,
                        (e) ->
                                e instanceof LivingEntity living
                                        && TameableUtil.isFriend(this.mob, living));

        // 射線上に味方がいる場合はreturn
        if (result.isPresent() && result.get().getType() != HitResult.Type.MISS) {
            return;
        }

        // 射撃実行
        this.keyInputManager.input(KeyInputManager.Key.FIRE, true);
        this.mob.swingHand(Hand.MAIN_HAND);
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.zanshin = 0;
    }

    @Override
    protected float getMaxRange(ItemStack itemStack) {
        return 16;
    }

    @Override
    protected Optional<LeverActionGunItem> getWeaponInstance(ItemStack itemStack) {
        if (itemStack.getItem() instanceof LeverActionGunItem leverActionGunItem) {
            return Optional.of(leverActionGunItem);
        }
        return Optional.empty();
    }
}
