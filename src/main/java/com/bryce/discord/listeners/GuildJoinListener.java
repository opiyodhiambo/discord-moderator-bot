package com.bryce.discord.listeners;

import com.bryce.discord.services.DataService;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildJoinListener extends ListenerAdapter {

    private final DataService dataService;

    public GuildJoinListener(DataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        dataService.saveGuildInfo(event.getGuild());
        System.out.println("[GuildJoinListener] Saved guild info for: " + event.getGuild().getName());
    }
}