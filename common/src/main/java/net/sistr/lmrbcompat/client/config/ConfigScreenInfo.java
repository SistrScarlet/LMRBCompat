package net.sistr.lmrbcompat.client.config;

import net.minecraft.client.gui.screen.Screen;

import java.util.function.Function;

public class ConfigScreenInfo {
    private final String name;
    private final String translatable;
    private final Function<Screen, Screen> screenFactory;

    protected ConfigScreenInfo(String name, String translatable, Function<Screen, Screen> screenFactory) {
        this.name = name;
        this.translatable = translatable;
        this.screenFactory = screenFactory;
    }

    public static ConfigScreenInfo of(String name, String translatable, Function<Screen, Screen> screenFactory) {
        return new ConfigScreenInfo(name, translatable, screenFactory);
    }

    public String getName() {
        return name;
    }

    public String getTranslatable() {
        return translatable;
    }

    public Screen getScreen(Screen parent) {
        return screenFactory.apply(parent);
    }
}
