package com.bryce.discord.commands;

import com.bryce.discord.services.ConfigService;
import com.bryce.discord.services.DataService;
import com.bryce.discord.services.LoggingService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.Color;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UtilityCommands {
    private final DataService dataService;
    private final ConfigService configService;
    private final LoggingService loggingService;

    private static final String[] AUTHORIZED_USER_IDS = {"529480987525251082", "689519709988585648"};

    public UtilityCommands(DataService dataService, ConfigService configService) {
        this.dataService = dataService;
        this.configService = configService;
        this.loggingService = new LoggingService();
    }

    public void handlePurge(SlashCommandInteractionEvent event) {
        int amount = event.getOption("amount").getAsInt();
        User targetUser = event.getOption("user") != null ? event.getOption("user").getAsUser() : null;

        if (amount < 1 || amount > 100) {
            event.reply("Please provide a number between 1 and 100.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();

        event.deferReply(true).queue();

        channel.getHistory().retrievePast(amount).queue(messages -> {
            List<Message> messagesToDelete;

            if (targetUser != null) {
                messagesToDelete = messages.stream()
                        .filter(message -> message.getAuthor().equals(targetUser))
                        .collect(Collectors.toList());
            } else {
                messagesToDelete = messages;
            }

            if (messagesToDelete.isEmpty()) {
                event.getHook().sendMessage("No messages found to delete.").queue();
                return;
            }

            channel.purgeMessages(messagesToDelete);

            String description;
            if (targetUser != null) {
                description = String.format("**%d** messages from **%s** were purged in %s",
                        messagesToDelete.size(), targetUser.getName(), channel.getAsMention());
            } else {
                description = String.format("**%d** messages were purged in %s",
                        messagesToDelete.size(), channel.getAsMention());
            }

            EmbedBuilder purgeEmbed = new EmbedBuilder()
                    .setTitle("🗑️ Messages Purged")
                    .setDescription(description)
                    .addField("Moderator", event.getUser().getName(), true)
                    .setColor(Color.BLUE)
                    .setTimestamp(Instant.now());

            event.getHook().sendMessage("Successfully deleted " + messagesToDelete.size() + " messages.").queue();

            TextChannel logChannel = loggingService.getLogChannel(event.getGuild(), ConfigService.PURGE_LOG_CHANNEL_NAME);
            if (logChannel != null) {
                logChannel.sendMessageEmbeds(purgeEmbed.build()).queue();
            } else {
                event.getChannel().sendMessageEmbeds(purgeEmbed.build()).queue();
            }
        });
    }

    public void handleSaveSystem(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        boolean isAuthorized = false;

        for (String id : AUTHORIZED_USER_IDS) {
            if (id.equals(userId)) {
                isAuthorized = true;
                break;
            }
        }
        if (!isAuthorized) {
            event.reply("You don't have permission to use this command.").setEphemeral(true).queue();
            return;
        }
        event.reply("💾 Starting save operation...").setEphemeral(true).queue();
        System.out.println("Manual save triggered by " + event.getUser().getName() + " (ID: " + userId + ")");
        try {
            dataService.saveAllData();
            CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> {
                event.getHook().editOriginal("💾 All moderation data has been saved successfully.").queue(
                        success -> System.out.println("Save command response sent successfully"),
                        error -> System.err.println("Error sending save command response: " + error.getMessage())
                );
            });
        } catch (Exception e) {
            System.err.println("Error during save operation: " + e.getMessage());
            e.printStackTrace();

            event.getHook().editOriginal("❌ An error occurred while saving data: " + e.getMessage()).queue(
                    success -> System.out.println("Error response sent successfully"),
                    error -> System.err.println("Error sending error response: " + error.getMessage())
            );
        }
    }
    public void handleExportDb(SlashCommandInteractionEvent event) {
        List<String> allowedUserIds = List.of(
                "529480987525251082",
                "689519709988585648"
        );

        if (!allowedUserIds.contains(event.getUser().getId())) {
            event.reply("❌ You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        File dbFile = new File("modbot.db");

        if (!dbFile.exists()) {
            event.reply("❌ modbot.db not found!").setEphemeral(true).queue();
            return;
        }

        event.reply("📤 Exporting modbot.db...").setEphemeral(true)
                .addFiles(FileUpload.fromData(dbFile, "modbot.db"))
                .queue();
    }

    public static boolean isUserAuthorized(String userId) {
        for (String id : AUTHORIZED_USER_IDS) {
            if (id.equals(userId)) {
                return true;
            }
        }
        return false;
    }
}