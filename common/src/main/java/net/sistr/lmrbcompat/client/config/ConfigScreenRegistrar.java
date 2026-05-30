package net.sistr.lmrbcompat.client.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * 設定画面（{@link net.minecraft.client.gui.screen.Screen} を参照する）の登録をクライアント側に隔離するためのクラス。
 *
 * <p>{@code AbstractCompat#init()} は専用サーバーでも実行されるため、{@code Screen} を参照するラムダを 共通メソッド本体に置くと、ラムダの
 * invokedynamic ブートストラップで {@code Screen} がロードされ {@code RuntimeDistCleaner}
 * に弾かれてクラッシュする。画面登録ロジックをこのクラスへ移し、 クライアント環境でのみ呼び出すことで {@code Screen} のロードを回避する。
 */
@Environment(EnvType.CLIENT)
public class ConfigScreenRegistrar {

    public static <T extends ConfigData> void register(
            String uniqueID, String name, Class<T> configClass) {
        ConfigScreenManager.getINSTANCE()
                .register(
                        uniqueID,
                        ConfigScreenInfo.of(
                                "LMRBCompat " + name,
                                "configHub.button." + uniqueID,
                                screen -> AutoConfig.getConfigScreen(configClass, screen).get()));
    }
}
