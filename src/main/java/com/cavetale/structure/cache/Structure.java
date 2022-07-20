package com.cavetale.structure.cache;

import com.cavetale.core.util.Json;
import com.cavetale.structure.sqlite.SQLStructure;
import com.cavetale.structure.struct.Cuboid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;

@Data
public final class Structure implements Keyed {
    /** The index is unique per world. */
    protected final int id;
    protected final String world;
    protected final NamespacedKey key;
    protected final Cuboid boundingBox;
    protected final List<StructurePart> children = new ArrayList<>();
    protected int referenceCount; // count referencing StructureRegion instances

    protected Structure(final SQLStructure row, final String world) {
        this.id = row.getId();
        this.world = world;
        this.key = NamespacedKey.fromString(row.getType());
        this.boundingBox = row.getCuboid();
        if (key.getNamespace().equals("minecraft")) {
            parseVanillaChildren(row);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseVanillaChildren(SQLStructure row) {
        Map<String, Object> structureMap = (Map<String, Object>) Json.deserialize(row.getJson(), Map.class);
        List<Map<String, Object>> childMaps = (List<Map<String, Object>>) structureMap.get("Children");
        if (childMaps == null) throw new IllegalArgumentException("Missing children: " + structureMap);
        for (Map<String, Object> childMap : childMaps) {
            StructurePart part = new StructurePart(childMap);
            children.add(part);
        }
    }

    public StructurePart getChildAt(int x, int y, int z) {
        for (StructurePart child : children) {
            if (child.boundingBox.contains(x, y, z)) return child;
        }
        return null;
    }

    public StructurePart getChildAt(Block block) {
        return getChildAt(block.getX(), block.getY(), block.getZ());
    }

    @Override
    public NamespacedKey getKey() {
        return key;
    }

    public boolean isVanilla() {
        return key.getNamespace().equals("minecraft");
    }
}
