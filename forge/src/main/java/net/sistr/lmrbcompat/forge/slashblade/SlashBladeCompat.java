package net.sistr.lmrbcompat.forge.slashblade;

import static mods.flammpfeil.slashblade.item.ItemSlashBlade.BLADESTATE;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidrebirth.api.mode.ItemMatcher;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.lmrbcompat.compat.AbstractCompat;
import net.sistr.lmrbcompat.forge.slashblade.mode.SlashBladeMode;
import net.sistr.lmrbcompat.mixin.forge.slashblade.MixinLittleMaidEntity;
import net.sistr.lmrbcompat.reflection.ReflectionUtil;

public class SlashBladeCompat extends AbstractCompat<SlashBladeConfig> {
    public static SlashBladeCompat INSTANCE;
    private SlashBladeInputHandler inputHandler;

    @FunctionalInterface
    private interface SlashBladeInputHandler {
        boolean slashInput(ItemStack stack, LivingEntity mob, boolean isR);
    }

    public SlashBladeCompat() {
        super("slashblade", SlashBladeConfig.class);
    }

    public void init() {
        super.init();
        INSTANCE = this;

        // 初期化時にバージョン判定し、適切なハンドラをキャッシュ
        if (ReflectionUtil.isClassExist(
                "mods.flammpfeil.slashblade.capability.slashblade.ComboState")) {
            inputHandler = SlashBladeOriginal::slashInput;
        } else {
            inputHandler = SlashBladeResharped::slashInput;
        }
        register(
                "samurai",
                ModeType.<SlashBladeMode>builder(
                                (type, entity) -> new SlashBladeMode(entity, type, "Samurai"))
                        .addItemMatcher(
                                (stack) ->
                                        stack.getItem() instanceof ItemSlashBlade
                                                && !isBroken(stack),
                                ItemMatcher.Priority.HIGH)
                        .build());
    }

    private boolean isBroken(ItemStack stack) {
        return stack.getCapability(BLADESTATE).map(ISlashBladeState::isBroken).orElse(false);
    }

    @Override
    public String getName() {
        return "SlashBlade";
    }

    public boolean slashInput(ItemStack stack, LivingEntity mob, boolean isR) {
        return inputHandler.slashInput(stack, mob, isR);
    }

    /** Called from {@link MixinLittleMaidEntity} */
    public static void tickLittleMaid(LittleMaidEntity maid) {
        var inv = maid.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof ItemSlashBlade) {
                stack.inventoryTick(maid.getWorld(), maid, i, false);
            }
        }

        var mainHandStack = maid.getMainHandStack();
        if (!mainHandStack.isEmpty() && mainHandStack.getItem() instanceof ItemSlashBlade) {
            mainHandStack.inventoryTick(maid.getWorld(), maid, 0, true);
        }
        var offHandStack = maid.getOffHandStack();
        if (!offHandStack.isEmpty() && offHandStack.getItem() instanceof ItemSlashBlade) {
            offHandStack.inventoryTick(maid.getWorld(), maid, inv.size(), true);
        }
    }
}
