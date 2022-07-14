package com.cavetale.structure;

import com.cavetale.structure.cache.StructureCache;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class StructurePlugin extends JavaPlugin implements Listener {
    private static StructurePlugin instance;
    protected final StructureCache structureCache = new StructureCache();
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
            int count = structureCache.load(world);
            getLogger().info(world.getName() + ": " + count + " structures loaded");
        }
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        structureCache.clear();
        coreStructures.unregister();
    }

    @EventHandler
    private void onWorldLoad(WorldLoadEvent event) {
        int count = structureCache.load(event.getWorld());
        getLogger().info(event.getWorld().getName() + ": " + count + " structures loaded");
    }

    @EventHandler
    private void onWorldUnload(WorldUnloadEvent event) {
        structureCache.unload(event.getWorld());
    }

    public static StructureCache structureCache() {
        return instance.structureCache;
    }
}
