package com.cavetale.structure.event;

import com.cavetale.structure.cache.Structure;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a structure is loaded into the memory cache because it
 * is part of a partially loaded region.
 *
 * Clients may use this event to initialize the StructureParts, or
 * prepare extraneous structures or workers.
 */
@Getter @RequiredArgsConstructor
public final class StructureLoadEvent extends Event {
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
