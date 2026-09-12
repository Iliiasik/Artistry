package iliiasik.artistry.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CanvasSignature {

    public static final String NBT_KEY = "signature";
    public static final int MAX_NAME_LENGTH = 32;

    @Nullable
    private String playerName = null;
    @Nullable
    private UUID playerUuid = null;

    public boolean isSigned() {
        return playerName != null;
    }

    @Nullable
    public String playerName() {
        return playerName;
    }

    @Nullable
    public UUID playerUuid() {
        return playerUuid;
    }

    public void sign(String name, UUID uuid) {
        String sanitized = sanitize(name);
        if (sanitized == null) return;
        playerName = sanitized;
        playerUuid = uuid;
    }

    public void clear() {
        playerName = null;
        playerUuid = null;
    }

    public void applyRemote(@Nullable String name) {
        playerName = name == null ? null : sanitize(name);
        if (playerName == null) playerUuid = null;
    }

    public void copyFrom(CanvasSignature other) {
        playerName = other.playerName;
        playerUuid = other.playerUuid;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        if (playerName != null) tag.putString("name", playerName);
        if (playerUuid != null) {
            tag.putLong("uuid_most", playerUuid.getMostSignificantBits());
            tag.putLong("uuid_least", playerUuid.getLeastSignificantBits());
        }
        return tag;
    }

    public void fromNbt(CompoundTag tag) {
        playerName = tag.contains("name") ? sanitize(tag.getString("name")) : null;
        playerUuid = tag.contains("uuid_most") && tag.contains("uuid_least")
                ? new UUID(tag.getLong("uuid_most"), tag.getLong("uuid_least"))
                : null;
    }

    public static boolean isSignedStack(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_KEY)) return false;
        return !tag.getCompound(NBT_KEY).getString("name").isBlank();
    }

    @Nullable
    private static String sanitize(String name) {
        String value = name.trim();
        if (value.isEmpty()) return null;
        return value.length() > MAX_NAME_LENGTH ? value.substring(0, MAX_NAME_LENGTH) : value;
    }
}
