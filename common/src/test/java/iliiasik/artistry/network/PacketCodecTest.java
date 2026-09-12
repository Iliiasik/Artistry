package iliiasik.artistry.network;

import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.support.McFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketCodecTest {

    private static <T extends ArtistryPacket> T roundTrip(T packet, Function<FriendlyByteBuf, T> reader) {
        FriendlyByteBuf buf = McFixture.buffer();
        packet.write(buf);
        T decoded = reader.apply(buf);
        assertEquals(0, buf.readableBytes(), "codec left unread bytes");
        return decoded;
    }

    private static final BlockPos POS = new BlockPos(-1234, 71, 5678);
    private static final UUID UUID_A = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID UUID_B = UUID.fromString("ffffffff-ffff-4fff-bfff-ffffffffffff");

    @Test
    @DisplayName("PosterTarget survives the round trip for both variants")
    void posterTargetRoundTrip() {
        for (PosterTarget target : List.of(
                new PosterTarget.World(POS),
                new PosterTarget.Held(InteractionHand.MAIN_HAND),
                new PosterTarget.Held(InteractionHand.OFF_HAND))) {
            FriendlyByteBuf buf = McFixture.buffer();
            PosterTarget.write(buf, target);
            assertEquals(target, PosterTarget.read(buf));
            assertEquals(0, buf.readableBytes());
        }
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
        assertEquals(size, roundTrip(size, SetCanvasSizeC2SPacket::read));

        List<CanvasData.PixelChange> changes = List.of(
                new CanvasData.PixelChange((byte) 1, (byte) 2, (short) 3, 4),
                new CanvasData.PixelChange((byte) 5, (byte) 6, CanvasData.COLOR_PIXEL, 0xFF00FF00));

        SaveCanvasC2SPacket save = new SaveCanvasC2SPacket(new PosterTarget.Held(InteractionHand.OFF_HAND), changes);
        assertEquals(save, roundTrip(save, SaveCanvasC2SPacket::read));

        SyncCanvasS2CPacket sync = new SyncCanvasS2CPacket(POS, changes);
        assertEquals(sync, roundTrip(sync, SyncCanvasS2CPacket::read));
    }

    @Test
    @DisplayName("Image transfer packets keep their payload byte for byte")
    void imageTransferPackets() {
        byte[] bytes = McFixture.pattern(4096);

        UploadImageC2SPacket upload =
                new UploadImageC2SPacket(new PosterTarget.World(POS), bytes.length, 0, bytes);
        UploadImageC2SPacket decodedUpload = roundTrip(upload, UploadImageC2SPacket::read);
        assertEquals(upload.target(), decodedUpload.target());
        assertEquals(bytes.length, decodedUpload.total());
        assertEquals(0, decodedUpload.offset());
        assertArrayEquals(bytes, decodedUpload.chunk());

        DeliverImageS2CPacket deliver = new DeliverImageS2CPacket(UUID_A, bytes.length, 0, bytes);
        DeliverImageS2CPacket decodedDeliver = roundTrip(deliver, DeliverImageS2CPacket::read);
        assertEquals(UUID_A, decodedDeliver.uuid());
        assertEquals(bytes.length, decodedDeliver.total());
        assertArrayEquals(bytes, decodedDeliver.chunk());
    }

    @Test
    @DisplayName("A chunk from the middle of an image keeps its offset")
    void imageChunkOffsetSurvives() {
        byte[] chunk = McFixture.pattern(1500);

        UploadImageC2SPacket upload =
                new UploadImageC2SPacket(new PosterTarget.Held(InteractionHand.MAIN_HAND), 90000, 30000, chunk);
        UploadImageC2SPacket decoded = roundTrip(upload, UploadImageC2SPacket::read);
        assertEquals(90000, decoded.total());
        assertEquals(30000, decoded.offset());
        assertArrayEquals(chunk, decoded.chunk());
    }

    @Test
    @DisplayName("An empty image payload is still a valid packet")
    void emptyImagePayload() {
        DeliverImageS2CPacket deliver = new DeliverImageS2CPacket(UUID_A, 0, 0, new byte[0]);
        assertEquals(0, roundTrip(deliver, DeliverImageS2CPacket::read).chunk().length);
    }

    @Test
    @DisplayName("A chunked upload stays under the serverbound payload limit")
    void uploadChunksFitThePayloadLimit() {
        byte[] chunk = new byte[ArtistryNetwork.UPLOAD_CHUNK_SIZE];
        UploadImageC2SPacket upload = new UploadImageC2SPacket(new PosterTarget.World(POS), 1 << 20, 0, chunk);

        FriendlyByteBuf buf = McFixture.buffer();
        upload.write(buf);

        assertTrue(buf.readableBytes() <= 32767,
                "a single upload chunk must fit a vanilla custom payload, got " + buf.readableBytes());
    }

    @Test
    @DisplayName("Image manipulation packets survive the round trip")
    void imageManipulationPackets() {
        PosterTarget target = new PosterTarget.World(POS);

        MoveCanvasImageC2SPacket move = new MoveCanvasImageC2SPacket(target, UUID_A, 3, 4, 9, 11);
        assertEquals(move, roundTrip(move, MoveCanvasImageC2SPacket::read));

        DeleteCanvasImageC2SPacket delete = new DeleteCanvasImageC2SPacket(target, UUID_B);
        assertEquals(delete, roundTrip(delete, DeleteCanvasImageC2SPacket::read));

        TogglePixelizeC2SPacket toggle = new TogglePixelizeC2SPacket(target, UUID_A);
        assertEquals(toggle, roundTrip(toggle, TogglePixelizeC2SPacket::read));

        LockCanvasImageC2SPacket lock = new LockCanvasImageC2SPacket(target, UUID_A, true);
        assertEquals(lock, roundTrip(lock, LockCanvasImageC2SPacket::read));

        RequestImageC2SPacket request = new RequestImageC2SPacket(UUID_B);
        assertEquals(request, roundTrip(request, RequestImageC2SPacket::read));

        ImageEvictedS2CPacket evicted = new ImageEvictedS2CPacket(List.of(UUID_A, UUID_B));
        assertEquals(evicted, roundTrip(evicted, ImageEvictedS2CPacket::read));
    }

    @Test
    @DisplayName("ImageUploaded keeps a null position for held posters")
    void imageUploadedNullablePosition() {
        ImageUploadedS2CPacket withPos = new ImageUploadedS2CPacket(POS, UUID_A, 1, 2, 3, 4);
        assertEquals(withPos, roundTrip(withPos, ImageUploadedS2CPacket::read));

        ImageUploadedS2CPacket withoutPos = new ImageUploadedS2CPacket(null, UUID_A, 1, 2, 3, 4);
        assertNull(roundTrip(withoutPos, ImageUploadedS2CPacket::read).pos());
    }

    @Test
    @DisplayName("Image lock sync keeps a null owner when the lock is released")
    void imageLockSyncNullableOwner() {
        SyncImageLockS2CPacket locked = new SyncImageLockS2CPacket(POS, UUID_A, UUID_B);
        assertEquals(locked, roundTrip(locked, SyncImageLockS2CPacket::read));

        SyncImageLockS2CPacket unlocked = new SyncImageLockS2CPacket(POS, UUID_A, null);
        assertNull(roundTrip(unlocked, SyncImageLockS2CPacket::read).playerUuid());
    }

    @Test
    @DisplayName("Signature sync keeps a null position and a null name")
    void signaturePackets() {
        SignPosterC2SPacket sign = new SignPosterC2SPacket(new PosterTarget.World(POS));
        assertEquals(sign, roundTrip(sign, SignPosterC2SPacket::read));

        SyncSignatureS2CPacket signed = new SyncSignatureS2CPacket(POS, "Author");
        assertEquals(signed, roundTrip(signed, SyncSignatureS2CPacket::read));

        SyncSignatureS2CPacket held = new SyncSignatureS2CPacket(null, "Author");
        assertNull(roundTrip(held, SyncSignatureS2CPacket::read).pos());

        SyncSignatureS2CPacket cleared = new SyncSignatureS2CPacket(POS, null);
        assertNull(roundTrip(cleared, SyncSignatureS2CPacket::read).playerName());
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
        SyncImageLayerS2CPacket decoded = roundTrip(packet, SyncImageLayerS2CPacket::read);

        assertEquals(POS, decoded.pos());
        assertEquals(2, decoded.images().size());

        CanvasImage decodedFirst = decoded.images().get(0);
        assertEquals(UUID_A, decodedFirst.uuid);
        assertEquals(1, decodedFirst.gridX);
        assertEquals(2, decodedFirst.gridY);
        assertEquals(6, decodedFirst.gridW);
        assertEquals(7, decodedFirst.gridH);
        assertTrue(decodedFirst.pixelized);

        CanvasImage decodedSecond = decoded.images().get(1);
        assertEquals(UUID_B, decodedSecond.uuid);
        assertEquals(UUID_A, decodedSecond.lockedByPlayer);
    }

    @Test
    @DisplayName("Presence and view packets survive the round trip")
    void presencePackets() {
        CanvasViewC2SPacket view = new CanvasViewC2SPacket(POS, true);
        assertEquals(view, roundTrip(view, CanvasViewC2SPacket::read));

        CanvasCursorC2SPacket cursor = new CanvasCursorC2SPacket(POS, (short) 120, (short) -30);
        assertEquals(cursor, roundTrip(cursor, CanvasCursorC2SPacket::read));

        CanvasCursorS2CPacket cursorS2C = new CanvasCursorS2CPacket(POS, UUID_A, (short) 1, (short) 2);
        assertEquals(cursorS2C, roundTrip(cursorS2C, CanvasCursorS2CPacket::read));

        CanvasPresenceLeaveS2CPacket leave = new CanvasPresenceLeaveS2CPacket(POS, UUID_B);
        assertEquals(leave, roundTrip(leave, CanvasPresenceLeaveS2CPacket::read));

        CanvasEnterRequestC2SPacket enter = new CanvasEnterRequestC2SPacket(POS);
        assertEquals(enter, roundTrip(enter, CanvasEnterRequestC2SPacket::read));

        CanvasEnterAllowedS2CPacket allowed = new CanvasEnterAllowedS2CPacket(POS, true);
        assertEquals(allowed, roundTrip(allowed, CanvasEnterAllowedS2CPacket::read));

        PosterRemovedS2CPacket removed = new PosterRemovedS2CPacket(POS);
        assertEquals(removed, roundTrip(removed, PosterRemovedS2CPacket::read));
    }

    @Test
    @DisplayName("Server settings survive the round trip")
    void serverSettingsRoundTrip() {
        ServerSettingsS2CPacket settings = new ServerSettingsS2CPacket(true, 25, 250);
        assertEquals(settings, roundTrip(settings, ServerSettingsS2CPacket::read));
    }

    @Test
    @DisplayName("Every packet has a unique identifier in the mod namespace")
    void packetIdsAreUniqueAndNamespaced() {
        List<ResourceLocation> ids = List.of(
                SetCanvasSizeC2SPacket.ID,
                SaveCanvasC2SPacket.ID,
                UploadImageC2SPacket.ID,
                MoveCanvasImageC2SPacket.ID,
                DeleteCanvasImageC2SPacket.ID,
                TogglePixelizeC2SPacket.ID,
                LockCanvasImageC2SPacket.ID,
                RequestImageC2SPacket.ID,
                CanvasViewC2SPacket.ID,
                CanvasCursorC2SPacket.ID,
                CanvasEnterRequestC2SPacket.ID,
                SignPosterC2SPacket.ID,
                SyncCanvasS2CPacket.ID,
                PosterRemovedS2CPacket.ID,
                ImageUploadedS2CPacket.ID,
                DeliverImageS2CPacket.ID,
                SyncImageLayerS2CPacket.ID,
                SyncImageLockS2CPacket.ID,
                ImageEvictedS2CPacket.ID,
                CanvasCursorS2CPacket.ID,
                CanvasPresenceLeaveS2CPacket.ID,
                CanvasEnterAllowedS2CPacket.ID,
                ServerSettingsS2CPacket.ID,
                SyncSignatureS2CPacket.ID);

        Set<ResourceLocation> unique = new HashSet<>();
        for (ResourceLocation id : ids) {
            assertEquals("artistry", id.getNamespace(), id + " is not namespaced");
            assertTrue(unique.add(id), "duplicate packet id " + id);
        }
        assertEquals(24, unique.size());
    }

    @Test
    @DisplayName("Each packet reports its own identifier")
    void packetsReportTheirId() {
        assertEquals(PosterRemovedS2CPacket.ID, new PosterRemovedS2CPacket(POS).id());
        assertEquals(RequestImageC2SPacket.ID, new RequestImageC2SPacket(UUID_A).id());
    }
}
