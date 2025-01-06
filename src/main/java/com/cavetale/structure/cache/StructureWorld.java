package com.cavetale.structure.cache;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.structure.event.StructureLoadEvent;
import com.cavetale.structure.event.StructureUnloadEvent;
import com.cavetale.structure.sqlite.SQLiteDataStore;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.scheduler.BukkitTask;
import static com.cavetale.structure.StructurePlugin.log;
import static com.cavetale.structure.StructurePlugin.structurePlugin;
import static com.cavetale.structure.StructurePlugin.warn;
import static java.util.Objects.requireNonNull;

@Getter
@RequiredArgsConstructor
public final class StructureWorld {
    private final String worldName;
    private final Map<Integer, Structure> structureCache = new HashMap<>();
    private final Map<Vec2i, StructureRegion> regionCache = new HashMap<>();
    private File sqliteFile;
    private SQLiteDataStore dataStore;
    private BukkitTask pruneTask;

    protected void enable(World world) {
        sqliteFile = new File(world.getWorldFolder(), "structures.db");
        if (sqliteFile.exists()) {
            dataStore = new SQLiteDataStore(worldName, sqliteFile);
            dataStore.enable();
            log("[" + worldName + "] Data store enabled");
        } else {
            warn("[" + worldName + "] Data store not found");
        }
        pruneTask = Bukkit.getScheduler().runTaskTimer(structurePlugin(), this::prune, 200L, 200L);
        for (Chunk chunk : world.getLoadedChunks()) {
            onChunkLoad(chunk.getX(), chunk.getZ());
        }
    }

    protected void disable() {
        structureCache.clear();
        regionCache.clear();
        if (dataStore != null) {
            dataStore.disable();
            dataStore = null;
        }
        if (pruneTask != null) {
            pruneTask.cancel();
            pruneTask = null;
        }
    }

    /**
     * This class is lazy with the generation of the data store.  When
     * no file is found, the data store will never be created.  This
     * method asserts that it exists and is ready.
     */
    public SQLiteDataStore getOrCreateDataStore() {
        if (dataStore == null) {
            World world = requireNonNull(Bukkit.getWorld(worldName));
            dataStore = new SQLiteDataStore(worldName, sqliteFile);
            dataStore.enable();
            log("[" + worldName + "] Data store enabled");
        }
        return dataStore;
    }

    public Structure getOrLoadStructure(int id) {
        Structure result = getStructure(id);
        return result != null
            ? result
            : loadStructure(id);
    }

    public Structure at(Vec3i vec) {
        final int regionX = vec.x >> 9;
        final int regionZ = vec.z >> 9;
        StructureRegion region = getRegion(regionX, regionZ);
        for (Structure structure : region.structures) {
            if (!structure.boundingBox.contains(vec)) continue;
            if (structure.hasChildren() && !structure.childContains(vec)) continue;
            return structure;
        }
        return null;
    }

    public List<Structure> allAt(Vec3i vec) {
        final int regionX = vec.x >> 9;
        final int regionZ = vec.z >> 9;
        StructureRegion region = getRegion(regionX, regionZ);
        final List<Structure> result = new ArrayList<>();
        for (Structure structure : region.structures) {
            if (structure.boundingBox.contains(vec)) result.add(structure);
        }
        return result;
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
                    if (list.contains(structure)) {
                        continue;
                    }
                    if (cuboid.overlaps(structure.boundingBox)) {
                        list.add(structure);
                    }
                }
            }
        }
        return list;
    }

    public List<Structure> allLoaded() {
        return List.copyOf(structureCache.values());
    }

    /**
     * Get region from cached if cached, otherwise load it.
     * This will not increase the referenceCount but will put the
     * region in the cache and update its lastUse timestamp.
     */
    protected StructureRegion getRegion(int x, int z) {
        StructureRegion region = regionCache.get(Vec2i.of(x, z));
        if (region == null) {
            region = loadRegion(x, z);
            Vec2i vec = Vec2i.of(x, z);
            regionCache.put(vec, region);
        }
        region.lastUse = System.currentTimeMillis();
        for (Structure structure : region.structures) {
            if (structure.referenceCount == 0) {
                structureCache.put(structure.getId(), structure);
                new StructureLoadEvent(structure).callEvent();
            }
            structure.referenceCount += 1;
        }
        return region;
    }

    private StructureRegion loadRegion(int x, int z) {
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

    private Structure getStructure(int id) {
        Structure cached = structureCache.get(id);
        return cached != null
            ? cached
            : loadStructure(id);
    }

    private Structure loadStructure(int id) {
        return dataStore != null
            ? dataStore.getStructure(id)
            : null;
    }

    /**
     * Ensure the region containing this chunk is cached and bump its
     * reference count.
     * Do the same for structures contained in the region, provided
     * the region is newly cached.
     */
    protected void onChunkLoad(int chunkX, int chunkZ) {
        final int regionX = chunkX >> 5;
        final int regionZ = chunkZ >> 5;
        StructureRegion region = getRegion(regionX, regionZ);
        region.referenceCount += 1;
    }

    protected void onChunkUnload(int chunkX, int chunkZ) {
        final int regionX = chunkX >> 5;
        final int regionZ = chunkZ >> 5;
        Vec2i vec = Vec2i.of(regionX, regionZ);
        StructureRegion region = regionCache.get(vec);
        if (region == null) {
            throw new IllegalStateException("Unloaded region not cached: " + vec);
        }
        region.referenceCount -= 1;
        tryToEvict(vec, region);
    }

    /**
     * Unload one chunks if it does not have references and has not
     * been in use (lastUse) for at least 10 seconds.
     */
    private void tryToEvict(Vec2i vec, StructureRegion region) {
        if (region.referenceCount > 0) return;
        if (region.lastUse > System.currentTimeMillis() - 10_000L) return;
        regionCache.remove(vec);
        for (Structure structure : region.structures) {
            structure.referenceCount -= 1;
            if (structure.referenceCount <= 0) {
                new StructureUnloadEvent(structure).callEvent();
                structureCache.remove(structure.getId());
            }
        }
    }

    /**
     * Try to unload all regions via StructureWorld#tryToEvict.
     */
    private void prune() {
        for (Vec2i vec : List.copyOf(regionCache.keySet())) {
            StructureRegion region = regionCache.get(vec);
            tryToEvict(vec, region);
        }
    }

    protected void addStructure(Structure structure) {
        List<Vec2i> regions = getOrCreateDataStore().addStructure(structure);
        for (Vec2i region : regions) {
            StructureRegion structureRegion = regionCache.get(region);
            if (structureRegion != null) {
                structureRegion.structures.add(structure);
                structure.referenceCount += 1;
            }
        }
        if (structure.referenceCount > 0) {
            structureCache.put(structure.getId(), structure);
        }
    }

    protected void updateStructure(Structure structure) {
        getOrCreateDataStore().updateStructureJson(structure);
    }

    protected void updateDiscovered(Structure structure) {
        getOrCreateDataStore().updateDiscovered(structure);
    }

    protected Biome biomeAt(Vec3i vec) {
        if (dataStore == null) return null;
        return dataStore.getChunkBiome(vec.x >> 4, vec.z >> 4);
    }

    protected Map<Vec2i, Biome> allBiomes() {
        return dataStore != null
            ? dataStore.getAllBiomes()
            : Map.of();
    }
}
