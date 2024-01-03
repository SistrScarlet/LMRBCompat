package net.sistr.lmrbcompat.forge.fn5728.mode;

import fn5728.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.IRangedWeapon;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.mode.RangedAttackBaseMode;
import net.sistr.lmrbcompat.forge.fn5728.FN5728Compat;

public class ShooterMode extends RangedAttackBaseMode {
    protected final LittleMaidEntity maid;
    protected int reloadTime;
    protected int shootInterval;
    protected int inSightTime;

    public ShooterMode(ModeType<ShooterMode> modeType, String name, LittleMaidEntity maid) {
        super(modeType, name, maid);
        this.maid = maid;
    }

    @Override
    public boolean shouldExecute() {
        return reloadTime > 0 || super.shouldExecute();
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        if (maid.getMainHandStack().getItem() instanceof IFN_ItemP90) {
            this.mob.play(LMSounds.SHOOT_BURST);
        }
        this.mob.swingHand(Hand.MAIN_HAND);
    }

    @Override
    public void tick() {
        if (shootInterval > 0) {
            shootInterval--;
        }
        var stack = this.maid.getMainHandStack();
        if (stack.getItem() instanceof IFN_ItemFN5728 gun
                && gun.isReload(stack) && hasAmmo()) {
            reloading(gun, stack);
        } else {
            this.reloadTime = 0;
        }
        super.tick();
    }

    protected boolean hasAmmo() {
        var stack = this.maid.getMainHandStack();
        if (stack.getItem() instanceof IFN_ItemFN5728) {
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
        }
        return false;
    }

    private boolean isAmmo(ItemStack stack) {
        return stack.getItem() == mod_IFN_FN5728Guns.item_ss190.get();
    }

    private void reloading(IFN_ItemFN5728 gun, ItemStack stack) {
        var world = this.maid.getWorld();
        //リロード開始処理
        if (this.reloadTime++ <= 0) {
            world.playSound(null, this.maid.getX(), this.maid.getY(), this.maid.getZ(),
                    IFN_SoundEvent.getSound(gun.release_sound),
                    SoundCategory.NEUTRAL, 1.0F, 1.0F);
            this.maid.swingHand(Hand.MAIN_HAND);
            return;
        }

        //リロードが終わっていないなら終了
        if (this.reloadTime < gun.reloadtime) {
            return;
        }
        this.reloadTime = 0;

        //リロード完了処理
        world.playSound(null, this.maid.getX(), this.maid.getY(), this.maid.getZ(),
                IFN_SoundEvent.getSound(gun.reload_sound),
                SoundCategory.NEUTRAL, 1.0F, 1.0F);

        //無限なら弾消費無しで完了(本来は1発以上持ってないとダメだが面倒なので…)
        if (EnchantmentHelper.getEquipmentLevel(Enchantments.INFINITY, this.maid) > 0) {
            gun.setDamage(stack, 0);
            return;
        }

        //オフハンドにある弾を込める
        int remain = stack.getDamage();
        var off = this.maid.getOffHandStack();
        if (isAmmo(off)) {
            int amount = off.getCount();
            if (amount > 0) {
                off.decrement(remain);
                if (off.isEmpty()) {
                    this.maid.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }
                //remainを減少させる
                remain = Math.max(0, remain - amount);
            }
        }

        //インベントリにある弾を込める
        var inv = this.maid.getInventory();
        int size = inv.size();
        for (int i = 0; i < size; i++) {
            var slot = inv.getStack(i);
            if (isAmmo(slot)) {
                int amount = slot.getCount();
                if (amount > 0) {
                    slot.decrement(remain);
                    if (slot.isEmpty()) {
                        inv.setStack(i, ItemStack.EMPTY);
                    }
                    //remainを減少させる
                    remain = Math.max(0, remain - amount);
                }
            }
        }

        stack.setDamage(remain);
    }

    @Override
    protected void tickRangedAttack(LivingEntity target, ItemStack stack,
                                    boolean canSee, double distanceSq, float maxRange) {
        if (canSee) {
            inSightTime++;
        } else {
            inSightTime = 0;
        }
        var item = stack.getItem();
        if (!(item instanceof IFN_ItemFN5728 gun)) {
            return;
        }

        var world = this.maid.getWorld();

        //視界に入れてすぐ、リロード中、射程外、または射線が通らない場合は撃たない
        if (inSightTime < 10 || gun.isReload(stack) || !canSee || distanceSq >= maxRange * maxRange) {
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
        if (item == mod_IFN_FN5728Guns.item_fiveseven.get()) {
            shootInterval = 10;
            this.maid.swingHand(Hand.MAIN_HAND);
        } else {
            shootInterval = 2;
        }

        //射撃処理

        world.playSound(null, this.maid.getX(), this.maid.getY(), this.maid.getZ(),
                IFN_SoundEvent.getSound(gun.fire_sound),
                SoundCategory.NEUTRAL, 1.0F, 1.0F);

        for (int pe = 0; pe < gun.pellet; ++pe) {
            IFN_EntitySS190 bulletEntity = new IFN_EntitySS190(world, this.maid);
            int ep = EnchantmentHelper.getEquipmentLevel(Enchantments.POWER, this.maid);
            if (gun.powor == -1) {
                bulletEntity.flare = true;
            } else {
                bulletEntity.powor = gun.powor + ep;
            }
            bulletEntity.setGravity(gun.gra);
            bulletEntity.exlevel = gun.exlevel;
            int fm = EnchantmentHelper.getEquipmentLevel(Enchantments.FLAME, this.maid);
            if (fm > 0) {
                bulletEntity.flame = true;
            }
            float bbure = gun.bure;
            if (this.maid.isCrawling()) {
                bbure = gun.bureads;
            }
            bulletEntity.setVelocity(this.maid,
                    this.maid.getPitch() + (this.maid.getRandom().nextFloat() * 2 - 1) * 5,
                    this.maid.getYaw() + (this.maid.getRandom().nextFloat() * 2 - 1) * 5,
                    0.0F, gun.speed, bbure);
            if (!world.isClient()) world.spawnEntity(bulletEntity);
        }

        stack.setDamage(stack.getDamage() + 1);

        //撃ち切ったタイミングで敵が消滅した場合もリロードするためここでリロード開始
        if (gun.isReload(stack) && hasAmmo()) {
            this.reloadTime++;
            world.playSound(null, this.maid.getX(), this.maid.getY(), this.maid.getZ(),
                    IFN_SoundEvent.getSound(gun.release_sound),
                    SoundCategory.NEUTRAL, 1.0F, 1.0F);
            this.maid.swingHand(Hand.MAIN_HAND);
        }

        //パーティクル
        double xx11 = 0;
        double zz11 = 0;
        double yy11 = 0;
        float xz;
        if (this.maid.getMainArm() == Arm.RIGHT) {
            xz = 1.57f;
        } else {
            xz = -1.57f;
        }
        double yy = gun.fire_posy;
        if (this.maid.isCrawling()) {
            yy = gun.fire_posy - 0.2F;
        }
        double zzz = gun.fire_posz * Math.cos(Math.toRadians(-this.maid.getPitch()));
        var rad = MathHelper.PI / 180f;
        xx11 -= MathHelper.sin(this.maid.getHeadYaw() * rad) * zzz;
        zz11 += MathHelper.cos(this.maid.getHeadYaw() * rad) * zzz;
        xx11 -= MathHelper.sin(this.maid.getHeadYaw() * rad + xz) * gun.fire_posx;
        zz11 += MathHelper.cos(this.maid.getHeadYaw() * rad + xz) * gun.fire_posx;
        yy11 = MathHelper.sqrt((float) (zzz * zzz)) * Math.tan(Math.toRadians(-this.maid.getPitch())) * 1D;
        world.addParticle(ParticleTypes.SMOKE,
                this.maid.getX() + xx11,
                this.maid.getY() + yy + yy11,
                this.maid.getZ() + zz11,
                0.0D, 0.1D, 0.0D);
    }

    @Override
    public void resetTask() {
        super.resetTask();
        inSightTime = 0;
        reloadTime = 0;
    }

    @Override
    protected float getMaxRange(ItemStack itemStack) {
        var item = itemStack.getItem();
        float range = 15;
        if (item instanceof IRangedWeapon rangedWeapon) {
            range = rangedWeapon.getMaxRange_LMRB(itemStack, this.mob);
        }
        return range * FN5728Compat.getConfig().getShooterRangeFactor();
    }
}
