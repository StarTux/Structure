package com.cavetale.structure.cache;

import com.cavetale.core.struct.Cuboid;
import java.util.List;
import java.util.Map;
import lombok.Data;
import static java.util.Objects.requireNonNull;

@Data
public final class StructurePart {
    protected final String id;
    protected final Cuboid boundingBox;

    protected StructurePart(final String id, final Cuboid cuboid) {
        this.id = id;
        this.boundingBox = cuboid;
    }

    /**
     * Constructor for vanilla structures.
     */
    @SuppressWarnings("unchecked")
    protected StructurePart(final Map<String, Object> partMap) {
        this.id = (String) requireNonNull(partMap.get("id"));
        this.boundingBox = Cuboid.ofList((List<Number>) requireNonNull(partMap.get("BB")));
    }
}
