package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import cjs.DF_Plugin.upgrade.specialability.passive.TridentPassiveListener;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackflowAbility implements ISpecialAbility {

    // 개발 환경의 컴파일 오류를 우회하기 위해, 블록의 Material 정보만 저장합니다.
    private record OriginalBlockState(Material type) {}

    // 현재 공격 단계(1 또는 2)를 저장합니다. 이 맵에 키가 존재하면 능력이 활성 상태임을 의미합니다.
    private final Map<UUID, Integer> playerAttackPhase = new HashMap<>();
    // 현재 급류 돌진 중인 작업을 저장합니다.
    private final Map<UUID, BukkitTask> activeDashTasks = new HashMap<>();


    @Override
    public String getInternalName() {
        return "backflow";
    }

    @Override
    public String getDisplayName() {
        return "§3역류";
    }

    @Override
    public String getDescription() {
        return "§7[2회 충전] 우클릭으로 급류를 발동합니다. 두 번째 타격 시, 첫 번째로 적중한 대상에게 마무리 일격을 가합니다.";
    }

    @Override
    public double getCooldown() {
        return DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.special-abilities.backflow.cooldown", 10.0);
    }

    @Override
    public int getMaxCharges() {
        return 2;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event, Player player, ItemStack item) {
        if (!event.getAction().isRightClick()) return;

        SpecialAbilityManager manager = DF_Main.getInstance().getSpecialAbilityManager();

        // 능력 사용 전 현재 충전 횟수를 확인합니다.
        SpecialAbilityManager.ChargeInfo chargeInfo = manager.getChargeInfo(player, this);
        int currentCharges = (chargeInfo != null) ? chargeInfo.current() : getMaxCharges();

        if (manager.tryUseAbility(player, this, item)) {
            // 남은 충전량이 1개였다면 2단계, 아니면 1단계 공격입니다.
            int phase = (currentCharges == 1) ? 2 : 1;
            if (!forceRiptide(player, item, phase)) {
                // 급류 발동 실패 시(물/비가 아닐 때 등) 충전량을 되돌려줍니다.
                manager.refundCharge(player, this);
            }
        }
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event, Player player, ItemStack item) {
        // 돌진 중일 때 발생하는 바닐라 급류 데미지는 취소합니다.
        if (activeDashTasks.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * 패시브로 발사된 추가 투사체가 적에게 맞았을 때 호출됩니다.
     * @param player 능력을 사용한 플레이어
     * @param target 투사체에 맞은 대상
     */
    public void handlePassiveTridentHit(Player player, LivingEntity target) {
        // 능력이 활성 상태일 때만 처리합니다.
        if (playerAttackPhase.containsKey(player.getUniqueId())) {
            processHit(player, target);
        }
    }

    /**
     * 플레이어의 급류 돌진이 적에게 맞았을 때 호출됩니다.
     * @param player 능력을 사용한 플레이어
     * @param target 플레이어와 충돌한 대상
     */
    private void handleDashHit(Player player, LivingEntity target) {
        // 돌진이 끝났으므로, 관련 작업을 정리합니다.
        cancelDashTask(player.getUniqueId());
        processHit(player, target);
    }

    /**
     * 모든 타격(돌진, 패시브 투사체)을 처리하는 중앙 로직입니다.
     * @param player 능력을 사용한 플레이어
     * @param target 피해를 입은 대상
     */
    private void processHit(Player player, LivingEntity target) {
        // 현재 활성화된 공격 단계를 가져오고, 중복 처리를 방지하기 위해 즉시 상태를 제거합니다.
        Integer phase = playerAttackPhase.remove(player.getUniqueId());
        if (phase == null) {
            return; // 이미 다른 타격에 의해 처리된 경우
        }

        // 패시브 투사체가 먼저 맞았을 경우, 플레이어의 돌진을 중지시킵니다.
        cancelDashTask(player.getUniqueId());

        if (phase == 1) {
            // 1단계 공격: 10의 피해를 줍니다.
            double phase1Damage = DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.special-abilities.backflow.details.phase1-damage", 10.0);
            target.damage(phase1Damage, player);
            player.sendMessage("§b[역류]§7 공격 적중!");
        } else if (phase == 2) {
            // 2단계 공격: 첫 번째로 맞은 대상에게 즉시 마무리 일격을 가합니다.
            performFinisher(player, target);
        }
    }

    private void performFinisher(Player player, LivingEntity target) {
        double phase2Damage = DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.special-abilities.backflow.details.phase2-damage", 20.0);
        target.damage(phase2Damage, player);

        // 1. 대상을 물기둥에 가둡니다.
        trapInWater(target, 80); // 4초(80틱) 동안 물기둥 유지
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false)); // 5초간 발광
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 5, false, false)); // 물기둥에 머무르도록 강한 둔화 효과 부여

        // 2. 플레이어가 대상의 위로 급류를 사용하여 이동합니다.
        Location teleportLoc = target.getLocation().add(0, 5, 0);
        player.teleport(teleportLoc);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.5f, 1.2f); // 상승 효과음
        player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation(), 50, 0.5, 1, 0.5, 0.2);

        // 3. 짧은 지연 후, 위에서 아래로 급류를 사용하여 대상을 내리꽂습니다.
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }
                player.setPose(Pose.SPIN_ATTACK);
                player.setVelocity(new Vector(0, -3.0, 0)); // 하강 속도
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 1.5f, 1.0f); // 하강 효과음
            }
        }.runTaskLater(DF_Main.getInstance(), 10L); // 0.5초 지연

        // 4. 대상이 땅에 닿았을 때의 파티클 효과
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (target.isOnGround() || target.isDead() || ticks++ > 100) { // 5초 타임아웃
                    target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation().add(0, 0.5, 0), 300, 2.0, 0.5, 2.0, 0);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.2f, 0.8f);
                    this.cancel();
                }
            }
        }.runTaskTimer(DF_Main.getInstance(), 0L, 1L);
    }

    /**
     * 대상을 일시적인 물기둥에 가둡니다.
     * @param target 가둘 대상 엔티티
     * @param durationTicks 물기둥이 유지될 시간 (틱 단위)
     */
    private void trapInWater(LivingEntity target, int durationTicks) {
        final Location center = target.getLocation();
        final World world = center.getWorld();
        final Map<Location, OriginalBlockState> originalBlocks = new HashMap<>();
        final int radius = 2;
        final int height = 4;

        // 원통형 물기둥을 생성하고 원래 블록 정보를 저장합니다.
        for (int y = 0; y < height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (new Vector(x, 0, z).lengthSquared() < (radius * radius)) {
                        Location loc = center.clone().add(x, y, z);
                        // 월드 파괴를 방지하기 위해 고체가 아닌 블록만 물로 변경합니다.
                        if (!loc.getBlock().getType().isSolid()) {
                            originalBlocks.put(loc.clone(), new OriginalBlockState(loc.getBlock().getType()));
                            loc.getBlock().setType(Material.WATER);
                        }
                    }
                }
            }
        }

        // 일정 시간 후 원래 블록으로 복구하는 작업을 예약합니다.
        new BukkitRunnable() {
            @Override
            public void run() {
                originalBlocks.forEach((loc, state) -> {
                    // 다른 요인에 의해 블록이 변경되었을 수 있으므로, 여전히 물 블록일 때만 복구합니다.
                    if (loc.getBlock().getType() == Material.WATER) {
                        loc.getBlock().setType(state.type());
                    }
                });
            }
        }.runTaskLater(DF_Main.getInstance(), durationTicks);
    }

    private boolean forceRiptide(Player player, ItemStack tridentItem, int phase) {
        if (tridentItem == null || tridentItem.getType() != Material.TRIDENT || !tridentItem.hasItemMeta()) {
            return false;
        }
        int riptideLevel = tridentItem.getEnchantmentLevel(Enchantment.RIPTIDE);
        if (riptideLevel <= 0) return false;

        // 이미 돌진 중이면 중복 실행 방지
        if (activeDashTasks.containsKey(player.getUniqueId())) {
            return false;
        }

        // 1. 속도 및 사운드 등 기본 효과 적용
        player.setVelocity(player.getLocation().getDirection().multiply(1.5 + (riptideLevel * 0.2)));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.5f, 1.0f);

        if (player.getGameMode() != GameMode.CREATIVE) {
            tridentItem.damage(1, player);
        }

        // 2. 패시브 추가 투사체 발사
        int upgradeLevel = DF_Main.getInstance().getUpgradeManager().getUpgradeLevel(tridentItem);
        if (upgradeLevel > 0) {
            TridentPassiveListener.launchAdditionalTridents(player, upgradeLevel, player.getEyeLocation(), player.getLocation().getDirection());
        }

        // 3. 호환성을 위한 이벤트 호출
        Bukkit.getPluginManager().callEvent(new PlayerRiptideEvent(player, tridentItem));

        // 4. 공격 상태 및 애니메이션/타격 판정을 위한 독립 작업 시작
        playerAttackPhase.put(player.getUniqueId(), phase);

        BukkitTask dashTask = new BukkitRunnable() {
            private final int maxTicks = 60; // 3초간 지속
            private int ticksLived = 0;

            @Override
            public void run() {
                // --- 작업 종료 조건 ---
                if (!player.isOnline() || player.isDead() || ticksLived++ > maxTicks) {
                    cancelDashTask(player.getUniqueId());
                    return;
                }
                if (ticksLived > 3 && player.isOnGround()) { // 발동 직후 땅에 닿아 바로 취소되는 것을 방지
                    cancelDashTask(player.getUniqueId());
                    return;
                }

                // --- 핵심 로직 ---
                // 애니메이션이 취소되지 않도록 매 틱마다 포즈를 강제로 설정
                player.setPose(Pose.SPIN_ATTACK);

                // 회전 애니메이션과 함께 물보라 파티클을 지속적으로 생성하여 이펙트를 강화합니다.
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);

                // 상체 중심, 지름 3(반지름 1.5)의 범위 내 엔티티를 확인하여 타격 판정을 개선합니다.
                Location checkCenter = player.getLocation().add(0, 1.0, 0);
                double radius = 1.5;
                for (Entity entity : player.getWorld().getNearbyEntities(checkCenter, radius, radius, radius)) {
                    if (entity instanceof LivingEntity target && !entity.getUniqueId().equals(player.getUniqueId())) {
                        handleDashHit(player, target); // 타격 성공
                        return; // handleDashHit에서 작업이 취소되므로 즉시 종료
                    }
                }
            }
        }.runTaskTimer(DF_Main.getInstance(), 0L, 1L);

        activeDashTasks.put(player.getUniqueId(), dashTask);
        return true;
    }

    private void cancelDashTask(UUID playerUUID) {
        if (activeDashTasks.containsKey(playerUUID)) {
            activeDashTasks.get(playerUUID).cancel();
            activeDashTasks.remove(playerUUID);

            // 돌진이 끝나면 플레이어의 포즈를 원래대로 되돌립니다.
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.getPose() == Pose.SPIN_ATTACK) {
                player.setPose(Pose.STANDING);
            }
        }
    }

    @Override
    public void onCleanup(Player player) {
        cancelDashTask(player.getUniqueId());
        playerAttackPhase.remove(player.getUniqueId());
    }
}