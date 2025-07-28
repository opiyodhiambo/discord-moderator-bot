package com.bryce.discord.listeners;

import com.bryce.discord.analytics.ActionType;
import com.bryce.discord.analytics.ModerationAnalytics;
import com.bryce.discord.services.ConfigService;
import com.bryce.discord.services.DataService;
import com.bryce.discord.services.LoggingService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Arrays;

public class MessageListener extends ListenerAdapter {
    private final DataService dataService;
    private final ConfigService configService;
    private final LoggingService loggingService;
    private final ModerationAnalytics analytics;

    public MessageListener(DataService dataService, ConfigService configService, ModerationAnalytics analytics) {
        this.dataService = dataService;
        this.configService = configService;
        this.loggingService = new LoggingService();
        this.analytics = analytics;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Member member = event.getMember();
        if (member != null && configService.hasAdminPermissions(member)) {
            return;
        }

        String channelId = event.getChannel().getId();
        Message message = event.getMessage();

        if (configService.getMediaOnlyChannels().contains(channelId)) {
            boolean hasMedia = !message.getAttachments().isEmpty() || containsLinks(message.getContentRaw());
            boolean hasOnlyMedia = message.getContentRaw().trim().isEmpty() || containsOnlyLinks(message.getContentRaw());

            if (!hasMedia || !hasOnlyMedia) {
                message.delete().queue();
                loggingService.sendWarningEmbed(event.getChannel().asTextChannel(),
                        "Only images and links are allowed in this channel (no regular chat messages).", null);

                analytics.recordAction(ActionType.MESSAGE_DELETE, event.getJDA().getSelfUser(), event.getAuthor(),
                        "Deleted message in Media-Only Channel", 0, 0);

                return;
            }
            return;
        }

        if (configService.getScreenshotOnlyChannels().contains(channelId)) {
            if (message.getAttachments().isEmpty() || !message.getContentRaw().isEmpty()) {
                message.delete().queue();
                loggingService.sendWarningEmbed(event.getChannel().asTextChannel(),
                        "Only images/screenshots are allowed in this channel (no text).", null);

                analytics.recordAction(ActionType.MESSAGE_DELETE, event.getJDA().getSelfUser(), event.getAuthor(),
                        "Deleted message in Screenshot-Only Channel", 0, 0);

                return;
            }
            if (message.getAttachments().stream().anyMatch(attachment ->
                    !attachment.isImage())) {
                message.delete().queue();
                loggingService.sendWarningEmbed(event.getChannel().asTextChannel(),
                        "Only images/screenshots are allowed in this channel.", null);

                analytics.recordAction(ActionType.MESSAGE_DELETE, event.getJDA().getSelfUser(), event.getAuthor(),
                        "Deleted non-image attachment in Screenshot-Only Channel", 0, 0);

                return;
            }
            return;
        }

        if (configService.getNoMessageChannels().contains(channelId)) {
            message.delete().queue();
            loggingService.sendWarningEmbed(event.getChannel().asTextChannel(),
                    "Messages are not allowed in this channel.", null);

            analytics.recordAction(ActionType.MESSAGE_DELETE, event.getJDA().getSelfUser(), event.getAuthor(),
                    "Deleted message in No-Message Channel", 0, 0);

            return;
        }

        if (configService.getNoMediaChannels().contains(channelId)) {
            if (!message.getAttachments().isEmpty() || containsLinks(message.getContentRaw())) {
                message.delete().queue();
                loggingService.sendWarningEmbed(event.getChannel().asTextChannel(),
                        "Media and links are not allowed in this channel.", null);

                analytics.recordAction(ActionType.MESSAGE_DELETE, event.getJDA().getSelfUser(), event.getAuthor(),
                        "Deleted media/link in No-Media Channel", 0, 0);

                return;
            }
        }

        if (configService.getNoContentChannels().contains(channelId)) {
            message.delete().queue();
            loggingService.sendWarningEmbed(event.getChannel().asTextChannel(),
                    "No content is allowed in this channel.", null);

            analytics.recordAction(ActionType.MESSAGE_DELETE, event.getJDA().getSelfUser(), event.getAuthor(),
                    "Deleted message in No-Content Channel", 0, 0);
        }
    }

    private boolean containsLinks(String content) {
        return content.matches(".*https?://\\S+.*");
    }

    private boolean containsOnlyLinks(String content) {
        String[] words = content.trim().split("\\s+");

        return Arrays.stream(words)
                .filter(word -> !word.isEmpty())
                .allMatch(word -> word.matches("https?://\\S+.*"));
    }
}