package com.bryce.discord.services;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.Permission;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigService {
    private final Set<String> noMessageChannels = new HashSet<>();
    private final Set<String> noMediaChannels = new HashSet<>();
    private final Set<String> noContentChannels = new HashSet<>();
    private final Set<String> screenshotOnlyChannels = new HashSet<>();
    private final Set<String> mediaOnlyChannels = new HashSet<>();
    private final Set<String> moderatorRoles = new HashSet<>();
    private final Set<String> adminRoles = new HashSet<>();

    private final Map<String, Boolean> permissionCache = new ConcurrentHashMap<>();

    public static final String MODERATION_LOG_CHANNEL_NAME = "moderation-logs";
    public static final String PURGE_LOG_CHANNEL_NAME = "server-logs";

    public boolean hasModeratorPermissions(Member member) {
        if (member == null) {
            return false;
        }

        String cacheKey = "mod:" + member.getId();
        if (permissionCache.containsKey(cacheKey)) {
            return permissionCache.get(cacheKey);
        }

        boolean hasPermissions = member.hasPermission(Permission.MESSAGE_MANAGE) ||
                member.hasPermission(Permission.MODERATE_MEMBERS) ||
                hasAdminPermissions(member);

        if (!hasPermissions) {
            hasPermissions = member.getRoles().stream()
                    .anyMatch(role -> moderatorRoles.contains(role.getId()) ||
                            adminRoles.contains(role.getId()));
        }

        permissionCache.put(cacheKey, hasPermissions);
        return hasPermissions;
    }

    public boolean hasAdminPermissions(Member member) {
        if (member == null) {
            return false;
        }

        String cacheKey = "admin:" + member.getId();
        if (permissionCache.containsKey(cacheKey)) {
            return permissionCache.get(cacheKey);
        }

        boolean hasPermissions = member.hasPermission(Permission.ADMINISTRATOR) ||
                member.hasPermission(Permission.MANAGE_SERVER) ||
                member.hasPermission(Permission.BAN_MEMBERS);

        if (!hasPermissions) {
            hasPermissions = member.getRoles().stream()
                    .anyMatch(role -> adminRoles.contains(role.getId()));
        }

        permissionCache.put(cacheKey, hasPermissions);
        return hasPermissions;
    }

    public void clearPermissionCache(String memberId) {
        permissionCache.remove("mod:" + memberId);
        permissionCache.remove("admin:" + memberId);
    }

    public void addNoMessageChannel(String channelId) {
        noMessageChannels.add(channelId);
    }

    public void addNoMediaChannel(String channelId) {
        noMediaChannels.add(channelId);
    }

    public void addNoContentChannel(String channelId) {
        noContentChannels.add(channelId);
    }

    public void addScreenshotOnlyChannel(String channelId) {
        screenshotOnlyChannels.add(channelId);
    }

    public void addMediaOnlyChannel(String channelId) {
        mediaOnlyChannels.add(channelId);
    }

    public void addModeratorRole(String roleId) {
        moderatorRoles.add(roleId);
    }

    public void addAdminRole(String roleId) {
        adminRoles.add(roleId);
    }

    public Set<String> getNoMessageChannels() {
        return noMessageChannels;
    }

    public Set<String> getNoMediaChannels() {
        return noMediaChannels;
    }

    public Set<String> getNoContentChannels() {
        return noContentChannels;
    }

    public Set<String> getScreenshotOnlyChannels() {
        return screenshotOnlyChannels;
    }

    public Set<String> getMediaOnlyChannels() {
        return mediaOnlyChannels;
    }
    public boolean canModerate(Member moderator, Member target) {
        if (moderator == null || target == null) {
            return false;
        }
        if (hasAdminPermissions(moderator)) {
            return true;
        }
        if (hasModeratorPermissions(target) || hasAdminPermissions(target)) {
            return false;
        }
        return hasModeratorPermissions(moderator);
    }
}