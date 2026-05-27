package iliiasik.artistry.client.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import iliiasik.artistry.client.util.ModTextures;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class ImageToolWidget extends ClickableWidget {

    public enum Action { DELETE, PIXELIZE }

    public interface ActionListener {
        void onAction(Action action);
    }

    private final ActionListener listener;
    private final HoverFadeHelper hoverDelete = new HoverFadeHelper();
    private final HoverFadeHelper hoverPixelize = new HoverFadeHelper();
    private boolean visible = false;

    public ImageToolWidget(int x, int y, int w, int h, ActionListener listener) {
        super(x, y, w, h, Text.empty());
        this.listener = listener;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        int btnH = getHeight() / 2;
        int half = getWidth();

        boolean onDelete   = mouseX >= getX() && mouseX < getX() + half && mouseY >= getY() && mouseY < getY() + btnH;
        boolean onPixelize = mouseX >= getX() && mouseX < getX() + half && mouseY >= getY() + btnH && mouseY < getY() + getHeight();

        hoverDelete.update(onDelete);
        hoverPixelize.update(onPixelize);

        hoverDelete.applyShaderColor();
        ctx.drawTexture(ModTextures.DELETE, getX(), getY(), half, btnH, 0f, 0f, 64, 64, 64, 64);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        hoverPixelize.applyShaderColor();
        ctx.drawTexture(ModTextures.PIXELIZE, getX(), getY() + btnH, half, btnH, 0f, 0f, 64, 64, 64, 64);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || button != 0) return false;
        int btnH = getHeight() / 2;
        if (mouseX >= getX() && mouseX < getX() + getWidth()) {
            if (mouseY >= getY() && mouseY < getY() + btnH) {
                listener.onAction(Action.DELETE);
                return true;
            }
            if (mouseY >= getY() + btnH && mouseY < getY() + getHeight()) {
                listener.onAction(Action.PIXELIZE);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}