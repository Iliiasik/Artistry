package iliiasik.artistry.client.ui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class HexInputWidget extends ClickableWidget {

    private static final int CURSOR_BLINK_MS = 500;
    private static final int TEXT_COLOR      = 0xFFFDF7E8;
    private static final int SEL_COLOR       = 0x664444FF;
    private static final int CURSOR_COLOR    = 0xFFFDF7E8;

    private String text = "#";
    private int cursorPos = 1;
    private int selectionStart = 1;
    private boolean focused = false;
    private boolean visible = true;
    private long focusTime = 0;
    private Consumer<String> changedListener;
    private float paletteScale = 1f;

    public HexInputWidget(int x, int y, int w, int h) {
        super(x, y, w, h, Text.empty());
    }

    public void setPaletteScale(float scale) {
        this.paletteScale = scale;
    }

    public void setChangedListener(Consumer<String> listener) {
        this.changedListener = listener;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible) setFocused(false);
    }

    public boolean isVisible() {
        return visible;
    }

    public String getText() {
        return text;
    }

    public void setText(String newText) {
        text = newText;
        cursorPos = clampCursor(text.length());
        selectionStart = cursorPos;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) focusTime = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (!isMouseOver(mouseX, mouseY)) {
            setFocused(false);
            return false;
        }
        if (button == 0) {
            setFocused(true);
            int clicked = screenXToCharIndex((int) mouseX);
            cursorPos = clicked;
            selectionStart = clicked;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
        if (!focused) return false;
        boolean shift = (modifiers & 1) != 0;
        boolean ctrl  = (modifiers & 2) != 0;

        switch (keyCode) {
            case 263 -> { moveCursor(cursorPos - 1, shift); return true; }
            case 262 -> { moveCursor(cursorPos + 1, shift); return true; }
            case 268 -> { moveCursor(1, shift); return true; }
            case 269 -> { moveCursor(text.length(), shift); return true; }
            case 259 -> { handleBackspace(ctrl); return true; }
            case 261 -> { handleDelete(ctrl); return true; }
            case 65  -> { if (ctrl) { selectAll(); return true; } }
            case 67  -> { if (ctrl) { copySelection(); return true; } }
            case 86  -> { if (ctrl) { pasteFromClipboard(); return true; } }
            case 88  -> { if (ctrl) { cutSelection(); return true; } }
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!visible) return false;
        if (!focused) return false;
        char upper = Character.toUpperCase(chr);
        if (isHexChar(upper)) {
            insertChar(upper);
            return true;
        }
        return false;
    }

    private boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F');
    }

    private int clampCursor(int pos) {
        return Math.max(1, Math.min(pos, text.length()));
    }

    private void moveCursor(int newPos, boolean shift) {
        newPos = clampCursor(newPos);
        if (!shift) selectionStart = newPos;
        cursorPos = newPos;
    }

    private void selectAll() {
        selectionStart = 1;
        cursorPos = text.length();
    }

    private int selMin() { return Math.min(cursorPos, selectionStart); }
    private int selMax() { return Math.max(cursorPos, selectionStart); }
    private boolean hasSelection() { return cursorPos != selectionStart; }

    private void deleteSelection() {
        int lo = selMin();
        int hi = selMax();
        lo = Math.max(lo, 1);
        if (lo >= hi) return;
        text = text.substring(0, lo) + text.substring(hi);
        cursorPos = lo;
        selectionStart = lo;
    }

    private void insertChar(char c) {
        if (hasSelection()) deleteSelection();
        if (text.length() >= 7) return;
        int pos = clampCursor(cursorPos);
        text = text.substring(0, pos) + c + text.substring(pos);
        cursorPos = pos + 1;
        selectionStart = cursorPos;
        notifyChanged();
    }

    private void handleBackspace(boolean ctrl) {
        if (hasSelection()) {
            deleteSelection();
            notifyChanged();
            return;
        }
        if (cursorPos <= 1) return;
        if (ctrl) {
            text = "#" + text.substring(cursorPos);
            cursorPos = 1;
            selectionStart = 1;
        } else {
            text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
            cursorPos--;
            selectionStart = cursorPos;
        }
        notifyChanged();
    }

    private void handleDelete(boolean ctrl) {
        if (hasSelection()) {
            deleteSelection();
            notifyChanged();
            return;
        }
        if (cursorPos >= text.length()) return;
        if (ctrl) {
            text = text.substring(0, cursorPos);
            selectionStart = cursorPos;
        } else {
            text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
        }
        notifyChanged();
    }

    private void copySelection() {
        if (!hasSelection()) return;
        String sel = text.substring(selMin(), selMax());
        MinecraftClient.getInstance().keyboard.setClipboard(sel);
    }

    private void cutSelection() {
        if (!hasSelection()) return;
        copySelection();
        deleteSelection();
        notifyChanged();
    }

    private void pasteFromClipboard() {
        String clip = MinecraftClient.getInstance().keyboard.getClipboard();
        if (clip == null || clip.isEmpty()) return;
        if (hasSelection()) deleteSelection();
        for (char c : clip.toCharArray()) {
            char upper = Character.toUpperCase(c);
            if (isHexChar(upper)) insertChar(upper);
        }
    }

    private void notifyChanged() {
        if (changedListener != null) changedListener.accept(text);
    }

    private float textScale() {
        return paletteScale * (5f / 9f);
    }

    private int screenXToCharIndex(int screenX) {
        var renderer = MinecraftClient.getInstance().textRenderer;
        float ts = textScale();
        int virtualW = (int)(getWidth() / ts);
        int textOffsetX = (virtualW - renderer.getWidth(text)) / 2;
        int anchorX = getX() + (int)(textOffsetX * ts);
        int best = 1;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 1; i <= text.length(); i++) {
            int cx = anchorX + (int)(renderer.getWidth(text.substring(0, i)) * ts);
            int dist = Math.abs(screenX - cx);
            if (dist < bestDist) { bestDist = dist; best = i; }
        }
        int distToStart = Math.abs(screenX - anchorX);
        if (distToStart < bestDist) best = 1;
        return best;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        var renderer = MinecraftClient.getInstance().textRenderer;
        float ts = textScale();


        int cursorH = renderer.fontHeight + 2;
        int centerY = getY() + getHeight() / 2;
        int textY = centerY - (int)(cursorH * ts / 2) + (int)(ts);

        int virtualW = (int)(getWidth() / ts);
        int textOffsetX = (virtualW - renderer.getWidth(text)) / 2;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(getX(), textY, 0);
        ctx.getMatrices().scale(ts, ts, 1f);

        if (focused && hasSelection()) {
            int lo = selMin();
            int hi = selMax();
            int selX0 = textOffsetX + renderer.getWidth(text.substring(0, lo));
            int selX1 = textOffsetX + renderer.getWidth(text.substring(0, hi));
            ctx.fill(selX0, -1, selX1, renderer.fontHeight + 1, SEL_COLOR);
        }

        ctx.drawText(renderer, text, textOffsetX, 0, TEXT_COLOR, true);

        if (focused) {
            long elapsed = System.currentTimeMillis() - focusTime;
            boolean showCursor = (elapsed / CURSOR_BLINK_MS) % 2 == 0;
            if (showCursor) {
                int curX = textOffsetX + renderer.getWidth(text.substring(0, cursorPos));
                ctx.fill(curX, -1, curX + 1, renderer.fontHeight + 1, CURSOR_COLOR);
            }
        }

        ctx.getMatrices().pop();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}