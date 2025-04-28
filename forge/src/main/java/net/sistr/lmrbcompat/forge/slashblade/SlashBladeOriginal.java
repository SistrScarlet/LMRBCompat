package net.sistr.lmrbcompat.forge.slashblade;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.InputCommand;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.sistr.lmrbcompat.reflection.ReflectionUtil;

public class SlashBladeOriginal {

    public static boolean slashInput(ItemStack stack, LivingEntity mob, boolean isR) {
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map((state) -> {
                    var input = isR ? InputCommand.R_CLICK : InputCommand.L_CLICK;
                    mob.getCapability(ItemSlashBlade.INPUT_STATE)
                            .ifPresent((s) -> s.getCommands().add(input));
                    // ISlashBladeStateがdefaultだからか、実機だとprogressComboがNoSuchMethodになるため、
                    // リフレクションを使用して呼び出す
                    // Object combo = state.progressCombo(mob);
                    Object combo = ReflectionUtil.exec(state, "progressCombo", LivingEntity.class)
                            .orElseThrow()
                            .exec(mob);
                    mob.getCapability(ItemSlashBlade.INPUT_STATE)
                            .ifPresent((s) -> s.getCommands().remove(input));

                    // ComboState combo = state.progressCombo(playerIn);
                    // combo != ComboState.NONE
                    return combo != ReflectionUtil.getStaticField(
                                    "mods.flammpfeil.slashblade.capability.slashblade.ComboState",
                                    "NONE")
                            .orElse(null);
                }).orElse(false);
    }

}
