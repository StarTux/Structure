package com.cavetale.structure;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.core.util.Json;
import com.cavetale.structure.cache.Structure;
import com.cavetale.structure.cache.StructurePart;
import com.cavetale.structure.cache.StructureWorld;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class StructureCommand extends AbstractCommand<StructurePlugin> {
    protected StructureCommand(final StructurePlugin plugin) {
        super(plugin, "structure");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").arguments("<world> <id>")
            .description("Print structure info")
            .completers(CommandArgCompleter.supplyList(() -> List.copyOf(plugin.getStructureCache().getWorlds().keySet())),
                        CommandArgCompleter.integer(i -> i > 0))
            .senderCaller(this::info);
        rootNode.addChild("discovered").arguments("<world> <id> <discovered>")
            .description("Set discovered state")
            .completers(CommandArgCompleter.supplyList(() -> List.copyOf(plugin.getStructureCache().getWorlds().keySet())),
                        CommandArgCompleter.integer(i -> i > 0),
                        CommandArgCompleter.BOOLEAN)
            .senderCaller(this::discovered);
        rootNode.addChild("worldinfo").arguments("<world>")
            .description("Print world info")
            .completers(CommandArgCompleter.supplyList(() -> List.copyOf(plugin.getStructureCache().getWorlds().keySet())))
            .senderCaller(this::worldInfo);
        // Player Commands
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
        final CommandNode sqliteNode = rootNode.addChild("sqlite")
            .description("SQLite commands");
        sqliteNode.addChild("update").arguments("<world> <sql...>")
            .description("Execute an SQL update")
            .completers(CommandArgCompleter.supplyList(() -> List.copyOf(plugin.getStructureCache().getWorlds().keySet())))
            .senderCaller(this::sqliteUpdate);
        sqliteNode.addChild("query").arguments("<world> <sql...>")
            .description("Execute an SQL query")
            .completers(CommandArgCompleter.supplyList(() -> List.copyOf(plugin.getStructureCache().getWorlds().keySet())))
            .senderCaller(this::sqliteQuery);
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final String worldName = args[0];
        final int id = CommandArgCompleter.requireInt(args[1], i -> i > 0);
        final StructureWorld structureWorld = plugin.getStructureCache().getWorlds().get(worldName);
        if (structureWorld == null) {
            throw new CommandWarn("World not found: " + worldName);
        }
        if (structureWorld.getDataStore() == null) {
            throw new CommandWarn("World does not have an SQLite data store: " + worldName);
        }
        final Structure structure = structureWorld.getOrLoadStructure(id);
        if (structure == null) {
            throw new CommandWarn("Structure not found: " + worldName + "/" + id);
        }
        sender.sendMessage(textOfChildren(text("Structure #" + structure.getId(), GRAY),
                                          text(" " + structure.getKey(), YELLOW),
                                          text(" (" + structure.getBoundingBox() + ")", GRAY),
                                          text(" disovered=" + structure.isDiscovered(), AQUA),
                                          text(" children=" + structure.getChildren().size(), AQUA)));
        return true;
    }

    private boolean worldInfo(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final String worldName = args[0];
        final StructureWorld structureWorld = plugin.getStructureCache().getWorlds().get(worldName);
        if (structureWorld == null) {
            throw new CommandWarn("World not found: " + worldName);
        }
        sender.sendMessage(textOfChildren(text("World ", GRAY), text(structureWorld.getWorldName())));
        sender.sendMessage(textOfChildren(text("  Cached Structures ", GRAY), text(structureWorld.getStructureCache().size())));
        sender.sendMessage(textOfChildren(text("  Cached Regions ", GRAY), text(structureWorld.getRegionCache().size())));
        return true;
    }

    private boolean discovered(CommandSender sender, String[] args) {
        if (args.length != 3) return false;
        final String worldName = args[0];
        final int id = CommandArgCompleter.requireInt(args[1], i -> i > 0);
        final boolean discovered = CommandArgCompleter.requireBoolean(args[2]);
        final StructureWorld structureWorld = plugin.getStructureCache().getWorlds().get(worldName);
        if (structureWorld == null) {
            throw new CommandWarn("World not found: " + worldName);
        }
        if (structureWorld.getDataStore() == null) {
            throw new CommandWarn("World does not have an SQLite data store: " + worldName);
        }
        final Structure structure = structureWorld.getOrLoadStructure(id);
        if (structure == null) {
            throw new CommandWarn("Structure not found: " + worldName + "/" + id);
        }
        structure.setDiscovered(discovered);
        sender.sendMessage(textOfChildren(text("New discovered value of structure ", YELLOW),
                                          text(worldName, WHITE),
                                          text("/", GRAY),
                                          text(id, WHITE),
                                          text(" is now ", YELLOW),
                                          (structure.isDiscovered()
                                           ? text("True", GREEN)
                                           : text("False", RED))));
        return true;
    }

    private void here(Player player) {
        final Block block = player.getLocation().getBlock();
        final Structure structure = plugin.getStructureCache().at(block);
        if (structure == null) {
            final String xyz = block.getX() + " " + block.getY() + " " + block.getZ();
            throw new CommandWarn("No structure here: " + block.getWorld().getName() + " " + xyz);
        }
        player.sendMessage(textOfChildren(text("Structure #" + structure.getId(), GRAY),
                                          text(" " + structure.getKey(), YELLOW),
                                          text(" (" + structure.getBoundingBox() + ")", GRAY),
                                          text(" disovered=" + structure.isDiscovered(), AQUA),
                                          text(" children=" + structure.getChildren().size(), AQUA)));
        for (StructurePart part : structure.getChildren()) {
            if (!part.getBoundingBox().contains(block)) {
                continue;
            }
            player.sendMessage(textOfChildren(text("- StructurePart ", GRAY),
                                              text(part.getId(), YELLOW),
                                              text(" (" + part.getBoundingBox() + ")", GRAY)));
        }
        if (!structure.isVanilla()) {
            player.sendMessage(text("json=" + structure.getJson(), YELLOW));
        }
    }

    private void highlight(Player player) {
        final Block block = player.getLocation().getBlock();
        final Structure structure = plugin.getStructureCache().at(block);
        if (structure == null) {
            final String xyz = block.getX() + " " + block.getY() + " " + block.getZ();
            throw new CommandWarn("No structure here: " + block.getWorld().getName() + " " + xyz);
        }
        structure.getBoundingBox().highlight(player.getWorld(), 0.0, loc -> player.spawnParticle(Particle.HAPPY_VILLAGER, loc, 1, 0.0, 0.0, 0.0, 0.0));
        player.sendMessage(text("Highlighting " + structure.getChildren().size() + " parts"));
        for (StructurePart part : structure.getChildren()) {
            part.getBoundingBox().highlight(player.getWorld(), 0.0, loc -> player.spawnParticle(Particle.END_ROD, loc, 1, 0.0, 0.0, 0.0, 0.0));
        }
    }

    private boolean nearby(Player player, String[] args) {
        if (args.length > 2) return false;
        final Block block = player.getLocation().getBlock();
        int r = args.length >= 1
            ? CommandArgCompleter.requireInt(args[0], i -> i > 0)
            : 64;
        final Cuboid cuboid = new Cuboid(block.getX() - r, block.getWorld().getMinHeight(), block.getZ() - r,
                                         block.getX() + r, block.getWorld().getMaxHeight(), block.getZ() + r);
        player.sendMessage(text("Finding structures within " + r + " block radius:", YELLOW));
        for (Structure structure : plugin.getStructureCache().within(player.getWorld().getName(), cuboid)) {
            final Vec3i center = structure.getBoundingBox().getCenter();
            final String coords = center.x + " " + center.y + " " + center.z;
            final String command = "/tp " + player.getName() + " " + coords;
            player.sendMessage(textOfChildren(text("- Structure #" + structure.getId(), GRAY),
                                              text(" " + structure.getKey(), YELLOW),
                                              text(" (" + structure.getBoundingBox() + ")", GRAY),
                                              text(" discovered:", GRAY), text("" + structure.isDiscovered(), AQUA),
                                              text(" children:", GRAY), text(structure.getChildren().size(), AQUA),
                                              text(" vanilla:", GRAY), text("" + structure.isVanilla(), AQUA),
                                              text(" inside:", GRAY), text("" + structure.getBoundingBox().contains(block), AQUA))
                               .hoverEvent(showText(text(command, GRAY)))
                               .clickEvent(suggestCommand(command))
                               .insertion(coords));
        }
        return true;
    }

    private void biome(Player player) {
        final Block block = player.getLocation().getBlock();
        player.sendMessage(textOfChildren(text("Biome at ", GRAY),
                                          text(block.getX()), text(",", GRAY),
                                          text(block.getY()), text(",", GRAY),
                                          text(block.getZ()), text(":", GRAY),
                                          text(" " + plugin.getStructureCache().biomeAt(block))));
    }

    private boolean sqliteUpdate(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        final String worldName = args[0];
        final String sql = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        final StructureWorld structureWorld = plugin.getStructureCache().getWorlds().get(worldName);
        if (structureWorld == null) {
            throw new CommandWarn("World not found: " + worldName);
        }
        if (structureWorld.getDataStore() == null) {
            throw new CommandWarn("World does not have an SQLite data store: " + worldName);
        }
        sender.sendMessage(textOfChildren(text("Executing SQLite update on " + worldName + ": ", YELLOW),
                                          text(sql, GRAY)));
        final int result = structureWorld.getDataStore().executeUpdate(sql);
        sender.sendMessage(textOfChildren(text("Return code: ", YELLOW),
                                          text(result, GRAY)));
        return true;
    }

    private boolean sqliteQuery(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        final String worldName = args[0];
        final String sql = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        final StructureWorld structureWorld = plugin.getStructureCache().getWorlds().get(worldName);
        if (structureWorld == null) {
            throw new CommandWarn("World not found: " + worldName);
        }
        if (structureWorld.getDataStore() == null) {
            throw new CommandWarn("World does not have an SQLite data store: " + worldName);
        }
        sender.sendMessage(textOfChildren(text("Executing SQLite update on " + worldName + ": ", YELLOW),
                                          text(sql, GRAY)));
        final List<Map<String, Object>> results = structureWorld.getDataStore().executeQuery(sql);
        for (int i = 0; i < results.size(); i += 1) {
            sender.sendMessage(textOfChildren(text(i + " ", GRAY),
                                              text(Json.serialize(results.get(i)), WHITE)));
        }
        sender.sendMessage(textOfChildren(text("Total results: ", YELLOW),
                                          text(results.size(), GRAY)));
        return true;
    }
}
