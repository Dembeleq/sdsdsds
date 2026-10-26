package tr.chicagorp;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.UUID;

public class Commands implements CommandExecutor {
    private final ChicagoRP p;
    public Commands(ChicagoRP p) { this.p = p; }

    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player pl)) { U.msg(s, "Bu komut oyuncular icin."); return true; }
        switch (c.getName().toLowerCase()) {
            case "meslek" -> p.jobs.command(pl, a);
            case "pvp" -> p.pvp.toggle(pl, a);
            case "profil" -> profil(pl, a);
            case "kimlik" -> kimlik(pl, a);
            case "ehliyet" -> ehliyet(pl);
            case "arsa" -> p.plots.command(pl, a);
            case "jenerator" -> p.plots.generator(pl, a);
            case "kilit" -> p.plots.lock(pl);
            case "tutukla" -> p.police.arrest(pl, a);
            case "polis" -> p.police.kit(pl);
            case "siparis" -> p.orders.command(pl, a);
            case "tarla", "kimya", "elalti" -> p.chain.command(c.getName().toLowerCase(), pl, a);
            case "sehir" -> p.city.command(pl, a);
            case "telefon" -> p.phone.open(pl);
            case "galeri" -> p.vehicles.openGallery(pl);
            case "arac" -> arac(pl, a);
            case "cete" -> p.gang.command(pl, a);
            case "ode" -> ode(pl, a);
            case "paraver" -> paraver(pl, a, false);
            case "paraayarla" -> paraver(pl, a, true);
            case "harita" -> { var m = p.minimap.item(); if (m == null) U.msg(pl, "Şehir henüz kurulmamış."); else pl.getInventory().addItem(m); }
            case "rpreload" -> rpreload(pl);
            case "para" -> U.msg(pl, "Bakiye: &a" + (long) p.data.money(pl.getUniqueId()) + " TL");
        }
        return true;
    }

    private void paraver(Player pl, String[] a, boolean set) {
        if (!pl.hasPermission("rp.admin")) { U.msg(pl, "Yetkin yok."); return; }
        if (a.length < 2) { U.msg(pl, set ? "/paraayarla <oyuncu> <miktar>" : "/paraver <oyuncu> <miktar>  (eksi sayı para siler)"); return; }
        Player t = Bukkit.getPlayer(a[0]);
        double m;
        try { m = Double.parseDouble(a[1]); } catch (NumberFormatException e) { U.msg(pl, "Geçersiz miktar."); return; }
        if (t == null) { U.msg(pl, "Oyuncu bulunamadı."); return; }
        if (set) p.data.set(t.getUniqueId(), "money", m); else p.data.add(t.getUniqueId(), m);
        U.msg(pl, "&a" + t.getName() + " bakiyesi: " + (long) p.data.money(t.getUniqueId()) + " TL");
        if (t != pl) U.msg(t, "&6Bakiyen güncellendi: &f" + (long) p.data.money(t.getUniqueId()) + " TL");
    }

    private void rpreload(Player pl) {
        if (!pl.hasPermission("rp.admin")) return;
        p.reloadConfig();
        U.msg(pl, "Config yeniden yüklendi.");
    }

    private void arac(Player pl, String[] a) {
        if (a.length > 0) switch (a[0]) {
            case "kilit" -> { p.vehicles.toggleLock(pl); return; }
            case "yolcu" -> { p.vehicles.toggleAnyone(pl); return; }
            case "izin" -> { if (a.length > 1) p.vehicles.toggleAllow(pl, a[1]); else U.msg(pl, "/arac izin <oyuncu>"); return; }
            case "at" -> { if (a.length > 1) p.vehicles.kick(pl, Bukkit.getPlayer(a[1])); else p.vehicles.kickAll(pl); return; }
            case "ayar" -> { p.vehicles.openSettings(pl); return; }
            default -> { }
        }
        if (a.length > 0 && a[0].equals("park")) { p.vehicles.remove(pl.getUniqueId()); return; }
        if (a.length > 0 && a[0].equals("is")) {
            String j = p.data.job(pl.getUniqueId());
            if (j.equals("taksici")) p.vehicles.spawn(pl, "taksi", true);
            else if (j.equals("mekanik")) p.vehicles.spawn(pl, "hatchback", true);
            else U.msg(pl, "Bedava iş aracı sadece taksici ve mekanik için.");
            return;
        }
        p.vehicles.openGarage(pl);
    }

    private void ode(Player pl, String[] a) {
        if (a.length < 2) { U.msg(pl, "/ode <oyuncu> <miktar>"); return; }
        Player t = Bukkit.getPlayer(a[0]);
        double m;
        try { m = Double.parseDouble(a[1]); } catch (NumberFormatException e) { U.msg(pl, "Geçersiz miktar."); return; }
        if (t == null || t == pl || m <= 0) { U.msg(pl, "Geçersiz oyuncu veya miktar."); return; }
        if (!p.data.take(pl.getUniqueId(), m)) { U.msg(pl, "Yetersiz bakiye."); return; }
        p.data.add(t.getUniqueId(), m);
        U.msg(pl, "&a" + (long) m + " TL gönderildi: " + t.getName());
        U.msg(t, "&a" + pl.getName() + " sana " + (long) m + " TL gönderdi.");
    }

    private void profil(Player pl, String[] a) {
        Player t = a.length > 0 ? Bukkit.getPlayer(a[0]) : pl;
        if (t == null) { U.msg(pl, "Oyuncu bulunamadi."); return; }
        UUID u = t.getUniqueId();
        U.msg(pl, "&e" + t.getName() + " &7| Bakiye: &a" + (long) p.data.money(u) + " &7| Seviye: &b" + p.data.level(u)
                + " &7| Meslek: &f" + p.data.job(u) + " &7| Çete: &f" + p.gang.info(u) + " &7| Arsa: &f" + p.plots.owned(u) + " &7| Jenerator: &f" + p.data.gens(u));
    }

    private void kimlik(Player pl, String[] a) {
        Player t = pl;
        if (a.length > 0) {
            if (!p.police.isPolice(pl)) { U.msg(pl, "Baskasinin kimligini sadece polis gorebilir."); return; }
            t = Bukkit.getPlayer(a[0]);
            if (t == null) { U.msg(pl, "Oyuncu bulunamadi."); return; }
        }
        UUID u = t.getUniqueId();
        U.msg(pl, "&eKIMLIK &7TC: &f" + p.data.get(u, "tc", "?") + " &7Dogum: &f" + p.data.get(u, "dogum", "?")
                + " &7Kan: &f" + p.data.get(u, "kan", "?") + " &7Sabika: &f" + p.data.get(u, "sabika", 0));
    }

    private void ehliyet(Player pl) {
        UUID u = pl.getUniqueId();
        if ((boolean) p.data.get(u, "ehliyet", false)) { U.msg(pl, "Zaten ehliyetin var."); return; }
        double price = p.getConfig().getDouble("ehliyet-price");
        if (!p.data.take(u, price)) { U.msg(pl, "Yetersiz bakiye (" + (long) price + " TL)."); return; }
        p.data.set(u, "ehliyet", true);
        U.msg(pl, "Ehliyet alindi. Arac eklentin bu degeri kontrol etmeli: players.yml -> ehliyet");
    }
}
