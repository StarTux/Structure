package com.cavetale.structure.cache;

import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.core.util.Json;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import static java.util.Objects.requireNonNull;

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
        this.palette = requireNonNull(tag.palette);
        this.data = tag.data;
    }

    public Vec3i getSectionVector() {
        return Vec3i.of(chunkX, chunkY, chunkZ);
    }

    public Vec2i getChunkVector() {
        return Vec2i.of(chunkX, chunkZ);
    }

    /**
     * blockX, blockY, blockZ in [0 .. 15]
     */
    public String getBiomeName(int blockX, int blockY, int blockZ) {
        if (data == null || data.length == 0) {
            return palette[0];
        }
        int bitSize = 1;
        int mask = 1;
        while ((1 << bitSize) < data.length) {
            bitSize += 1;
            mask = (mask << 1) | 1;
        }
        final int index = blockY * 4
            + blockZ
            + blockX / 4;
        final int bitIndex = index * bitSize;
        final int arrayIndex = bitIndex / 64;
        final int innerBitIndex = bitIndex & 63;
        final int value = (int) ((data[arrayIndex] >> innerBitIndex) & mask);
        return palette[value];
    }

    public List<String> getPalette() {
        return List.of(palette);
    }

    public List<Integer> getBiomeIndexes() {
        List<Integer> result = new ArrayList<>(16);
        if (data == null || data.length == 0) {
            for (int i = 0; i < 16; i += 1) {
                result.add(0);
            }
            return result;
        } else {
            int bitSize = 1;
            int mask = 1;
            while ((1 << bitSize) < data.length) {
                bitSize += 1;
                mask = (mask << 1) | 1;
            }
            for (long entry : data) {
                for (int innerBitIndex = 0; innerBitIndex < 64; innerBitIndex += bitSize) {
                    final int value = (int) ((entry >> innerBitIndex) & mask);
                    result.add(value);
                }
            }
            return result;
        }
    }

    public List<String> getBiomeNames() {
        List<String> result = new ArrayList<>(16);
        for (int index : getBiomeIndexes()) {
            result.add(palette[index]);
        }
        return result;
    }

    public List<NamespacedKey> getBiomeKeys() {
        List<NamespacedKey> result = new ArrayList<>(16);
        for (String name : getBiomeNames()) {
            result.add(NamespacedKey.fromString(name));
        }
        return result;
    }

    public List<Biome> getBiomes() {
        List<Biome> result = new ArrayList<>(16);
        for (NamespacedKey key : getBiomeKeys()) {
            Biome biome = Registry.BIOME.get(key);
            if (biome != null) {
                result.add(biome);
            }
        }
        return result;
    }
}
