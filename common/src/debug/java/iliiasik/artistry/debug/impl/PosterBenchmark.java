package iliiasik.artistry.debug.impl;

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

    public static int place(ServerPlayer player, int count, int canvasSize, int imagesPerPoster) {
        ServerLevel level = player.serverLevel();
        Direction facing = player.getDirection();
        Direction posterFacing = facing.getOpposite();
        Direction right = facing.getClockWise();

        BlockPos origin = player.blockPosition().relative(facing, WALL_DISTANCE);
        BlockState support = Blocks.STONE.defaultBlockState();
        BlockState poster = ModBlocks.POSTER.get().defaultBlockState().setValue(PosterBlock.FACING, posterFacing);

        int width = (int) Math.ceil(Math.sqrt(count));
        int placed = 0;
        Random random = new Random(count * 31L + canvasSize);

        for (int index = 0; index < count; index++) {
            int column = index % width;
            int row = index / width;

            BlockPos pos = origin.relative(right, column - width / 2).above(row);
            if (pos.getY() >= level.getMaxBuildHeight() - 1) break;

            level.setBlock(pos.relative(facing), support, 3);
            level.setBlock(pos, poster, 3);

            if (level.getBlockEntity(pos) instanceof PosterBlockEntity entity) {
                fillCanvas(entity.canvasData, canvasSize, random);
                fillImages(entity.imageLayer, canvasSize, imagesPerPoster, random);
                entity.markDirtyAndSync();
                placed++;
            }
        }

        return placed;
    }

    public static int clear(ServerLevel level, BlockPos center, int radius) {
        int removed = 0;
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.getBlockState(pos).is(ModBlocks.POSTER.get())) continue;
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
                UUID uuid = ImageStorage.save(noisePng(IMAGE_PIXELS, IMAGE_PIXELS, random));
                int offset = i * Math.max(1, canvasSize / 8);
                int x = Math.clamp(offset, 0, canvasSize - size);
                int y = Math.clamp(offset, 0, canvasSize - size);
                CanvasImage image = new CanvasImage(uuid, x, y, size, size);
                image.pixelized = i % 2 == 1;
                layer.addImage(image);
            } catch (IOException ignored) {
                return;
            }
        }
    }

    private static byte[] noisePng(int width, int height, Random random) throws IOException {
        byte[] raw = new byte[height * (1 + width * 4)];
        int cursor = 0;
        for (int y = 0; y < height; y++) {
            raw[cursor++] = 0;
            for (int x = 0; x < width; x++) {
                raw[cursor++] = (byte) random.nextInt(256);
                raw[cursor++] = (byte) random.nextInt(256);
                raw[cursor++] = (byte) random.nextInt(256);
                raw[cursor++] = (byte) 0xFF;
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

        ByteBuffer header = ByteBuffer.allocate(13);
        header.putInt(width);
        header.putInt(height);
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
