package iliiasik.artistry.network;

import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.support.McFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketCodecTest {

    private static <T> T roundTrip(StreamCodec<RegistryFriendlyByteBuf, T> codec, T value) {
        RegistryFriendlyByteBuf buf = McFixture.buffer();
        codec.encode(buf, value);
        T decoded = codec.decode(buf);
        assertEquals(0, buf.readableBytes(), "codec left unread bytes");
        return decoded;
    }

    private static final BlockPos POS = new BlockPos(-1234, 71, 5678);
    private static final UUID UUID_A = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID UUID_B = UUID.fromString("ffffffff-ffff-4fff-bfff-ffffffffffff");

    @Test
    @DisplayName("PosterTarget survives the round trip for both variants")
    void posterTargetRoundTrip() {
        assertEquals(new PosterTarget.World(POS), roundTrip(PosterTarget.CODEC, new PosterTarget.World(POS)));
        assertEquals(new PosterTarget.Held(InteractionHand.MAIN_HAND),
                roundTrip(PosterTarget.CODEC, new PosterTarget.Held(InteractionHand.MAIN_HAND)));
        assertEquals(new PosterTarget.Held(InteractionHand.OFF_HAND),
                roundTrip(PosterTarget.CODEC, new PosterTarget.Held(InteractionHand.OFF_HAND)));
    }

    @Test
    @DisplayName("Pixel change lists survive the round trip including extreme values")
    void pixelChangeListRoundTrip() {
        List<CanvasData.PixelChange> changes = new ArrayList<>();
        changes.add(new CanvasData.PixelChange((byte) 0, (byte) 0, (short) 0, 0));
        changes.add(new CanvasData.PixelChange((byte) 31, (byte) 31, CanvasData.COLOR_PIXEL, 0xFFFFFFFF));
        changes.add(new CanvasData.PixelChange((byte) 15, (byte) 7, Short.MAX_VALUE, 0x7F123456));

        FriendlyByteBuf buf = McFixture.buffer();
        CanvasData.PixelChange.writeList(buf, changes);
        List<CanvasData.PixelChange> decoded = CanvasData.PixelChange.readList(buf);

        assertEquals(changes, decoded);
        assertEquals(0, buf.readableBytes());
    }

    @Test
    @DisplayName("An empty pixel change list encodes and decodes cleanly")
    void emptyPixelChangeList() {
        FriendlyByteBuf buf = McFixture.buffer();
        CanvasData.PixelChange.writeList(buf, List.of());
        assertTrue(CanvasData.PixelChange.readList(buf).isEmpty());
    }

    @Test
    @DisplayName("Canvas size and save packets survive the round trip")
    void canvasPackets() {
        SetCanvasSizeC2SPacket size = new SetCanvasSizeC2SPacket(new PosterTarget.World(POS), 32);
        assertEquals(size, roundTrip(SetCanvasSizeC2SPacket.CODEC, size));

        List<CanvasData.PixelChange> changes = List.of(
                new CanvasData.PixelChange((byte) 1, (byte) 2, (short) 3, 4),
                new CanvasData.PixelChange((byte) 5, (byte) 6, CanvasData.COLOR_PIXEL, 0xFF00FF00));

        SaveCanvasC2SPacket save = new SaveCanvasC2SPacket(new PosterTarget.Held(InteractionHand.OFF_HAND), changes);
        assertEquals(save, roundTrip(SaveCanvasC2SPacket.CODEC, save));

        SyncCanvasS2CPacket sync = new SyncCanvasS2CPacket(POS, changes);
        assertEquals(sync, roundTrip(SyncCanvasS2CPacket.CODEC, sync));
    }

    @Test
    @DisplayName("Image transfer packets keep their payload byte for byte")
    void imageTransferPackets() {
        byte[] bytes = McFixture.pattern(4096);

        UploadImageC2SPacket upload = new UploadImageC2SPacket(new PosterTarget.World(POS), bytes);
        UploadImageC2SPacket decodedUpload = roundTrip(UploadImageC2SPacket.CODEC, upload);
        assertEquals(upload.target(), decodedUpload.target());
        assertArrayEquals(bytes, decodedUpload.bytes());

        DeliverImageS2CPacket deliver = new DeliverImageS2CPacket(UUID_A, bytes);
        DeliverImageS2CPacket decodedDeliver = roundTrip(DeliverImageS2CPacket.CODEC, deliver);
        assertEquals(UUID_A, decodedDeliver.uuid());
        assertArrayEquals(bytes, decodedDeliver.bytes());
    }

    @Test
    @DisplayName("An empty image payload is still a valid packet")
    void emptyImagePayload() {
        DeliverImageS2CPacket deliver = new DeliverImageS2CPacket(UUID_A, new byte[0]);
        assertEquals(0, roundTrip(DeliverImageS2CPacket.CODEC, deliver).bytes().length);
    }

    @Test
    @DisplayName("Image manipulation packets survive the round trip")
    void imageManipulationPackets() {
        PosterTarget target = new PosterTarget.World(POS);

        MoveCanvasImageC2SPacket move = new MoveCanvasImageC2SPacket(target, UUID_A, 3, 4, 9, 11);
        assertEquals(move, roundTrip(MoveCanvasImageC2SPacket.CODEC, move));

        DeleteCanvasImageC2SPacket delete = new DeleteCanvasImageC2SPacket(target, UUID_B);
        assertEquals(delete, roundTrip(DeleteCanvasImageC2SPacket.CODEC, delete));

        TogglePixelizeC2SPacket toggle = new TogglePixelizeC2SPacket(target, UUID_A);
        assertEquals(toggle, roundTrip(TogglePixelizeC2SPacket.CODEC, toggle));

        LockCanvasImageC2SPacket lock = new LockCanvasImageC2SPacket(target, UUID_A, true);
        assertEquals(lock, roundTrip(LockCanvasImageC2SPacket.CODEC, lock));

        RequestImageC2SPacket request = new RequestImageC2SPacket(UUID_B);
        assertEquals(request, roundTrip(RequestImageC2SPacket.CODEC, request));

        ImageEvictedS2CPacket evicted = new ImageEvictedS2CPacket(List.of(UUID_A, UUID_B));
        assertEquals(evicted, roundTrip(ImageEvictedS2CPacket.CODEC, evicted));
    }

    @Test
    @DisplayName("ImageUploaded keeps a null position for held posters")
    void imageUploadedNullablePosition() {
        ImageUploadedS2CPacket withPos = new ImageUploadedS2CPacket(POS, UUID_A, 1, 2, 3, 4);
        assertEquals(withPos, roundTrip(ImageUploadedS2CPacket.CODEC, withPos));

        ImageUploadedS2CPacket withoutPos = new ImageUploadedS2CPacket(null, UUID_A, 1, 2, 3, 4);
        assertNull(roundTrip(ImageUploadedS2CPacket.CODEC, withoutPos).pos());
    }

    @Test
    @DisplayName("Image lock sync keeps a null owner when the lock is released")
    void imageLockSyncNullableOwner() {
        SyncImageLockS2CPacket locked = new SyncImageLockS2CPacket(POS, UUID_A, UUID_B);
        assertEquals(locked, roundTrip(SyncImageLockS2CPacket.CODEC, locked));

        SyncImageLockS2CPacket unlocked = new SyncImageLockS2CPacket(POS, UUID_A, null);
        assertNull(roundTrip(SyncImageLockS2CPacket.CODEC, unlocked).playerUuid());
    }

    @Test
    @DisplayName("Image layer sync keeps every image field")
    void imageLayerSync() {
        CanvasImage first = new CanvasImage(UUID_A, 1, 2, 6, 7);
        first.pixelized = true;
        first.addedSeq = 5;
        CanvasImage second = new CanvasImage(UUID_B, 8, 9, 4, 4);
        second.lockedByPlayer = UUID_A;

        SyncImageLayerS2CPacket packet = new SyncImageLayerS2CPacket(POS, List.of(first, second));
        SyncImageLayerS2CPacket decoded = roundTrip(SyncImageLayerS2CPacket.CODEC, packet);

        assertEquals(POS, decoded.pos());
        assertEquals(2, decoded.images().size());

        CanvasImage decodedFirst = decoded.images().get(0);
        assertEquals(UUID_A, decodedFirst.uuid);
        assertEquals(1, decodedFirst.gridX);
        assertEquals(2, decodedFirst.gridY);
        assertEquals(6, decodedFirst.gridW);
        assertEquals(7, decodedFirst.gridH);
        assertTrue(decodedFirst.pixelized);

        assertEquals(UUID_B, decoded.images().get(1).uuid);
    }

    @Test
    @DisplayName("Presence and view packets survive the round trip")
    void presencePackets() {
        CanvasViewC2SPacket view = new CanvasViewC2SPacket(POS, true);
        assertEquals(view, roundTrip(CanvasViewC2SPacket.CODEC, view));

        CanvasCursorC2SPacket cursor = new CanvasCursorC2SPacket(POS, (short) 120, (short) -30);
        assertEquals(cursor, roundTrip(CanvasCursorC2SPacket.CODEC, cursor));

        CanvasCursorS2CPacket cursorS2C = new CanvasCursorS2CPacket(POS, UUID_A, (short) 1, (short) 2);
        assertEquals(cursorS2C, roundTrip(CanvasCursorS2CPacket.CODEC, cursorS2C));

        CanvasPresenceLeaveS2CPacket leave = new CanvasPresenceLeaveS2CPacket(POS, UUID_B);
        assertEquals(leave, roundTrip(CanvasPresenceLeaveS2CPacket.CODEC, leave));

        CanvasEnterRequestC2SPacket enter = new CanvasEnterRequestC2SPacket(POS);
        assertEquals(enter, roundTrip(CanvasEnterRequestC2SPacket.CODEC, enter));

        CanvasEnterAllowedS2CPacket allowed = new CanvasEnterAllowedS2CPacket(POS, true);
        assertEquals(allowed, roundTrip(CanvasEnterAllowedS2CPacket.CODEC, allowed));

        PosterRemovedS2CPacket removed = new PosterRemovedS2CPacket(POS);
        assertEquals(removed, roundTrip(PosterRemovedS2CPacket.CODEC, removed));
    }

    @Test
    @DisplayName("Server settings survive the round trip")
    void serverSettingsRoundTrip() {
        ServerSettingsS2CPacket settings = new ServerSettingsS2CPacket(true, 25, 250);
        assertEquals(settings, roundTrip(ServerSettingsS2CPacket.CODEC, settings));
    }

    @Test
    @DisplayName("Every payload type has a unique identifier in the mod namespace")
    void payloadTypesAreUniqueAndNamespaced() {
        List<CustomPacketPayload.Type<?>> types = List.of(
                SetCanvasSizeC2SPacket.TYPE,
                SaveCanvasC2SPacket.TYPE,
                UploadImageC2SPacket.TYPE,
                MoveCanvasImageC2SPacket.TYPE,
                DeleteCanvasImageC2SPacket.TYPE,
                TogglePixelizeC2SPacket.TYPE,
                LockCanvasImageC2SPacket.TYPE,
                RequestImageC2SPacket.TYPE,
                CanvasViewC2SPacket.TYPE,
                CanvasCursorC2SPacket.TYPE,
                CanvasEnterRequestC2SPacket.TYPE,
                SyncCanvasS2CPacket.TYPE,
                PosterRemovedS2CPacket.TYPE,
                ImageUploadedS2CPacket.TYPE,
                DeliverImageS2CPacket.TYPE,
                SyncImageLayerS2CPacket.TYPE,
                SyncImageLockS2CPacket.TYPE,
                ImageEvictedS2CPacket.TYPE,
                CanvasCursorS2CPacket.TYPE,
                CanvasPresenceLeaveS2CPacket.TYPE,
                CanvasEnterAllowedS2CPacket.TYPE,
                ServerSettingsS2CPacket.TYPE);

        Set<ResourceLocation> ids = new HashSet<>();
        for (CustomPacketPayload.Type<?> type : types) {
            assertEquals("artistry", type.id().getNamespace(), type.id() + " is not namespaced");
            assertTrue(ids.add(type.id()), "duplicate payload id " + type.id());
        }
        assertEquals(22, ids.size());
    }

    @Test
    @DisplayName("Each payload reports its own type instance")
    void payloadsReportTheirType() {
        assertEquals(PosterRemovedS2CPacket.TYPE, new PosterRemovedS2CPacket(POS).type());
        assertEquals(RequestImageC2SPacket.TYPE, new RequestImageC2SPacket(UUID_A).type());
    }
}
