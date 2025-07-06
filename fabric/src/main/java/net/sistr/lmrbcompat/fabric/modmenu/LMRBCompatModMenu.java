package net.sistr.lmrbcompat.fabric.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.sistr.lmrbcompat.client.config.ConfigScreenManager;

public class LMRBCompatModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> ConfigScreenManager.getINSTANCE().getConfigScreen(parent);
    }
}
