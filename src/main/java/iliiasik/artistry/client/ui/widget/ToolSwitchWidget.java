package iliiasik.artistry.client.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import iliiasik.artistry.client.ClientServerSettings;
import iliiasik.artistry.client.tools.DrawingTool;
import iliiasik.artistry.client.util.ModTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class ToolSwitchWidget extends AbstractWidget {

    private DrawingTool activeTool = DrawingTool.BRUSH;
    private final Consumer<DrawingTool> onToolChanged;
    private boolean visible = true;

    private final HoverFadeHelper hoverBrush   = new HoverFadeHelper();
    private final HoverFadeHelper hoverEraser  = new HoverFadeHelper();
    private final HoverFadeHelper hoverPipette = new HoverFadeHelper();
    private final HoverFadeHelper hoverImage   = new HoverFadeHelper();

    public ToolSwitchWidget(int x, int y, int w, int h, Consumer<DrawingTool> onToolChanged) {
        super(x, y, w, h, Component.empty());
        this.onToolChanged = onToolChanged;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    private int btnSize() {
        return getWidth();
    }

    private int gap() {
        return Math.round(getWidth() * (6.0f / 64.0f));
    }

    private int brushY()   { return getY(); }
    private int eraserY()  { return getY() + btnSize() + gap(); }
    private int pipetteY() { return getY() + btnSize() * 2 + gap() * 2; }
    private int imageY()   { return getY() + btnSize() * 3 + gap() * 3; }

    private boolean overBrush(double mx, double my) {
        return mx >= getX() && mx < getX() + btnSize() && my >= brushY() && my < brushY() + btnSize();
    }
    private boolean overEraser(double mx, double my) {
        return mx >= getX() && mx < getX() + btnSize() && my >= eraserY() && my < eraserY() + btnSize();
    }
    private boolean overPipette(double mx, double my) {
        return mx >= getX() && mx < getX() + btnSize() && my >= pipetteY() && my < pipetteY() + btnSize();
    }
    private boolean overImage(double mx, double my) {
        return mx >= getX() && mx < getX() + btnSize() && my >= imageY() && my < imageY() + btnSize();
    }

    @Override
    protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        hoverBrush.update(overBrush(mouseX, mouseY));
        hoverEraser.update(overEraser(mouseX, mouseY));
        hoverPipette.update(overPipette(mouseX, mouseY));

        drawTool(ctx, brushY(),   hoverBrush,   activeTool == DrawingTool.BRUSH,
                ModTextures.BRUSH,   ModTextures.BRUSH_ACTIVE);
        drawTool(ctx, eraserY(),  hoverEraser,  activeTool == DrawingTool.ERASER,
                ModTextures.ERASER,  ModTextures.ERASER_ACTIVE);
        drawTool(ctx, pipetteY(), hoverPipette, activeTool == DrawingTool.PIPETTE,
                ModTextures.PIPETTE, ModTextures.PIPETTE_ACTIVE);

        if (!ClientServerSettings.imagesDisabled()) {
            hoverImage.update(overImage(mouseX, mouseY));
            drawTool(ctx, imageY(), hoverImage, false,
                    ModTextures.IMAGE, ModTextures.IMAGE);
        }
    }

    private void drawTool(GuiGraphics ctx, int y, HoverFadeHelper fade, boolean active,
                          ResourceLocation normalTex, ResourceLocation activeTex) {
        ResourceLocation tex = active ? activeTex : normalTex;
        if (!active) fade.applyShaderColor(); else RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        ctx.blit(tex, getX(), y, btnSize(), btnSize(), 0f, 0f, 32, 32, 32, 32);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !visible) return false;
        if (overBrush(mouseX, mouseY)) {
            activeTool = DrawingTool.BRUSH;
            onToolChanged.accept(activeTool);
            return true;
        }
        if (overEraser(mouseX, mouseY)) {
            activeTool = DrawingTool.ERASER;
            onToolChanged.accept(activeTool);
            return true;
        }
        if (overPipette(mouseX, mouseY)) {
            activeTool = DrawingTool.PIPETTE;
            onToolChanged.accept(activeTool);
            return true;
        }
        if (!ClientServerSettings.imagesDisabled() && overImage(mouseX, mouseY)) {
            onToolChanged.accept(DrawingTool.IMAGE);
            return true;
        }
        return false;
    }

    public void setActiveTool(DrawingTool tool) {
        this.activeTool = tool;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}
}