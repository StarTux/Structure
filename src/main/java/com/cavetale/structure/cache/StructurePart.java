package com.cavetale.structure.cache;

import java.util.List;
import java.util.Map;
import lombok.Data;
import static java.util.Objects.requireNonNull;

@Data
public final class StructurePart {
    protected final String id;
    protected final Cuboid boundingBox;

    @SuppressWarnings("unchecked")
    protected StructurePart(final Map<String, Object> partMap) {
        this.id = (String) requireNonNull(partMap.get("id"));
        this.boundingBox = Cuboid.of((List<Number>) requireNonNull(partMap.get("BB")));
    }
}
