package net.sistr.lmrbcompat.forge.fn5728;

import fn5728.mod_IFN_FN5728Guns;
import net.sistr.littlemaidrebirth.api.mode.ItemMatcher;
import net.sistr.littlemaidrebirth.api.mode.ItemMatchers;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.lmrbcompat.compat.AbstractCompat;
import net.sistr.lmrbcompat.forge.fn5728.mode.ShooterMode;

public class FN5728Compat extends AbstractCompat<FN5728Config> {
    public static FN5728Compat INSTANCE;

    public FN5728Compat() {
        super("fn5728", FN5728Config.class);
    }

    public void init() {
        super.init();
        INSTANCE = this;
        register("shooter", ModeType
                .<ShooterMode>builder((type, entity) -> new ShooterMode(type, "Shooter", entity))
                .addItemMatcher(ItemMatchers.item(mod_IFN_FN5728Guns.item_fiveseven.get()), ItemMatcher.Priority.HIGH)
                .addItemMatcher(ItemMatchers.item(mod_IFN_FN5728Guns.item_p90.get()), ItemMatcher.Priority.HIGH)
                .build());
    }

    @Override
    public String getName() {
        return "fn5728";
    }

}
