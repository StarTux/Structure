package com.cavetale.structure.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;

@Data
public final class Structure implements Keyed {
    /** The index is unique per world and storage file. */
    protected final int id;
    protected final String world;
    protected final NamespacedKey key;
    protected final Cuboid boundingBox;
    protected final List<StructurePart> children = new ArrayList<>();

    @SuppressWarnings("unchecked")
    protected Structure(final int id, final String world, final Map<String, Object> structureMap) {
        this.id = id;
        this.world = world;
        this.key = NamespacedKey.fromString((String) structureMap.get("id"));
        if (this.key == null) throw new IllegalArgumentException("Invalid key: " + key);
        List<Map<String, Object>> childMaps = (List<Map<String, Object>>) structureMap.get("Children");
        if (childMaps == null) throw new IllegalArgumentException("Missing children: " + structureMap);
        for (Map<String, Object> childMap : childMaps) {
            StructurePart part = new StructurePart(childMap);
            children.add(part);
        }
        int ax = children.get(0).boundingBox.ax;
        int ay = children.get(0).boundingBox.ay;
        int az = children.get(0).boundingBox.az;
        int bx = children.get(0).boundingBox.bx;
        int by = children.get(0).boundingBox.by;
        int bz = children.get(0).boundingBox.bz;
        for (StructurePart child : children) {
            ax = Math.min(ax, child.boundingBox.ax);
            ay = Math.min(ay, child.boundingBox.ay);
            az = Math.min(az, child.boundingBox.az);
            bx = Math.max(bx, child.boundingBox.bx);
            by = Math.max(by, child.boundingBox.by);
            bz = Math.max(bz, child.boundingBox.bz);
        }
        this.boundingBox = new Cuboid(ax, ay, az, bx, by, bz);
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
