
package com.frankenstein.core;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class FrankensteinCore extends PluginBase {

    private Config homes;
    private Config mutes;
    private Config sizes;

    private final Map<String, String> requests = new HashMap<>();
    private final Map<String, Long> requestTimes = new HashMap<>();
    private final Map<String, Location> backs = new HashMap<>();
    private final Map<String, Boolean> flying = new HashMap<>();
    private final Map<String, Boolean> vanished = new HashMap<>();

    private static final long REQUEST_LIFETIME = 60000L;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        homes = new Config(new File(getDataFolder(), "homes.yml"), Config.YAML);
        mutes = new Config(new File(getDataFolder(), "mutes.yml"), Config.YAML);
        sizes = new Config(new File(getDataFolder(), "sizes.yml"), Config.YAML);

        getServer().getPluginManager()
            .registerEvents(new CoreListener(this), this);

        getLogger().info("FrankensteinCore 1.2.0 enabled");
    }

    @Override
    public void onDisable() {
        homes.save();
        mutes.save();
        sizes.save();
    }

    private String id(Player p) {
        return p.getUniqueId().toString();
    }

    private String name(Player p) {
        return p.getName().toLowerCase(Locale.ROOT);
    }

    private boolean admin(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage("§cНедостаточно прав!");
            return false;
        }
        return true;
    }

    public boolean isMuted(String playerName) {
        return mutes.getBoolean(playerName.toLowerCase(Locale.ROOT));
    }

    public void applySavedSize(Player player) {
        String value = sizes.getString(id(player), "1.0");

        try {
            float scale = Float.parseFloat(value);
            if (scale >= 0.5f && scale <= 3.0f) {
                player.setScale(scale);
            } else {
                player.setScale(1.0f);
            }
        } catch (NumberFormatException e) {
            player.setScale(1.0f);
        }
    }

    private boolean teleport(Player player, Location destination) {
        Location previous = player.getLocation();

        if (!player.teleport(destination)) {
            player.sendMessage("§cТелепортация не удалась.");
            return false;
        }

        backs.put(id(player), previous);
        return true;
    }

    @Override
    public boolean onCommand(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда доступна только игрокам.");
            return true;
        }

        Player p = (Player) sender;
        String playerId = id(p);
        String playerName = name(p);

        switch (cmd) {

            case "sethome": {
                Location loc = p.getLocation();

                homes.set(playerName,
                    loc.getX() + ";" + loc.getY() + ";" + loc.getZ()
                    + ";" + p.getLevel().getFolderName()
                    + ";" + loc.getYaw() + ";" + loc.getPitch());

                homes.save();
                p.sendMessage("§aДом сохранён!");
                return true;
            }

            case "home": {
                String home = homes.getString(playerName, "");

                if (home.isEmpty()) {
                    p.sendMessage("§cДом не найден.");
                    return true;
                }

                try {
                    String[] h = home.split(";");
                    if (h.length != 6) {
                        p.sendMessage("§cДанные дома повреждены.");
                        return true;
                    }

                    Level level = getServer().getLevelByName(h[3]);
                    if (level == null) {
                        p.sendMessage("§cМир дома не найден.");
                        return true;
                    }

                    Location destination = new Location(
                        Double.parseDouble(h[0]),
                        Double.parseDouble(h[1]),
                        Double.parseDouble(h[2]),
                        Double.parseDouble(h[4]),
                        Double.parseDouble(h[5]),
                        level
                    );

                    if (teleport(p, destination)) {
                        p.sendMessage("§aТы дома!");
                    }
                } catch (Exception e) {
                    p.sendMessage("§cОшибка данных дома.");
                    getLogger().warning("Home error: " + e.getMessage());
                }
                return true;
            }

            case "delhome": {
                homes.remove(playerName);
                homes.save();
                p.sendMessage("§aДом удалён.");
                return true;
            }

            case "tpa": {
                if (args.length != 1) {
                    p.sendMessage("§cИспользуй: /tpa <игрок>");
                    return true;
                }

                Player target = getServer().getPlayer(args[0]);

                if (target == null || target == p) {
                    p.sendMessage("§cИгрок не найден или указан ты сам.");
                    return true;
                }

                String targetId = id(target);
                Long oldTime = requestTimes.get(targetId);

                if (oldTime != null &&
                    System.currentTimeMillis() - oldTime <= REQUEST_LIFETIME) {
                    p.sendMessage("§cУ игрока уже есть активный запрос.");
                    return true;
                }

                requests.put(targetId, playerId);
                requestTimes.put(targetId, System.currentTimeMillis());

                target.sendMessage("§e" + p.getName()
                    + " хочет телепортироваться к тебе. /tpaccept или /tpdeny");

                p.sendMessage("§aЗапрос отправлен на 60 секунд.");
                return true;
            }

            case "tpaccept": {
                String requesterId = requests.get(playerId);
                Long created = requestTimes.get(playerId);

                if (requesterId == null || created == null) {
                    p.sendMessage("§cНет запросов.");
                    return true;
                }

                if (System.currentTimeMillis() - created > REQUEST_LIFETIME) {
                    requests.remove(playerId);
                    requestTimes.remove(playerId);
                    p.sendMessage("§cЗапрос истёк.");
                    return true;
                }

                Player requester = null;
                for (Player online : getServer().getOnlinePlayers().values()) {
                    if (id(online).equals(requesterId)) {
                        requester = online;
                        break;
                    }
                }

                if (requester == null) {
                    requests.remove(playerId);
                    requestTimes.remove(playerId);
                    p.sendMessage("§cИгрок уже вышел.");
                    return true;
                }

                if (teleport(requester, p.getLocation())) {
                    requests.remove(playerId);
                    requestTimes.remove(playerId);
                    requester.sendMessage("§aЗапрос принят!");
                    p.sendMessage("§aТелепортация выполнена.");
                }
                return true;
            }

            case "tpdeny": {
                String deniedId = requests.remove(playerId);
                requestTimes.remove(playerId);

                if (deniedId == null) {
                    p.sendMessage("§cНет запросов.");
                    return true;
                }

                for (Player online : getServer().getOnlinePlayers().values()) {
                    if (id(online).equals(deniedId)) {
                        online.sendMessage("§cЗапрос отклонён.");
                        break;
                    }
                }

                p.sendMessage("§eЗапрос отклонён.");
                return true;
            }

            case "back": {
                Location previous = backs.get(playerId);

                if (previous == null) {
                    p.sendMessage("§cНет предыдущего места.");
                    return true;
                }

                if (p.teleport(previous)) {
                    backs.remove(playerId);
                    p.sendMessage("§aВозвращение выполнено.");
                } else {
                    p.sendMessage("§cНе удалось вернуться.");
                }
                return true;
            }

            case "spawn": {
                Level level = getServer().getDefaultLevel();

                if (level == null) {
                    p.sendMessage("§cМир спавна не найден.");
                    return true;
                }

                Location destination = Location.fromObject(
                    level.getSafeSpawn(), level
                );

                if (teleport(p, destination)) {
                    p.sendMessage("§aТы на спавне!");
                }
                return true;
            }

            case "fly": {
                if (!admin(p)) return true;

                boolean enabled = !flying.getOrDefault(playerId, false);
                flying.put(playerId, enabled);
                p.setAllowFlight(enabled);

                p.sendMessage(enabled
                    ? "§aПолёт включён."
                    : "§cПолёт выключен.");
                return true;
            }

            case "v": {
                if (!admin(p)) return true;

                boolean hide = !vanished.getOrDefault(playerId, false);
                vanished.put(playerId, hide);

                for (Player online : getServer().getOnlinePlayers().values()) {
                    if (online == p) continue;

                    if (hide) {
                        online.hidePlayer(p);
                    } else {
                        online.showPlayer(p);
                    }
                }

                p.sendMessage(hide
                    ? "§aНевидимость включена."
                    : "§cНевидимость выключена.");
                return true;
            }

            case "gm": {
                if (!admin(p)) return true;

                if (args.length != 1) {
                    p.sendMessage("§cИспользуй: /gm 0-3");
                    return true;
                }

                try {
                    int mode = Integer.parseInt(args[0]);

                    if (mode < 0 || mode > 3) {
                        p.sendMessage("§cРежим должен быть от 0 до 3.");
                        return true;
                    }

                    if (p.setGamemode(mode)) {
                        p.sendMessage("§aРежим игры изменён.");
                    } else {
                        p.sendMessage("§cНе удалось изменить режим.");
                    }
                } catch (NumberFormatException e) {
                    p.sendMessage("§cУкажи число от 0 до 3.");
                }
                return true;
            }

            case "mute": {
                if (!admin(p)) return true;
                if (args.length != 1) {
                    p.sendMessage("§cИспользуй: /mute <игрок>");
                    return true;
                }

                mutes.set(args[0].toLowerCase(Locale.ROOT), true);
                mutes.save();
                p.sendMessage("§aМут выдан.");
                return true;
            }

            case "unmute": {
                if (!admin(p)) return true;
                if (args.length != 1) {
                    p.sendMessage("§cИспользуй: /unmute <игрок>");
                    return true;
                }

                mutes.remove(args[0].toLowerCase(Locale.ROOT));
                mutes.save();
                p.sendMessage("§aМут снят.");
                return true;
            }

            case "size": {
                if (!admin(p)) return true;

                if (args.length != 1) {
                    p.sendMessage("§eРазмеры: 0.5, 1.0, 1.5, 2.0, 2.5, 3.0");
                    p.sendMessage("§eСброс: /size reset");
                    return true;
                }

                if (args[0].equalsIgnoreCase("reset")) {
                    p.setScale(1.0f);
                    sizes.set(playerId, "1.0");
                    sizes.save();
                    p.sendMessage("§aРазмер сброшен.");
                    return true;
                }

                try {
                    float scale = Float.parseFloat(args[0]);

                    if (scale != 0.5f && scale != 1.0f &&
                        scale != 1.5f && scale != 2.0f &&
                        scale != 2.5f && scale != 3.0f) {
                        p.sendMessage("§cДопустимые размеры: 0.5, 1.0, 1.5, 2.0, 2.5, 3.0");
                        return true;
                    }

                    p.setScale(scale);
                    sizes.set(playerId, String.valueOf(scale));
                    sizes.save();

                    p.sendMessage("§aРазмер установлен: " + scale);
                } catch (NumberFormatException e) {
                    p.sendMessage("§cНеверный размер.");
                }
                return true;
            }
        }
