package com.bryce.discord;

import com.bryce.discord.analytics.ModerationAnalytics;
import com.bryce.discord.commands.CommandManager;
import com.bryce.discord.listeners.MessageListener;
import com.bryce.discord.services.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class BryceModeratingBot {

    private static final int AUTOSAVE_MINUTES = 15;
    private final DataService dataService;
    private final ConfigService configService;
    private final ModerationAnalytics analytics;
    private final MessageListener messageListener;
    private final CommandManager commandManager;
    private final BackupService backupService;

    public BryceModeratingBot() {
        backupService = new BackupService();
        backupService.restoreFromBackup();

        DatabaseInitializer.initializeDatabase();

        dataService = new DataService();
        dataService.loadAllData();
        dataService.loadMuteRoleIdFromDatabase();

        configService = new ConfigService();

        analytics = new ModerationAnalytics(dataService);

        messageListener = new MessageListener(dataService, configService, analytics);

        commandManager = new CommandManager(dataService, configService, analytics);

        setupChannelRestrictions();

        setupRoles();

        setupAutoSave();

        backupService.startAutoBackup();

        System.out.println("🔄 Bot initialized with backup protection!");
    }

    private void setupChannelRestrictions() {
        configService.addMediaOnlyChannel("1099664949384593499");
        configService.addMediaOnlyChannel("1380263450445611109");

        configService.addScreenshotOnlyChannel("1295140886543601695");
        configService.addScreenshotOnlyChannel("1099699665567498322");
        configService.addScreenshotOnlyChannel("1380242836661862410");

        configService.addNoMediaChannel("1099483814377562192");
        configService.addNoMediaChannel("1133911285596176454");
        configService.addNoMediaChannel("1380242691291353161");
        configService.addNoMediaChannel("1380243050034495498");
        configService.addNoMediaChannel("1380242747021328444");

        configService.addNoContentChannel("1187274071252152350");
        configService.addNoContentChannel("1184329514940121182");
        configService.addNoContentChannel("1184604933576720424");
        configService.addNoContentChannel("1187241916237103247");
        configService.addNoContentChannel("1187242107270877255");
        configService.addNoContentChannel("1188664085798199357");
        configService.addNoContentChannel("1190126946064023734");
        configService.addNoContentChannel("1165378682395832491");
        configService.addNoContentChannel("1196203347187876040");
        configService.addNoContentChannel("1196203326744834108");
        configService.addNoContentChannel("1119245460884951132");
        configService.addNoContentChannel("1296259272258097182");
        configService.addNoContentChannel("1296611356266598462");
        configService.addNoContentChannel("1283566740550451200");
        configService.addNoContentChannel("1099664933823721593");

        configService.addNoContentChannel("1389821552240885820");
        configService.addNoContentChannel("1389821562030657606");
        configService.addNoContentChannel("1389821610025947166");
        configService.addNoContentChannel("1389821628585611275");
        configService.addNoContentChannel("1389821647451852860");
        configService.addNoContentChannel("1389821656406691983");
        configService.addNoContentChannel("1389821674198798527");
        configService.addNoContentChannel("1389821701151264779");
        configService.addNoContentChannel("1389821724819980408");
    }

    private void setupRoles() {
        configService.addModeratorRole("1211714929665515540");
        configService.addModeratorRole("1329555577646354483");
        configService.addModeratorRole("980866114353508412");

        configService.addAdminRole("898223053832601611");
        configService.addAdminRole("709747039562366977");
        configService.addAdminRole("1284891611922432010");
        configService.addAdminRole("1162105401366556732");
        configService.addAdminRole("1352351516882894968");
        configService.addAdminRole("977646240789577849");
        configService.addAdminRole("1380227523472003142");
        configService.addAdminRole("1380227657438068776");
    }

    private void setupAutoSave() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (dataService.isDataModified()) {
                    dataService.saveAllData();
                    backupService.createBackup();
                }
            }
        }, AUTOSAVE_MINUTES * 60 * 1000, AUTOSAVE_MINUTES * 60 * 1000);
    }

    public static void main(String[] args) {
        BryceModeratingBot bot = new BryceModeratingBot();

        String token = System.getenv("BOT_TOKEN");

        if (token == null || token.isEmpty()) {
            token = readTokenFromFile("discloud.config", "BOT_TOKEN=");
        }

        System.out.println("Token value: " + (token != null ? token.substring(0, 5) + "..." : "null"));

        if (token == null || token.isEmpty()) {
            System.out.println("ERROR: No bot token found! Check your discloud.config file.");
            return;
        }

        String statusText = readTokenFromFile("discloud.config", "BOT_STATUS=");
        if (statusText == null || statusText.isEmpty()) {
            statusText = "Moderating Channels!";
        }

        String onlineStatusStr = readTokenFromFile("discloud.config", "BOT_ONLINE_STATUS=");
        OnlineStatus onlineStatus = OnlineStatus.IDLE;
        if (onlineStatusStr != null && !onlineStatusStr.isEmpty()) {
            try {
                onlineStatus = OnlineStatus.valueOf(onlineStatusStr);
            } catch (Exception e) {
                System.out.println("Invalid online status, using IDLE");
            }
        }

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(bot.messageListener)
                .addEventListeners(bot.commandManager)
                .addEventListeners(new com.bryce.discord.listeners.GuildJoinListener(bot.dataService))
                .setStatus(onlineStatus)
                .setActivity(Activity.playing(statusText))
                .build();

        jda.addEventListener(new net.dv8tion.jda.api.hooks.ListenerAdapter() {
            @Override
            public void onCommandAutoCompleteInteraction(net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent event) {
                if (event.getName().equals("savesystem")) {
                    String[] authorizedIds = {"529480987525251082", "689519709988585648"};
                    String userId = event.getUser().getId();
                    boolean isAuthorized = false;
                    for (String id : authorizedIds) {
                        if (id.equals(userId)) {
                            isAuthorized = true;
                            break;
                        }
                    }
                    if (!isAuthorized) {
                        event.replyChoices().queue();
                    }
                }
            }
        });

        try {
            jda.awaitReady();
            bot.commandManager.registerCommands(jda);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🔄 Bot shutting down - creating final backup and saving data...");

            try {

                bot.dataService.saveAllData();

                bot.backupService.onShutdown();

                if (jda != null) {
                    jda.shutdown();
                    try {
                        if (!jda.awaitShutdown(5, java.util.concurrent.TimeUnit.SECONDS)) {
                            jda.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        jda.shutdownNow();
                    }
                }

                System.out.println("✅ Shutdown complete!");
            } catch (Exception e) {
                System.err.println("❌ Error during shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }));
    }

    private static String readTokenFromFile(String filename, String prefix) {
        try {
            File file = new File(filename);
            System.out.println("Looking for " + prefix + " in file: " + file.getAbsolutePath() + " - exists: " + file.exists());
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(prefix)) {
                        String value = line.substring(prefix.length()).trim();
                        reader.close();
                        return value;
                    }
                }
                reader.close();
            }
        } catch (IOException e) {
            System.out.println("Error reading file " + filename + ": " + e.getMessage());
        }
        return null;
    }
}