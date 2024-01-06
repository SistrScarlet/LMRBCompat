package net.sistr.lmrbcompat.forge.classicguns.mode;

import classicguns.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.api.mode.IRangedWeapon;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.lmrbcompat.forge.classicguns.ClassicGunsCompat;
import net.sistr.lmrbcompat.mode.AbstractShooterMode;

public class ShooterMode extends AbstractShooterMode<CGItemGunBase> {

    public ShooterMode(ModeType<ShooterMode> modeType, String name, LittleMaidEntity maid) {
        super(modeType, name, maid);
    }

    @Override
    protected boolean isGunItem(ItemStack stack) {
        return stack.getItem() instanceof CGItemGunBase;
    }

    @Override
    protected CGItemGunBase castGunItem(ItemStack stack) {
        return ((CGItemGunBase) stack.getItem());
    }

    @Override
    protected boolean isFullAuto() {
        return gunItem instanceof CGItemGun_AR;
    }

    @Override
    protected boolean shouldReload() {
        return getAmmoAmount() <= 0;
    }

    @Override
    protected boolean isAmmo(ItemStack stack) {
        return gunItem.isAmmo(stack);
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
                CGSoundEvent.getSound(gunItem.reload_sound),
                SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    @Override
    protected void playReloadEndSound() {

    }

    @Override
    protected void shootBullet() {
        fireBullet(gunStack, this.maid.getWorld(), this.maid);
    }

    private void fireBullet(ItemStack stack, World world, LittleMaidEntity maid) {
        world.playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                CGSoundEvent.getSound(gunItem.fire_sound), SoundCategory.NEUTRAL, 3.0F, 1.0F);

        for (int pe = 0; pe < gunItem.pellet; ++pe) {
            CGEntityBullet bullet = new CGEntityBullet(world, maid);
            int ep = EnchantmentHelper.getEquipmentLevel(Enchantments.POWER, maid);
            if (gunItem.powor == -1) {
                bullet.flare = true;
            } else {
                bullet.powor = gunItem.powor + ep;
            }

            bullet.setGravity(gunItem.gra);
            bullet.exlevel = gunItem.exlevel;
            int fm = EnchantmentHelper.getEquipmentLevel(Enchantments.FLAME, maid);
            if (fm > 0) {
                bullet.flame = true;
            }

            float bbure = gunItem.bure;
            if (maid.isInSneakingPose()) {
                bbure = gunItem.bureads;
            }

            bullet.setVelocity(maid, maid.getPitch(), maid.getYaw(), 0.0F, gunItem.speed, bbure);
            if (!world.isClient) {
                world.spawnEntity(bullet);
            }
        }

        double xx11 = gunItem.recoil;
        if (maid.isInSneakingPose()) {
            xx11 = gunItem.recoilads;
        }

        double zz11 = maid.getPitch();
        zz11 += (double) (world.random.nextFloat() * -2.0F) * xx11;
        maid.setPitch((float) zz11);
        xx11 = 0.0;
        zz11 = 0.0;
        double yy11 = 0.0;
        float xz = 1.57F;
        if (!maid.getMainHandStack().isEmpty() && !maid.getOffHandStack().isEmpty()) {
            if (maid.getMainHandStack() == stack) {
                xz = 1.57F;
            } else if (maid.getOffHandStack() == stack) {
                xz = -1.57F;
            }
        } else if (maid.isInSneakingPose()) {
            xz = 0.0F;
        } else if (maid.getMainHandStack() == stack) {
            xz = 1.57F;
        } else if (maid.getOffHandStack() == stack) {
            xz = -1.57F;
        }

        double yy = gunItem.fire_posy;
        if (maid.isInSneakingPose()) {
            yy = gunItem.fire_posy - 0.2F;
        }

        double zzz = (double) gunItem.fire_posz * Math.cos(Math.toRadians(-maid.getPitch()));
        xx11 -= (double) MathHelper.sin(maid.headYaw * 0.017453292F) * zzz;
        zz11 += (double) MathHelper.cos(maid.headYaw * 0.017453292F) * zzz;
        xx11 -= MathHelper.sin(maid.headYaw * 0.017453292F + xz) * gunItem.fire_posx;
        zz11 += MathHelper.cos(maid.headYaw * 0.017453292F + xz) * gunItem.fire_posx;
        yy11 = (double) MathHelper.sqrt((float) (zzz * zzz)) * Math.tan(Math.toRadians(-maid.getPitch())) * 1.0;
        world.addParticle(ParticleTypes.SMOKE,
                maid.getX() + xx11, maid.getY() + yy + yy11, maid.getZ() + zz11,
                0.0, 0.1, 0.0);
    }

    @Override
    protected void shootEffect() {

    }

    @Override
    protected int getShootIntervalLength() {
        return gunItem instanceof CGItemGun_SR ? 10 : gunItem.getCycleCount(gunStack) + 2;
    }

    @Override
    protected void playShootSound() {
        this.maid.getWorld().playSound(null, this.maid.getX(), this.maid.getY(), this.maid.getZ(),
                CGSoundEvent.getSound(gunItem.fire_sound),
                SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    @Override
    protected float getMaxRange(ItemStack itemStack) {
        var item = itemStack.getItem();
        float range = 15;
        if (item instanceof IRangedWeapon rangedWeapon) {
            range = rangedWeapon.getMaxRange_LMRB(itemStack, this.mob);
        }
        return range * ClassicGunsCompat.getConfig().getShooterRangeFactor();
    }
}
