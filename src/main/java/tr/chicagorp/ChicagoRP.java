package tr.chicagorp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Random;

public class ChicagoRP extends JavaPlugin implements Listener {
    public Data data;
    public PvpManager pvp;
    public PlotManager plots;
    public JobManager jobs;
    public Police police;
    public OrderSystem orders;
    public SupplyChain chain;
    public CityBuilder city;
    public Vehicles vehicles;
    public Phone phone;
    public Gang gang;
    public Minimap minimap;

    @Override public void onEnable() {
        saveDefaultConfig();
        data = new Data(this);
        plots = new PlotManager(this);
        pvp = new PvpManager(this);
        jobs = new JobManager(this);
        police = new Police(this);
        orders = new OrderSystem(this);
        chain = new SupplyChain(this);
        city = new CityBuilder(this);
        vehicles = new Vehicles(this);
        phone = new Phone(this);
        gang = new Gang(this);
        minimap = new Minimap(this);
        Bukkit.getScheduler().runTask(this, minimap::init);
        for (Object l : new Object[]{this, plots, pvp, jobs, police, vehicles, phone})
            Bukkit.getPluginManager().registerEvents((Listener) l, this);
        Commands cmds = new Commands(this);
        for (String c : getDescription().getCommands().keySet()) getCommand(c).setExecutor(cmds);
        Bukkit.getScheduler().runTaskTimer(this, () -> { data.save(); plots.save(); }, 6000L, 6000L);
        Bukkit.getScheduler().runTaskTimer(this, plots::tickGenerators, 1200L, 1200L);
        Bukkit.getScheduler().runTaskTimer(this, orders::tick, 200L, 200L);
    }

    @Override public void onDisable() { vehicles.removeAll(); data.save(); plots.save(); gang.save(); }

    @EventHandler public void onJoin(PlayerJoinEvent e) {
        Player pl = e.getPlayer();
        e.joinMessage(U.L.deserialize(U.fmt(getConfig().getString("join-message", ""), pl)));
        pl.showTitle(Title.title(U.L.deserialize(U.fmt(getConfig().getString("welcome-title", "&6&l{server}"), pl)),
                U.L.deserialize(U.fmt(getConfig().getString("welcome-subtitle", "&7Hoş geldin &f{player}"), pl))));
        String url = getConfig().getString("resource-pack.url", "");
        if (!url.isEmpty()) pl.setResourcePack(url, getConfig().getString("resource-pack.sha1", ""), getConfig().getBoolean("resource-pack.required", true));
        if (!data.has(pl.getUniqueId())) {
            var u = pl.getUniqueId();
            Random r = new Random();
            data.set(u, "money", getConfig().getDouble("start-money"));
            StringBuilder tc = new StringBuilder("" + (1 + r.nextInt(9)));
            for (int i = 0; i < 10; i++) tc.append(r.nextInt(10));
            data.set(u, "tc", tc.toString());
            data.set(u, "dogum", (1 + r.nextInt(28)) + "." + (1 + r.nextInt(12)) + "." + (1985 + r.nextInt(20)));
            data.set(u, "kan", new String[]{"A+", "A-", "B+", "0+", "0-", "AB+"}[r.nextInt(6)]);
        }
        phone.give(pl);
        var s = getConfig().getConfigurationSection("spawn");
        var w = Bukkit.getWorld(s.getString("world"));
        if (w != null && !pl.hasPlayedBefore())
            pl.teleport(new org.bukkit.Location(w, s.getDouble("x") + .5, s.getDouble("y"), s.getDouble("z") + .5));
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) {
        e.quitMessage(U.L.deserialize(U.fmt(getConfig().getString("quit-message", ""), e.getPlayer())));
    }
}
