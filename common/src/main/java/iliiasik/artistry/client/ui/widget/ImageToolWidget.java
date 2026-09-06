package iliiasik.artistry.client.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import iliiasik.artistry.client.util.ModTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class ImageToolWidget extends AbstractWidget {

    public enum Action { DELETE, PIXELIZE }

    public interface ActionListener {
        void onAction(Action action);
    }

    private final ActionListener listener;
    private final HoverFadeHelper hoverDelete  = new HoverFadeHelper();
    private final HoverFadeHelper hoverPixelize = new HoverFadeHelper();

    public ImageToolWidget(int x, int y, int w, int h, ActionListener listener) {
        super(x, y, w, h, Component.empty());
        this.listener = listener;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    private int btnSize() { return getWidth(); }

    private int gap() { return Math.round(getWidth() * (6.0f / 64.0f)); }

    private int deleteY()   { return getY(); }
    private int pixelizeY() { return getY() + btnSize() + gap(); }

    @Override
    protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        boolean onDelete   = mouseX >= getX() && mouseX < getX() + btnSize()
                && mouseY >= deleteY() && mouseY < deleteY() + btnSize();
        boolean onPixelize = mouseX >= getX() && mouseX < getX() + btnSize()
                && mouseY >= pixelizeY() && mouseY < pixelizeY() + btnSize();

        hoverDelete.update(onDelete);
        hoverPixelize.update(onPixelize);

        hoverDelete.applyShaderColor();
        ctx.blit(ModTextures.DELETE, getX(), deleteY(), btnSize(), btnSize(), 0f, 0f, 64, 64, 64, 64);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        hoverPixelize.applyShaderColor();
        ctx.blit(ModTextures.PIXELIZE, getX(), pixelizeY(), btnSize(), btnSize(), 0f, 0f, 64, 64, 64, 64);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || button != 0) return false;
        if (mouseX >= getX() && mouseX < getX() + btnSize()) {
            if (mouseY >= deleteY() && mouseY < deleteY() + btnSize()) {
                listener.onAction(Action.DELETE);
                return true;
            }
            if (mouseY >= pixelizeY() && mouseY < pixelizeY() + btnSize()) {
                listener.onAction(Action.PIXELIZE);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}
}