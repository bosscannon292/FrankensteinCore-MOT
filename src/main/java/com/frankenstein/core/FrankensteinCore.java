package com.frankenstein.core;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FrankensteinCore extends PluginBase {

    private Config homes;
    private Config mutes;
    private final Map<String, String> requests = new HashMap<>();
    private final Map<String, Boolean> vanished = new HashMap<>();
    private final Map<String, Boolean> flying = new HashMap<>();
    private final Map<String, cn.nukkit.level.Location> backs = new HashMap<>();

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        homes = new Config(new File(getDataFolder(), "homes.yml"), Config.YAML);
        mutes = new Config(new File(getDataFolder(), "mutes.yml"), Config.YAML);

        getServer().getPluginManager().registerEvents(new CoreListener(this), this);
        getLogger().info("FrankensteinCore успешно запущен!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может использовать только игрок.");
            return true;
        }

        Player p = (Player) sender;
        String name = p.getName().toLowerCase();

        switch (cmd) {
            case "sethome":
                homes.set(name, p.getLocation().getX() + "," +
                    p.getLocation().getY() + "," +
                    p.getLocation().getZ() + "," +
                    p.getLevel().getName());
                homes.save();
                p.sendMessage("§aДом сохранён!");
                return true;

            case "home":
                String home = homes.getString(name, "");
                if (home.isEmpty()) {
                    p.sendMessage("§cУ тебя нет дома.");
                    return true;
                }
                String[] h = home.split(",");
                backs.put(name, p.getLocation());
                p.teleport(new cn.nukkit.level.Location(
                    Double.parseDouble(h[0]),
                    Double.parseDouble(h[1]),
                    Double.parseDouble(h[2]),
                    p.getLevel(),
                    0, 0
                ));
                p.sendMessage("§aТелепортация домой!");
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
                requests.put(target.getName().toLowerCase(), name);
                target.sendMessage("§eИгрок " + p.getName() + " просит телепортацию. /tpaccept или /tpdeny");
                p.sendMessage("§aЗапрос отправлен.");
                return true;

            case "tpaccept":
                String requester = requests.remove(name);
                if (requester == null) {
                    p.sendMessage("§cНет запросов.");
                    return true;
                }
                Player from = getServer().getPlayer(requester);
                if (from != null) {
                    backs.put(requester, from.getLocation());
                    from.teleport(p);
                    from.sendMessage("§aЗапрос принят.");
                }
                return true;

            case "tpdeny":
                requests.remove(name);
                p.sendMessage("§eЗапрос отклонён.");
                return true;

            case "back":
                cn.nukkit.level.Location back = backs.get(name);
                if (back == null) {
                    p.sendMessage("§cНет предыдущего места.");
                    return true;
                }
                p.teleport(back);
                return true;

            case "fly":
                if (!p.isOp()) {
                    p.sendMessage("§cНет прав.");
                    return true;
                }
                boolean fly = !flying.getOrDefault(name, false);
                flying.put(name, fly);
                p.setAllowFlight(fly);
                p.sendMessage(fly ? "§aПолёт включён." : "§cПолёт выключен.");
                return true;

            case "v":
                if (!p.isOp()) {
                    p.sendMessage("§cНет прав.");
                    return true;
                }
                boolean hide = !vanished.getOrDefault(name, false);
                vanished.put(name, hide);
                for (Player online : getServer().getOnlinePlayers().values()) {
                    if (hide) online.hidePlayer(p);
                    else online.showPlayer(p);
                }
                p.sendMessage(hide ? "§aНевидимость включена." : "§cНевидимость выключена.");
                return true;

            case "mute":
                if (!p.isOp() || args.length < 1) {
                    p.sendMessage("§cИспользуй: /mute <игрок>");
                    return true;
                }
                mutes.set(args[0].toLowerCase(), true);
                mutes.save();
                p.sendMessage("§aИгроку выдан мут.");
                return true;

            case "unmute":
                if (!p.isOp() || args.length < 1) {
                    p.sendMessage("§cИспользуй: /unmute <игрок>");
                    return true;
                }
                mutes.remove(args[0].toLowerCase());
                mutes.save();
                p.sendMessage("§aМут снят.");
                return true;
        }
        return false;
    }

    public boolean isMuted(String name) {
        return mutes.getBoolean(name.toLowerCase(), false);
    }
}
