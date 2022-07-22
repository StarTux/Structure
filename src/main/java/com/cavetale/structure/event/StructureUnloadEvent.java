package com.cavetale.structure.event;

import com.cavetale.structure.cache.Structure;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a structure is evicted from the memory cache.
 */
@Getter @RequiredArgsConstructor
public final class StructureUnloadEvent extends Event {
    private final Structure structure;

    /**
     * Required by Event.
     */
    @Getter private static HandlerList handlerList = new HandlerList();

    /**
     * Required by Event.
     */
    @Override public HandlerList getHandlers() {
        return handlerList;
    }
}
