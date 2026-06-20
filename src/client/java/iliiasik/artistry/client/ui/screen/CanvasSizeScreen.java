package iliiasik.artistry.client.ui.screen;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.util.ModTextures;
import iliiasik.artistry.network.PosterTarget;
import iliiasik.artistry.network.SetCanvasSizeC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public class CanvasSizeScreen extends Screen {

    private static final int VIRTUAL_W  = 120;
    private static final int VIRTUAL_H  = 64;
    private static final int BTN_SIZE   = 32;
    private static final int BTN_GAP    = 4;
    private static final int BTN_MARGIN = 8;
    private static final int TEXT_Y     = 10;
    private static final int[] SIZES    = {8, 16, 32};

    private final PosterBlockEntity targetEntity;
    private final ItemStack targetStack;
    private final Hand targetHand;

    private float scale;
    private int originX;
    private int originY;

    public CanvasSizeScreen(PosterBlockEntity entity) {
        super(Text.empty());
        this.targetEntity = entity;
        this.targetStack  = null;
        this.targetHand   = null;
    }

    public CanvasSizeScreen(ItemStack stack, Hand hand) {
        super(Text.empty());
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
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        recalc();

        ctx.getMatrices().push();
        ctx.getMatrices().translate(originX, originY, 0);
        ctx.getMatrices().scale(scale, scale, 1f);

        ctx.drawTexture(
                ModTextures.SIZE_SCREEN,
                0, 0,
                VIRTUAL_W, VIRTUAL_H,
                0f, 0f,
                VIRTUAL_W, VIRTUAL_H,
                VIRTUAL_W, VIRTUAL_H
        );

        String title = Text.translatable("screen.artistry.choose_size").getString();
        assert client != null;
        int textW = client.textRenderer.getWidth(title);
        ctx.drawText(client.textRenderer, title,
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

            ctx.drawTexture(
                    ModTextures.SIZE_SWITCHER,
                    bx, by,
                    BTN_SIZE, BTN_SIZE,
                    0f, 0f,
                    BTN_SIZE, BTN_SIZE,
                    BTN_SIZE, BTN_SIZE
            );

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            String label = SIZES[i] + "x";
            int lw = client.textRenderer.getWidth(label);
            ctx.drawText(client.textRenderer, label,
                    bx + (BTN_SIZE - lw) / 2, by + BTN_SIZE / 2 - 4, 0xFF666155, false);
        }

        ctx.getMatrices().pop();
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
            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                ClientPlayNetworking.send(new SetCanvasSizeC2SPacket(new PosterTarget.World(targetEntity.getPos()), size));
            }
            MinecraftClient.getInstance().setScreen(new PaintScreen(targetEntity));
        } else if (targetStack != null) {
            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                ClientPlayNetworking.send(new SetCanvasSizeC2SPacket(new PosterTarget.Held(targetHand), size));
            }
            MinecraftClient.getInstance().setScreen(new PaintScreen(targetStack, targetHand, size));
        }
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }
}