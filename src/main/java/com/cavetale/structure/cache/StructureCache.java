package com.cavetale.structure.cache;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * World container.
 */
public final class StructureCache {
    private final Map<String, StructureWorld> worlds = new HashMap<>();

    public void enable(World world) {
        StructureWorld structureWorld = new StructureWorld(world.getName());
        structureWorld.enable(world);
        worlds.put(world.getName(), structureWorld);
    }

    public void disable(World world) {
        worlds.remove(world.getName()).disable();
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

    public Structure at(String worldName, Vec3i vector) {
        StructureWorld sworld = worlds.get(worldName);
        return sworld != null
            ? sworld.at(vector)
            : null;
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

    public void onChunkLoad(String worldName, int chunkX, int chunkZ) {
        worlds.get(worldName).onChunkLoad(chunkX, chunkZ);
    }

    public void onChunkUnload(String worldName, int chunkX, int chunkZ) {
        worlds.get(worldName).onChunkUnload(chunkX, chunkZ);
    }

    public void addStructure(Structure structure) {
        StructureWorld structureWorld = worlds.get(structure.getWorld());
        if (structureWorld == null) throw new IllegalStateException("World not found: " + structure);
        structureWorld.addStructure(structure);
    }

    /**
     * Save the new JSON data to database.
     * Usually called by Structure#saveJsonData().
     */
    public void updateStructure(Structure structure) {
        StructureWorld structureWorld = worlds.get(structure.getWorld());
        if (structureWorld == null) throw new IllegalStateException("World not found: " + structure);
        structureWorld.updateStructure(structure);
    }

    public NamespacedKey biomeAt(Block block) {
        StructureWorld structureWorld = worlds.get(block.getWorld().getName());
        return structureWorld != null
            ? structureWorld.biomeAt(Vec3i.of(block))
            : null;
    }

    public List<BiomeSection> biomeSections(World world) {
        StructureWorld structureWorld = worlds.get(world.getName());
        return structureWorld != null
            ? structureWorld.biomeSections()
            : null;
    }
}
