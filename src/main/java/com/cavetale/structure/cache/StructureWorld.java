package com.cavetale.structure.cache;

import com.cavetale.structure.sqlite.SQLiteDataStore;
import com.cavetale.structure.struct.Cuboid;
import com.cavetale.structure.struct.Vec2i;
import com.cavetale.structure.struct.Vec3i;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Chunk;
import org.bukkit.World;
import static com.cavetale.structure.StructurePlugin.log;
import static com.cavetale.structure.StructurePlugin.warn;

@RequiredArgsConstructor
public final class StructureWorld {
    private final String worldName;
    private final Map<Integer, Structure> structureCache = new HashMap<>();
    private final Map<Vec2i, StructureRegion> regionCache = new HashMap<>();
    private SQLiteDataStore dataStore;

    protected void enable(World world) {
        for (Chunk chunk : world.getLoadedChunks()) {
            loadChunk(chunk.getX(), chunk.getZ());
        }
        File worldFolder = world.getWorldFolder();
        File sqliteFile = new File(worldFolder, "structures.db");
        if (sqliteFile.exists()) {
            dataStore = new SQLiteDataStore(sqliteFile);
            dataStore.enable();
            log("[" + worldName + "] Data store enabled");
        } else {
            warn("[" + worldName + "] Data store not found");
        }
    }

    protected void disable() {
        structureCache.clear();
        regionCache.clear();
        if (dataStore != null) {
            dataStore.disable();
            dataStore = null;
        }
    }

    public Structure at(Vec3i vec) {
        final int regionX = vec.x >> 9;
        final int regionZ = vec.z >> 9;
        StructureRegion region = getRegion(regionX, regionZ);
        for (Structure structure : region.structures) {
            if (structure.boundingBox.contains(vec)) return structure;
        }
        return null;
    }

    public List<Structure> within(Cuboid cuboid) {
        List<Structure> list = new ArrayList<>();
        final int rax = cuboid.ax >> 9;
        final int raz = cuboid.az >> 9;
        final int rbx = cuboid.bx >> 9;
        final int rbz = cuboid.bz >> 9;
        for (int regionZ = raz; regionZ <= rbz; regionZ += 1) {
            for (int regionX = rax; regionX <= rbx; regionX += 1) {
                StructureRegion region = getRegion(regionX, regionZ);
                for (Structure structure : region.structures) {
                    if (cuboid.overlaps(structure.boundingBox)) {
                        list.add(structure);
                    }
                }
            }
        }
        return list;
    }

    /**
     * Get region from cached if cached, otherwise load it.
     */
    protected StructureRegion getRegion(int x, int z) {
        StructureRegion cached = regionCache.get(Vec2i.of(x, z));
        return cached != null
            ? cached
            : loadRegion(x, z);
    }

    protected StructureRegion loadRegion(int x, int z) {
        log("[" + worldName + "] Loading region: " + x + ", " + z);
        StructureRegion result = new StructureRegion();
        if (dataStore != null) {
            for (int id : dataStore.getStructureRefs(x, z)) {
                Structure structure = getStructure(id);
                if (structure != null) {
                    result.structures.add(structure);
                }
            }
        }
        return result;
    }

    protected Structure getStructure(int id) {
        Structure cached = structureCache.get(id);
        return cached != null
            ? cached
            : loadStructure(id);
    }

    protected Structure loadStructure(int id) {
        return dataStore != null
            ? new Structure(dataStore.getStructure(id), worldName)
            : null;
    }

    /**
     * Ensure the region containing this chunk is cached and bump its
     * reference count.
     * Do the same for structures contained in the region, provided
     * the region is newly cached.
     */
    protected void loadChunk(int chunkX, int chunkZ) {
        final int regionX = chunkX >> 5;
        final int regionZ = chunkZ >> 5;
        StructureRegion region = getRegion(regionX, regionZ);
        if (region.referenceCount == 0) {
            Vec2i vec = Vec2i.of(regionX, regionZ);
            log("[" + worldName + "] New region cached: " + vec);
            regionCache.put(vec, region);
            for (Structure structure : region.structures) {
                if (structure.referenceCount == 0) {
                    structureCache.put(structure.getId(), structure);
                }
                structure.referenceCount += 1;
            }
        }
        region.referenceCount += 1;
    }

    protected void unloadChunk(int chunkX, int chunkZ) {
        final int regionX = chunkX >> 5;
        final int regionZ = chunkZ >> 5;
        Vec2i vec = Vec2i.of(regionX, regionZ);
        StructureRegion region = regionCache.get(vec);
        if (region == null) {
            throw new IllegalStateException("Unloaded region not cached: " + vec);
        }
        region.referenceCount -= 1;
        if (region.referenceCount <= 0) {
            log("[" + worldName + "] Region evicted: " + vec);
            regionCache.remove(vec);
            for (Structure structure : region.structures) {
                structure.referenceCount -= 1;
                if (structure.referenceCount <= 0) {
                    structureCache.remove(structure.getId());
                }
            }
        }
    }
}
