package com.cavetale.structure.sqlite;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.structure.cache.Structure;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import lombok.RequiredArgsConstructor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import static com.cavetale.structure.StructurePlugin.log;
import static com.cavetale.structure.StructurePlugin.logger;
import static com.cavetale.structure.StructurePlugin.warn;

@RequiredArgsConstructor
public final class SQLiteDataStore {
    private final String worldName;
    private final File databaseFile;
    private Connection connection;
    private PreparedStatement stmtFindStructureRef;
    private PreparedStatement stmtFindStructure;
    private PreparedStatement stmtInsertStructure;
    private PreparedStatement stmtUpdateStructure;
    private PreparedStatement stmtUpdateDiscovered;
    private PreparedStatement stmtInsertBiome;
    private PreparedStatement stmtFindBiome;
    private PreparedStatement stmtGetAllBiomes;
    private Statement stmt;

    public void enable() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException(cnfe);
        }
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS `structures` ("
                              + " `id` INTEGER PRIMARY KEY,"
                              + " `type` VARCHAR(255) NOT NULL,"
                              + " `chunk_x` INTEGER NOT NULL,"
                              + " `chunk_z` INTEGER NOT NULL,"
                              + " `ax` INTEGER NOT NULL,"
                              + " `ay` INTEGER NOT NULL,"
                              + " `az` INTEGER NOT NULL,"
                              + " `bx` INTEGER NOT NULL,"
                              + " `by` INTEGER NOT NULL,"
                              + " `bz` INTEGER NOT NULL,"
                              + " `json` TEXT NOT NULL,"
                              + " `discovered` INTEGER NOT NULL"
                              + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS `struct_refs` ("
                              + " `id` INTEGER PRIMARY KEY,"
                              + " `structure_id` INTEGER NOT NULL,"
                              + " `region_x` INTEGER NOT NULL,"
                              + " `region_z` INTEGER NOT NULL,"
                              + " UNIQUE(`region_x`, `region_z`, `structure_id`)"
                              + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS `biomes` ("
                              + " `id` INTEGER PRIMARY KEY,"
                              + " `chunk_x` INTEGER NOT NULL,"
                              + " `chunk_z` INTEGER NOT NULL,"
                              + " `biome` TEXT NOT NULL,"
                              + " UNIQUE(`chunk_x`, `chunk_z`) ON CONFLICT REPLACE"
                              + ")");
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
        updateStructuresTable();
        try {
            stmt = connection.createStatement();
            stmtFindStructureRef = connection.prepareStatement("SELECT * FROM `struct_refs` WHERE `region_x` = ? AND `region_z` = ?");
            stmtFindStructure = connection.prepareStatement("SELECT * FROM `structures` WHERE `id` = ?");
            stmtInsertStructure = connection.prepareStatement("INSERT INTO `structures`"
                                                              + " (`type`, `chunk_x`, `chunk_z`, `ax`, `ay`, `az`, `bx`, `by`, `bz`, `json`)"
                                                              + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                                              Statement.RETURN_GENERATED_KEYS);
            stmtUpdateStructure = connection.prepareStatement("UPDATE `structures` SET `json` = ? WHERE `id` = ?");
            stmtUpdateDiscovered = connection.prepareStatement("UPDATE `structures` SET `discovered` = ? WHERE `id` = ?");
            stmtInsertBiome = connection.prepareStatement("INSERT INTO `biomes` (`chunk_x`, `chunk_z`, `biome`) VALUES (?, ?, ?)");
            stmtFindBiome = connection.prepareStatement("SELECT `biome` FROM `biomes` WHERE `chunk_x` = ? AND `chunk_z` = ?");
            stmtGetAllBiomes = connection.prepareStatement("SELECT * FROM `biomes`");
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    public void disable() {
        try {
            stmtFindStructureRef.close();
            stmtFindStructure.close();
            stmtInsertStructure.close();
            stmtUpdateStructure.close();
            stmtUpdateDiscovered.close();
            stmtInsertBiome.close();
            stmtFindBiome.close();
            stmtGetAllBiomes.close();
            stmt.close();
            connection.close();
        } catch (SQLException sqle) {
            logger().log(Level.SEVERE, "[" + worldName + "] Disabling", sqle);
        }
    }

    /**
     * Find structure references.
     * @return the id list
     */
    public List<Integer> getStructureRefs(int x, int z) {
        try {
            stmtFindStructureRef.setInt(1, x);
            stmtFindStructureRef.setInt(2, z);
            try (ResultSet resultSet = stmtFindStructureRef.executeQuery()) {
                List<Integer> list = new ArrayList<>();
                while (resultSet.next()) {
                    list.add(resultSet.getInt("structure_id"));
                }
                return list;
            }
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    public Structure getStructure(int id) {
        try {
            stmtFindStructure.setInt(1, id);
            try (ResultSet resultSet = stmtFindStructure.executeQuery()) {
                if (resultSet.next()) {
                    Structure structure = new Structure(worldName,
                                                        NamespacedKey.fromString(resultSet.getString("type")),
                                                        Vec2i.of(resultSet.getInt("chunk_x"),
                                                                 resultSet.getInt("chunk_z")),
                                                        new Cuboid(resultSet.getInt("ax"),
                                                                   resultSet.getInt("ay"),
                                                                   resultSet.getInt("az"),
                                                                   resultSet.getInt("bx"),
                                                                   resultSet.getInt("by"),
                                                                   resultSet.getInt("bz")),
                                                        resultSet.getString("json"),
                                                        resultSet.getInt("discovered") != 0);
                    structure.setId(resultSet.getInt("id"));
                    return structure;
                } else {
                    return null;
                }
            }
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    public List<Structure> getStructures(List<Integer> ids) {
        if (ids.isEmpty()) return List.of();
        List<Structure> list = new ArrayList<>();
        List<String> idStrings = new ArrayList<>(ids.size());
        for (int id : ids) idStrings.add("" + id);
        try (ResultSet resultSet = stmt.executeQuery("SELECT * FROM `structures` WHERE `id` IN (" + String.join(", ", idStrings) + ")")) {
            while (resultSet.next()) {
                Structure structure = new Structure(worldName,
                                                    NamespacedKey.fromString(resultSet.getString("type")),
                                                    Vec2i.of(resultSet.getInt("chunk_x"),
                                                             resultSet.getInt("chunk_z")),
                                                    new Cuboid(resultSet.getInt("ax"),
                                                               resultSet.getInt("ay"),
                                                               resultSet.getInt("az"),
                                                               resultSet.getInt("bx"),
                                                               resultSet.getInt("by"),
                                                               resultSet.getInt("bz")),
                                                    resultSet.getString("json"),
                                                    resultSet.getInt("discovered") != 0);
                structure.setId(resultSet.getInt("id"));
                list.add(structure);
            }
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
        return list;
    }

    public List<Structure> getStructures(int x, int z) {
        return getStructures(getStructureRefs(x, z));
    }

    public List<Vec2i> addStructure(Structure structure) {
        try {
            stmtInsertStructure.setString(1, structure.getKey().toString());
            stmtInsertStructure.setInt(2, structure.getChunk().getX());
            stmtInsertStructure.setInt(3, structure.getChunk().getZ());
            Cuboid cuboid = structure.getBoundingBox();
            stmtInsertStructure.setInt(4, cuboid.ax);
            stmtInsertStructure.setInt(5, cuboid.ay);
            stmtInsertStructure.setInt(6, cuboid.az);
            stmtInsertStructure.setInt(7, cuboid.bx);
            stmtInsertStructure.setInt(8, cuboid.by);
            stmtInsertStructure.setInt(9, cuboid.bz);
            stmtInsertStructure.setString(10, structure.getJson());
            final int structureId;
            stmtInsertStructure.executeUpdate();
            try (ResultSet generatedKeys = stmtInsertStructure.getGeneratedKeys()) {
                if (!generatedKeys.next()) throw new IllegalStateException("No id: " + structure);
                structureId = generatedKeys.getInt(1);
                structure.setId(structureId);
            }
            // Reference
            final int cax = cuboid.ax >> 9;
            final int caz = cuboid.az >> 9;
            final int cbx = cuboid.bx >> 9;
            final int cbz = cuboid.bz >> 9;
            List<String> values = new ArrayList<>();
            List<Vec2i> result = new ArrayList<>();
            for (int cz = caz; cz <= cbz; cz += 1) {
                for (int cx = cax; cx <= cbx; cx += 1) {
                    values.add("(" + structureId + "," + cx + "," + cz + ")");
                    result.add(Vec2i.of(cx, cz));
                }
            }
            stmt.execute("INSERT INTO `struct_refs` (`structure_id`, `region_x`, `region_z`) VALUES " + String.join(", ", values));
            return result;
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    public void updateStructureJson(Structure structure) {
        try {
            stmtUpdateStructure.setString(1, structure.getJson());
            stmtUpdateStructure.setInt(2, structure.getId());
            stmtUpdateStructure.executeUpdate();
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    public void updateDiscovered(Structure structure) {
        try {
            stmtUpdateDiscovered.setInt(1, structure.isDiscovered() ? 1 : 0);
            stmtUpdateDiscovered.setInt(2, structure.getId());
            stmtUpdateDiscovered.executeUpdate();
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    public void setBiome(int chunkX, int chunkZ, String biome) {
        try {
            stmtInsertBiome.setInt(1, chunkX);
            stmtInsertBiome.setInt(2, chunkZ);
            stmtInsertBiome.setString(3, biome);
            stmtInsertBiome.executeUpdate();
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    public Biome getChunkBiome(int chunkX, int chunkZ) {
        try {
            stmtFindBiome.setInt(1, chunkX);
            stmtFindBiome.setInt(2, chunkZ);
            try (ResultSet resultSet = stmtFindBiome.executeQuery()) {
                if (!resultSet.next()) return null;
                String name = resultSet.getString("biome");
                if (name == null) return null;
                NamespacedKey namespacedKey = NamespacedKey.fromString(name);
                Biome biome = Registry.BIOME.get(namespacedKey);
                if (biome == null) {
                    warn("[" + worldName + "] Invalid biome: " + name);
                    return null;
                }
                return biome;
            }
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    public Map<Vec2i, Biome> getAllBiomes() {
        try (ResultSet resultSet = stmtGetAllBiomes.executeQuery()) {
            Map<Vec2i, Biome> result = new HashMap<>();
            while (resultSet.next()) {
                Vec2i vec = new Vec2i(resultSet.getInt("chunk_x"),
                                      resultSet.getInt("chunk_z"));
                String name = resultSet.getString("biome");
                NamespacedKey namespacedKey = NamespacedKey.fromString(name);
                Biome biome = Registry.BIOME.get(namespacedKey);
                if (biome == null) {
                    warn("[" + worldName + "] Invalid biome: " + name);
                    continue;
                }
                result.put(vec, biome);
            }
            return result;
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    public int executeUpdate(String sql) {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    public List<Map<String, Object>> executeQuery(String sql) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            final List<Map<String, Object>> result = new ArrayList<>();
            final ResultSetMetaData meta = resultSet.getMetaData();
            final int columnCount = meta.getColumnCount();
            final List<String> columnNames = new ArrayList<>(columnCount);
            for (int i = 0; i < columnCount; i += 1) {
                columnNames.add(meta.getColumnName(i + 1));
            }
            while (resultSet.next()) {
                final Map<String, Object> entry = new LinkedHashMap<>();
                result.add(entry);
                for (String columnName : columnNames) {
                    entry.put(columnName, resultSet.getObject(columnName));
                }
            }
            return result;
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    private void updateStructuresTable() {
        boolean discoveredColumnExists;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT `discovered` FROM `structures` LIMIT 1")) {
            discoveredColumnExists = true;
        } catch (SQLException sqle) {
            discoveredColumnExists = false;
        }
        if (discoveredColumnExists) return;
        final String sql = "ALTER TABLE `structures` ADD COLUMN `discovered` INTEGER NOT NULL DEFAULT 0";
        log("[" + worldName + "] Adding discovered column...");
        try (Statement statement = connection.createStatement()) {
            final int result = statement.executeUpdate(sql);
            log("[" + worldName + "] Added discovered column => " + result);
        } catch (SQLException sqle) {
            logger().log(Level.SEVERE, "[" + worldName + "] Adding discovered column: " + sql, sqle);
        }
    }
}
