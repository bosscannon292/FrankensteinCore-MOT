
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

public class FrankensteinCore extends PluginBase {

    private Config homes;
    private Config mutes;

    private final Map<String, String> requests = new HashMap<>();
    private final Map<String, Location> backs = new HashMap<>();
    private final Map<String, Boolean> flying = new HashMap<>();
    private final Map<String, Boolean> vanished = new HashMap<>();

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        homes = new Config(
            new File(getDataFolder(), "homes.yml"),
            Config.YAML
        );

        mutes = new Config(
            new File(getDataFolder(), "mutes.yml"),
            Config.YAML
        );

        getServer().getPluginManager()
            .registerEvents(new CoreListener(this), this);

        getLogger().info("FrankensteinCore успешно запущен!");
    }

    @Override
    public void onDisable() {
        if (homes != null) homes.save();
        if (mutes != null) mutes.save();
    }

    private boolean admin(Player p) {
        if (!p.isOp()) {
            p.sendMessage("§cНедостаточно прав!");
            return false;
        }
        return true;
    }

    public boolean isMuted(String name) {
        return mutes.getBoolean(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean onCommand(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игрокам.");
            return true;
        }

        Player p = (Player) sender;
        String name = p.getName().toLowerCase(Locale.ROOT);
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        switch (cmd) {

            case "sethome":
                Location loc = p.getLocation();

                homes.set(name,
                    loc.getX() + ";" +
                    loc.getY() + ";" +
                    loc.getZ() + ";" +
                    p.getLevel().getFolderName() + ";" +
                    loc.getYaw() + ";" +
                    loc.getPitch()
                );
                homes.save();
                p.sendMessage("§aДом сохранён!");
                return true;

            case "home":
                String home = homes.getString(name, "");

                if (home.isEmpty()) {
                    p.sendMessage("§cДом не найден.");
                    return true;
                }

                try {
                    String[] h = home.split(";");
                    Level level = getServer().getLevelByName(h[3]);

                    if (level == null) {
                        p.sendMessage("§cМир дома не найден.");
                        return true;
                    }

                    backs.put(name, p.getLocation());

                    p.teleport(new Location(
                        Double.parseDouble(h[0]),
                        Double.parseDouble(h[1]),
                        Double.parseDouble(h[2]),
                        Double.parseDouble(h[4]),
                        Double.parseDouble(h[5]),
                        level
                    ));

                    p.sendMessage("§aТы дома!");
                } catch (Exception e) {
                    p.sendMessage("§cОшибка данных дома.");
                }
                return true;

            case "delhome":
                homes.remove(name);
                homes.save();
                p.sendMessage("§aДом удалён.");
                return true;

            case "tpa":
                if (args.length != 1) {
                    p.sendMessage("§cИспользуй: /tpa <игрок>");
                    return true;
                }

                Player target = getServer().getPlayer(args[0]);

                if (target == null) {
                    p.sendMessage("§cИгрок не найден.");
                    return true;
                }

                if (target == p) {
                    p.sendMessage("§cНельзя отправить запрос самому себе.");
                    return true;
                }

                requests.put(
                    target.getName().toLowerCase(Locale.ROOT),
                    name
                );

                target.sendMessage(
                    "§eИгрок " + p.getName() +
                    " хочет телепортироваться к тебе. /tpaccept или /tpdeny"
                );

                p.sendMessage("§aЗапрос отправлен!");
                return true;

            case "tpaccept":
                String requester = requests.remove(name);

                if (requester == null) {
                    p.sendMessage("§cНет запросов.");
                    return true;
                }

                Player from = getServer().getPlayer(requester);

                if (from == null) {
                    p.sendMessage("§cИгрок уже вышел.");
                    return true;
                }

                backs.put(requester, from.getLocation());
                from.teleport(p.getLocation());

                from.sendMessage("§aЗапрос принят!");
                p.sendMessage("§aТелепортация выполнена.");
                return true;

            case "tpdeny":
                String denied = requests.remove(name);

                if (denied == null) {
                    p.sendMessage("§cНет запросов.");
                    return true;
                }

                Player deniedPlayer = getServer().getPlayer(denied);

                if (deniedPlayer != null) {
                    deniedPlayer.sendMessage("§cЗапрос отклонён.");
                }

                p.sendMessage("§eЗапрос отклонён.");
                return true;

            case "back":
                Location back = backs.remove(name);

                if (back == null) {
                    p.sendMessage("§cНет предыдущего места.");
                    return true;
                }

                p.teleport(back);
                p.sendMessage("§aВозвращение выполнено.");
                return true;

            case "spawn":
                Level defaultLevel = getServer().getDefaultLevel();

                if (defaultLevel != null) {
                    backs.put(name, p.getLocation());
                    p.teleport(Location.fromObject(
                        defaultLevel.getSafeSpawn()
                    ));
                    p.sendMessage("§aТы на спавне!");
                }
                return true;

            case "fly":
                if (!admin(p)) return true;

                boolean fly = !flying.getOrDefault(name, false);
                flying.put(name, fly);
                p.setAllowFlight(fly);

                p.sendMessage(fly
                    ? "§aПолёт включён."
                    : "§cПолёт выключен."
                );
                return true;

            case "v":
                if (!admin(p)) return true;

                boolean hide = !vanished.getOrDefault(name, false);
                vanished.put(name, hide);

                for (Player online : getServer()
                    .getOnlinePlayers().values()) {

                    if (hide) {
                        online.hidePlayer(p);
                    } else {
                        online.showPlayer(p);
                    }
                }

                p.sendMessage(hide
                    ? "§aНевидимость включена."
                    : "§cНевидимость выключена."
                );
                return true;

            case "gm":
                if (!admin(p)) return true;

                if (args.length != 1) {
                    p.sendMessage("§cИспользуй: /gm 0, 1, 2 или 3");
                    return true;
                }

                try {
                    int mode = Integer.parseInt(args[0]);

                    if (mode < 0 || mode > 3) {
                        p.sendMessage("§cРежим должен быть от 0 до 3.");
                        return true;
                    }

                    p.setGamemode(mode);
                    p.sendMessage("§aРежим игры изменён.");
                } catch (NumberFormatException e) {
                    p.sendMessage("§cУкажи число от 0 до 3.");
                }
                return true;

            case "mute":
                if (!admin(p)) return true;

                if (args.length < 1) {
                    p.sendMessage("§cИспользуй: /mute <игрок>");
                    return true;
                }

                mutes.set(args[0].toLowerCase(Locale.ROOT), true);
                mutes.save();
                p.sendMessage("§aМут выдан.");
                return true;

            case "unmute":
                if (!admin(p)) return true;

                if (args.length < 1) {
                    p.sendMessage("§cИспользуй: /unmute <игрок>");
                    return true;
                }

                mutes.remove(args[0].toLowerCase(Locale.ROOT));
                mutes.save();
                p.sendMessage("§aМут снят.");
                return true;
        }

        return false;
    }
                                                    }
