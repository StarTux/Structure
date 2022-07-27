package com.cavetale.structure.cache;

import com.cavetale.core.struct.Vec3i;
import com.cavetale.core.util.Json;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value @RequiredArgsConstructor
public final class BiomeSection {
    private final int chunkX;
    private final int chunkY;
    private final int chunkZ;
    private final String[] palette;
    private final long[] data;

    private static final class Tag {
        String[] palette;
        long[] data;
    }

    public BiomeSection(final int chunkX, final int chunkY, final int chunkZ, final String json) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        Tag tag = Json.deserialize(json, Tag.class);
        this.palette = tag.palette;
        this.data = tag.data;
    }

    public Vec3i getChunkVector() {
        return Vec3i.of(chunkX, chunkY, chunkZ);
    }

    /**
     * x, y, z in [0 .. 15]
     */
    public String getBiome(int x, int y, int z) {
        if (data == null || data.length == 0) {
            return palette[0];
        }
        int bitSize = 1;
        int mask = 1;
        while ((1 << bitSize) < data.length) {
            bitSize += 1;
            mask = (mask << 1) | 1;
        }
        final int index = y * 4// / 4 * 16
            + z// / 4 * 4
            + x / 4;
        final int bitIndex = index * bitSize;
        final int arrayIndex = bitIndex / 64;
        final int innerBitIndex = bitIndex & 63;
        int value = (int) ((data[arrayIndex] >> innerBitIndex) & mask);
        return palette[value];
    }
}
