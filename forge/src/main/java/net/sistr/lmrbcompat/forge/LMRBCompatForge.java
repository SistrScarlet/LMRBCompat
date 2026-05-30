package net.sistr.lmrbcompat.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.sistr.lmrbcompat.LMRBCompat;
import net.sistr.lmrbcompat.client.LMRBCompatClient;
import net.sistr.lmrbcompat.client.config.ConfigScreenManager;
import net.sistr.lmrbcompat.compat.CompatUtil;
import net.sistr.lmrbcompat.forge.classicguns.ClassicGunsCompat;
import net.sistr.lmrbcompat.forge.fn5728.FN5728Compat;
import net.sistr.lmrbcompat.forge.gvclib.GVCLibCompat;
import net.sistr.lmrbcompat.forge.slashblade.SlashBladeCompat;

@Mod(LMRBCompat.MOD_ID)
public class LMRBCompatForge {
    public LMRBCompatForge(FMLJavaModLoadingContext context) {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(LMRBCompat.MOD_ID, context.getModEventBus());
        context.getModEventBus().addListener(this::onCommonSetup);

        if (FMLEnvironment.dist.isClient()) {
            LMRBCompatClient.initClient();

            // ConfigScreenHandler はクライアント専用クラスのため、専用サーバーで参照しないよう
            // クライアント環境でのみ登録する。
            context.registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () ->
                            new ConfigScreenHandler.ConfigScreenFactory(
                                    (client, parent) ->
                                            ConfigScreenManager.getINSTANCE()
                                                    .getConfigScreen(parent)));
        }
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        var modIds =
                FMLLoader.getLoadingModList().getMods().stream().map(ModInfo::getModId).toList();
        CompatUtil.init(modIds);
        LMRBCompat.init();

        LMRBCompat.loadCompat("fn5728", FN5728Compat::new);
        LMRBCompat.loadCompat("classicguns", ClassicGunsCompat::new);
        LMRBCompat.loadCompat("gvclib", GVCLibCompat::new);
        LMRBCompat.loadCompat("slashblade", SlashBladeCompat::new);
    }
}
