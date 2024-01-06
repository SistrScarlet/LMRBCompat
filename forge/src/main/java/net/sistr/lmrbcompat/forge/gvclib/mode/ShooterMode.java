package net.sistr.lmrbcompat.forge.gvclib.mode;

import gvclib.entity.bullet.EntityB_Bullet;
import gvclib.event.GVCSoundEvent;
import gvclib.item.ItemGunBase;
import gvclib.item.ItemGun_AR;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.sistr.littlemaidrebirth.api.mode.IRangedWeapon;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.lmrbcompat.forge.gvclib.GVCLibCompat;
import net.sistr.lmrbcompat.mode.AbstractShooterMode;

public class ShooterMode extends AbstractShooterMode<ItemGunBase> {

    public ShooterMode(ModeType<ShooterMode> modeType, String name, LittleMaidEntity maid) {
        super(modeType, name, maid);
    }

    @Override
    protected boolean isGunItem(ItemStack stack) {
        return stack.getItem() instanceof ItemGunBase;
    }

    @Override
    protected ItemGunBase castGunItem(ItemStack stack) {
        return ((ItemGunBase) stack.getItem());
    }

    @Override
    protected boolean isFullAuto() {
        return gunItem instanceof ItemGun_AR;
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
        var sound = ForgeRegistries.SOUND_EVENTS.getValue(new Identifier(gunItem.gun_mod_id, gunItem.reload_sound));
        if (sound == null) {
            sound = GVCSoundEvent.getSound(gunItem.reload_sound);
        }
        this.maid.getWorld().playSound(null, this.maid.getX(), this.maid.getY(), this.maid.getZ(),
                sound,
                SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    @Override
    protected void playReloadEndSound() {

    }

    @Override
    protected void shootBullet() {
        var world = this.maid.getWorld();

        for (int pe = 0; pe < gunItem.pellet; ++pe) {
            EntityB_Bullet bullet = new EntityB_Bullet(world, this.maid);
            int ep = EnchantmentHelper.getEquipmentLevel(Enchantments.POWER, this.maid);
            if (gunItem.powor == -1) {
                bullet.flare = true;
            } else {
                bullet.powor = gunItem.powor + ep;
            }
            bullet.setGravity(gunItem.gra);
            bullet.exlevel = gunItem.exlevel;
            int fm = EnchantmentHelper.getEquipmentLevel(Enchantments.FLAME, this.maid);
            if (fm > 0) {
                bullet.flame = true;
            }
            float bbure = gunItem.bure;
            if (this.maid.isInSneakingPose()) {
                bbure = gunItem.bureads;
            }
            bullet.setVelocity(this.maid, this.maid.getPitch(), this.maid.getYaw(), 0.0F, gunItem.speed, bbure);
            if (!world.isClient()) world.spawnEntity(bullet);
        }
    }

    @Override
    protected void shootEffect() {
        double xx11 = 0;
        double zz11 = 0;
        double yy11 = 0;
        float xz;
        if (this.maid.isInSneakingPose()) {
            xz = 0;
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
        xx11 -= MathHelper.sin(this.maid.getHeadYaw() * 0.01745329252F) * zzz;
        zz11 += MathHelper.cos(this.maid.getHeadYaw() * 0.01745329252F) * zzz;
        xx11 -= MathHelper.sin(this.maid.getHeadYaw() * 0.01745329252F + xz) * gunItem.fire_posx;
        zz11 += MathHelper.cos(this.maid.getHeadYaw() * 0.01745329252F + xz) * gunItem.fire_posx;
        yy11 = MathHelper.sqrt((float) (zzz * zzz)) * Math.tan(Math.toRadians(-this.maid.getPitch())) * 1D;
        this.maid.getWorld().addParticle(ParticleTypes.SMOKE,
                this.maid.getX() + xx11, this.maid.getY() + yy + yy11, this.maid.getZ() + zz11,
                0.0D, 0.1D, 0.0D);
    }

    @Override
    protected int getShootIntervalLength() {
        return isFullAuto() ? gunItem.cycle : 10;
    }

    @Override
    protected void playShootSound() {
        var sound = ForgeRegistries.SOUND_EVENTS.getValue(new Identifier(gunItem.gun_mod_id, gunItem.fire_sound));
        if (sound == null) {
            sound = GVCSoundEvent.getSound(gunItem.fire_sound);
        }
        this.maid.getWorld().playSound(null, this.maid.getX(), this.maid.getY(), this.maid.getZ(),
                sound,
                SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    @Override
    protected float getMaxRange(ItemStack itemStack) {
        var item = itemStack.getItem();
        float range = 15;
        if (item instanceof IRangedWeapon rangedWeapon) {
            range = rangedWeapon.getMaxRange_LMRB(itemStack, this.mob);
        }
        return range * GVCLibCompat.getConfig().getShooterRangeFactor();
    }
}
