package com.cavetale.structure.cache;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

/**
 * World container.
 */
@Getter
public final class StructureCache {
    private final Map<String, StructureWorld> worlds = new HashMap<>();

    public StructureWorld enable(World world) {
        final StructureWorld old = worlds.get(world.getName());
        if (old != null) return old;
        StructureWorld structureWorld = new StructureWorld(world.getName());
        structureWorld.enable(world);
        worlds.put(world.getName(), structureWorld);
        return structureWorld;
    }

    public void disable(World world) {
        worlds.remove(world.getName()).disable();
    }

    public StructureWorld getWorld(World world) {
        StructureWorld result = worlds.get(world.getName());
        if (result == null) {
            result = enable(world);
        }
        return result;
    }

    public void disable() {
        for (StructureWorld it : worlds.values()) {
            it.disable();
        }
        worlds.clear();
    }

    public Structure at(Block block) {
        return at(block.getWorld().getName(), Vec3i.of(block));
    }

    public List<Structure> allAt(Block block) {
        return allAt(block.getWorld().getName(), Vec3i.of(block));
    }

    public Structure at(String worldName, Vec3i vector) {
        StructureWorld sworld = worlds.get(worldName);
        return sworld != null
            ? sworld.at(vector)
            : null;
    }

    public List<Structure> allAt(String worldName, Vec3i vector) {
        StructureWorld sworld = worlds.get(worldName);
        return sworld != null
            ? sworld.allAt(vector)
            : List.of();
    }

    public List<Structure> within(String worldName, Cuboid cuboid) {
        StructureWorld sworld = worlds.get(worldName);
        return sworld != null
            ? sworld.within(cuboid)
            : List.of();
    }

    public List<Structure> within(String worldName, Cuboid cuboid, NamespacedKey key) {
        StructureWorld sworld = worlds.get(worldName);
        if (sworld == null) return List.of();
        List<Structure> result = new ArrayList<>();
        for (Structure structure : sworld.within(cuboid)) {
            if (key.equals(structure.getKey())) {
                result.add(structure);
            }
        }
        return result;
    }

    public List<Structure> allLoaded(String worldName) {
        StructureWorld sworld = worlds.get(worldName);
        return sworld != null
            ? sworld.allLoaded()
            : List.of();
    }

    public void onChunkLoad(World world, int chunkX, int chunkZ) {
        getWorld(world).onChunkLoad(chunkX, chunkZ);
    }

    public void onChunkUnload(World world, int chunkX, int chunkZ) {
        final var structureWorld = worlds.get(world.getName());
        if (structureWorld == null) return;
        structureWorld.onChunkUnload(chunkX, chunkZ);
    }

    public void addStructure(Structure structure) {
        StructureWorld structureWorld = worlds.get(structure.getWorldName());
        if (structureWorld == null) throw new IllegalStateException("World not found: " + structure);
        structureWorld.addStructure(structure);
    }

    /**
     * Save the new JSON data to database.
     * Usually called by Structure#saveJsonData().
     */
    public void updateStructure(Structure structure) {
        StructureWorld structureWorld = worlds.get(structure.getWorldName());
        if (structureWorld == null) {
            throw new IllegalStateException("World not found: " + structure);
        }
        structureWorld.updateStructure(structure);
    }

    /**
     * Save the new discovered value to database.
     * Usually called by Structure#setDiscovered().
     */
    public void updateDiscovered(Structure structure) {
        StructureWorld structureWorld = worlds.get(structure.getWorldName());
        if (structureWorld == null) {
            throw new IllegalStateException("World not found: " + structure);
        }
        structureWorld.updateDiscovered(structure);
    }

    public Biome biomeAt(Block block) {
        StructureWorld structureWorld = worlds.get(block.getWorld().getName());
        return structureWorld != null
            ? structureWorld.biomeAt(Vec3i.of(block))
            : null;
    }

    public Map<Vec2i, Biome> allBiomes(World world) {
        return allBiomes(world.getName());
    }

    public Map<Vec2i, Biome> allBiomes(String worldName) {
        StructureWorld structureWorld = worlds.get(worldName);
        return structureWorld != null
            ? structureWorld.allBiomes()
            : Map.of();
    }
}
