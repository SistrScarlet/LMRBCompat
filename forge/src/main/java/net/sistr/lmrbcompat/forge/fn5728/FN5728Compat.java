package net.sistr.lmrbcompat.forge.fn5728;

import fn5728.mod_IFN_FN5728Guns;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.sistr.littlemaidrebirth.api.mode.ItemMatcher;
import net.sistr.littlemaidrebirth.api.mode.ItemMatchers;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.lmrbcompat.forge.fn5728.mode.ShooterMode;

public class FN5728Compat {
    private static ConfigHolder<FN5728Config> CONFIG_HOLDER;

    public void init() {
        register("shooter", ModeType
                .<ShooterMode>builder((type, entity) -> new ShooterMode(type, "Shooter", entity))
                .addItemMatcher(ItemMatchers.item(mod_IFN_FN5728Guns.item_fiveseven.get()), ItemMatcher.Priority.HIGH)
                .addItemMatcher(ItemMatchers.item(mod_IFN_FN5728Guns.item_p90.get()), ItemMatcher.Priority.HIGH)
                .build());

        AutoConfig.register(FN5728Config.class, GsonConfigSerializer::new);
        CONFIG_HOLDER = AutoConfig.getConfigHolder(FN5728Config.class);

        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> AutoConfig.getConfigScreen(FN5728Config.class, parent).get()));
    }

    private static void register(String id, ModeType<?> modeType) {
        ModeManager.INSTANCE.register(new Identifier("lmrbcompat-fn5728", id), modeType);
    }

    public static FN5728Config getConfig() {
        return CONFIG_HOLDER.getConfig();
    }

}
