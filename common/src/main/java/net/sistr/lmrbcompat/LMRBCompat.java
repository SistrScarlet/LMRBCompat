package net.sistr.lmrbcompat;

import java.util.function.Supplier;
import net.sistr.lmrbcompat.actionarms.ActionArmsCompat;
import net.sistr.lmrbcompat.compat.AbstractCompat;
import net.sistr.lmrbcompat.compat.CompatUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LMRBCompat {
    public static final String MOD_ID = "lmrbcompat";
    public static final Logger LOGGER = LogManager.getLogger();

    public static void init() {
        loadCompat("actionarms", ActionArmsCompat::new);
    }

    public static void loadCompat(String modId, Supplier<AbstractCompat<?>> factory) {
        CompatUtil.ifLoaded(
                modId,
                id -> {
                    try {
                        factory.get().init();
                    } catch (LinkageError e) {
                        LOGGER.warn("Failed to load compat for {}: {}", modId, e.getMessage());
                    }
                });
    }
}
