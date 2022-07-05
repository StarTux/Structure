package com.cavetale.structure;

import com.cavetale.core.structure.Structures;
import com.cavetale.structure.cache.Structure;
import com.cavetale.structure.cache.StructurePart;
import lombok.RequiredArgsConstructor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;

@RequiredArgsConstructor
public final class CoreStructures implements Structures {
    private final StructurePlugin plugin;

    @Override
    public NamespacedKey structureKeyAt(Block block) {
        Structure structure = plugin.structureCache.at(block);
        return structure != null ? structure.getKey() : null;
    }

    @Override
    public String structurePartNameAt(Block block) {
        Structure structure = plugin.structureCache.at(block);
        if (structure == null) return null;
        StructurePart part = structure.getChildAt(block);
        return part != null ? part.getId() : null;
    }

    @Override
    public boolean structureAt(Block block) {
        return plugin.structureCache.at(block) != null;
    }

    @Override
    public boolean structurePartAt(Block block) {
        Structure structure = plugin.structureCache.at(block);
        return structure != null && structure.getChildAt(block) != null;
    }
}
