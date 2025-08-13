package cjs.DF_Plugin.upgrade.specialability;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

public interface ISpecialAbility {
    String getInternalName();
    String getDisplayName();
    String getDescription();
    double getCooldown();

    default void onPlayerInteract(PlayerInteractEvent event, Player player, ItemStack item) {}
    default void onDamageByEntity(EntityDamageByEntityEvent event, Player player, ItemStack item) {}
    default void onToggleFlight(PlayerToggleFlightEvent event, Player player, ItemStack item) {}
    default void onPlayerMove(PlayerMoveEvent event, Player player, ItemStack item) {}
    default void onEntityDamage(EntityDamageEvent event, Player player, ItemStack item) {}
    default void onEntityShootBow(EntityShootBowEvent event, Player player, ItemStack item) {}
    default void onProjectileLaunch(ProjectileLaunchEvent event, Player player, ItemStack item) {}
    default void onPlayerRiptide(PlayerRiptideEvent event, Player player, ItemStack item) {}
    default void onPlayerFish(org.bukkit.event.player.PlayerFishEvent event, Player player, ItemStack item) {}
    default void onPlayerToggleSneak(PlayerToggleSneakEvent event, Player player, ItemStack item) {}

    default void onCleanup(Player player) {}
}