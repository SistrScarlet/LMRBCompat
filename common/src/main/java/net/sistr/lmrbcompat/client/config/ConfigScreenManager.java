package net.sistr.lmrbcompat.client.config;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.TranslatableText;

public class ConfigScreenManager {
    private static final ConfigScreenManager INSTANCE = new ConfigScreenManager("lmrbcompat");
    private final String id;
    private final Object2ObjectMap<String, ConfigScreenInfo> map = new Object2ObjectOpenHashMap<>();

    public ConfigScreenManager(String id) {
        this.id = id;
    }

    public static ConfigScreenManager getINSTANCE() {
        return INSTANCE;
    }

    public void register(String id, ConfigScreenInfo screenInfo) {
        map.put(id, screenInfo);
    }

    public Screen getConfigScreen(Screen parent) {
        return new ConfigHubScreen(new TranslatableText("config." + id + ".title"), parent, this.map);
    }
}
