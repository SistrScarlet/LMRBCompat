package net.sistr.lmrbcompat.forge.fn5728.mode;

import fn5728.IFN_EntitySS190;
import fn5728.IFN_ItemFN5728;
import fn5728.IFN_SoundEvent;
import fn5728.mod_IFN_FN5728Guns;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidrebirth.api.mode.IRangedWeapon;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.lmrbcompat.forge.fn5728.FN5728Compat;
import net.sistr.lmrbcompat.mode.AbstractShooterMode;

public class ShooterMode extends AbstractShooterMode<IFN_ItemFN5728> {

    public ShooterMode(ModeType<ShooterMode> modeType, String name, LittleMaidEntity maid) {
        super(modeType, name, maid);
    }

    @Override
    protected boolean isGunItem(ItemStack stack) {
        return stack.getItem() instanceof IFN_ItemFN5728;
    }

    @Override
    protected IFN_ItemFN5728 castGunItem(ItemStack stack) {
        return ((IFN_ItemFN5728) stack.getItem());
    }

    @Override
    protected boolean isFullAuto() {
        return gunItem == mod_IFN_FN5728Guns.item_p90.get();
    }

    @Override
    protected boolean shouldReload() {
        return getAmmoAmount() <= 0;
    }

    @Override
    protected boolean isAmmo(ItemStack stack) {
        return stack.getItem() == mod_IFN_FN5728Guns.item_ss190.get();
    }

    @Override
    protected boolean isMagazineReload() {
        return false;
    }

    @Override
    protected int getReloadLength() {
        return gunItem.reloadtime;
    }

    @Override
    protected boolean isInfinity() {
        return EnchantmentHelper.getEquipmentLevel(Enchantments.INFINITY, this.maid) > 0;
    }

    @Override
    protected int getMaxAmmoAmount() {
        return gunStack.getMaxDamage() - 1;
    }

    @Override
    protected int getAmmoAmount() {
        return getMaxAmmoAmount() - gunStack.getDamage();
    }

    @Override
    protected void setAmmoAmount(int amount) {
        gunStack.setDamage(getMaxAmmoAmount() - amount);
    }

    @Override
    protected void playReloadStartSound() {
        this.maid.getWorld().playSound(null, this.maid.getX(), this.maid.getY(), this.maid.getZ(),
                IFN_SoundEvent.getSound(gunItem.release_sound),
                SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    @Override
    protected void playReloadEndSound() {
        this.maid.getWorld().playSound(null, this.maid.getX(), this.maid.getY(), this.maid.getZ(),
                IFN_SoundEvent.getSound(gunItem.reload_sound),
                SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    @Override
    protected void shootBullet() {
        var gun = gunItem;
        var world = this.maid.getWorld();
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
            if (this.maid.isInSneakingPose()) {
                bbure = gun.bureads;
            }
            bulletEntity.setVelocity(this.maid,
                    this.maid.getPitch() + (this.maid.getRandom().nextFloat() * 2 - 1) * 5,
                    this.maid.getYaw() + (this.maid.getRandom().nextFloat() * 2 - 1) * 5,
                    0.0F, gun.speed, bbure);
            if (!world.isClient()) world.spawnEntity(bulletEntity);
        }
    }

    @Override
    protected void shootEffect() {
        //パーティクル
        double xx11 = 0;
        double zz11 = 0;
        double yy11 = 0;
        float xz;
        if (this.maid.isInSneakingPose()) {
            xz = 0F;
        } else {
            if (this.maid.getMainArm() == Arm.RIGHT) {
                xz = 1.57f;
            } else {
                xz = -1.57f;
            }
        }
        double yy = gunItem.fire_posy;
        if (this.maid.isInSneakingPose()) {
            yy = gunItem.fire_posy - 0.2F;
        }
        double zzz = gunItem.fire_posz * Math.cos(Math.toRadians(-this.maid.getPitch()));
        var rad = MathHelper.PI / 180f;
        xx11 -= MathHelper.sin(this.maid.getHeadYaw() * rad) * zzz;
        zz11 += MathHelper.cos(this.maid.getHeadYaw() * rad) * zzz;
        xx11 -= MathHelper.sin(this.maid.getHeadYaw() * rad + xz) * gunItem.fire_posx;
        zz11 += MathHelper.cos(this.maid.getHeadYaw() * rad + xz) * gunItem.fire_posx;
        yy11 = MathHelper.sqrt((float) (zzz * zzz)) * Math.tan(Math.toRadians(-this.maid.getPitch())) * 1D;
        this.maid.getWorld().addParticle(ParticleTypes.SMOKE,
                this.maid.getX() + xx11,
                this.maid.getY() + yy + yy11,
                this.maid.getZ() + zz11,
                0.0D, 0.1D, 0.0D);
    }

    @Override
    protected int getShootIntervalLength() {
        return mod_IFN_FN5728Guns.item_fiveseven.get() == gunItem ? 10 : 2;
    }

    @Override
    protected void playShootSound() {
        this.maid.getWorld().playSound(null, this.maid.getX(), this.maid.getY(), this.maid.getZ(),
                IFN_SoundEvent.getSound(gunItem.fire_sound),
                SoundCategory.NEUTRAL, 1.0F, 1.0F);
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
