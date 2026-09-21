package iliiasik.artistry.data;

import iliiasik.artistry.support.McFixture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanvasSignatureTest {

    private static final long SIGNED_AT = 1_789_000_000_000L;

    @BeforeAll
    static void boot() {
        McFixture.bootstrap();
    }

    private static ItemStack signedStack(String name) {
        CompoundTag signature = new CompoundTag();
        signature.putString("name", name);
        CompoundTag tag = new CompoundTag();
        tag.put(CanvasSignature.NBT_KEY, signature);
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @Test
    @DisplayName("A fresh signature is not signed")
    void freshIsUnsigned() {
        CanvasSignature signature = new CanvasSignature();
        assertFalse(signature.isSigned());
        assertNull(signature.playerName());
        assertNull(signature.playerUuid());
    }

    @Test
    @DisplayName("Signing stores the name, the uuid and the date")
    void signStoresAuthor() {
        CanvasSignature signature = new CanvasSignature();
        UUID uuid = UUID.randomUUID();
        signature.sign("Iliiasik", uuid, SIGNED_AT);

        assertTrue(signature.isSigned());
        assertEquals("Iliiasik", signature.playerName());
        assertEquals(uuid, signature.playerUuid());
        assertEquals(SIGNED_AT, signature.signedAt());
    }

    @Test
    @DisplayName("A blank name never signs the canvas")
    void blankNameIsRejected() {
        CanvasSignature signature = new CanvasSignature();
        signature.sign("   ", UUID.randomUUID(), SIGNED_AT);
        assertFalse(signature.isSigned());
        assertEquals(CanvasSignature.UNKNOWN_DATE, signature.signedAt());
    }

    @Test
    @DisplayName("An overlong name is cut to the limit")
    void longNameIsTruncated() {
        CanvasSignature signature = new CanvasSignature();
        signature.sign("x".repeat(CanvasSignature.MAX_NAME_LENGTH + 20), UUID.randomUUID(), SIGNED_AT);
        String name = signature.playerName();
        assertNotNull(name);
        assertEquals(CanvasSignature.MAX_NAME_LENGTH, name.length());
    }

    @Test
    @DisplayName("A signature survives an nbt round trip")
    void nbtRoundTrip() {
        CanvasSignature source = new CanvasSignature();
        UUID uuid = UUID.randomUUID();
        source.sign("Author", uuid, SIGNED_AT);

        CanvasSignature restored = new CanvasSignature();
        restored.fromNbt(source.toNbt());

        assertEquals("Author", restored.playerName());
        assertEquals(uuid, restored.playerUuid());
        assertEquals(SIGNED_AT, restored.signedAt());
    }

    @Test
    @DisplayName("A signature written before dates existed reads back as unknown")
    void legacySignatureHasNoDate() {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("name", "Author");

        CanvasSignature restored = new CanvasSignature();
        restored.fromNbt(legacy);

        assertTrue(restored.isSigned());
        assertEquals(CanvasSignature.UNKNOWN_DATE, restored.signedAt());
    }

    @Test
    @DisplayName("Reading empty nbt clears the signature")
    void emptyNbtClears() {
        CanvasSignature signature = new CanvasSignature();
        signature.sign("Author", UUID.randomUUID(), SIGNED_AT);
        signature.fromNbt(new CompoundTag());
        assertFalse(signature.isSigned());
        assertEquals(CanvasSignature.UNKNOWN_DATE, signature.signedAt());
    }

    @Test
    @DisplayName("A remote update without a name unsigns the canvas")
    void remoteClear() {
        CanvasSignature signature = new CanvasSignature();
        signature.sign("Author", UUID.randomUUID(), SIGNED_AT);
        signature.applyRemote(null, null, SIGNED_AT);
        assertFalse(signature.isSigned());
        assertNull(signature.playerUuid());
        assertEquals(CanvasSignature.UNKNOWN_DATE, signature.signedAt());
    }

    @Test
    @DisplayName("A remote update carries the uuid and the date across")
    void remoteCarriesDate() {
        CanvasSignature signature = new CanvasSignature();
        UUID uuid = UUID.randomUUID();
        signature.applyRemote("Author", uuid, SIGNED_AT);
        assertTrue(signature.isSigned());
        assertEquals(uuid, signature.playerUuid());
        assertEquals(SIGNED_AT, signature.signedAt());
    }

    @Test
    @DisplayName("Stacks are recognised as signed only with a real name")
    void signedStackDetection() {
        assertFalse(CanvasSignature.isSignedStack(new ItemStack(Items.PAPER)));
        assertFalse(CanvasSignature.isSignedStack(signedStack("  ")));
        assertTrue(CanvasSignature.isSignedStack(signedStack("Author")));
    }
}
