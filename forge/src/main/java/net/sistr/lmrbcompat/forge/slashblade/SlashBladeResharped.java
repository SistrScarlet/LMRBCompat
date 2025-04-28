package net.sistr.lmrbcompat.forge.slashblade;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.InputCommand;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import net.sistr.lmrbcompat.reflection.ReflectionUtil;

public class SlashBladeResharped {

    public static boolean slashInput(ItemStack stack, LivingEntity mob, boolean isR) {
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map((state) -> {
                    var input = isR ? InputCommand.R_CLICK : InputCommand.L_CLICK;
                    mob.getCapability(ItemSlashBlade.INPUT_STATE)
                            .ifPresent((s) -> s.getCommands().add(input));
                    Object combo = state.progressCombo(mob);
                    mob.getCapability(ItemSlashBlade.INPUT_STATE)
                            .ifPresent((s) -> s.getCommands().remove(input));

                    // Identifier combo = state.progressCombo(playerIn);
                    // !combo.equals(ComboStateRegistry.NONE.getId())

                    return ReflectionUtil.getStaticField(
                                    "mods.flammpfeil.slashblade.registry.ComboStateRegistry",
                                    "NONE")
                            .map(o -> ((RegistryObject<?>) o).getId())
                            .map(comboState -> !combo.equals(comboState))
                            .orElse(false);
                }).orElse(false);
    }
}
