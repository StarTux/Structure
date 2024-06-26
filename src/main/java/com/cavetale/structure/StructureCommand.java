package com.cavetale.structure;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.structure.cache.Structure;
import com.cavetale.structure.cache.StructurePart;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class StructureCommand extends AbstractCommand<StructurePlugin> {
    protected StructureCommand(final StructurePlugin plugin) {
        super(plugin, "structure");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("here").denyTabCompletion()
            .description("Show current structure")
            .playerCaller(this::here);
        rootNode.addChild("highlight").denyTabCompletion()
            .description("Highlight current structure")
            .playerCaller(this::highlight);
        rootNode.addChild("nearby").arguments("[radius]")
            .completers(CommandArgCompleter.integer(i -> i > 0))
            .description("Find structures nearby")
            .playerCaller(this::nearby);
        rootNode.addChild("biome").denyTabCompletion()
            .description("Get biome")
            .playerCaller(this::biome);
    }

    protected void here(Player player) {
        Block block = player.getLocation().getBlock();
        Structure structure = plugin.structureCache.at(block);
        String xyz = block.getX() + " " + block.getY() + " " + block.getZ();
        if (structure == null) throw new CommandWarn("No structure here: " + block.getWorld().getName() + " " + xyz);
        player.sendMessage(text("Structure " + structure.getKey() + " (" + structure.getBoundingBox() + ") c=" + structure.getChildren().size(), AQUA));
        for (StructurePart part : structure.getChildren()) {
            if (!part.getBoundingBox().contains(block)) continue;
            player.sendMessage(text("- StructurePart " + part.getId() + " (" + part.getBoundingBox() + ")", YELLOW));
        }
        if (!structure.isVanilla()) {
            player.sendMessage(text("json=" + structure.getJson(), YELLOW));
        }
    }

    protected void highlight(Player player) {
        Block block = player.getLocation().getBlock();
        Structure structure = plugin.structureCache.at(block);
        String xyz = block.getX() + " " + block.getY() + " " + block.getZ();
        if (structure == null) throw new CommandWarn("No structure here: " + block.getWorld().getName() + " " + xyz);
        structure.getBoundingBox().highlight(player.getWorld(), 0.0, loc -> player.spawnParticle(Particle.HAPPY_VILLAGER, loc, 1, 0.0, 0.0, 0.0, 0.0));
        player.sendMessage(text("Highlighting " + structure.getChildren().size() + " parts"));
        for (StructurePart part : structure.getChildren()) {
            part.getBoundingBox().highlight(player.getWorld(), 0.0, loc -> player.spawnParticle(Particle.END_ROD, loc, 1, 0.0, 0.0, 0.0, 0.0));
        }
    }

    private boolean nearby(Player player, String[] args) {
        if (args.length > 2) return false;
        Block block = player.getLocation().getBlock();
        int r = args.length >= 1
            ? CommandArgCompleter.requireInt(args[0], i -> i > 0)
            : 64;
        Cuboid cuboid = new Cuboid(block.getX() - r, block.getWorld().getMinHeight(), block.getZ() - r,
                                   block.getX() + r, block.getWorld().getMaxHeight(), block.getZ() + r);
        player.sendMessage(text("Finding structures within " + r + " block radius:", YELLOW));
        for (Structure structure : plugin.structureCache.within(player.getWorld().getName(), cuboid)) {
            player.sendMessage(text("- Structure " + structure.getKey()
                                    + " (" + structure.getBoundingBox() + ")"
                                    + " children:" + structure.getChildren().size()
                                    + " vanilla:" + structure.isVanilla()
                                    + " inside:" + structure.getBoundingBox().contains(block), AQUA));
        }
        return true;
    }

    private void biome(Player player) {
        Block block = player.getLocation().getBlock();
        player.sendMessage(join(noSeparators(),
                                text("Biome at ", GRAY),
                                text(block.getX()), text(",", GRAY),
                                text(block.getY()), text(",", GRAY),
                                text(block.getZ()), text(":", GRAY),
                                text(" " + plugin.structureCache.biomeAt(block))));
    }
}
