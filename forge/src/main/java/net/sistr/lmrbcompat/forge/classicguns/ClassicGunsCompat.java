package net.sistr.lmrbcompat.forge.classicguns;

import classicguns.CGItemGunBase;
import net.sistr.littlemaidrebirth.api.mode.ItemMatcher;
import net.sistr.littlemaidrebirth.api.mode.ItemMatchers;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.lmrbcompat.compat.AbstractCompat;
import net.sistr.lmrbcompat.forge.classicguns.mode.ShooterMode;

public class ClassicGunsCompat extends AbstractCompat<ClassicGunsConfig> {
    public static ClassicGunsCompat INSTANCE;

    public ClassicGunsCompat() {
        super("classicguns", ClassicGunsConfig.class);
    }

    public void init() {
        super.init();
        INSTANCE = this;
        register("shooter", ModeType
                .<ShooterMode>builder((type, entity) -> new ShooterMode(type, "Shooter", entity))
                .addItemMatcher(ItemMatchers.clazz(CGItemGunBase.class), ItemMatcher.Priority.NORMAL)
                .build());
    }

    @Override
    public String getName() {
        return "ClassicGuns";
    }
}
