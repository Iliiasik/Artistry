package iliiasik.artistry.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CanvasSignature {

    public static final String NBT_KEY = "signature";
    public static final int MAX_NAME_LENGTH = 32;

    public static final long UNKNOWN_DATE = 0L;

    @Nullable
    private String playerName = null;
    @Nullable
    private UUID playerUuid = null;
    private long signedAt = UNKNOWN_DATE;

    public boolean isSigned() {
        return playerName != null;
    }

    public long signedAt() {
        return signedAt;
    }

    @Nullable
    public String playerName() {
        return playerName;
    }

    @Nullable
    public UUID playerUuid() {
        return playerUuid;
    }

    public void sign(String name, UUID uuid, long epochMillis) {
        String sanitized = sanitize(name);
        if (sanitized == null) return;
        playerName = sanitized;
        playerUuid = uuid;
        signedAt = epochMillis;
    }

    public void clear() {
        playerName = null;
        playerUuid = null;
        signedAt = UNKNOWN_DATE;
    }

    public void applyRemote(@Nullable String name, @Nullable UUID uuid, long epochMillis) {
        playerName = name == null ? null : sanitize(name);
        if (playerName == null) {
            playerUuid = null;
            signedAt = UNKNOWN_DATE;
            return;
        }
        playerUuid = uuid;
        signedAt = epochMillis;
    }

    public void copyFrom(CanvasSignature other) {
        playerName = other.playerName;
        playerUuid = other.playerUuid;
        signedAt = other.signedAt;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        if (playerName != null) tag.putString("name", playerName);
        if (playerUuid != null) {
            tag.putLong("uuid_most", playerUuid.getMostSignificantBits());
            tag.putLong("uuid_least", playerUuid.getLeastSignificantBits());
        }
        if (signedAt != UNKNOWN_DATE) tag.putLong("signed_at", signedAt);
        return tag;
    }

    public void fromNbt(CompoundTag tag) {
        playerName = tag.contains("name") ? sanitize(tag.getString("name")) : null;
        playerUuid = tag.contains("uuid_most") && tag.contains("uuid_least")
                ? new UUID(tag.getLong("uuid_most"), tag.getLong("uuid_least"))
                : null;
        signedAt = tag.contains("signed_at") ? tag.getLong("signed_at") : UNKNOWN_DATE;
    }

    public static boolean isSignedStack(ItemStack stack) {
        CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
        if (comp == null) return false;
        CompoundTag tag = comp.copyTag();
        if (!tag.contains(NBT_KEY)) return false;
        return !tag.getCompound(NBT_KEY).getString("name").isBlank();
    }

    @Nullable
    private static String sanitize(String name) {
        String value = name.trim();
        if (value.isEmpty()) return null;
        return value.length() > MAX_NAME_LENGTH ? value.substring(0, MAX_NAME_LENGTH) : value;
    }
}
