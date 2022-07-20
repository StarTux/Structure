package com.cavetale.structure.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Value;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

@Value
public final class Cuboid {
    public static final Cuboid ZERO = new Cuboid(0, 0, 0, 0, 0, 0);
    public final int ax;
    public final int ay;
    public final int az;
    public final int bx;
    public final int by;
    public final int bz;

    public static Cuboid of(List<Number> list) {
        if (list.size() != 6) throw new IllegalArgumentException("" + list);
        return new Cuboid(list.get(0).intValue(), list.get(1).intValue(), list.get(2).intValue(),
                          list.get(3).intValue(), list.get(4).intValue(), list.get(5).intValue());
    }

    public boolean contains(int x, int y, int z) {
        return x >= ax && x <= bx
            && y >= ay && y <= by
            && z >= az && z <= bz;
    }

    public boolean contains(Block block) {
        return contains(block.getX(), block.getY(), block.getZ());
    }

    public boolean contains(Vec3i v) {
        return contains(v.x, v.y, v.z);
    }

    @Override
    public String toString() {
        return ax + "," + ay + "," + az + "-" + bx + "," + by + "," + bz;
    }

    public int getSizeX() {
        return bx - ax + 1;
    }

    public int getSizeY() {
        return by - ay + 1;
    }

    public int getSizeZ() {
        return bz - az + 1;
    }

    public int getVolume() {
        return getSizeX() * getSizeY() * getSizeZ();
    }

    /**
     * Highlight this cuboid. This may be called every tick and with
     * the provided arguments will do the rest. A balanced interval
     * and scale are required to make the highlight contiguous while
     * reducint lag.
     * @param the current time in ticks
     * @param interval interval between ticks
     * @param scale how many inbetween dots to make over time
     * @param callback method to call for every point
     * @return true if the callback was called (probably many times),
     *   false if we're waiting for the interval.
     */
    public boolean highlight(World world, int timer, int interval, int scale, Consumer<Location> callback) {
        if (timer % interval != 0) return false;
        double offset = (double) ((timer / interval) % scale) / (double) scale;
        return highlight(world, offset, callback);
    }

    /**
     * Highlight this cuboid. This is a utility function for the other highlight method but may be called on its own, probably with an offset of 0.
     * @param world the world
     * @param offset the offset to highlight, between each corner point and the next
     * @param callback will be called for each point
     */
    public boolean highlight(World world, double offset, Consumer<Location> callback) {
        if (!world.isChunkLoaded(ax >> 4, az >> 4)) return false;
        Block origin = world.getBlockAt(ax, ay, az);
        Location loc = origin.getLocation();
        int sizeX = getSizeX();
        int sizeY = getSizeY();
        int sizeZ = getSizeZ();
        for (int y = 0; y < sizeY; y += 1) {
            double dy = (double) y + offset;
            callback.accept(loc.clone().add(0, dy, 0));
            callback.accept(loc.clone().add(0, dy, sizeZ));
            callback.accept(loc.clone().add(sizeX, dy, 0));
            callback.accept(loc.clone().add(sizeX, dy, sizeZ));
        }
        for (int z = 0; z < sizeZ; z += 1) {
            double dz = (double) z + offset;
            callback.accept(loc.clone().add(0, 0, dz));
            callback.accept(loc.clone().add(0, sizeY, dz));
            callback.accept(loc.clone().add(sizeX, 0, dz));
            callback.accept(loc.clone().add(sizeX, sizeY, dz));
        }
        for (int x = 0; x < sizeX; x += 1) {
            double dx = (double) x + offset;
            callback.accept(loc.clone().add(dx, 0, 0));
            callback.accept(loc.clone().add(dx, 0, sizeZ));
            callback.accept(loc.clone().add(dx, sizeY, 0));
            callback.accept(loc.clone().add(dx, sizeY, sizeZ));
        }
        return true;
    }

    public Vec3i getMin() {
        return new Vec3i(ax, ay, az);
    }

    public Vec3i getMax() {
        return new Vec3i(bx, by, bz);
    }

    public List<Vec3i> enumerate() {
        List<Vec3i> result = new ArrayList<>(getVolume());
        for (int y = ay; y <= by; y += 1) {
            for (int z = az; z <= bz; z += 1) {
                for (int x = ax; x <= bx; x += 1) {
                    result.add(new Vec3i(x, y, z));
                }
            }
        }
        return result;
    }

    public boolean overlaps(Cuboid other) {
        boolean x = other.ax <= this.bx && other.bx >= this.ax;
        boolean y = other.ay <= this.by && other.by >= this.ay;
        boolean z = other.az <= this.bz && other.bz >= this.az;
        return x && y && z;
    }
}
