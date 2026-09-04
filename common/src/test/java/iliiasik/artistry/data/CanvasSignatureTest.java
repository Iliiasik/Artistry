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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanvasSignatureTest {

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
    @DisplayName("Signing stores the name and the uuid")
    void signStoresAuthor() {
        CanvasSignature signature = new CanvasSignature();
        UUID uuid = UUID.randomUUID();
        signature.sign("Iliiasik", uuid);

        assertTrue(signature.isSigned());
        assertEquals("Iliiasik", signature.playerName());
        assertEquals(uuid, signature.playerUuid());
    }

    @Test
    @DisplayName("A blank name never signs the canvas")
    void blankNameIsRejected() {
        CanvasSignature signature = new CanvasSignature();
        signature.sign("   ", UUID.randomUUID());
        assertFalse(signature.isSigned());
    }

    @Test
    @DisplayName("An overlong name is cut to the limit")
    void longNameIsTruncated() {
        CanvasSignature signature = new CanvasSignature();
        signature.sign("x".repeat(CanvasSignature.MAX_NAME_LENGTH + 20), UUID.randomUUID());
        assertEquals(CanvasSignature.MAX_NAME_LENGTH, signature.playerName().length());
    }

    @Test
    @DisplayName("A signature survives an nbt round trip")
    void nbtRoundTrip() {
        CanvasSignature source = new CanvasSignature();
        UUID uuid = UUID.randomUUID();
        source.sign("Author", uuid);

        CanvasSignature restored = new CanvasSignature();
        restored.fromNbt(source.toNbt());

        assertEquals("Author", restored.playerName());
        assertEquals(uuid, restored.playerUuid());
    }

    @Test
    @DisplayName("Reading empty nbt clears the signature")
    void emptyNbtClears() {
        CanvasSignature signature = new CanvasSignature();
        signature.sign("Author", UUID.randomUUID());
        signature.fromNbt(new CompoundTag());
        assertFalse(signature.isSigned());
    }

    @Test
    @DisplayName("A remote update without a name unsigns the canvas")
    void remoteClear() {
        CanvasSignature signature = new CanvasSignature();
        signature.sign("Author", UUID.randomUUID());
        signature.applyRemote(null);
        assertFalse(signature.isSigned());
        assertNull(signature.playerUuid());
    }

    @Test
    @DisplayName("Stacks are recognised as signed only with a real name")
    void signedStackDetection() {
        assertFalse(CanvasSignature.isSignedStack(new ItemStack(Items.PAPER)));
        assertFalse(CanvasSignature.isSignedStack(signedStack("  ")));
        assertTrue(CanvasSignature.isSignedStack(signedStack("Author")));
    }
}
