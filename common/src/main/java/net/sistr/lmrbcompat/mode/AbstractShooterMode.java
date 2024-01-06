package net.sistr.lmrbcompat.mode;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.mode.RangedAttackBaseMode;

public abstract class AbstractShooterMode<T extends Item> extends RangedAttackBaseMode {
    protected final LittleMaidEntity maid;
    protected ItemStack gunStack;
    protected T gunItem;
    protected int reloadTime;
    protected int shootInterval;
    protected int inSightTime;

    public AbstractShooterMode(ModeType<? extends AbstractShooterMode> modeType, String name, LittleMaidEntity maid) {
        super(modeType, name, maid);
        this.maid = maid;
    }

    @Override
    public boolean shouldExecute() {
        var stack = this.maid.getMainHandStack();
        if (!isGunItem(stack)) {
            return false;
        }
        this.gunStack = stack;
        this.gunItem = castGunItem(stack);
        if (getAmmoAmount() <= 0 && !hasAmmo()) {
            return false;
        }
        return super.shouldExecute();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return reloadTime > 0 || super.shouldContinueExecuting();
    }

    abstract protected boolean isGunItem(ItemStack stack);

    abstract protected T castGunItem(ItemStack stack);

    @Override
    public void startExecuting() {
        super.startExecuting();
        if (isFullAuto()) {
            this.mob.play(LMSounds.SHOOT_BURST);
        }
        this.mob.swingHand(Hand.MAIN_HAND);
    }

    abstract protected boolean isFullAuto();

    @Override
    public void tick() {
        if (shootInterval > 0) {
            shootInterval--;
        }
        if (shouldReload()) {
            reloading();
        } else {
            this.reloadTime = 0;
        }
        super.tick();
    }

    abstract protected boolean shouldReload();

    protected boolean hasAmmo() {
        //弾がオフハンドにある
        if (isAmmo(this.maid.getOffHandStack())) {
            return true;
        }

        //弾がインベントリにある
        var inventory = this.maid.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            var slot = inventory.getStack(i);
            if (isAmmo(slot)) {
                return true;
            }
        }
        return false;
    }

    abstract protected boolean isAmmo(ItemStack stack);

    protected void reloading() {
        //リロード開始処理
        if (this.reloadTime++ <= 0) {
            playReloadStartSound();
            this.maid.swingHand(Hand.MAIN_HAND);
            return;
        }

        //リロードが終わっていないなら終了
        if (this.reloadTime < getReloadLength()) {
            return;
        }
        this.reloadTime = 0;

        //リロード完了処理
        playReloadEndSound();

        //無限なら弾消費無しで完了(本来は1発以上持ってないとダメだが面倒なので…)
        if (isInfinity()) {
            setAmmoAmount(getMaxAmmoAmount());
            return;
        }

        consumeAmmo();
    }

    protected void consumeAmmo() {
        //オフハンドにある弾を込める
        if (isMagazineReload()) {
            final int maxAmmo = getMaxAmmoAmount();
            var off = this.maid.getOffHandStack();
            if (!off.isEmpty() && isAmmo(off)) {
                int amount = off.getCount();
                if (amount > 0) {
                    //リロード後、弾アイテムがゼロになる場合
                    if (amount == 1) {
                        off.setCount(0);
                        this.maid.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                        setAmmoAmount(maxAmmo);
                    } else {//弾アイテムが残る場合
                        off.decrement(1);
                        setAmmoAmount(maxAmmo);
                    }
                    return;
                }
            }

            //インベントリにある弾を込める
            var inv = this.maid.getInventory();
            int size = inv.size();
            for (int i = 0; i < size; i++) {
                var slot = inv.getStack(i);
                int amount = slot.getCount();
                if (slot.isEmpty() || !isAmmo(slot) || amount <= 0) {
                    continue;
                }
                //リロード後、弾アイテムがゼロになる場合
                if (amount == 1) {
                    slot.setCount(0);
                    inv.setStack(i, ItemStack.EMPTY);
                    setAmmoAmount(maxAmmo);
                } else {//弾アイテムが残る場合
                    slot.decrement(1);
                    setAmmoAmount(maxAmmo);
                }
                return;
            }
        } else {
            final int maxAmmo = getMaxAmmoAmount();
            int remain = getAmmoAmount();
            var off = this.maid.getOffHandStack();
            if (!off.isEmpty() && isAmmo(off)) {
                int amount = off.getCount();
                if (amount > 0) {
                    //リロード後、弾アイテムがゼロになる場合
                    if (amount <= maxAmmo - remain) {
                        off.decrement(0);
                        this.maid.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                        remain += amount;
                    } else {//弾アイテムが残る場合
                        off.decrement(maxAmmo - remain);
                        remain = maxAmmo;
                    }
                }
            }

            //インベントリにある弾を込める
            var inv = this.maid.getInventory();
            int size = inv.size();
            for (int i = 0; i < size; i++) {
                if (remain >= maxAmmo) {
                    break;
                }
                var slot = inv.getStack(i);
                int amount = slot.getCount();
                if (slot.isEmpty() || !isAmmo(slot) || amount <= 0) {
                    continue;
                }
                //リロード後、弾アイテムがゼロになる場合
                if (amount <= maxAmmo - remain) {
                    slot.decrement(0);
                    inv.setStack(i, ItemStack.EMPTY);
                    remain += amount;
                } else {//弾アイテムが残る場合
                    slot.decrement(maxAmmo - remain);
                    remain = maxAmmo;
                }
            }

            setAmmoAmount(remain);
        }
    }

    abstract protected boolean isMagazineReload();

    abstract protected int getReloadLength();

    abstract protected boolean isInfinity();

    abstract protected int getMaxAmmoAmount();

    abstract protected int getAmmoAmount();

    abstract protected void setAmmoAmount(int amount);

    abstract protected void playReloadStartSound();

    abstract protected void playReloadEndSound();

    @Override
    protected void tickRangedAttack(LivingEntity target, ItemStack stack,
                                    boolean canSee, double distanceSq, float maxRange) {
        if (canSee) {
            inSightTime++;
        } else {
            inSightTime = 0;
        }

        //視界に入れてすぐ、リロード中、射程外、または射線が通らない場合は撃たない
        if (inSightTime < 10 || shouldReload() || distanceSq >= maxRange * maxRange) {
            return;
        }

        var result = this.raycastShootLine(target, maxRange,
                (e) -> e instanceof LivingEntity living && this.mob.isFriend(living));

        if (result.isPresent() && result.get().getType() != HitResult.Type.MISS) {
            return;
        }

        if (shootInterval > 0) {
            return;
        }
        shootInterval = getShootIntervalLength();
        if (shootInterval >= 10) {
            this.maid.swingHand(Hand.MAIN_HAND);
        }

        //射撃処理

        playShootSound();

        shootBullet();
        shootEffect();

        stack.setDamage(stack.getDamage() + 1);

        //撃ち切ったタイミングで敵が消滅した場合もリロードするためここでリロード開始
        if (shouldReload() && hasAmmo()) {
            this.reloadTime++;
            playReloadStartSound();
            this.maid.swingHand(Hand.MAIN_HAND);
        }
    }

    abstract protected void shootBullet();

    abstract protected void shootEffect();

    abstract protected int getShootIntervalLength();

    abstract protected void playShootSound();

    @Override
    public void resetTask() {
        super.resetTask();
        inSightTime = 0;
        reloadTime = 0;
    }

}
