package iliiasik.artistry.client.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.util.ModTextures;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.network.ArtistryNetwork;
import iliiasik.artistry.network.PosterTarget;
import iliiasik.artistry.network.SetCanvasSizeC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class CanvasSizeScreen extends Screen {

    private static final int VIRTUAL_W  = 120;
    private static final int VIRTUAL_H  = 64;
    private static final int BTN_SIZE   = 32;
    private static final int BTN_GAP    = 4;
    private static final int BTN_MARGIN = 8;
    private static final int TEXT_Y     = 10;
    private static final int[] SIZES    = CanvasData.SIZES;

    private final PosterBlockEntity targetEntity;
    private final ItemStack targetStack;
    private final InteractionHand targetHand;

    private float scale;
    private int originX;
    private int originY;

    public CanvasSizeScreen(PosterBlockEntity entity) {
        super(Component.empty());
        this.targetEntity = entity;
        this.targetStack  = null;
        this.targetHand   = null;
    }

    public CanvasSizeScreen(ItemStack stack, InteractionHand hand) {
        super(Component.empty());
        this.targetEntity = null;
        this.targetStack  = stack;
        this.targetHand   = hand;
    }

    private int s(int v) { return Math.round(v * scale); }

    private void recalc() {
        float scaleX = (float) width  / 900f;
        float scaleY = (float) height / 600f;
        scale   = Math.min(scaleX, scaleY) * 3.0f;
        originX = (width  - s(VIRTUAL_W)) / 2;
        originY = (height - s(VIRTUAL_H)) / 2;
    }

    @Override
    protected void init() {
        super.init();
        recalc();
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        recalc();

        ctx.pose().pushPose();
        ctx.pose().translate(originX, originY, 0);
        ctx.pose().scale(scale, scale, 1f);

        ctx.blit(
                ModTextures.SIZE_SCREEN,
                0, 0,
                VIRTUAL_W, VIRTUAL_H,
                0f, 0f,
                VIRTUAL_W, VIRTUAL_H,
                VIRTUAL_W, VIRTUAL_H
        );

        String title = Component.translatable("screen.artistry.choose_size").getString();
        int textW = this.font.width(title);
        ctx.drawString(this.font, title,
                (VIRTUAL_W - textW) / 2, TEXT_Y, 0xFFFDF7E8, false);

        int vMouseX = (int) ((mouseX - originX) / scale);
        int vMouseY = (int) ((mouseY - originY) / scale);

        for (int i = 0; i < SIZES.length; i++) {
            int bx = BTN_MARGIN + i * (BTN_SIZE + BTN_GAP);
            int by = VIRTUAL_H - BTN_MARGIN - BTN_SIZE;

            boolean hovered = vMouseX >= bx && vMouseX < bx + BTN_SIZE
                    && vMouseY >= by && vMouseY < by + BTN_SIZE;

            float brightness = hovered ? 1.0f : 200f / 255f;
            RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0f);

            ctx.blit(
                    ModTextures.SIZE_SWITCHER,
                    bx, by,
                    BTN_SIZE, BTN_SIZE,
                    0f, 0f,
                    BTN_SIZE, BTN_SIZE,
                    BTN_SIZE, BTN_SIZE
            );

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            String label = SIZES[i] + "x";
            int lw = this.font.width(label);
            ctx.drawString(this.font, label,
                    bx + (BTN_SIZE - lw) / 2, by + BTN_SIZE / 2 - 4, 0xFF666155, false);
        }

        ctx.pose().popPose();
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int vx = (int) ((mouseX - originX) / scale);
        int vy = (int) ((mouseY - originY) / scale);
        int by = VIRTUAL_H - BTN_MARGIN - BTN_SIZE;
        for (int i = 0; i < SIZES.length; i++) {
            int bx = BTN_MARGIN + i * (BTN_SIZE + BTN_GAP);
            if (vx >= bx && vx < bx + BTN_SIZE && vy >= by && vy < by + BTN_SIZE) {
                onSizeChosen(SIZES[i]);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onSizeChosen(int size) {
        if (targetEntity != null) {
            targetEntity.canvasData.canvasSize = size;
            if (Minecraft.getInstance().getConnection() != null) {
                ArtistryNetwork.sendToServer(
                        new SetCanvasSizeC2SPacket(new PosterTarget.World(targetEntity.getBlockPos()), size));
            }
            Minecraft.getInstance().setScreen(new PaintScreen(targetEntity));
        } else if (targetStack != null) {
            if (Minecraft.getInstance().getConnection() != null) {
                ArtistryNetwork.sendToServer(
                        new SetCanvasSizeC2SPacket(new PosterTarget.Held(targetHand), size));
            }
            Minecraft.getInstance().setScreen(new PaintScreen(targetStack, targetHand, size));
        }
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics context) {}
}
