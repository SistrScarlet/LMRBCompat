package net.sistr.lmrbcompat.compat;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.lmrbcompat.LMRBCompat;
import net.sistr.lmrbcompat.client.config.ConfigScreenInfo;
import net.sistr.lmrbcompat.client.config.ConfigScreenManager;

public abstract class AbstractCompat<T extends ConfigData> {
    protected final String compatId;
    protected final Class<T> configClass;
    protected ConfigHolder<T> CONFIG_HOLDER;

    public AbstractCompat(String compatId, Class<T> configClass) {
        this.compatId = compatId;
        this.configClass = configClass;
    }

    public void init() {
        AutoConfig.register(configClass, GsonConfigSerializer::new);
        CONFIG_HOLDER = AutoConfig.getConfigHolder(configClass);

        var uniqueID = getUniqueID();

        ConfigScreenManager.getINSTANCE().register(uniqueID,
                ConfigScreenInfo.of("LMRBCompat " + getName(),
                        "configHub.button." + uniqueID,
                        screen -> AutoConfig.getConfigScreen(configClass, screen).get()));
    }

    protected void register(String id, ModeType<?> modeType) {
        ModeManager.INSTANCE.register(new Identifier(getUniqueID(), id), modeType);
    }

    public String getUniqueID() {
        return LMRBCompat.MOD_ID + "-" + compatId;
    }

    public String getCompatId() {
        return compatId;
    }

    public T getConfig() {
        return CONFIG_HOLDER.getConfig();
    }

    public abstract String getName();

}
