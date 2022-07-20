package com.cavetale.structure.sqlite;

import com.cavetale.structure.struct.Cuboid;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public final class SQLStructure {
    private int id;
    private String type;
    private int chunkX;
    private int chunkZ;
    private int ax;
    private int ay;
    private int az;
    private int bx;
    private int by;
    private int bz;
    private String json;

    public Cuboid getCuboid() {
        return new Cuboid(ax, ay, az, bx, by, bz);
    }
}
