package net.sistr.lmrbcompat.client.config;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public class ConfigHubScreen extends Screen {
    private final Screen parentScreen;
    private final ImmutableMap<String, ConfigScreenInfo> map;

    protected ConfigHubScreen(Text title, Screen parentScreen, Map<String, ConfigScreenInfo> map) {
        super(title);
        this.parentScreen = parentScreen;
        this.map = ImmutableMap.copyOf(map);
    }

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(createBackButton());

        int index = 0;

        int width = (int) (this.width * 0.6f);
        int height = (int) (this.height * 0.6f);
        int left = (this.width - width) / 2;
        int top = (this.height - height) / 2;

        int buttonWidth = width / 3;
        int buttonHeight = this.textRenderer.fontHeight * 2;

        for (Map.Entry<String, ConfigScreenInfo> entry : map.entrySet()) {
            var info = entry.getValue();
            var title = Text.translatable(info.getTranslatable());
            var builder = new ButtonWidget.Builder(title,
                    (button) -> this.client.setScreen(info.getScreen(this)));
            builder.position(
                    left + (index % 3) * buttonWidth,
                    top + (index / 3) * buttonHeight
            );
            builder.size(buttonWidth, buttonHeight);
            this.addDrawableChild(builder.build());
            index++;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.shouldCloseOnEsc()) {
            this.client.setScreen(this.parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected ButtonWidget createBackButton() {
        var builder = new ButtonWidget.Builder(Text.of("back"),
                (button) -> this.client.setScreen(this.parentScreen));
        builder.position(5, 5);
        builder.size(40, 20);
        return builder.build();
    }

}
