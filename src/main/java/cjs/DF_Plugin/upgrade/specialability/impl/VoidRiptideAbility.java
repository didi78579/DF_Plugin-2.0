package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.actionbar.ActionBarManager;
import cjs.DF_Plugin.settings.GameConfigManager;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.entity.AbstractArrow;

import java.util.List;
import java.util.Random;
public class VoidRiptideAbility implements ISpecialAbility {

    @Override
    public String getInternalName() { return "void_riptide"; }

    @Override
    public String getDisplayName() { return "§b허공 급류"; }

    @Override
    public String getDescription() { return "§7우클릭으로 허공에 물을 생성하고, 투척/급류 시 추가 투사체를 발사합니다."; }

    @Override
    public double getCooldown() {
        return DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.ability-cooldowns.void_riptide", 5.0);
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event, Player player, ItemStack item) {
        GameConfigManager configManager = DF_Main.getInstance().getGameConfigManager();
        int requiredLevel = configManager.getConfig().getInt("upgrade.ability-attributes.void_riptide.required-level", 10);
        int level = getLevelFromLore(item.getItemMeta().getLore());
        if (level < requiredLevel) return; // 설정된 레벨 전용 능력

        SpecialAbilityManager manager = DF_Main.getInstance().getSpecialAbilityManager();
        if (manager.isAbilityOnCooldown(player, this, item)) {
            long secondsLeft = (manager.getRemainingCooldown(player, this, item) + 999) / 1000;
            ActionBarManager.sendActionBar(player, String.format("%s §e%d초", this.getDisplayName(), secondsLeft));
            return;
        }

        createTemporaryWaterBlock(player);
        ActionBarManager.sendActionBar(player, "§b허공 급류 발동!");
        manager.setCooldown(player, this, item);
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event, Player player, ItemStack item) {
        int level = getLevelFromLore(item.getItemMeta().getLore());
        if (level > 0 && event.getEntity() instanceof Trident trident) {
            launchAdditionalTridents(player, level, trident.getLocation(), trident.getVelocity());
        }
    }

    @Override
    public void onPlayerRiptide(PlayerRiptideEvent event, Player player, ItemStack item) {
        int level = getLevelFromLore(item.getItemMeta().getLore());
        if (level > 0) {
            GameConfigManager configManager = DF_Main.getInstance().getGameConfigManager();
            double velocityMultiplier = configManager.getConfig().getDouble("upgrade.ability-attributes.void_riptide.riptide-velocity-multiplier", 2.0);
            launchAdditionalTridents(player, level, player.getLocation(), player.getLocation().getDirection().multiply(velocityMultiplier));
        }
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event, Player player, ItemStack item) {
        // 능력자 본인이 자신의 삼지창에 맞는 경우, 데미지와 효과를 모두 무효화합니다.
        // 이 로직은 레벨과 관계없이 항상 적용됩니다.
        if (event.getEntity().equals(player)) {
            event.setCancelled(true);
            return;
        }

        // 이 메서드는 Listener에 의해 Trident가 엔티티를 맞췄을 때 호출됩니다. 'player'는 투척자입니다.
        int level = getLevelFromLore(item.getItemMeta().getLore());
        GameConfigManager configManager = DF_Main.getInstance().getGameConfigManager();

        // 레벨 1 이상: 둔화 효과
        if (level >= 1 && event.getEntity() instanceof LivingEntity victim) {
            int duration = (int) (configManager.getConfig().getDouble("upgrade.ability-attributes.void_riptide.slowness-duration-seconds", 3.0) * 20);
            int amplifier = configManager.getConfig().getInt("upgrade.ability-attributes.void_riptide.slowness-level", 2) - 1;
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amplifier));
        }

        // 레벨 10: 투사체 면역 및 회수 불가
        int requiredLevel = configManager.getConfig().getInt("upgrade.ability-attributes.void_riptide.required-level", 10);
        if (level >= requiredLevel && event.getDamager() instanceof Trident trident) {
            trident.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
        }
    }

    private void launchAdditionalTridents(Player player, int level, Location origin, Vector direction) {
        World world = player.getWorld();
        Random random = new Random();

        GameConfigManager configManager = DF_Main.getInstance().getGameConfigManager();
        double spreadRadius = configManager.getConfig().getDouble("upgrade.ability-attributes.void_riptide.spread-radius", 3.0);
        double spreadVelocity = configManager.getConfig().getDouble("upgrade.ability-attributes.void_riptide.spread-velocity-multiplier", 0.1);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < level; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * spreadRadius * 2;
                    double offsetY = (random.nextDouble() - 0.5) * spreadRadius * 2;
                    double offsetZ = (random.nextDouble() - 0.5) * spreadRadius * 2;

                    Location spawnLocation = origin.clone().add(offsetX, offsetY, offsetZ);

                    Trident additionalTrident = (Trident) world.spawnEntity(spawnLocation, EntityType.TRIDENT);
                    additionalTrident.setShooter(player);
                    additionalTrident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    additionalTrident.setVelocity(direction.clone().add(new Vector(offsetX, offsetY, offsetZ).normalize().multiply(spreadVelocity)));
                }
            }
        }.runTaskLater(DF_Main.getInstance(), 1L); // 약간의 딜레이를 주어 동시 발사 문제를 회피
    }

    private int getLevelFromLore(List<String> lore) {
        if (lore == null) return 0;
        for (String line : lore) {
            if (line.startsWith(ChatColor.BLUE + "추가 투사체 수:")) {
                try {
                    return Integer.parseInt(ChatColor.stripColor(line).replaceAll("[^0-9]", ""));
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    /**
     * 플레이어의 위치에 1초간 물을 생성합니다.
     * @param player 대상 플레이어
     */
    private void createTemporaryWaterBlock(Player player) {
        Location loc = player.getLocation();
        Block block = loc.getBlock();

        if (block.getType() == Material.AIR) {
            block.setType(Material.WATER);
            new BukkitRunnable() {
                public void run() { if (block.getType() == Material.WATER) block.setType(Material.AIR); }
            }.runTaskLater(DF_Main.getInstance(), 20L); // 1초 후 물 제거
        }
    }
}