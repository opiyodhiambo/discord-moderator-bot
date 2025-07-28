package com.bryce.discord.services;

import com.bryce.discord.models.WarnRecord;
import com.bryce.discord.models.ModAction;
import com.bryce.discord.analytics.ActionType;
import net.dv8tion.jda.api.entities.Guild;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

public class DataService {

    private static final boolean DEBUG_MODE = false;

    private String muteRoleId = null;
    private boolean warningsModified = false;

    public void loadAllData() {
        loadMuteRoleIdFromDatabase();
        warningsModified = false;
    }

    public void saveAllData() {
        warningsModified = false;
    }

    public boolean isDataModified() {
        return warningsModified;
    }

    public String getMuteRoleId() {
        return muteRoleId;
    }

    public void setMuteRoleId(String muteRoleId) {
        saveMuteRoleIdToDatabase(muteRoleId);
    }

    public void markWarningsModified() {
        warningsModified = true;
    }

    public void addWarning(WarnRecord warn) {
        saveWarnRecord(warn);
        markWarningsModified();
    }

    public List<WarnRecord> getAllWarnings() {
        return loadWarnRecords();
    }

    public List<WarnRecord> getWarningsForUser(String userId) {
        try {
            return DatabaseManager.executeWithRetry(conn -> {
                List<WarnRecord> records = new ArrayList<>();
                String sql = "SELECT userId, moderatorId, reason, timestamp FROM warnings WHERE userId = ?";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, userId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            WarnRecord record = new WarnRecord(
                                    rs.getString("userId"),
                                    rs.getString("moderatorId"),
                                    rs.getString("reason"),
                                    rs.getLong("timestamp")
                            );
                            records.add(record);
                        }
                    }
                }
                return records;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void deleteWarningsForUser(String userId) {
        try {
            DatabaseManager.executeWithRetry(conn -> {
                String sql = "DELETE FROM warnings WHERE userId = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, userId);
                    int rows = pstmt.executeUpdate();
                    System.out.println("Deleted " + rows + " warnings for userId: " + userId);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveWarnRecord(WarnRecord warn) {
        try {
            DatabaseManager.executeWithRetry(conn -> {
                String sql = "INSERT INTO warnings (userId, moderatorId, reason, timestamp) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, warn.getUserId());
                    pstmt.setString(2, warn.getModeratorId());
                    pstmt.setString(3, warn.getReason());
                    pstmt.setLong(4, warn.getTimestamp());

                    if (DEBUG_MODE) {
                        System.out.println("[DEBUG] Saving warning to DB for userId=" + warn.getUserId());
                    }
                    pstmt.executeUpdate();
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<WarnRecord> loadWarnRecords() {
        try {
            return DatabaseManager.executeWithRetry(conn -> {
                List<WarnRecord> records = new ArrayList<>();
                String sql = "SELECT userId, moderatorId, reason, timestamp FROM warnings";

                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        WarnRecord record = new WarnRecord(
                                rs.getString("userId"),
                                rs.getString("moderatorId"),
                                rs.getString("reason"),
                                rs.getLong("timestamp")
                        );
                        records.add(record);
                    }
                }
                return records;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveModerationAnalytics(ModAction action) {
        try {
            DatabaseManager.executeWithRetry(conn -> {
                String sql = "INSERT INTO moderation_analytics (action, moderatorId, moderatorName, targetId, targetName, reason, timestamp, duration, count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, action.getActionType().name());
                    pstmt.setString(2, action.getModeratorId());
                    pstmt.setString(3, action.getModeratorName());
                    pstmt.setString(4, action.getTargetId());
                    pstmt.setString(5, action.getTargetName());
                    pstmt.setString(6, action.getReason());
                    pstmt.setLong(7, action.getTimestamp());
                    pstmt.setInt(8, action.getDuration());
                    pstmt.setInt(9, action.getCount());

                    pstmt.executeUpdate();
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ModAction> loadModerationAnalytics() {
        try {
            return DatabaseManager.executeWithRetry(conn -> {
                List<ModAction> actions = new ArrayList<>();
                String sql = "SELECT action, moderatorId, moderatorName, targetId, targetName, reason, timestamp, duration, count FROM moderation_analytics";

                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        ModAction action = new ModAction(
                                ActionType.valueOf(rs.getString("action")),
                                rs.getString("moderatorId") != null ? rs.getString("moderatorId") : "unknown",
                                rs.getString("moderatorName") != null ? rs.getString("moderatorName") : "Unknown",
                                rs.getString("targetId") != null ? rs.getString("targetId") : "0",
                                rs.getString("targetName") != null ? rs.getString("targetName") : "Unknown",
                                rs.getString("reason") != null ? rs.getString("reason") : "",
                                rs.getLong("timestamp"),
                                rs.getInt("duration"),
                                rs.getInt("count")
                        );
                        actions.add(action);
                    }
                }
                return actions;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveMuteRoleIdToDatabase(String muteRoleId) {
        try {
            DatabaseManager.executeWithRetry(conn -> {
                String sql = "INSERT OR REPLACE INTO bot_settings (key, value) VALUES ('muteRoleId', ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, muteRoleId);
                    pstmt.executeUpdate();

                    this.muteRoleId = muteRoleId;
                    System.out.println("Saved muteRoleId to database: " + muteRoleId);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadMuteRoleIdFromDatabase() {
        try {
            String result = DatabaseManager.executeWithRetry(conn -> {
                String sql = "SELECT value FROM bot_settings WHERE key = 'muteRoleId'";
                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {

                    if (rs.next()) {
                        return rs.getString("value");
                    }
                }
                return null;
            });

            if (result != null) {
                muteRoleId = result;
                System.out.println("Loaded muteRoleId from database: " + muteRoleId);
            } else {
                muteRoleId = null;
                System.out.println("muteRoleId not found in database, using null.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            muteRoleId = null;
        }
    }

    public void saveGuildInfo(Guild guild) {
        try {
            DatabaseManager.executeWithRetry(conn -> {
                String sql = "INSERT OR REPLACE INTO guilds (guildId, guildName, joinedTimestamp) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, guild.getId());
                    pstmt.setString(2, guild.getName());
                    pstmt.setLong(3, System.currentTimeMillis());

                    pstmt.executeUpdate();
                    System.out.println("Saved guild info: " + guild.getName());
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveCommandLog(String userId, String userName, String commandName) {
        try {
            DatabaseManager.executeWithRetry(conn -> {
                String sql = "INSERT INTO command_logs (userId, userName, commandName, timestamp) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, userId);
                    pstmt.setString(2, userName);
                    pstmt.setString(3, commandName);
                    pstmt.setLong(4, System.currentTimeMillis());

                    pstmt.executeUpdate();
                    System.out.println("Logged command: " + commandName + " by " + userName);
                }
                return null;
            });
        } catch (Exception e) {
            System.err.println("Failed to log command: " + e.getMessage());
        }
    }
}