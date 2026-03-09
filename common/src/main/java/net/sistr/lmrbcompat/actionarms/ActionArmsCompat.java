package net.sistr.lmrbcompat.actionarms;

import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.littlemaidrebirth.api.mode.ItemMatcher;
import net.sistr.littlemaidrebirth.api.mode.ItemMatchers;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.lmrbcompat.actionarms.mode.ShooterMode;
import net.sistr.lmrbcompat.compat.AbstractCompat;

public class ActionArmsCompat extends AbstractCompat<ActionArmsConfig> {
    public static ActionArmsCompat INSTANCE;

    public ActionArmsCompat() {
        super("actionarms", ActionArmsConfig.class);
    }

    public void init() {
        super.init();
        INSTANCE = this;
        register(
                "shooter",
                ModeType.<ShooterMode>builder(
                                (type, entity) -> new ShooterMode(type, "Shooter", entity))
                        .addItemMatcher(
                                ItemMatchers.clazz(LeverActionGunItem.class),
                                ItemMatcher.Priority.HIGH)
                        .build());
    }

    @Override
    public String getName() {
        return "actionarms";
    }
}
