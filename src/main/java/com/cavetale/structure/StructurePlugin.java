package com.cavetale.structure;

import com.cavetale.structure.cache.StructureCache;
import java.util.logging.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class StructurePlugin extends JavaPlugin implements Listener {
    private static StructurePlugin instance;
    private final StructureCache structureCache = new StructureCache();
    private final StructureCommand structureCommand = new StructureCommand(this);
    private final CoreStructures coreStructures = new CoreStructures(this);

    @Override
    public void onLoad() {
        instance = this;
        coreStructures.register();
    }

    @Override
    public void onEnable() {
        structureCommand.enable();
        for (World world : Bukkit.getWorlds()) {
            structureCache.enable(world);
        }
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        structureCache.disable();
        coreStructures.unregister();
    }

    @EventHandler
    private void onWorldLoad(WorldLoadEvent event) {
        structureCache.enable(event.getWorld());
    }

    @EventHandler
    private void onWorldUnload(WorldUnloadEvent event) {
        structureCache.disable(event.getWorld());
    }

    @EventHandler
    private void onChunkLoad(ChunkLoadEvent event) {
        structureCache.onChunkLoad(event.getWorld(), event.getChunk().getX(), event.getChunk().getZ());
    }

    @EventHandler
    private void onChunkUnload(ChunkUnloadEvent event) {
        structureCache.onChunkUnload(event.getWorld(), event.getChunk().getX(), event.getChunk().getZ());
    }

    public static StructurePlugin structurePlugin() {
        return instance;
    }

    public static StructureCache structureCache() {
        return instance.structureCache;
    }

    public static Logger logger() {
        return instance.getLogger();
    }

    public static void log(String msg) {
        instance.getLogger().info(msg);
    }

    public static void warn(String msg) {
        instance.getLogger().warning(msg);
    }
}
