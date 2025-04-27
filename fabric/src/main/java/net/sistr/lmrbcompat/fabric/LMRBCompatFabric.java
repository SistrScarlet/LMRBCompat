package net.sistr.lmrbcompat.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.sistr.lmrbcompat.LMRBCompat;
import net.fabricmc.api.ModInitializer;
import net.sistr.lmrbcompat.client.LMRBCompatClient;
import net.sistr.lmrbcompat.compat.CompatUtil;

public class LMRBCompatFabric implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
        LMRBCompat.init();
        var modIds = FabricLoader.getInstance().getAllMods()
                .stream()
                .map(mod -> mod.getMetadata().getId())
                .toList();
        CompatUtil.init(modIds);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void onInitializeClient() {
        LMRBCompatClient.initClient();
    }
}