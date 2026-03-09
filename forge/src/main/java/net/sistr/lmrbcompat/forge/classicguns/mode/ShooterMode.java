package net.sistr.lmrbcompat.forge.classicguns.mode;

import classicguns.*;
import java.util.Optional;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Arm;
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
    protected boolean isWeaponItem(ItemStack stack) {
        return stack.getItem() instanceof CGItemGunBase;
    }

    @Override
    protected Optional<CGItemGunBase> getWeaponInstance(ItemStack stack) {
        return Optional.of(((CGItemGunBase) stack.getItem()));
    }

    @Override
    protected boolean isFullAuto() {
        return weapon instanceof CGItemGun_AR;
    }

    @Override
    protected boolean shouldReload() {
        return getAmmoAmount() <= 0;
    }

    @Override
    protected boolean isAmmo(ItemStack stack) {
        return weapon.isAmmo(stack);
    }

    @Override
    protected boolean isMagazineReload() {
        return false;
    }

    @Override
    protected int getReloadLength() {
        return weapon.reloadtime;
    }

    @Override
    protected boolean isInfinity() {
        return EnchantmentHelper.getEquipmentLevel(Enchantments.INFINITY, this.maid) > 0;
    }

    @Override
    protected int getMaxAmmoAmount() {
        return weaponStack.getMaxDamage() - 1;
    }

    @Override
    protected int getAmmoAmount() {
        return getMaxAmmoAmount() - weaponStack.getDamage();
    }

    @Override
    protected void setAmmoAmount(int amount) {
        weaponStack.setDamage(getMaxAmmoAmount() - amount);
    }

    @Override
    protected void playReloadStartSound() {
        this.maid
                .getWorld()
                .playSound(
                        null,
                        this.maid.getX(),
                        this.maid.getY(),
                        this.maid.getZ(),
                        CGSoundEvent.getSound(weapon.reload_sound),
                        SoundCategory.NEUTRAL,
                        1.0F,
                        1.0F);
    }

    @Override
    protected void playReloadEndSound() {}

    @Override
    protected void shootBullet() {
        World world = this.maid.getWorld();
        world.playSound(
                null,
                this.maid.getX(),
                this.maid.getY(),
                this.maid.getZ(),
                CGSoundEvent.getSound(weapon.fire_sound),
                SoundCategory.NEUTRAL,
                3.0F,
                1.0F);

        for (int pe = 0; pe < weapon.pellet; ++pe) {
            CGEntityBullet bullet = new CGEntityBullet(world, this.maid);
            int ep = EnchantmentHelper.getEquipmentLevel(Enchantments.POWER, this.maid);
            if (weapon.powor == -1) {
                bullet.flare = true;
            } else {
                bullet.powor = weapon.powor + ep;
            }

            bullet.setGravity(weapon.gra);
            bullet.exlevel = weapon.exlevel;
            int fm = EnchantmentHelper.getEquipmentLevel(Enchantments.FLAME, this.maid);
            if (fm > 0) {
                bullet.flame = true;
            }

            float bbure = weapon.bure;
            if (this.maid.isInSneakingPose()) {
                bbure = weapon.bureads;
            }

            bullet.setVelocity(
                    this.maid, this.maid.getPitch(), this.maid.getYaw(), 0.0F, weapon.speed, bbure);
            if (!world.isClient) {
                world.spawnEntity(bullet);
            }
        }

        double xx11 = weapon.recoil;
        if (this.maid.isInSneakingPose()) {
            xx11 = weapon.recoilads;
        }

        double zz11 = this.maid.getPitch();
        zz11 += (double) (world.random.nextFloat() * -2.0F) * xx11;
        this.maid.setPitch((float) zz11);
        xx11 = 0.0;
        zz11 = 0.0;
        double yy11 = 0.0;
        float xz = 1.57F;
        if (this.maid.isInSneakingPose()) {
            xz = 0;
        } else {
            if (this.maid.getMainArm() == Arm.RIGHT) {
                xz = 1.57F;
            } else {
                xz = -1.57F;
            }
        }

        double yy = weapon.fire_posy;
        if (this.maid.isInSneakingPose()) {
            yy = weapon.fire_posy - 0.2F;
        }

        double zzz = (double) weapon.fire_posz * Math.cos(Math.toRadians(-this.maid.getPitch()));
        xx11 -= (double) MathHelper.sin(this.maid.headYaw * 0.017453292F) * zzz;
        zz11 += (double) MathHelper.cos(this.maid.headYaw * 0.017453292F) * zzz;
        xx11 -= MathHelper.sin(this.maid.headYaw * 0.017453292F + xz) * weapon.fire_posx;
        zz11 += MathHelper.cos(this.maid.headYaw * 0.017453292F + xz) * weapon.fire_posx;
        yy11 =
                (double) MathHelper.sqrt((float) (zzz * zzz))
                        * Math.tan(Math.toRadians(-this.maid.getPitch()))
                        * 1.0;
        world.addParticle(
                ParticleTypes.SMOKE,
                this.maid.getX() + xx11,
                this.maid.getY() + yy + yy11,
                this.maid.getZ() + zz11,
                0.0,
                0.1,
                0.0);

        this.weaponStack.damage(
                1, this.maid, (e) -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
    }

    @Override
    protected void shootEffect() {}

    @Override
    protected int getShootIntervalLength() {
        return weapon instanceof CGItemGun_SR ? 10 : weapon.getCycleCount(weaponStack) + 2;
    }

    @Override
    protected void playShootSound() {
        this.maid
                .getWorld()
                .playSound(
                        null,
                        this.maid.getX(),
                        this.maid.getY(),
                        this.maid.getZ(),
                        CGSoundEvent.getSound(weapon.fire_sound),
                        SoundCategory.NEUTRAL,
                        1.0F,
                        1.0F);
    }

    @Override
    protected float getMaxRange(ItemStack itemStack) {
        var item = itemStack.getItem();
        float range = 15;
        if (item instanceof IRangedWeapon rangedWeapon) {
            range = rangedWeapon.getMaxRange_LMRB(itemStack, this.mob);
        }
        return range * ClassicGunsCompat.INSTANCE.getConfig().getShooterRangeFactor();
    }
}
