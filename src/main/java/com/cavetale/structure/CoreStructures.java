package com.cavetale.structure;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.structure.Structures;
import com.cavetale.structure.cache.Structure;
import com.cavetale.structure.cache.StructurePart;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;

@RequiredArgsConstructor
public final class CoreStructures implements Structures {
    private final StructurePlugin plugin;

    @Override
    public NamespacedKey structureKeyAt(Block block) {
        Structure structure = plugin.getStructureCache().at(block);
        return structure != null ? structure.getKey() : null;
    }

    @Override
    public String structurePartNameAt(Block block) {
        Structure structure = plugin.getStructureCache().at(block);
        if (structure == null) return null;
        StructurePart part = structure.getChildAt(block);
        return part != null ? part.getId() : null;
    }

    @Override
    public boolean structureAt(Block block) {
        return plugin.getStructureCache().at(block) != null;
    }

    @Override
    public boolean structurePartAt(Block block) {
        Structure structure = plugin.getStructureCache().at(block);
        return structure != null && structure.getChildAt(block) != null;
    }

    @Override
    public com.cavetale.core.structure.Structure getStructureAt(Block block) {
        Structure structure = plugin.getStructureCache().at(block);
        return structure;
    }

    @Override
    public List<com.cavetale.core.structure.Structure> getAllStructuresAt(Block block) {
        List<com.cavetale.core.structure.Structure> result = new ArrayList<>();
        for (Structure it : plugin.getStructureCache().allAt(block)) {
            result.add(it);
        }
        return result;
    }

    @Override
    public List<com.cavetale.core.structure.Structure> getStructuresWithin(World world, Cuboid cuboid) {
        List<com.cavetale.core.structure.Structure> result = new ArrayList<>();
        for (Structure it : plugin.getStructureCache().within(world.getName(), cuboid)) {
            result.add(it);
        }
        return result;
    }
}
