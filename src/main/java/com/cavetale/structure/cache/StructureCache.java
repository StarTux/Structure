package com.cavetale.structure.cache;

import com.cavetale.core.util.Json;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

/**
 * Store all structures in one world for fast spatial lookup.  This
 * contaier is NOT aware of mirror worlds!
 */
public final class StructureCache {
    protected final Map<String, SpatialStructureCache> worlds = new HashMap<>();

    @SuppressWarnings("unchecked")
    public int load(World world) {
        File file = new File(world.getWorldFolder(), "structures.txt");
        if (!file.exists()) return 0;
        worlds.put(world.getName(), new SpatialStructureCache());
        int count = 0;
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while (true) {
                line = in.readLine();
                if (line == null) break;
                Map<String, Object> map = (Map<String, Object>) Json.deserialize(line, Map.class);
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Structure structure;
                    try {
                        structure = new Structure(world.getName(), entry.getKey(), (Map<String, Object>) entry.getValue());
                    } catch (IllegalArgumentException iaee) {
                        iaee.printStackTrace();
                        continue;
                    }
                    add(structure);
                    count += 1;
                }
            }
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        return count;
    }

    public void unload(World world) {
        worlds.remove(world.getName());
    }

    public void add(Structure structure) {
        SpatialStructureCache cache = worlds.get(structure.getWorld());
        if (cache != null) cache.insert(structure);
    }

    public void remove(Structure structure) {
        SpatialStructureCache cache = worlds.get(structure.getWorld());
        if (cache != null) cache.remove(structure);
    }

    public void resize(Structure structure, Cuboid oldCuboid, Cuboid newCuboid) {
        SpatialStructureCache cache = worlds.get(structure.getWorld());
        if (cache == null) return;
        cache.update(structure, oldCuboid, newCuboid);
    }

    public void clear() {
        worlds.clear();
    }

    public Structure at(Block block) {
        Structure structure = at(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        return structure != null && structure.cuboid.contains(block)
            ? structure
            : null;
    }

    public Structure at(final String world, int x, int y, int z) {
        SpatialStructureCache spatial = worlds.get(world);
        if (spatial == null) return null;
        return spatial.findStructureAt(x, y, z);
    }

    public List<Structure> within(final String world, Cuboid cuboid) {
        SpatialStructureCache spatial = worlds.get(world);
        if (spatial == null) return List.of();
        return spatial.findStructuresWithin(cuboid);
    }

    public List<Structure> inWorld(final String world) {
        SpatialStructureCache spatial = worlds.get(world);
        if (spatial == null) return List.of();
        return spatial.allStructures;
    }

    public void debug(CommandSender sender, String worldName) {
        SpatialStructureCache spatial = worlds.get(worldName);
        if (spatial == null) {
            sender.sendMessage("No cache: " + worldName);
            return;
        }
        List<SpatialStructureCache.XYSlot> slots = spatial.getAllSlots();
        Collections.sort(slots, (a, b) -> Integer.compare(a.structures.size(), b.structures.size()));
        int len = SpatialStructureCache.CHUNK_SIZE;
        int structureCount = 0;
        for (SpatialStructureCache.XYSlot slot : slots) {
            structureCount += slot.structures.size();
            sender.sendMessage("Slot " + slot.x + "," + slot.y
                               + " (" + (slot.x * len) + "," + (slot.y * len) + ")"
                               + ": " + slot.structures.size() + " structures");
        }
        sender.sendMessage("Total " + slots.size() + " slots, " + structureCount + " structures");
    }
}
