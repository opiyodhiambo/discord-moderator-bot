package com.bryce.discord.services;

import com.bryce.discord.models.WarnRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

public class DatabaseInitializer {
    public static void initializeDatabase() {
        try (Connection conn = DatabaseManager.getConnection()) {

            String warningsTable = "CREATE TABLE IF NOT EXISTS warnings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "userId TEXT NOT NULL," +
                    "moderatorId TEXT," +
                    "reason TEXT," +
                    "timestamp INTEGER" +
                    ")";

            String analyticsTable = "CREATE TABLE IF NOT EXISTS moderation_analytics (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "action TEXT NOT NULL," +
                    "moderatorId TEXT," +
                    "moderatorName TEXT," +
                    "targetId TEXT," +
                    "targetName TEXT," +
                    "reason TEXT," +
                    "timestamp INTEGER," +
                    "duration INTEGER," +
                    "count INTEGER" +
                    ")";

            String settingsTable = "CREATE TABLE IF NOT EXISTS bot_settings (" +
                    "key TEXT PRIMARY KEY," +
                    "value TEXT" +
                    ")";

            String guildsTable = "CREATE TABLE IF NOT EXISTS guilds (" +
                    "guildId TEXT PRIMARY KEY," +
                    "guildName TEXT," +
                    "joinedTimestamp INTEGER" +
                    ")";

            String commandLogsTable = "CREATE TABLE IF NOT EXISTS command_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "userId TEXT," +
                    "userName TEXT," +
                    "commandName TEXT," +
                    "timestamp INTEGER" +
                    ")";

            conn.createStatement().execute(warningsTable);
            conn.createStatement().execute(analyticsTable);
            conn.createStatement().execute(settingsTable);
            conn.createStatement().execute(guildsTable);
            conn.createStatement().execute(commandLogsTable);

            System.out.println("[DatabaseInitializer] Database tables initialized.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}