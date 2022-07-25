package com.cavetale.structure.cache;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.util.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.Data;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import static com.cavetale.structure.StructurePlugin.structureCache;

@Data
public final class Structure implements Keyed {
    /** The index is unique per world. */
    protected int id;
    protected final String world;
    protected final NamespacedKey key;
    protected final Vec2i chunk;
    protected final Cuboid boundingBox;
    protected String json;
    /**
     * The children are populated while parsing the JSON data.
     * Vanilla structures use it.  Beyond that it is optional and at
     * the discretion of the client plugin.
     */
    protected final List<StructurePart> children = new ArrayList<>();
    protected int referenceCount; // count referencing StructureRegion instances
    private Object data;

    public Structure(final String world, final NamespacedKey key, final Vec2i chunk, final Cuboid boundingBox, final String json) {
        this.world = world;
        this.key = key;
        this.chunk = chunk;
        this.boundingBox = boundingBox;
        this.json = json;
        if (key.getNamespace().equals("minecraft")) {
            parseVanillaChildren();
        }
    }

    @SuppressWarnings("unchecked")
    private void parseVanillaChildren() {
        Map<String, Object> structureMap = (Map<String, Object>) Json.deserialize(json, Map.class);
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

    /**
     * Parse the JSON data.  If they were already parsed with the
     * correct type, return the cached version.  Otherwise parse and
     * cache them.
     */
    public <T> T getJsonData(Class<T> clazz, Supplier<T> dfl) {
        if (data != null && clazz.isInstance(data)) {
            return clazz.cast(data);
        }
        T result = Json.deserialize(json, clazz);
        if (result == null) result = dfl.get();
        this.data = result;
        return result;
    }

    /**
     * Override the cached JSON data.  This will have no permanent
     * effect on the json field until saveJsonData() is called.
     */
    public void setJsonData(Object o) {
        this.data = o;
    }

    /**
     * Update the json field and save it in the database.
     */
    public void saveJsonData() {
        if (data == null) return;
        json = Json.serialize(data);
        structureCache().updateStructure(this);
    }

    public boolean isLoaded() {
        return referenceCount > 0;
    }
}
