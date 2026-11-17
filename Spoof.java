package com.trueopsec;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** All the (purely client-side) state + text rewriting. Nothing here is ever sent to the server. */
public final class Spoof {
    public static String username = "";
    public static boolean invisible = false;

    public static boolean boardOn = false;
    public static String boardName = "";
    public static String boardMoney = "";
    public static String boardKills = "";
    public static String boardShards = "";

    private static final Pattern NUM = Pattern.compile("\\$?\\d[\\d,]*(?:\\.\\d+)?[KMBTkmbt]?");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("trueopsec.properties");

    private Spoof() {}

    // ---------- persistence ----------
    public static void load() {
        try (InputStream in = Files.newInputStream(FILE)) {
            Properties p = new Properties();
            p.load(in);
            username = p.getProperty("username", "");
            invisible = Boolean.parseBoolean(p.getProperty("invisible", "false"));
            boardOn = Boolean.parseBoolean(p.getProperty("boardOn", "false"));
            boardName = p.getProperty("boardName", "");
            boardMoney = p.getProperty("boardMoney", "");
            boardKills = p.getProperty("boardKills", "");
            boardShards = p.getProperty("boardShards", "");
        } catch (Exception ignored) {
            // first run
        }
    }

    public static void save() {
        try (OutputStream out = Files.newOutputStream(FILE)) {
            Properties p = new Properties();
            p.setProperty("username", username);
            p.setProperty("invisible", Boolean.toString(invisible));
            p.setProperty("boardOn", Boolean.toString(boardOn));
            p.setProperty("boardName", boardName);
            p.setProperty("boardMoney", boardMoney);
            p.setProperty("boardKills", boardKills);
            p.setProperty("boardShards", boardShards);
            p.store(out, "True Opsec (client-side only)");
        } catch (Exception ignored) {
        }
    }

    // ---------- helpers ----------
    private static String realName() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getUser() == null ? null : mc.getUser().getName();
    }

    private static final class Seg {
        Style style;
        String text;
        Seg(Style s, String t) { style = s; text = t; }
    }

    private static List<Seg> flatten(Component c) {
        List<Seg> out = new ArrayList<>();
        c.visit((style, str) -> {
            out.add(new Seg(style, str));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static MutableComponent rebuild(List<Seg> segs) {
        MutableComponent out = Component.empty();
        for (Seg s : segs) out.append(Component.literal(s.text).withStyle(s.style));
        return out;
    }

    // ---------- username (chat, tab list, name tags) ----------
    /** Replaces your real name with the fake one. Returns the same instance if nothing changed. */
    public static Component applyName(Component c, String fake) {
        String real = realName();
        if (c == null || fake == null || fake.isBlank() || real == null || real.isEmpty()) return c;
        if (!c.getString().contains(real)) return c;
        List<Seg> segs = flatten(c);
        for (Seg s : segs) s.text = s.text.replace(real, fake);
        return rebuild(segs);
    }

    public static Component applyUsername(Component c) {
        return applyName(c, username);
    }

    // ---------- DonutSMP scoreboard ----------
    /** Used for every team-formatted name (sidebar lines, tab list, name tags). */
    public static Component applyTeamLine(Component c) {
        Component r = applyUsername(c);
        if (!boardOn) return r;

        // Board "name" wins over the username changer on these lines.
        if (!boardName.isBlank()) {
            String real = realName();
            List<Seg> tmp = flatten(r);
            boolean hit = false;
            for (Seg s : tmp) {
                for (String old : new String[]{real, username}) {
                    if (old != null && !old.isBlank() && s.text.contains(old)) {
                        s.text = s.text.replace(old, boardName);
                        hit = true;
                    }
                }
            }
            if (hit) r = rebuild(tmp);
        }

        List<Seg> segs = flatten(r);
        boolean changed = false;
        String pending = null;
        for (Seg s : segs) {
            String low = s.text.toLowerCase(Locale.ROOT);
            String kw = low.contains("shard") ? "shard"
                    : low.contains("kill") ? "kill"
                    : (low.contains("money") || low.contains("balance")) ? "money" : null;
            if (kw != null) {
                String val = valueFor(kw);
                if (val.isBlank()) { pending = null; continue; }
                Matcher m = NUM.matcher(s.text);
                if (m.find(Math.max(0, low.indexOf(kw)))) {
                    s.text = swap(s.text, m, kw, val);
                    changed = true;
                    pending = null;
                } else {
                    pending = kw; // number is in the next segment
                }
            } else if (pending != null) {
                Matcher m = NUM.matcher(s.text);
                if (m.find()) {
                    s.text = swap(s.text, m, pending, valueFor(pending));
                    changed = true;
                    pending = null;
                }
            }
        }
        return changed ? rebuild(segs) : r;
    }

    private static String valueFor(String kw) {
        return switch (kw) {
            case "money" -> boardMoney;
            case "kill" -> boardKills;
            default -> boardShards;
        };
    }

    private static String swap(String text, Matcher m, String kw, String val) {
        String repl = val;
        if (kw.equals("money") && m.group().startsWith("$") && !val.startsWith("$")) repl = "$" + val;
        return text.substring(0, m.start()) + repl + text.substring(m.end());
    }
}
