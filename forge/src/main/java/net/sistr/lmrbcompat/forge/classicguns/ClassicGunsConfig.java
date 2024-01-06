package net.sistr.lmrbcompat.forge.classicguns;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "lmrbcompat-classicguns")
public class ClassicGunsConfig implements ConfigData {

    private float shooterRangeFactor = 1.0f;

    public float getShooterRangeFactor() {
        return shooterRangeFactor;
    }
}
