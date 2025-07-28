package com.bryce.discord.commands;

import com.bryce.discord.analytics.ModerationAnalytics;
import com.bryce.discord.services.ConfigService;
import com.bryce.discord.services.DataService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager extends ListenerAdapter {
    private final DataService dataService;
    private final ConfigService configService;
    private final ModerationAnalytics analytics;

    private final ModerationCommands moderationCommands;
    private final UtilityCommands utilityCommands;

    public CommandManager(DataService dataService, ConfigService configService, ModerationAnalytics analytics) {
        this.dataService = dataService;
        this.configService = configService;
        this.analytics = analytics;

        this.moderationCommands = new ModerationCommands(dataService, configService, analytics);
        this.utilityCommands = new UtilityCommands(dataService, configService);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("savesystem")) {
            String userId = event.getUser().getId();
            if (UtilityCommands.isUserAuthorized(userId)) {
                utilityCommands.handleSaveSystem(event);
            } else {
                event.reply("You don't have permission to use this command.").setEphemeral(true).queue();
            }
            return;
        }
        switch (event.getName()) {
            case "warn":
                moderationCommands.handleWarn(event);
                break;
            case "setmuterole":
                moderationCommands.handleSetMuteRole(event);
                break;
            case "mute":
                moderationCommands.handleMute(event);
                break;
            case "unmute":
                moderationCommands.handleUnmute(event);
                break;
            case "timeout":
                moderationCommands.handleTimeout(event);
                break;
            case "untimeout":
                moderationCommands.handleUntimeout(event);
                break;
            case "ban":
                moderationCommands.handleBan(event);
                break;
            case "unban":
                moderationCommands.handleUnban(event);
                break;
            case "kick":
                moderationCommands.handleKick(event);
                break;
            case "purge":
                utilityCommands.handlePurge(event);
                break;
            case "exportdb":
                utilityCommands.handleExportDb(event);
                break;
        }
    }

    public void registerCommands(JDA jda) {
        List<CommandData> globalCommands = createGlobalCommands();

        jda.updateCommands().addCommands(globalCommands).queue(success -> {
            System.out.println("✅ Successfully registered " + globalCommands.size() + " global commands: " +
                    globalCommands.stream().map(CommandData::getName).collect(Collectors.joining(", ")));
        }, error -> {
            System.err.println("❌ Failed to register global commands: " + error.getMessage());
        });
    }

    private List<CommandData> createGlobalCommands() {
        List<CommandData> globalCommands = new ArrayList<>();

        globalCommands.add(Commands.slash("warn", "Give a warning to a user")
                .addOption(OptionType.USER, "user", "The user to warn", true)
                .addOption(OptionType.STRING, "reason", "Reason for the warning", true)
                .addOption(OptionType.ATTACHMENT, "evidence", "Evidence image", false)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED));

        globalCommands.add(Commands.slash("mute", "Mute a user in all channels")
                .addOption(OptionType.USER, "user", "The user to mute", true)
                .addOption(OptionType.STRING, "reason", "Reason for the mute", true)
                .addOption(OptionType.INTEGER, "duration", "Duration in minutes (default: permanent)", false)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED));

        globalCommands.add(Commands.slash("unmute", "Unmute a previously muted user")
                .addOption(OptionType.USER, "user", "The user to unmute", true)
                .addOption(OptionType.STRING, "reason", "Reason for unmuting", true)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED));

        globalCommands.add(Commands.slash("timeout", "Timeout a user for a specified duration")
                .addOption(OptionType.USER, "user", "The user to timeout", true)
                .addOption(OptionType.STRING, "reason", "Reason for the timeout", true)
                .addOption(OptionType.INTEGER, "duration", "Duration in minutes (default: 60)", false)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED));

        globalCommands.add(Commands.slash("untimeout", "Remove a timeout from a user")
                .addOption(OptionType.USER, "user", "The user to remove timeout from", true)
                .addOption(OptionType.STRING, "reason", "Reason for removing the timeout", true)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED));

        globalCommands.add(Commands.slash("ban", "Ban a user from the server")
                .addOption(OptionType.USER, "user", "The user to ban", true)
                .addOption(OptionType.STRING, "reason", "Reason for the ban", true)
                .addOption(OptionType.INTEGER, "delete_days", "Number of days of messages to delete (0-7)", false)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED));

        globalCommands.add(Commands.slash("unban", "Unban a user from the server")
                .addOption(OptionType.STRING, "user_id", "The Discord user ID of the banned user", true)
                .addOption(OptionType.STRING, "reason", "Reason for the unban", true)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED));

        globalCommands.add(Commands.slash("kick", "Kick a user from the server")
                .addOption(OptionType.USER, "user", "The user to kick", true)
                .addOption(OptionType.STRING, "reason", "Reason for the kick", true)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED));

        globalCommands.add(Commands.slash("purge", "Delete multiple messages from the channel")
                .addOption(OptionType.INTEGER, "amount", "Number of messages to delete (1-100)", true)
                .addOption(OptionType.USER, "user", "Optional: Delete only messages from this user", false)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED));

        globalCommands.add(Commands.slash("setmuterole", "Set the role to use for muting users")
                .addOption(OptionType.ROLE, "role", "The role to use for muting", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

        globalCommands.add(Commands.slash("savesystem", "Force save all moderation data")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

        globalCommands.add(Commands.slash("exportdb", "Export modbot.db database (owner only)")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

        return globalCommands;
    }
}