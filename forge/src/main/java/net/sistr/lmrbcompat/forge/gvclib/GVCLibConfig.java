package net.sistr.lmrbcompat.forge.gvclib;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "lmrbcompat-gvclib")
public class GVCLibConfig implements ConfigData {
    public float shooterRangeFactor = 1.0f;
}
