package com.bryce.discord.utils;

import com.bryce.discord.analytics.ActionType;

import java.awt.Color;

public class FormatUtils {
    public static String formatActionType(ActionType type) {
        switch (type) {
            case WARN:
                return "Warning";
            case MUTE:
                return "Mute";
            case UNMUTE:
                return "Unmute";
            case TIMEOUT:
                return "Timeout";
            case UNTIMEOUT:
                return "Untimeout";
            case BAN:
                return "Ban";
            case UNBAN:
                return "Unban";
            case KICK:
                return "Kick";
            case PURGE:
                return "Purge";
            case MESSAGE_DELETE:
                return "Message Delete";
            default:
                return type.toString();
        }
    }

    public static Color getColorForActionType(ActionType type) {
        switch (type) {
            case WARN:
                return Color.YELLOW;
            case MUTE:
                return new Color(128, 0, 128);
            case UNMUTE:
                return Color.GREEN;
            case TIMEOUT:
                return new Color(255, 165, 0);
            case UNTIMEOUT:
                return Color.GREEN;
            case BAN:
                return Color.RED;
            case UNBAN:
                return Color.GREEN;
            case KICK:
                return Color.ORANGE;
            case PURGE:
                return Color.BLUE;
            case MESSAGE_DELETE:
                return Color.ORANGE;
            default:
                return Color.GRAY;
        }
    }
}