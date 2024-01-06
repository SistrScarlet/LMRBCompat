package net.sistr.lmrbcompat.forge.classicguns;

import classicguns.CGItemGunBase;
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
import net.sistr.lmrbcompat.forge.classicguns.mode.ShooterMode;

public class ClassicGunsCompat {
    private static ConfigHolder<ClassicGunsConfig> CONFIG_HOLDER;

    public void init() {
        register("shooter", ModeType
                .<ShooterMode>builder((type, entity) -> new ShooterMode(type, "Shooter", entity))
                .addItemMatcher(ItemMatchers.clazz(CGItemGunBase.class), ItemMatcher.Priority.NORMAL)
                .build());

        AutoConfig.register(ClassicGunsConfig.class, GsonConfigSerializer::new);
        CONFIG_HOLDER = AutoConfig.getConfigHolder(ClassicGunsConfig.class);

        ConfigScreenManager.getINSTANCE().register("lmrbcompat-classicguns",
                ConfigScreenInfo.of("LMRBCompat ClassicGuns",
                        "configHub.button.lmrbcompat-classicguns",
                        screen -> AutoConfig.getConfigScreen(ClassicGunsConfig.class, screen).get()));
    }

    private static void register(String id, ModeType<?> modeType) {
        ModeManager.INSTANCE.register(new Identifier("lmrbcompat-classicguns", id), modeType);
    }

    public static ClassicGunsConfig getConfig() {
        return CONFIG_HOLDER.getConfig();
    }
}
