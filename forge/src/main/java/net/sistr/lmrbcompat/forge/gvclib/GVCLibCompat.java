package net.sistr.lmrbcompat.forge.gvclib;

import gvclib.item.ItemGunBase;
import net.sistr.littlemaidrebirth.api.mode.ItemMatcher;
import net.sistr.littlemaidrebirth.api.mode.ItemMatchers;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.lmrbcompat.compat.AbstractCompat;
import net.sistr.lmrbcompat.forge.gvclib.mode.ShooterMode;

public class GVCLibCompat extends AbstractCompat<GVCLibConfig> {
    public static GVCLibCompat INSTANCE;

    public GVCLibCompat() {
        super("gvclib", GVCLibConfig.class);
    }

    public void init() {
        super.init();
        INSTANCE = this;
        register("shooter", ModeType
                .<ShooterMode>builder((type, entity) -> new ShooterMode(type, "Shooter", entity))
                .addItemMatcher(ItemMatchers.clazz(ItemGunBase.class), ItemMatcher.Priority.HIGH)
                .build());
    }

    @Override
    public String getName() {
        return "GVCLib";
    }
}
