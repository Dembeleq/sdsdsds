package tr.chicagorp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import java.util.*;

/** Modsuz arac sistemi: gorunmez eyerli at (surus) + ItemDisplay 3D model. Kilit, yolcu izni, atma ayarlari. */
public class Vehicles implements Listener {
    public record Car(String id, String name, double price, double speed, Material mat, String ia, String model, double scale) {}

    static class Active {
        Horse horse; ItemDisplay body, cabin; UUID owner; boolean modelUsed;
        boolean locked = true, anyone = false;
        Set<UUID> allowed = new HashSet<>();
    }

    private final ChicagoRP p;
    private final Map<UUID, Active> active = new HashMap<>();   // sahip -> arac
    private final Map<UUID, UUID> horseOwner = new HashMap<>(); // at -> sahip

    public Vehicles(ChicagoRP p) {
        this.p = p;
        Bukkit.getScheduler().runTaskTimer(p, this::tick, 2L, 1L);
    }

    public Map<String, Car> cars() {
        Map<String, Car> m = new LinkedHashMap<>();
        var s = p.getConfig().getConfigurationSection("vehicles");
        if (s == null) return m;
        for (String id : s.getKeys(false)) {
            var c = s.getConfigurationSection(id);
            Material mat = Material.matchMaterial(c.getString("material", "GRAY_CONCRETE"));
            m.put(id, new Car(id, c.getString("name", id), c.getDouble("price"), c.getDouble("speed", 0.2),
                    mat == null ? Material.GRAY_CONCRETE : mat, c.getString("ia", ""), c.getString("model", "car_" + id), c.getDouble("scale", 1.5)));
        }
        return m;
    }

    public List<String> owned(UUID u) {
        List<String> l = new ArrayList<>();
        Object o = p.data.get(u, "cars", null);
        if (o instanceof List<?> li) for (Object x : li) l.add(String.valueOf(x));
        return l;
    }

    public void buy(Player pl, String id) {
        Car c = cars().get(id);
        UUID u = pl.getUniqueId();
        if (c == null) return;
        List<String> l = owned(u);
        if (l.contains(id)) { U.msg(pl, "Bu araca zaten sahipsin."); return; }
        if (!p.data.take(u, c.price())) { U.msg(pl, "Yetersiz bakiye: " + (long) c.price() + " TL"); return; }
        l.add(id); p.data.set(u, "cars", l);
        U.msg(pl, "&a" + c.name() + " satın alındı! Telefon > Araçlar'dan çıkarabilirsin.");
    }

    public void spawn(Player pl, String id, boolean free) {
        Car c = cars().get(id);
        UUID u = pl.getUniqueId();
        if (c == null) return;
        if (!(boolean) p.data.get(u, "ehliyet", false)) { U.msg(pl, "Ehliyetin yok. /ehliyet"); return; }
        if (!free && !owned(u).contains(id)) { U.msg(pl, "Bu araç senin değil."); return; }
        remove(u);
        Location l = pl.getLocation().add(pl.getLocation().getDirection().setY(0).normalize().multiply(2));
        World w = pl.getWorld();
        Active a = new Active();
        a.owner = u;
        a.locked = (boolean) p.data.get(u, "car_locked", true);
        a.anyone = (boolean) p.data.get(u, "car_anyone", false);
        Object al = p.data.get(u, "car_allowed", null);
        if (al instanceof List<?> li) for (Object x : li) { try { a.allowed.add(UUID.fromString(String.valueOf(x))); } catch (Exception ignored) { } }
        a.horse = w.spawn(l, Horse.class, h -> {
            h.setAdult(); h.setTamed(true);
            h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
            h.setInvisible(true); h.setSilent(true); h.setInvulnerable(true);
            h.setRemoveWhenFarAway(false); h.setPersistent(false);
            h.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(c.speed());
        });
        try { Bukkit.getMobGoals().removeAllGoals(a.horse); } catch (Throwable ignored) { }
        ItemStack ia = U.custom(c.ia(), null);
        boolean models = p.getConfig().getBoolean("models.enabled", true);
        if (ia != null) { a.body = display(w, l, ia, 1f, 1f, 1f, 0f, 0f); }
        else if (models) {
            float s = (float) c.scale();
            a.body = display(w, l, U.withModel(new ItemStack(Material.PAPER), c.model()), s, s, s, 0f, 0f);
            a.modelUsed = true;
        } else {
            a.body = display(w, l, new ItemStack(c.mat()), 1.7f, 0.6f, 3.4f, 0.5f, 0f);
            a.cabin = display(w, l, new ItemStack(Material.BLACK_STAINED_GLASS), 1.5f, 0.55f, 1.7f, 1.08f, -0.2f);
        }
        active.put(u, a);
        horseOwner.put(a.horse.getUniqueId(), u);
        a.horse.addPassenger(pl);
        U.msg(pl, "&e" + c.name() + " hazır. &7Araç ayarları: /arac ayar  •  İnmek için shift.");
    }

    private ItemDisplay display(World w, Location l, ItemStack it, float sx, float sy, float sz, float ty, float tz) {
        return w.spawn(l, ItemDisplay.class, d -> {
            d.setItemStack(it);
            d.setTransformation(new Transformation(new Vector3f(0f, ty, tz), new AxisAngle4f(0f, 0f, 0f, 1f), new Vector3f(sx, sy, sz), new AxisAngle4f(0f, 0f, 0f, 1f)));
            d.setPersistent(false);
            d.setTeleportDuration(1);
            d.setViewRange(2f);
        });
    }

    public void remove(UUID u) {
        Active a = active.remove(u);
        if (a == null) return;
        horseOwner.remove(a.horse.getUniqueId());
        a.horse.remove(); a.body.remove();
        if (a.cabin != null) a.cabin.remove();
    }

    public void removeAll() { for (UUID u : new ArrayList<>(active.keySet())) remove(u); }

    public boolean hasCar(UUID u) { return active.containsKey(u); }

    private boolean canRide(Player pl, Active a, boolean adding) {
        if (pl.getUniqueId().equals(a.owner)) return true;
        if (a.locked) return false;
        if (!(a.anyone || a.allowed.contains(pl.getUniqueId()))) return false;
        return !adding || a.horse.getPassengers().size() < p.getConfig().getInt("vehicle.max-passengers", 4);
    }

    private void tick() {
        float off = (float) p.getConfig().getDouble("models.vehicle-yaw-offset", 0);
        for (UUID u : new ArrayList<>(active.keySet())) {
            Active a = active.get(u);
            if (!a.horse.isValid()) { remove(u); continue; }
            Location l = a.horse.getLocation();
            if (a.modelUsed) l.setYaw(l.getYaw() + off);
            a.body.teleport(l);
            if (a.cabin != null) a.cabin.teleport(l);
            for (Entity e : new ArrayList<>(a.horse.getPassengers()))
                if (e instanceof Player pl && !canRide(pl, a, false)) { a.horse.removePassenger(pl); U.msg(pl, "&cAraçtan atıldın."); }
        }
    }

    // --------------------------------------------------------------- sahip ayarlari
    public void toggleLock(Player pl) {
        Active a = active.get(pl.getUniqueId());
        boolean now = a == null ? !(boolean) p.data.get(pl.getUniqueId(), "car_locked", true) : !a.locked;
        if (a != null) a.locked = now;
        p.data.set(pl.getUniqueId(), "car_locked", now);
        U.msg(pl, now ? "&cAraç kilitli: sadece sen binebilirsin." : "&aAraç kilidi açıldı.");
    }

    public void toggleAnyone(Player pl) {
        Active a = active.get(pl.getUniqueId());
        boolean now = a == null ? !(boolean) p.data.get(pl.getUniqueId(), "car_anyone", false) : !a.anyone;
        if (a != null) a.anyone = now;
        p.data.set(pl.getUniqueId(), "car_anyone", now);
        U.msg(pl, now ? "&aKilit açıkken herkes yolcu olabilir." : "&eSadece izin verdiklerin yolcu olabilir.");
    }

    public void toggleAllow(Player pl, String name) {
        Player t = Bukkit.getPlayer(name);
        if (t == null) { U.msg(pl, "Oyuncu bulunamadı."); return; }
        Active a = active.get(pl.getUniqueId());
        Set<UUID> set = a != null ? a.allowed : new HashSet<>();
        if (a == null) { Object al = p.data.get(pl.getUniqueId(), "car_allowed", null); if (al instanceof List<?> li) for (Object x : li) set.add(UUID.fromString(String.valueOf(x))); }
        boolean added = set.add(t.getUniqueId());
        if (!added) set.remove(t.getUniqueId());
        List<String> out = new ArrayList<>(); for (UUID x : set) out.add(x.toString());
        p.data.set(pl.getUniqueId(), "car_allowed", out);
        U.msg(pl, added ? "&a" + t.getName() + " yolcu olabilir (araç kilitli değilse)." : "&e" + t.getName() + " izni kaldırıldı.");
    }

    public void kick(Player pl, Player target) {
        Active a = active.get(pl.getUniqueId());
        if (a == null || target == null || target.equals(pl)) return;
        if (a.horse.getPassengers().contains(target)) { a.horse.removePassenger(target); U.msg(target, "&cAraçtan atıldın."); U.msg(pl, target.getName() + " araçtan atıldı."); }
    }

    public void kickAll(Player pl) {
        Active a = active.get(pl.getUniqueId());
        if (a == null) return;
        for (Entity e : new ArrayList<>(a.horse.getPassengers())) if (e instanceof Player t && !t.equals(pl)) kick(pl, t);
    }

    public void openSettings(Player pl) {
        Active a = active.get(pl.getUniqueId());
        if (a == null) { U.msg(pl, "Önce bir araç çıkar: /telefon > Araçlar"); return; }
        Gui g = new Gui("aracayar", 36, "&8Araç Ayarları");
        for (int i = 0; i < 36; i++) g.put(i, U.icon(i < 9 ? Material.MAGENTA_STAINED_GLASS_PANE : i < 27 ? Material.PURPLE_STAINED_GLASS_PANE : Material.BLUE_STAINED_GLASS_PANE, " "), null);
        g.put(10, U.app("app_kilit", Material.IRON_DOOR, a.locked ? "&cKilit: KİLİTLİ" : "&aKilit: AÇIK", "&7Kilitliyken sadece sen binebilirsin", "&eTıkla: değiştir"), "lock");
        g.put(11, U.app("app_sosyal", Material.PLAYER_HEAD, a.anyone ? "&aHerkes yolcu olabilir" : "&eSadece izinliler yolcu olur", "&7Kilit açıkken geçerli", "&eTıkla: değiştir"), "anyone");
        g.put(13, U.app("app_cete", Material.BARRIER, "&cTüm yolcuları at"), "kickall");
        List<String> names = new ArrayList<>();
        for (UUID x : a.allowed) { var op = Bukkit.getOfflinePlayer(x); names.add("&7- " + (op.getName() == null ? x.toString().substring(0, 6) : op.getName())); }
        g.put(15, U.icon(Material.BOOK, "&eİzinli yolcular", names.isEmpty() ? new String[]{"&7(boş) /arac izin <oyuncu>"} : names.toArray(new String[0])), null);
        int s = 19;
        for (Entity e : a.horse.getPassengers()) {
            if (!(e instanceof Player t) || t.getUniqueId().equals(pl.getUniqueId()) || s > 26) continue;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta m = (SkullMeta) head.getItemMeta(); m.setOwningPlayer(t); m.displayName(U.L.deserialize("&e" + t.getName() + " &7(at)")); head.setItemMeta(m);
            g.put(s++, head, "kick:" + t.getName());
        }
        g.put(35, U.icon(Material.ARROW, "&7Geri"), "back");
        pl.openInventory(g.inv);
    }

    // --------------------------------------------------------------- GUI
    public void openGarage(Player pl) {
        Gui g = new Gui("garaj", 36, "&8Garaj");
        for (int i = 0; i < 36; i++) g.put(i, U.icon(i < 18 ? Material.ORANGE_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE, " "), null);
        Map<String, Car> all = cars();
        int s = 0;
        for (String id : owned(pl.getUniqueId())) {
            Car c = all.get(id);
            if (c == null || s >= 18) continue;
            g.put(s++, U.withModel(U.icon(Material.PAPER, "&e" + c.name(), "&7Hız: &f" + (int) (c.speed() * 43) + " m/s", "&aTıkla: çıkar"), c.model()), "car:" + id);
        }
        g.put(30, U.app("app_arac", Material.BARRIER, "&cAracı park et"), "carpark");
        g.put(31, U.app("app_kilit", Material.IRON_DOOR, "&eAraç ayarları", "&7Kilit, yolcu izni, atma"), "settings");
        g.put(35, U.icon(Material.ARROW, "&7Geri"), "back");
        pl.openInventory(g.inv);
    }

    public void openGallery(Player pl) {
        Gui g = new Gui("galeri", 36, "&8Araç Galerisi");
        int s = 0;
        for (Car c : cars().values()) {
            if (s >= 27) break;
            boolean has = owned(pl.getUniqueId()).contains(c.id());
            g.put(s++, U.withModel(U.icon(Material.PAPER, (has ? "&a" : "&e") + c.name(), "&7Fiyat: &f" + (long) c.price() + " TL", "&7Hız: &f" + (int) (c.speed() * 43) + " m/s", has ? "&aSende var" : "&eTıkla: satın al"), c.model()), has ? null : "buy:" + c.id());
        }
        pl.openInventory(g.inv);
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Gui g)) return;
        if (!g.page.equals("garaj") && !g.page.equals("galeri") && !g.page.equals("aracayar")) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player pl)) return;
        String a = g.act.get(e.getRawSlot());
        if (a == null) return;
        if (a.startsWith("car:")) { pl.closeInventory(); spawn(pl, a.substring(4), false); }
        else if (a.equals("carpark")) { remove(pl.getUniqueId()); pl.closeInventory(); }
        else if (a.equals("settings")) openSettings(pl);
        else if (a.equals("lock")) { toggleLock(pl); openSettings(pl); }
        else if (a.equals("anyone")) { toggleAnyone(pl); openSettings(pl); }
        else if (a.equals("kickall")) { kickAll(pl); openSettings(pl); }
        else if (a.startsWith("kick:")) { kick(pl, Bukkit.getPlayer(a.substring(5))); openSettings(pl); }
        else if (a.equals("back")) p.phone.open(pl);
        else if (a.startsWith("buy:")) { buy(pl, a.substring(4)); openGallery(pl); }
    }

    @EventHandler public void onMount(EntityMountEvent e) {
        UUID owner = horseOwner.get(e.getMount().getUniqueId());
        if (owner == null || !(e.getEntity() instanceof Player pl)) return;
        Active a = active.get(owner);
        if (a != null && !canRide(pl, a, true)) { e.setCancelled(true); U.msg(pl, "&cBu araca binemezsin (kilitli ya da izin yok)."); }
    }

    @EventHandler public void onInteractCar(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !(e.getRightClicked() instanceof Horse h)) return;
        UUID owner = horseOwner.get(h.getUniqueId());
        if (owner == null) return;
        e.setCancelled(true);
        Player pl = e.getPlayer();
        Active a = active.get(owner);
        if (a == null || h.getPassengers().contains(pl)) return;
        if (pl.getUniqueId().equals(owner)) {
            List<Entity> others = new ArrayList<>(h.getPassengers());
            for (Entity o : others) h.removePassenger(o);
            h.addPassenger(pl);
            for (Entity o : others) h.addPassenger(o);
        } else if (canRide(pl, a, true)) h.addPassenger(pl);
        else U.msg(pl, "&cBu araç kilitli ya da izin verilmemiş.");
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { remove(e.getPlayer().getUniqueId()); }

    @EventHandler public void onShopBlock(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        if (!(e.getClickedBlock().getState() instanceof TileState ts)) return;
        String t = ts.getPersistentDataContainer().get(U.SHOP, PersistentDataType.STRING);
        if (t == null) return;
        e.setCancelled(true);
        if (t.equals("galeri")) openGallery(e.getPlayer());
        else if (t.equals("jenerator")) U.msg(e.getPlayer(), "Jeneratör almak için kendi arsanda &e/jenerator al &fyaz.");
    }
}
