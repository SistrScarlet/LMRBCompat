package net.sistr.lmrbcompat.forge.gvclib.mode;

import gvclib.entity.bullet.*;
import gvclib.event.GVCSoundEvent;
import gvclib.item.ItemAttachment;
import gvclib.item.ItemGunBase;
import gvclib.item.ItemGun_SR;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import net.sistr.littlemaidrebirth.api.mode.IRangedWeapon;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.lmrbcompat.forge.gvclib.GVCLibCompat;
import net.sistr.lmrbcompat.mode.AbstractShooterMode;

import java.util.Optional;

public class ShooterMode extends AbstractShooterMode<ItemGunBase> {

    public ShooterMode(ModeType<ShooterMode> modeType, String name, LittleMaidEntity maid) {
        super(modeType, name, maid);
    }

    @Override
    protected boolean isWeaponItem(ItemStack stack) {
        return stack.getItem() instanceof ItemGunBase;
    }

    @Override
    protected Optional<ItemGunBase> getWeaponInstance(ItemStack stack) {
        return Optional.of(((ItemGunBase) stack.getItem()));
    }

    @Override
    protected boolean isFullAuto() {
        return weapon instanceof ItemGun_SR;
    }

    @Override
    protected boolean shouldReload() {
        return getAmmoAmount() <= 0;
    }

    @Override
    protected boolean isAmmo(ItemStack stack) {
        return stack.getItem() == weapon.magazine;
    }

    @Override
    protected boolean isMagazineReload() {
        return true;
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
        var soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(new Identifier(weapon.gun_mod_id, weapon.reload_sound));
        if (soundEvent == null) {
            return;
        }
        this.maid.getWorld().playSound(null, this.maid.getX(), this.maid.getY(), this.maid.getZ(),
                soundEvent,
                SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    @Override
    protected void playReloadEndSound() {
    }

    @Override
    protected void shootBullet() {
        fireBullet(weaponStack, this.maid.getWorld(), this.maid);
    }

    protected void fireBullet(ItemStack stack, World worldIn, LivingEntity entity) {
        if (!weapon.isReload(stack)) {
            ItemAttachment supp = weapon.getAttachment(weapon, stack, 8);
            SoundEvent gun = ForgeRegistries.SOUND_EVENTS.getValue(new Identifier(weapon.gun_mod_id, weapon.fire_sound));
            if (supp != null) {
                SoundEvent gunsupp = ForgeRegistries.SOUND_EVENTS.getValue(new Identifier(weapon.gun_mod_id, weapon.fire_sound_supp));
                if (gun != null) {
                    worldIn.playSound(null, entity.getX(), entity.getY(), entity.getZ(), gunsupp, SoundCategory.NEUTRAL, 3.0F, 1.0F);
                } else {
                    worldIn.playSound(null, entity.getX(), entity.getY(), entity.getZ(), GVCSoundEvent.getSound(weapon.fire_sound_supp), SoundCategory.NEUTRAL, 3.0F, 1.0F);
                }
            } else if (gun != null) {
                worldIn.playSound(null, entity.getX(), entity.getY(), entity.getZ(), gun, SoundCategory.NEUTRAL, 3.0F, 1.0F);
            } else {
                worldIn.playSound(null, entity.getX(), entity.getY(), entity.getZ(), GVCSoundEvent.getSound(weapon.fire_sound), SoundCategory.NEUTRAL, 3.0F, 1.0F);
            }

            for (int pe = 0; pe < weapon.pellet; ++pe) {
                EntityBBase bullet = getBullet(worldIn, entity);

                int ep = EnchantmentHelper.getEquipmentLevel(Enchantments.POWER, entity);
                if (weapon.powor == -1) {
                    bullet.flare = true;
                } else {
                    bullet.powor = weapon.powor + ep;
                }

                bullet.setGravity(weapon.gra);
                bullet.exlevel = weapon.exlevel;
                int fm = EnchantmentHelper.getEquipmentLevel(Enchantments.FLAME, entity);
                if (fm > 0) {
                    bullet.flame = true;
                }

                float bbure = weapon.bure;
                if (entity.isInSneakingPose()) {
                    bbure = weapon.bureads;
                }

                bullet.setModel(weapon.bullet_model);
                bullet.setTex(weapon.bullet_tex);
                bullet.setVelocity(entity, entity.getPitch(), entity.getYaw(), 0.0F, weapon.speed, bbure);
                if (!worldIn.isClient) {
                    worldIn.spawnEntity(bullet);
                }
            }

            double reco = weapon.recoil;
            if (entity.isInSneakingPose()) {
                reco = weapon.recoilads;
            }

            double xro = entity.getPitch();
            xro += (double) (worldIn.random.nextFloat() * -2.0F) * reco;
            entity.setPitch((float) xro);
            reco = 0.0F;
            xro = 0.0F;
            double yy11;
            float xz = getXz(stack, entity);

            double yy = weapon.fire_posy;
            if (entity.isInSneakingPose()) {
                yy = (weapon.fire_posy - 0.3F);
            }

            double zzz = (double) weapon.fire_posz * Math.cos(Math.toRadians(-entity.getPitch()));
            reco -= (double) MathHelper.sin(entity.headYaw * ((float) Math.PI / 180F)) * zzz;
            xro += (double) MathHelper.cos(entity.headYaw * ((float) Math.PI / 180F)) * zzz;
            reco -= (MathHelper.sin(entity.headYaw * ((float) Math.PI / 180F) + xz) * weapon.fire_posx);
            xro += (MathHelper.cos(entity.headYaw * ((float) Math.PI / 180F) + xz) * weapon.fire_posx);
            yy11 = (double) MathHelper.sqrt((float) (zzz * zzz)) * Math.tan(Math.toRadians(-entity.getPitch())) * (double) 1.0F;
            if (supp == null) {
                EntityT_Flash flash = new EntityT_Flash(worldIn, entity);
                flash.gra = 0.03F;
                flash.timemax = 1;
                flash.setModel(weapon.bulletf_model);
                flash.setTex(weapon.bulletf_tex);
                flash.refreshPositionAndAngles(entity.getX() + reco, entity.getY() + (yy - (double) 0.0F) + yy11, entity.getZ() + xro, entity.headYaw, entity.prevPitch);
                flash.setVelocity(entity, entity.getPitch(), entity.getYaw(), 0.0F, 0.2F, 1.0F);
                if (!worldIn.isClient) {
                    worldIn.spawnEntity(flash);
                }
            }
            stack.damage(1, entity, l -> l.sendToolBreakStatus(entity.getActiveHand()));
        }
    }

    private EntityBBase getBullet(World worldIn, LivingEntity entity) {
        EntityBBase bullet;
        switch (weapon.gun_type) {
            case 1 -> bullet = new EntityB_HE(worldIn, entity);
            case 2 -> bullet = new EntityB_HEAT(worldIn, entity);
            case 3 -> bullet = new EntityB_AP(worldIn, entity);
            case 4, 8 -> bullet = new EntityB_Missile(worldIn, entity);
            case 6 -> bullet = new EntityB_Fire(worldIn, entity);
            default -> bullet = new EntityB_Bullet(worldIn, entity);
        }
        return bullet;
    }

    private static float getXz(ItemStack stack, LivingEntity entity) {
        float xz = 1.57F;
        if (!entity.getMainHandStack().isEmpty() && !entity.getOffHandStack().isEmpty()) {
            if (entity.getMainHandStack() == stack) {
                xz = 1.57F;
            } else if (entity.getOffHandStack() == stack) {
                xz = -1.57F;
            }
        } else if (entity.isInSneakingPose()) {
            xz = 0.0F;
        } else if (entity.getMainHandStack() == stack) {
            xz = 1.57F;
        } else if (entity.getOffHandStack() == stack) {
            xz = -1.57F;
        }
        return xz;
    }

    @Override
    protected void shootEffect() {
        //パーティクル
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
        double yy = weapon.fire_posy;
        if (this.maid.isInSneakingPose()) {
            yy = weapon.fire_posy - 0.2F;
        }
        double zzz = weapon.fire_posz * Math.cos(Math.toRadians(-this.maid.getPitch()));
        var rad = MathHelper.PI / 180f;
        xx11 -= MathHelper.sin(this.maid.getHeadYaw() * rad) * zzz;
        zz11 += MathHelper.cos(this.maid.getHeadYaw() * rad) * zzz;
        xx11 -= MathHelper.sin(this.maid.getHeadYaw() * rad + xz) * weapon.fire_posx;
        zz11 += MathHelper.cos(this.maid.getHeadYaw() * rad + xz) * weapon.fire_posx;
        yy11 = MathHelper.sqrt((float) (zzz * zzz)) * Math.tan(Math.toRadians(-this.maid.getPitch())) * 1D;
        this.maid.getWorld().addParticle(ParticleTypes.SMOKE,
                this.maid.getX() + xx11,
                this.maid.getY() + yy + yy11,
                this.maid.getZ() + zz11,
                0.0D, 0.1D, 0.0D);
    }

    @Override
    protected int getShootIntervalLength() {
        return weapon.cycle;
    }

    @Override
    protected void playShootSound() {
        var soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(new Identifier(weapon.gun_mod_id, weapon.fire_sound));
        if (soundEvent == null) {
            return;
        }
        this.maid.getWorld().playSound(null, this.maid.getX(), this.maid.getY(), this.maid.getZ(),
                soundEvent,
                SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    @Override
    protected float getMaxRange(ItemStack itemStack) {
        var item = itemStack.getItem();
        float range = 15;
        if (item instanceof IRangedWeapon rangedWeapon) {
            range = rangedWeapon.getMaxRange_LMRB(itemStack, this.mob);
        }
        return range * GVCLibCompat.INSTANCE.getConfig().shooterRangeFactor;
    }
}
