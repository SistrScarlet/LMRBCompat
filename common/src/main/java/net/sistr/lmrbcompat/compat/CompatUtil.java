package net.sistr.lmrbcompat.compat;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

public class CompatUtil {
    private static Set<String> modIds;

    public static void init(Collection<String> modIds) {
        CompatUtil.modIds = ImmutableSet.copyOf(modIds);
    }

    public static boolean isModLoaded(String modId) {
        return modIds.contains(modId);
    }

    public static void ifLoaded(String modId, Consumer<String> exec) {
        if (isModLoaded(modId)) {
            exec.accept(modId);
        }
    }

}
