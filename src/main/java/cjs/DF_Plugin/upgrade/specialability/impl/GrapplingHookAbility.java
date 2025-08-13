// C:/Users/CJS/IdeaProjects/DF_Plugin-2.0/src/main/java/cjs/DF_Plugin/upgrade/specialability/impl/GrapplingHookAbility.java
package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GrapplingHookAbility implements ISpecialAbility {

    private static final int MAX_CHARGES = 4;
    // 플레이어별로 고정된 훅의 위치를 저장합니다.
    private static final Map<UUID, Location> latchedHooks = new ConcurrentHashMap<>();

    @Override
    public String getInternalName() {
        return "grappling_hook";
    }

    @Override
    public String getDisplayName() {
        return "§a그래플링 훅";
    }

    @Override
    public String getDescription() {
        return "§7우클릭으로 훅을 발사하고, 웅크리기를 사용해 이동합니다.";
    }

    @Override
    public double getCooldown() {
        // 이 쿨다운은 모든 충전량을 소모했을 때만 적용됩니다.
        return DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.ability-cooldowns.grappling_hook", 120.0);    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event, Player player, ItemStack item) {
        // 이미 훅이 고정된 상태라면, 다시 발사하지 않고 취소합니다.
        if (latchedHooks.containsKey(player.getUniqueId())) {
            latchedHooks.remove(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.7f, 1.2f); // 훅 회수 사운드
            return;
        }

        // 쿨다운 중이거나 충전량이 없으면 발사할 수 없습니다.
        SpecialAbilityManager manager = DF_Main.getInstance().getSpecialAbilityManager();
        if (manager.isAbilityOnCooldown(player, this, item)) {
            return; // ActionBarManager가 쿨다운을 표시합니다.
        }
        SpecialAbilityManager.ChargeInfo chargeInfo = manager.getChargeInfo(player, this);
        int currentCharges = (chargeInfo != null) ? chargeInfo.current() : MAX_CHARGES;
        if (currentCharges <= 0) {
            return;
        }

        // 그래플링 훅 발사 시도
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        double maxDistance = 30.0;

        Location targetLocation = findTargetLocation(eyeLocation, direction, maxDistance);
        if (targetLocation == null) {
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.0f);
            return;
        }

        // 훅 고정 성공
        latchedHooks.put(player.getUniqueId(), targetLocation);
        // "철컥" 하는 사운드
        player.playSound(targetLocation, Sound.BLOCK_CHAIN_HIT, 1.0f, 1.2f); // 훅이 걸린 위치에서 나는 소리
    }

    // 이 메서드는 SpecialAbilityListener의 onPlayerToggleSneak에서 호출됩니다.
    @Override
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event, Player player, ItemStack item) {
        // 웅크리기를 시작할 때만 발동하고, 훅이 고정된 상태여야 합니다.
        if (!event.isSneaking() || !latchedHooks.containsKey(player.getUniqueId())) {
            return;
        }

        Location targetLocation = latchedHooks.remove(player.getUniqueId());
        if (targetLocation == null) return;

        // 그래플링 실행
        performGrappling(player, targetLocation);

        // 충전량 및 쿨다운 처리
        SpecialAbilityManager manager = DF_Main.getInstance().getSpecialAbilityManager();
        SpecialAbilityManager.ChargeInfo chargeInfo = manager.getChargeInfo(player, this);
        int currentCharges = (chargeInfo != null) ? chargeInfo.current() : MAX_CHARGES;

        currentCharges--;
        manager.setChargeInfo(player, this, currentCharges, MAX_CHARGES);

        if (currentCharges <= 0) {
            manager.setCooldown(player, this, item);
            manager.removeChargeInfo(player, this);
        }
    }

    // 이 메서드는 SpecialAbilityListener의 onEntityDamageByEntity에서 호출됩니다.
    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event, Player player, ItemStack item) {
        // 'player'는 이 능력을 가진 피격자(victim)입니다.
        // 훅이 고정된 상태에서 공격받으면 훅을 취소합니다.
        if (latchedHooks.containsKey(player.getUniqueId())) {
            latchedHooks.remove(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }
    }

    private void performGrappling(Player player, Location targetLocation) {
        // 이펙트 추가
        drawParticleLine(player.getEyeLocation(), targetLocation);

        // 당기기 시작 사운드 ("낚시찌 던지기")
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 0.8f);

        Vector velocity = targetLocation.toVector().subtract(player.getLocation().toVector()).normalize().multiply(2.5);
        player.setVelocity(velocity);
        // 이동 중 사운드 ("블레이즈 화염구")
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.2f);
    }

    private Location findTargetLocation(Location start, Vector direction, double maxDistance) {
        for (double i = 1; i <= maxDistance; i += 0.5) {
            Location checkLocation = start.clone().add(direction.clone().multiply(i));
            if (checkLocation.getBlock().getType().isSolid()) {
                return checkLocation;
            }
        }
        return null;
    }

    private void drawParticleLine(Location start, Location end) {
        World world = start.getWorld();
        if (world == null) return;

        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        if (distance < 1) return;
        direction.normalize();

        // 0.4 블록 간격으로 파티클을 생성합니다.
        for (double i = 0; i < distance; i += 0.4) {
            Location particleLoc = start.clone().add(direction.clone().multiply(i));
            world.spawnParticle(Particle.CRIT, particleLoc, 1, 0, 0, 0, 0);
        }
        world.spawnParticle(Particle.ENCHANT, end, 30, 0.5, 0.5, 0.5, 0.1);
    }
}