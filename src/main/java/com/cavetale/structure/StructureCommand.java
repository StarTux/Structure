package com.cavetale.structure;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.structure.cache.Cuboid;
import com.cavetale.structure.cache.Structure;
import com.cavetale.structure.cache.StructurePart;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class StructureCommand extends AbstractCommand<StructurePlugin> {
    protected StructureCommand(final StructurePlugin plugin) {
        super(plugin, "structure");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").denyTabCompletion()
            .description("Show current structure")
            .playerCaller(this::info);
        rootNode.addChild("highlight").denyTabCompletion()
            .description("Highlight current structure")
            .playerCaller(this::highlight);
        rootNode.addChild("nearby").denyTabCompletion()
            .description("Find structures nearby")
            .playerCaller(this::nearby);
        rootNode.addChild("debug").denyTabCompletion()
            .description("debug Command")
            .playerCaller(this::debug);
    }

    protected void info(Player player) {
        Block block = player.getLocation().getBlock();
        Structure structure = plugin.structureCache.at(block);
        String xyz = block.getX() + " " + block.getY() + " " + block.getZ();
        if (structure == null) throw new CommandWarn("No structure here: " + block.getWorld().getName() + " " + xyz);
        player.sendMessage(text("Structure " + structure.getType().getName() + " (" + structure.getCuboid() + ") c=" + structure.getChildren().size(), AQUA));
        for (StructurePart part : structure.getChildren()) {
            if (!part.getCuboid().contains(block)) continue;
            player.sendMessage(text("- StructurePart " + part.getId() + " (" + part.getCuboid() + ")", YELLOW));
        }
    }

    protected void highlight(Player player) {
        Block block = player.getLocation().getBlock();
        Structure structure = plugin.structureCache.at(block);
        String xyz = block.getX() + " " + block.getY() + " " + block.getZ();
        if (structure == null) throw new CommandWarn("No structure here: " + block.getWorld().getName() + " " + xyz);
        structure.getCuboid().highlight(player.getWorld(), 0.0, loc -> player.spawnParticle(Particle.VILLAGER_HAPPY, loc, 1, 0.0, 0.0, 0.0, 0.0));
        player.sendMessage(text("Highlighting " + structure.getChildren().size() + " parts"));
        for (StructurePart part : structure.getChildren()) {
            part.getCuboid().highlight(player.getWorld(), 0.0, loc -> player.spawnParticle(Particle.END_ROD, loc, 1, 0.0, 0.0, 0.0, 0.0));
        }
    }

    protected void nearby(Player player) {
        Block block = player.getLocation().getBlock();
        int r = 64;
        Cuboid cuboid = new Cuboid(block.getX() - r, block.getWorld().getMinHeight(), block.getZ() - r,
                                   block.getX() + r, block.getWorld().getMaxHeight(), block.getZ() + r);
        player.sendMessage(text("Finding structures within " + cuboid + ":", YELLOW));
        for (Structure structure : plugin.structureCache.within(player.getWorld().getName(), cuboid)) {
            player.sendMessage(text("- Structure " + structure.getType().getName()
                                    + " (" + structure.getCuboid() + ")"
                                    + " children=" + structure.getChildren().size()
                                    + " inside=" + structure.getCuboid().contains(block), AQUA));
        }
    }

    protected void debug(Player player) {
        plugin.structureCache.debug(player, player.getWorld().getName());
    }
}
