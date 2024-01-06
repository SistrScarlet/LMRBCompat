package net.sistr.lmrbcompat.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.sistr.lmrbcompat.LMRBCompat;
import net.sistr.lmrbcompat.client.LMRBCompatClient;
import net.sistr.lmrbcompat.client.config.ConfigScreenManager;
import net.sistr.lmrbcompat.forge.util.CompatUtil;
import net.sistr.lmrbcompat.reflection.ReflectionUtil;

@Mod(LMRBCompat.MOD_ID)
public class LMRBCompatForge {
    public LMRBCompatForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(LMRBCompat.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);

        LMRBCompat.init();
        if (FMLEnvironment.dist.isClient()) {
            LMRBCompatClient.initClient();
        }

        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> ConfigScreenManager.getINSTANCE().getConfigScreen(parent)));
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        String path = "net.sistr.lmrbcompat.forge.";
        //ハチャメチャなハードコードであるため、コードにエラーが出た場合でも無事起動できるようにする処置
        //多分リフレクションは無くても良いかも？
        CompatUtil.ifLoaded("fn5728",
                id -> ReflectionUtil.invoke(
                        path + "fn5728.FN5728Compat",
                        "init"));
        CompatUtil.ifLoaded("classicguns",
                id -> ReflectionUtil.invoke(
                        path + "classicguns.ClassicGunsCompat",
                        "init"));
    }
}