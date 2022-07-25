package com.cavetale.structure.sqlite;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.structure.cache.Structure;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import lombok.RequiredArgsConstructor;
import org.bukkit.NamespacedKey;
import static com.cavetale.structure.StructurePlugin.logger;

@RequiredArgsConstructor
public final class SQLiteDataStore {
    private final String worldName;
    private final File databaseFile;
    private Connection connection;
    private PreparedStatement stmtFindStructureRef;
    private PreparedStatement stmtFindStructure;
    private PreparedStatement stmtInsertStructure;
    private PreparedStatement stmtUpdateStructure;
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
                              + " `json` TEXT NOT NULL"
                              + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS `struct_refs` ("
                              + " `id` INTEGER PRIMARY KEY,"
                              + " `structure_id` INTEGER NOT NULL,"
                              + " `region_x` INTEGER NOT NULL,"
                              + " `region_z` INTEGER NOT NULL,"
                              + " UNIQUE(`region_x`, `region_z`, `structure_id`)"
                              + ")");
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
        try {
            stmt = connection.createStatement();
            stmtFindStructureRef = connection.prepareStatement("SELECT * FROM `struct_refs` WHERE `region_x` = ? AND `region_z` = ?");
            stmtFindStructure = connection.prepareStatement("SELECT * FROM `structures` WHERE `id` = ?");
            stmtInsertStructure = connection.prepareStatement("INSERT INTO `structures`"
                                                              + " (`type`, `chunk_x`, `chunk_z`, `ax`, `ay`, `az`, `bx`, `by`, `bz`, `json`)"
                                                              + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                                              Statement.RETURN_GENERATED_KEYS);
            stmtUpdateStructure = connection.prepareStatement("UPDATE `structures` SET `json` = ? WHERE `id` = ?");
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
            stmt.close();
            connection.close();
        } catch (SQLException sqle) {
            logger().log(Level.SEVERE, "", sqle);
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
                                                        resultSet.getString("json"));
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
                                                    resultSet.getString("json"));
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
}
