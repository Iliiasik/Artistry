package iliiasik.artistry.client.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.client.util.ModTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class SignButtonWidget extends AbstractWidget {

    private static final int TEXTURE_WIDTH = PaintDimensions.SIGN_TEXTURE_WIDTH;
    private static final int TEXTURE_HEIGHT = PaintDimensions.SIGN_TEXTURE_HEIGHT;

    private final Runnable onSign;
    private final HoverFadeHelper hoverFade = new HoverFadeHelper();

    public SignButtonWidget(int x, int y, int w, int h, Runnable onSign) {
        super(x, y, w, h, Component.empty());
        this.onSign = onSign;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setSize(int w, int h) {
        this.width = w;
        this.height = h;
    }

    @Override
    protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        hoverFade.update(isHovered());
        hoverFade.applyShaderColor();
        ctx.blit(ModTextures.SIGN,
                getX(), getY(),
                getWidth(), getHeight(),
                0f, 0f,
                TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            onSign.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && super.isMouseOver(mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}
}
