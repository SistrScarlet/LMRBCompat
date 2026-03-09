package net.sistr.lmrbcompat;

import net.sistr.lmrbcompat.compat.CompatUtil;
import net.sistr.lmrbcompat.reflection.ReflectionUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LMRBCompat {
    public static final String MOD_ID = "lmrbcompat";
    public static final Logger LOGGER = LogManager.getLogger();

    public static void init() {
        loadCompat("actionarms", "ActionArmsCompat");
    }

    private static void loadCompat(String modId, String compatPath) {
        String basePath = "net.sistr.lmrbcompat.";
        CompatUtil.ifLoaded(
                modId,
                id ->
                        ReflectionUtil.execWithInstancing(
                                basePath + modId + "." + compatPath, "init"));
    }
}
