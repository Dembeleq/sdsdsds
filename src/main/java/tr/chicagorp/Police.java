package tr.chicagorp;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import java.util.UUID;

public class Police implements Listener {
    private final ChicagoRP p;
    public Police(ChicagoRP p) { this.p = p; }

    public boolean isPolice(Player pl) { return p.data.job(pl.getUniqueId()).equals("polis"); }

    public void kit(Player pl) {
        if (!isPolice(pl)) { U.msg(pl, "Polis degilsin."); return; }
        Material[] m = {Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS};
        String[] n = {"&9POLIS Sapka", "&9POLIS Gomlek (arkada POLIS)", "&9POLIS Pantolon", "&9POLIS Bot"};
        for (int i = 0; i < 4; i++) {
            ItemStack it = U.custom(p.getConfig().getString("police-ia." + i, ""), U.item(m[i], n[i], "polis_unif"));
            if (it.getItemMeta() instanceof LeatherArmorMeta lm) { lm.setColor(Color.NAVY); it.setItemMeta(lm); }
            pl.getInventory().addItem(it);
        }
        pl.getInventory().addItem(U.item(Material.STICK, "&9Cop", "cop"));
    }

    public void arrest(Player pl, String[] a) {
        if (!isPolice(pl) || a.length == 0) { U.msg(pl, "/tutukla <oyuncu> (sadece polis)"); return; }
        Player t = Bukkit.getPlayer(a[0]);
        if (t == null || t.getLocation().distance(pl.getLocation()) > 4) { U.msg(pl, "Hedef yakinda degil."); return; }
        var j = p.getConfig().getConfigurationSection("jail");
        var w = Bukkit.getWorld(j.getString("world"));
        if (w == null) return;
        t.teleport(new Location(w, j.getDouble("x"), j.getDouble("y"), j.getDouble("z")));
        UUID tu = t.getUniqueId();
        p.data.set(tu, "sabika", (int) p.data.get(tu, "sabika", 0) + 1);
        p.data.add(pl.getUniqueId(), p.getConfig().getDouble("arrest-reward")); // para sadece tutuklamadan
        p.data.addXp(pl.getUniqueId(), 100);
        U.msg(pl, "Tutukladin: &a+" + p.getConfig().getInt("arrest-reward") + " TL");
        U.msg(t, "Tutuklandin.");
        long sec = p.getConfig().getLong("arrest-seconds");
        Bukkit.getScheduler().runTaskLater(p, () -> {
            if (!t.isOnline()) return;
            var s = p.getConfig().getConfigurationSection("spawn");
            t.teleport(new Location(Bukkit.getWorld(s.getString("world")), s.getDouble("x"), s.getDouble("y"), s.getDouble("z")));
            U.msg(t, "Serbest birakildin.");
        }, sec * 20L);
    }
}
