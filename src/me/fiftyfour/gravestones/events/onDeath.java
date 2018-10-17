package me.fiftyfour.gravestones.events;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import me.fiftyfour.gravestones.Gravestone;
import me.fiftyfour.gravestones.Main;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

public class onDeath implements Listener {

    private Plugin plugin = Main.getPlugin(Main.class);
    private WorldGuardPlugin getWorldGuard() {
        Plugin wgplugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (!(wgplugin instanceof WorldGuardPlugin)) {
            return null;
        }
        return (WorldGuardPlugin) wgplugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event){
        Location Ploc;
        Player p = event.getEntity();
        if (event.getKeepInventory())return;
        if(!(p.getGameMode().equals(GameMode.SURVIVAL)))return;
        if (isIventoryEmpty(p)) return;
        if (getWorldGuard() != null){
            if (!getWorldGuard().canBuild(p, p.getEyeLocation())) return;
        }
        if (Main.disabled) {
            event.setKeepInventory(true);
            return;
        }
        if (p.getKiller() == null || p.getKiller().equals(p)){
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            Ploc = p.getEyeLocation().getBlock().getLocation();
            if (p.getLastDamageCause().toString() != null) {
                String lastDamage = p.getLastDamageCause().getCause().toString();
                if (Ploc.getBlockY() >= 256) {
                    Ploc.setY(255);
                }
                if (lastDamage.equals("VOID")) {
                    Ploc.setY(1);
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "You seemed to have died in the void, Your grave was spawned just above the void.");
                } else if (lastDamage.equals("LAVA")) {
                    Ploc = getDeathLocation(p.getEyeLocation().getBlock().getLocation().clone());
                }
            }
            int graveNumber;
            if(Main.graves.get(p.getUniqueId()) != null) {
                graveNumber = Main.graves.get(p.getUniqueId()).toArray().length;
            }else{
                graveNumber = 0;
            }
            Gravestone grave = new Gravestone();
            final ArrayList<ItemStack> armorCont = new ArrayList<>(Arrays.asList(p.getInventory().getArmorContents()));
            final ArrayList<ItemStack> invCont = new ArrayList<>();
            for(ItemStack stack : p.getInventory().getStorageContents()){
                if(stack != null  && !stack.getType().equals(Material.AIR)) invCont.add(stack);
            }
            if (p.getInventory().getItemInOffHand() != null){
                grave.setOffHand(p.getInventory().getItemInOffHand());
            }
            grave.setArmor(armorCont);
            grave.setItems(invCont);
            grave.setEXPLevel(p.getTotalExperience());
            Ploc = Gravestone.checkNear(Ploc);
            grave.setLocation(Ploc);
            grave.setOwner(p.getUniqueId());
            grave.setNumber(graveNumber + 1);
            grave.setTimeTillExpire(10);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
            LocalDateTime now = LocalDateTime.now();
            Location holoLoc = Ploc.clone();
            holoLoc.add(0.5, 3, 0.5);
            Hologram hologram = HologramsAPI.createHologram(plugin, holoLoc);
            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta sm = (SkullMeta) skull.getItemMeta();
            sm.setOwningPlayer(p);
            skull.setItemMeta(sm);
            hologram.appendItemLine(skull);
            hologram.appendTextLine(ChatColor.GOLD + "" + ChatColor.BOLD + p.getName() + "'s Grave");
            hologram.appendTextLine(ChatColor.GOLD + "Time of Death: " + dtf.format(now));
            hologram.appendTextLine(ChatColor.GRAY + "" + ChatColor.ITALIC + "They" + event.getDeathMessage().replace(p.getName(), "").replace("was", "were"));
            grave.setHologram(hologram);
            Gravestone.createGrave(grave);
            p.getInventory().setArmorContents(null);
            p.getInventory().clear();
            event.setKeepInventory(false);
            event.setKeepLevel(false);
            p.setExp(0);
        }
    }
    private boolean isIventoryEmpty(Player player){
        for(ItemStack item : player.getInventory().getStorageContents()){
            if(item != null)
                return false;
        }
        if (player.getInventory().getItemInOffHand() != null) return false;
        for(ItemStack item : player.getInventory().getArmorContents()){
            if(item != null)
                return false;
        }
        return true;
    }
    private Location getDeathLocation(Location loc){
        if(loc.getY() >= 256){
            loc.setY(255);
        }
        while(loc.getBlock().isLiquid()){
            if(loc.getY() < 255){
                loc.add(0, 1, 0);
            }else{
                loc.add(1, 0, 0);
            }
        }
        return loc;
    }

}
