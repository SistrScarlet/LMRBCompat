package net.sistr.lmrbcompat.forge.gvclib;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "lmrbcompat-fn5728")
public class GVCLibConfig implements ConfigData {

    private float shooterRangeFactor = 1.0f;

    public float getShooterRangeFactor() {
        return shooterRangeFactor;
    }
}
