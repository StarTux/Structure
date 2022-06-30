package com.cavetale.structure.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Value;

final class SpatialStructureCache {
    protected static final int CHUNK_BITS = 8;
    protected static final int CHUNK_SIZE = 1 << CHUNK_BITS;
    protected final YList ylist = new YList();
    /** All structures in this world. */
    protected final List<Structure> allStructures = new ArrayList<>();

    public void insert(Structure structure) {
        applySlots(structure.getCuboid(), true, slot -> slot.structures.add(structure));
        allStructures.add(structure);
    }

    public void remove(Structure structure) {
        applySlots(structure.getCuboid(), false, slot -> slot.structures.remove(structure));
        allStructures.remove(structure);
    }

    public void update(Structure structure, Cuboid oldCuboid, Cuboid newCuboid) {
        applySlots(structure.getCuboid(), false, slot -> slot.structures.remove(structure));
        applySlots(structure.getCuboid(), true, slot -> slot.structures.add(structure));
    }

    public Structure findStructureAt(int worldX, int worldY, int worldZ) {
        int slotX = worldX >> CHUNK_BITS;
        int slotY = worldZ >> CHUNK_BITS;
        Slot slot = findSlot(slotX, slotY, false);
        if (slot == null) return null;
        for (Structure structure : slot.structures) {
            if (structure.cuboid.contains(worldX, worldY, worldZ)) return structure;
        }
        return null;
    }

    public List<Structure> findStructuresWithin(Cuboid cuboid) {
        int ax = cuboid.ax >> CHUNK_BITS;
        int bx = cuboid.bx >> CHUNK_BITS;
        int ay = cuboid.az >> CHUNK_BITS;
        int by = cuboid.bz >> CHUNK_BITS;
        List<Structure> result = new ArrayList<>();
        for (int y = ay; y <= by; y += 1) {
            for (int x = ax; x <= bx; x += 1) {
                Slot slot = findSlot(x, y, false);
                if (slot == null) continue;
                for (Structure structure : slot.structures) {
                    if (!result.contains(structure) && cuboid.overlaps(structure.getCuboid())) {
                        result.add(structure);
                    }
                }
            }
        }
        return result;
    }

    private void applySlots(final Cuboid cuboid, final boolean create, final Consumer<Slot> consumer) {
        final int ay = cuboid.az >> CHUNK_BITS;
        final int by = cuboid.bz >> CHUNK_BITS;
        final int ax = cuboid.ax >> CHUNK_BITS;
        final int bx = cuboid.bx >> CHUNK_BITS;
        for (int slotY = ay; slotY <= by; slotY += 1) {
            for (int slotX = ax; slotX <= bx; slotX += 1) {
                Slot slot = findSlot(slotX, slotY, create);
                if (slot != null) consumer.accept(slot);
            }
        }
    }

    private Slot findSlot(int slotX, int slotY, boolean create) {
        XList xlist = ylist.get(slotY, create);
        return xlist != null
            ? xlist.get(slotX, create)
            : null;
    }

    /**
     * The twin list contains 2 lists, one for positive, one for
     * negative indexes. Thus, it can grow in both directions.
     *
     * @param <T> Contained elements.
     */
    private abstract static class TwinList<T> {
        protected final List<T> positive = new ArrayList<>();
        protected final List<T> negative = new ArrayList<>();

        public int min() {
            return -negative.size();
        }

        public int max() {
            return positive.size();
        }

        public final T get(final int index, final boolean create) {
            return index < 0
                ? getHelper(negative, -index - 1, create)
                : getHelper(positive, index, create);
        }

        public final T remove(final int index) {
            if (index < 0) {
                final int index2 = -index - 1;
                if (index2 >= negative.size()) return null;
                T result = negative.get(index2);
                negative.set(index2, null);
                return result;
            } else {
                if (index >= positive.size()) return null;
                T result = positive.get(index);
                positive.set(index, null);
                return result;
            }
        }

        private T getHelper(final List<T> list, final int index, final boolean create) {
            if (!create && index >= list.size()) return null;
            while (index >= list.size()) list.add(null);
            T result = list.get(index);
            if (create && result == null) {
                result = create();
                list.set(index, result);
            }
            return result;
        }

        protected abstract T create();
    }

    protected static final class YList extends TwinList<XList> {
        @Override protected XList create() {
            return new XList();
        }
    }

    protected static final class XList extends TwinList<Slot> {
        @Override protected Slot create() {
            return new Slot();
        }
    }

    protected static final class Slot {
        protected final List<Structure> structures = new ArrayList<>();
    }

    /**
     * Object returned by getAllSlots(), which exists for
     * StructureCache#debug.
     */
    @Value
    protected static final class XYSlot {
        protected final int x;
        protected final int y;
        protected final List<Structure> structures;
    }

    protected List<XYSlot> getAllSlots() {
        List<XYSlot> result = new ArrayList<>();
        for (int y = ylist.min(); y <= ylist.max(); y += 1) {
            XList xlist = ylist.get(y, false);
            if (xlist == null) continue;
            for (int x = xlist.min(); x <= xlist.max(); x += 1) {
                Slot slot = xlist.get(x, false);
                if (slot == null) continue;
                result.add(new XYSlot(x, y, slot.structures));
            }
        }
        return result;
    }
}
