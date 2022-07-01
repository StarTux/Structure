package com.cavetale.structure.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.bukkit.StructureType;
import org.bukkit.block.Block;
import static java.util.Objects.requireNonNull;

@Data
public final class Structure {
    protected final StructureType type;
    protected final Cuboid cuboid;
    protected final String world;
    protected final List<StructurePart> children = new ArrayList<>();

    @SuppressWarnings("unchecked")
    protected Structure(final String world, final String key, final Map<String, Object> structureMap) {
        this.world = world;
        this.type = findStructureType(key);
        List<Map<String, Object>> childMaps = (List<Map<String, Object>>) structureMap.get("Children");
        if (childMaps == null) throw new IllegalArgumentException("Missing children: " + structureMap);
        for (Map<String, Object> childMap : childMaps) {
            StructurePart part = new StructurePart(childMap);
            children.add(part);
        }
        int ax = children.get(0).cuboid.ax;
        int ay = children.get(0).cuboid.ay;
        int az = children.get(0).cuboid.az;
        int bx = children.get(0).cuboid.bx;
        int by = children.get(0).cuboid.by;
        int bz = children.get(0).cuboid.bz;
        for (StructurePart child : children) {
            ax = Math.min(ax, child.cuboid.ax);
            ay = Math.min(ay, child.cuboid.ay);
            az = Math.min(az, child.cuboid.az);
            bx = Math.max(bx, child.cuboid.bx);
            by = Math.max(by, child.cuboid.by);
            bz = Math.max(bz, child.cuboid.bz);
        }
        this.cuboid = new Cuboid(ax, ay, az, bx, by, bz);
    }

    private static StructureType findStructureType(String key) {
        if (key.startsWith("minecraft:")) key = key.substring(10);
        StructureType result;
        result = StructureType.getStructureTypes().get(key);
        if (result != null) return result;
        String[] toks = key.split("_");
        key = String.join("_", Arrays.copyOfRange(toks, 0, toks.length - 1));
        result = StructureType.getStructureTypes().get(key);
        if (result != null) return result;
        throw new IllegalArgumentException("StructureType=" + key);
    }

    public StructurePart getChildAt(int x, int y, int z) {
        for (StructurePart child : children) {
            if (child.cuboid.contains(x, y, z)) return child;
        }
        return null;
    }

    public StructurePart getChildAt(Block block) {
        return getChildAt(block.getX(), block.getY(), block.getZ());
    }
}
