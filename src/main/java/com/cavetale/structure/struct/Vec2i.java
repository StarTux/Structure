package com.cavetale.structure.struct;

import lombok.Value;

@Value
public final class Vec2i {
    public static final Vec2i ZERO = new Vec2i(0, 0);
    public final int x;
    public final int z;

    public static Vec2i of(int nx, int nz) {
        return new Vec2i(nx, nz);
    }

    public Vec2i add(int dx, int dz) {
        return new Vec2i(x + dx, z + dz);
    }

    @Override
    public String toString() {
        return "" + x + "," + z;
    }
}
