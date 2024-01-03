package net.sistr.lmrbcompat.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.sistr.lmrbcompat.LMRBCompat;
import net.fabricmc.api.ModInitializer;
import net.sistr.lmrbcompat.client.LMRBCompatClient;

public class LMRBCompatFabric implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
        LMRBCompat.init();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void onInitializeClient() {
        LMRBCompatClient.initClient();
    }
}