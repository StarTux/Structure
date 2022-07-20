package com.cavetale.structure.sqlite;

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
import static com.cavetale.structure.StructurePlugin.logger;

@RequiredArgsConstructor
public final class SQLiteDataStore {
    private final File databaseFile;
    private Connection connection;
    private PreparedStatement stmtFindStructureRef;
    private PreparedStatement stmtFindStructure;
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
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    public void disable() {
        try {
            if (stmtFindStructureRef != null) {
                stmtFindStructureRef.close();
                stmtFindStructureRef = null;
            }
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            if (connection != null) {
                connection.close();
                connection = null;
            }
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

    public SQLStructure getStructure(int id) {
        try {
            stmtFindStructure.setInt(1, id);
            try (ResultSet resultSet = stmtFindStructure.executeQuery()) {
                if (resultSet.next()) {
                    return new SQLStructure(resultSet.getInt("id"),
                                            resultSet.getString("type"),
                                            resultSet.getInt("chunk_x"),
                                            resultSet.getInt("chunk_z"),
                                            resultSet.getInt("ax"),
                                            resultSet.getInt("ay"),
                                            resultSet.getInt("az"),
                                            resultSet.getInt("bx"),
                                            resultSet.getInt("by"),
                                            resultSet.getInt("bz"),
                                            resultSet.getString("json"));
                } else {
                    return null;
                }
            }
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
    }

    public List<SQLStructure> getStructures(List<Integer> ids) {
        if (ids.isEmpty()) return List.of();
        List<SQLStructure> list = new ArrayList<>();
        List<String> idStrings = new ArrayList<>(ids.size());
        for (int id : ids) idStrings.add("" + id);
        try (ResultSet resultSet = stmt.executeQuery("SELECT * FROM `structures` WHERE `id` IN (" + String.join(", ", idStrings) + ")")) {
            while (resultSet.next()) {
                list.add(new SQLStructure(resultSet.getInt("id"),
                                          resultSet.getString("type"),
                                          resultSet.getInt("chunk_x"),
                                          resultSet.getInt("chunk_z"),
                                          resultSet.getInt("ax"),
                                          resultSet.getInt("ay"),
                                          resultSet.getInt("az"),
                                          resultSet.getInt("bx"),
                                          resultSet.getInt("by"),
                                          resultSet.getInt("bz"),
                                          resultSet.getString("json")));
            }
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
        return list;
    }

    public List<SQLStructure> getStructures(int x, int z) {
        return getStructures(getStructureRefs(x, z));
    }
}
