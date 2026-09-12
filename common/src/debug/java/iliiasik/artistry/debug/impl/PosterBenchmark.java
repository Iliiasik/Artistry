package iliiasik.artistry.debug.impl;

import iliiasik.artistry.block.BannerBlock;
import iliiasik.artistry.block.BannerPart;
import iliiasik.artistry.block.ModBlocks;
import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.network.ArtistryNetwork;
import iliiasik.artistry.server.ImageStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public final class PosterBenchmark {

    private static final int PALETTE_LIMIT = 200;
    private static final int IMAGE_PIXELS = 128;
    private static final int WALL_DISTANCE = 6;

    private PosterBenchmark() {}

    public static int place(ServerLevel level, ServerPlayer player, int count, int canvasSize,
                            int imagesPerPoster, BenchmarkKind kind) {
        Direction facing = player.getDirection();
        Direction canvasFacing = facing.getOpposite();
        Direction right = facing.getClockWise();

        BlockPos origin = player.blockPosition().relative(facing, WALL_DISTANCE);
        int step = kind.slotSize();
        int width = (int) Math.ceil(Math.sqrt(count));
        int placed = 0;
        Random random = new Random(count * 31L + canvasSize);

        for (int index = 0; index < count; index++) {
            int column = index % width;
            int row = index / width;

            BlockPos pos = origin.relative(right, (column - width / 2) * step).above((int) ((long) row * step));
            if (pos.getY() + step >= level.getMaxBuildHeight() - 1) break;

            BlockPos filled = kind.bannerAt(index)
                    ? placeBanner(level, pos, facing, canvasFacing)
                    : placePoster(level, pos, facing, canvasFacing);

            if (level.getBlockEntity(filled) instanceof PosterBlockEntity entity) {
                fillCanvas(entity.canvasData, canvasSize, random);
                fillImages(entity.imageLayer, canvasSize, imagesPerPoster, random);
                entity.markDirtyAndSync();
                placed++;
            }
        }

        return placed;
    }

    private static BlockPos placePoster(ServerLevel level, BlockPos pos,
                                        Direction facing, Direction canvasFacing) {
        level.setBlock(pos.relative(facing), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, ModBlocks.POSTER.get().defaultBlockState()
                .setValue(PosterBlock.FACING, canvasFacing), 3);
        return pos;
    }

    private static BlockPos placeBanner(ServerLevel level, BlockPos pos,
                                        Direction facing, Direction canvasFacing) {
        BlockState base = ModBlocks.BANNER.get().defaultBlockState()
                .setValue(BannerBlock.FACING, canvasFacing);

        for (BannerPart part : BannerPart.values()) {
            BlockPos partPos = BannerBlock.partPos(pos, canvasFacing, part);
            level.setBlock(partPos.relative(facing), Blocks.STONE.defaultBlockState(), 3);
        }
        for (BannerPart part : BannerPart.values()) {
            BlockPos partPos = BannerBlock.partPos(pos, canvasFacing, part);
            level.setBlock(partPos, base.setValue(BannerBlock.PART, part), 3);
        }
        return pos;
    }

    public static int clear(ServerLevel level, BlockPos center, int radius) {
        int removed = 0;
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.POSTER.get()) && !state.is(ModBlocks.BANNER.get())) continue;
            BlockPos immutable = pos.immutable();
            ArtistryNetwork.broadcastPosterRemoved(level, immutable);
            level.removeBlockEntity(immutable);
            level.setBlock(immutable, Blocks.AIR.defaultBlockState(), 3);
            removed++;
        }

        return removed;
    }

    private static void fillCanvas(CanvasData data, int canvasSize, Random random) {
        data.canvasSize = canvasSize;
        for (int y = 0; y < canvasSize; y++) {
            for (int x = 0; x < canvasSize; x++) {
                if (random.nextInt(3) == 0) {
                    data.setColor(x, y, 0xFF000000 | random.nextInt(0x1000000));
                } else {
                    data.pixels[y][x] = (short) (1 + random.nextInt(PALETTE_LIMIT));
                    data.colors[y][x] = 0;
                }
            }
        }
    }

    private static void fillImages(CanvasImageLayer layer, int canvasSize, int imagesPerPoster, Random random) {
        int size = Math.max(CanvasImage.MIN_GRID, canvasSize / 2);
        for (int i = 0; i < imagesPerPoster; i++) {
            try {
                UUID uuid = ImageStorage.save(noisePng(random));
                int offset = i * Math.max(1, canvasSize / 8);
                int x = Mth.clamp(offset, 0, canvasSize - size);
                int y = Mth.clamp(offset, 0, canvasSize - size);
                CanvasImage image = new CanvasImage(uuid, x, y, size, size);
                image.pixelized = i % 2 == 1;
                layer.addImage(image);
            } catch (IOException ignored) {
                return;
            }
        }
    }

    private static byte[] noisePng(Random random) throws IOException {
        byte[] raw = new byte[PosterBenchmark.IMAGE_PIXELS * (1 + PosterBenchmark.IMAGE_PIXELS * 4)];
        int cursor = 0;
        for (int y = 0; y < PosterBenchmark.IMAGE_PIXELS; y++) {
            raw[cursor++] = 0;
            for (int x = 0; x < PosterBenchmark.IMAGE_PIXELS; x++) {
                raw[cursor++] = (byte) random.nextInt(256);
                raw[cursor++] = (byte) random.nextInt(256);
                raw[cursor++] = (byte) random.nextInt(256);
                raw[cursor++] = (byte) 0xFF;
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

        ByteBuffer header = ByteBuffer.allocate(13);
        header.putInt(PosterBenchmark.IMAGE_PIXELS);
        header.putInt(PosterBenchmark.IMAGE_PIXELS);
        header.put((byte) 8);
        header.put((byte) 6);
        header.put((byte) 0);
        header.put((byte) 0);
        header.put((byte) 0);

        writeChunk(out, "IHDR", header.array());
        writeChunk(out, "IDAT", deflate(raw));
        writeChunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try {
            deflater.setInput(data);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(data.length / 2);
            byte[] buffer = new byte[16 * 1024];
            while (!deflater.finished()) {
                int written = deflater.deflate(buffer);
                out.write(buffer, 0, written);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static void writeChunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        out.write(ByteBuffer.allocate(4).putInt(data.length).array());
        out.write(typeBytes);
        out.write(data);

        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }
}
