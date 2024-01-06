package net.sistr.lmrbcompat.forge.gvclib;

import gvclib.item.ItemGunBase;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.api.mode.ItemMatcher;
import net.sistr.littlemaidrebirth.api.mode.ItemMatchers;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.lmrbcompat.client.config.ConfigScreenInfo;
import net.sistr.lmrbcompat.client.config.ConfigScreenManager;
import net.sistr.lmrbcompat.forge.gvclib.mode.ShooterMode;

public class GVCLibCompat {
    private static ConfigHolder<GVCLibConfig> CONFIG_HOLDER;

    public void init() {
        register("shooter", ModeType
                .<ShooterMode>builder((type, entity) -> new ShooterMode(type, "Shooter", entity))
                .addItemMatcher(ItemMatchers.clazz(ItemGunBase.class), ItemMatcher.Priority.NORMAL)
                .build());

        AutoConfig.register(GVCLibConfig.class, GsonConfigSerializer::new);
        CONFIG_HOLDER = AutoConfig.getConfigHolder(GVCLibConfig.class);

        ConfigScreenManager.getINSTANCE().register("lmrbcompat-gvclib",
                ConfigScreenInfo.of("LMRBCompat GVCLib",
                        "configHub.button.lmrbcompat-gvclib",
                        screen -> AutoConfig.getConfigScreen(GVCLibConfig.class, screen).get()));
    }

    private static void register(String id, ModeType<?> modeType) {
        ModeManager.INSTANCE.register(new Identifier("lmrbcompat-gvclib", id), modeType);
    }

    public static GVCLibConfig getConfig() {
        return CONFIG_HOLDER.getConfig();
    }

}
