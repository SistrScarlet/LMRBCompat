package net.sistr.lmrbcompat.forge.util;

import com.google.common.collect.ImmutableSet;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import java.util.Set;
import java.util.function.Consumer;

public class CompatUtil {
    private static CompatUtil INSTANCE;
    private final Set<String> set;

    public CompatUtil() {
        var builder = new ImmutableSet.Builder<String>();
        for (ModInfo info : FMLLoader.getLoadingModList().getMods()) {
            builder.add(info.getModId());
        }
        this.set = builder.build();
    }

    public static boolean isModLoaded(String modId) {
        var instance = getInstance();
        return instance.set.contains(modId);
    }

    public static void ifLoaded(String modId, Consumer<String> exec) {
        if (isModLoaded(modId)) {
            exec.accept(modId);
        }
    }

    private static CompatUtil getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CompatUtil();
        }
        return INSTANCE;
    }

}
