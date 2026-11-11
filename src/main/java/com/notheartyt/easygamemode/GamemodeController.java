package com.notheartyt.easygamemode;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Sends the normal /gamemode command through the server and tracks whether the server confirmed it.
 * Nothing here needs a server-side mod. Gamemodes shown in the GUI always come from the server's
 * player list, never from what we sent.
 */
public final class GamemodeController {
    private GamemodeController() {}

    public static final GameMode[] MODES = {
            GameMode.SURVIVAL, GameMode.CREATIVE, GameMode.ADVENTURE, GameMode.SPECTATOR
    };

    public static final int GRAY = 0xFF9A9AB0, RED = 0xFFFF5555, GREEN = 0xFF55FF55, YELLOW = 0xFFFFEE55;

    private static String status = "";
    private static int statusColor = GRAY;

    private static GameMode pendingMode;
    private static String pendingName; // null = everyone
    private static boolean pendingAlready;
    private static long pendingAt;

    private static String lastCmd = "";
    private static long lastCmdAt;

    public static String label(GameMode mode) {
        if (mode == GameMode.SURVIVAL) return "Survival";
        if (mode == GameMode.CREATIVE) return "Creative";
        if (mode == GameMode.ADVENTURE) return "Adventure";
        if (mode == GameMode.SPECTATOR) return "Spectator";
        return "Unknown";
    }

    private static String commandId(GameMode mode) {
        return label(mode).toLowerCase(Locale.ROOT);
    }

    public static String name(PlayerListEntry entry) {
        return entry.getProfile().name();
    }

    public static String status() {
        return status;
    }

    public static int statusColor() {
        return statusColor;
    }

    private static void setStatus(String text, int color) {
        status = text;
        statusColor = color;
    }

    public static void clearStatus() {
        status = "";
        statusColor = GRAY;
    }

    public static void reset() {
        pendingMode = null;
        pendingName = null;
        clearStatus();
    }

    /** Online players, sorted by name. Rebuilt on demand so joins and leaves show up automatically. */
    public static List<PlayerListEntry> players() {
        List<PlayerListEntry> out = new ArrayList<>();
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return out;
        Collection<PlayerListEntry> src = handler.getListedPlayerListEntries();
        if (src.isEmpty()) src = handler.getPlayerList();
        for (PlayerListEntry e : src) {
            String n = name(e);
            if (n != null && !n.isEmpty()) out.add(e);
        }
        out.sort(Comparator.comparing((PlayerListEntry e) -> name(e).toLowerCase(Locale.ROOT)));
        return out;
    }

    /** One click: change one player (playerName) or everyone (playerName == null). */
    public static void set(String playerName, GameMode mode) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null || mc.player == null) {
            setStatus("Not connected to a server.", RED);
            return;
        }
        // The server only sends commands we are allowed to run. Never try to bypass that.
        if (handler.getCommandDispatcher().getRoot().getChild("gamemode") == null) {
            setStatus("You don't have permission to use /gamemode on this server.", RED);
            return;
        }

        String cmd = "gamemode " + commandId(mode) + " " + (playerName == null ? "@a" : playerName);
        long now = System.currentTimeMillis();
        if (cmd.equals(lastCmd) && now - lastCmdAt < 500) return; // ignore double clicks
        lastCmd = cmd;
        lastCmdAt = now;

        boolean already;
        if (playerName == null) {
            List<PlayerListEntry> ps = players();
            already = !ps.isEmpty();
            for (PlayerListEntry e : ps) {
                if (e.getGameMode() != mode) {
                    already = false;
                    break;
                }
            }
        } else {
            PlayerListEntry e = handler.getPlayerListEntry(playerName);
            already = e != null && e.getGameMode() == mode;
        }

        handler.sendChatCommand(cmd);
        pendingMode = mode;
        pendingName = playerName;
        pendingAlready = already;
        pendingAt = now;
        setStatus("Sent: /" + cmd, GRAY);
    }

    /** Called every frame by the screen: confirms or times out the last request. */
    public static void poll() {
        if (pendingMode == null) return;
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) {
            pendingMode = null;
            return;
        }
        long age = System.currentTimeMillis() - pendingAt;
        String who = pendingName == null ? "Everyone" : pendingName;
        String mode = label(pendingMode);

        if (pendingAlready) {
            if (age > 900) {
                setStatus(who + " is already in " + mode + " mode.", GRAY);
                pendingMode = null;
            }
            return;
        }

        if (pendingName != null) {
            PlayerListEntry e = handler.getPlayerListEntry(pendingName);
            if (e == null) {
                setStatus(pendingName + " is no longer online.", YELLOW);
                pendingMode = null;
                return;
            }
            if (e.getGameMode() == pendingMode) {
                setStatus(pendingName + " is now in " + mode + " mode.", GREEN);
                pendingMode = null;
                return;
            }
        } else {
            List<PlayerListEntry> ps = players();
            boolean all = !ps.isEmpty();
            for (PlayerListEntry e : ps) {
                if (e.getGameMode() != pendingMode) {
                    all = false;
                    break;
                }
            }
            if (all) {
                setStatus("Everyone is now in " + mode + " mode.", GREEN);
                pendingMode = null;
                return;
            }
        }

        if (age > 2500) {
            setStatus("The server did not confirm the change.", YELLOW);
            pendingMode = null;
        }
    }

    /** Server chat/system messages: a red message right after our command means it was rejected. */
    public static void onGameMessage(Text message, boolean overlay) {
        if (overlay || pendingMode == null) return;
        if (System.currentTimeMillis() - pendingAt > 3000) return;
        TextColor color = message.getStyle().getColor();
        if (color == null) return;
        int rgb = color.getRgb() & 0xFFFFFF;
        if (rgb == 0xFF5555 || rgb == 0xAA0000) {
            String s = message.getString();
            if (s.length() > 70) s = s.substring(0, 70) + "...";
            setStatus("Rejected: " + s, RED);
            pendingMode = null;
        }
    }
}
