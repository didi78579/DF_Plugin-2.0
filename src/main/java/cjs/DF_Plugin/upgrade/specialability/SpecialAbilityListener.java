package cjs.DF_Plugin.upgrade.specialability;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.specialability.impl.BackflowAbility;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class SpecialAbilityListener implements Listener {

    private final DF_Main plugin;
    private final SpecialAbilityManager specialAbilityManager;

    public SpecialAbilityListener(DF_Main plugin) {
        this.plugin = plugin;
        this.specialAbilityManager = plugin.getSpecialAbilityManager();
    }

    // --- 상태 정리(Cleanup) 이벤트 핸들러 ---

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 플레이어가 서버를 나갈 때 모든 능력의 상태를 정리합니다.
        specialAbilityManager.getAllAbilities().forEach(ability -> ability.onCleanup(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 플레이어가 죽었을 때 모든 능력의 상태를 정리합니다.
        specialAbilityManager.getAllAbilities().forEach(ability -> ability.onCleanup(event.getEntity()));
    }

    @EventHandler(priority = EventPriority.MONITOR) // 다른 플러그인의 이벤트 취소를 방해하지 않도록 MONITOR로 설정
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        // 플레이어가 손에 든 아이템을 바꿀 때, 이전에 들고 있던 아이템의 능력 상태를 정리합니다.
        ItemStack previousItem = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
        if (previousItem != null) {
            specialAbilityManager.getAbilityFromItem(previousItem) // 이제 Optional을 반환합니다.
                    .ifPresent(ability -> ability.onCleanup(event.getPlayer()));
        }
    }

    // --- 능력 발동 이벤트 핸들러 ---

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        Action action = event.getAction();
        boolean isRightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean isLeftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;

        // 방패는 좌클릭, 그 외에는 우클릭으로 발동
        boolean shouldTrigger = (item.getType() == Material.SHIELD && isLeftClick) || (item.getType() != Material.SHIELD && isRightClick);

        if (!shouldTrigger) {
            return;
        }

        specialAbilityManager.getAbilityFromItem(item).ifPresent(ability -> {
            // 리스너는 이벤트를 전달할 뿐, 모든 쿨다운 및 사용 조건 확인은 각 능력 클래스가 책임집니다.
            ability.onPlayerInteract(event, player, item);
        });
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // --- 공격자(Attacker)의 능력 처리 ---
        Player attacker = null;
        ItemStack weapon = null;

        // 공격의 주체를 찾습니다. (직접 공격 플레이어 또는 투사체 발사자)
        if (event.getDamager() instanceof Player p) {
            attacker = p;
            weapon = attacker.getInventory().getItemInMainHand();
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof Player p) {
            attacker = p;
            // 투사체 종류에 따라 사용한 무기를 추정합니다.
            if (projectile instanceof Arrow) {
                ItemStack mainHand = attacker.getInventory().getItemInMainHand();
                ItemStack offHand = attacker.getInventory().getItemInOffHand();
                if (mainHand.getType() == Material.BOW || mainHand.getType() == Material.CROSSBOW) {
                    weapon = mainHand;
                } else if (offHand.getType() == Material.BOW || offHand.getType() == Material.CROSSBOW) {
                    weapon = offHand;
                }
            } else if (projectile instanceof Trident) {
                ItemStack mainHand = attacker.getInventory().getItemInMainHand();
                ItemStack offHand = attacker.getInventory().getItemInOffHand();

                // '역류' 능력의 패시브 투사체 처리
                if (projectile.hasMetadata("df_backflow_projectile") && event.getEntity() instanceof LivingEntity target) {
                    // 플레이어가 자신의 투사체에 맞는 경우 무시
                    if (target.getUniqueId().equals(p.getUniqueId())) {
                        event.setCancelled(true);
                        return;
                    }

                    ItemStack tridentInHand = (mainHand.getType() == Material.TRIDENT) ? mainHand : offHand;
                    if (tridentInHand.getType() == Material.TRIDENT) {
                        specialAbilityManager.getAbilityFromItem(tridentInHand).ifPresent(ability -> {
                            if (ability instanceof BackflowAbility backflowAbility) {
                                backflowAbility.handlePassiveTridentHit(p, target);
                                event.setCancelled(true); // 기본 데미지 및 다른 효과(둔화) 방지
                            }
                        });
                        // 이벤트가 처리되었으면 여기서 종료하여, 아래의 일반 무기 처리 로직이 실행되지 않도록 합니다.
                        if (event.isCancelled()) return;
                    }
                }

                if (mainHand.getType() == Material.TRIDENT) {
                    weapon = mainHand;
                } else if (offHand.getType() == Material.TRIDENT) {
                    weapon = offHand;
                }
            }
        }

        // 공격자와 사용한 무기가 식별된 경우, 능력 발동
        if (attacker != null && weapon != null) {
            final Player finalAttacker = attacker; // 람다에서 사용하기 위해 final 변수에 할당
            final ItemStack finalWeapon = weapon; // weapon은 이미 effectively final이지만, 명확성을 위해 final로 선언합니다.
            specialAbilityManager.getAbilityFromItem(finalWeapon)
                    .ifPresent(ability -> ability.onDamageByEntity(event, finalAttacker, finalWeapon));
        }

        // --- 피격자(Victim)의 능력 처리 ---
        if (event.getEntity() instanceof Player victim) {
            // 1. 갑옷 능력 (항상 방어/유틸 능력으로 간주)
            for (ItemStack armor : victim.getInventory().getArmorContents()) {
                if (armor == null || armor.getType() == Material.AIR) continue;
                specialAbilityManager.getAbilityFromItem(armor)
                        .ifPresent(ability -> ability.onDamageByEntity(event, victim, armor));
            }

            // 2. 양손에 든 아이템의 능력 (방어/유틸리티 아이템만)
            handleVictimHeldItem(event, victim, victim.getInventory().getItemInMainHand());
            handleVictimHeldItem(event, victim, victim.getInventory().getItemInOffHand());
        }
    }

    /**
     * 피격자가 손에 든 아이템의 방어/유틸리티 능력을 처리합니다.
     * 공격용 무기에 붙은 능력은 발동되지 않도록, 특정 아이템 타입(방패, 낚싯대 등)만 허용합니다.
     */
    private void handleVictimHeldItem(EntityDamageByEntityEvent event, Player victim, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        Material itemType = item.getType();

        // 피격 시 발동해야 하는 방어/유틸리티 아이템 타입만 명시적으로 허용합니다.
        if (itemType == Material.SHIELD || itemType == Material.FISHING_ROD) {
            specialAbilityManager.getAbilityFromItem(item)
                    .ifPresent(ability -> ability.onDamageByEntity(event, victim, item));
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // 플레이어가 착용한 모든 장비를 확인하여 능력을 찾습니다.
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null) {
                specialAbilityManager.getAbilityFromItem(armor).ifPresent(ability -> {
                    ability.onPlayerToggleFlight(event, player, armor);
                });
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // 갑옷 부위의 능력을 확인합니다 (레깅스, 부츠 등).
        // 슈퍼 점프(레깅스)와 공중 대시(부츠)는 모두 웅크리기로 발동되므로,
        // 이 핸들러에서 모든 갑옷을 순회하며 처리하는 것이 효율적입니다.
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null) {
                specialAbilityManager.getAbilityFromItem(armor)
                        .ifPresent(ability -> ability.onPlayerToggleSneak(event, player, armor));
            }
        }

        // 손에 든 아이템 능력 (그래플링 훅 등)
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        specialAbilityManager.getAbilityFromItem(itemInHand)
                .ifPresent(ability -> ability.onPlayerToggleSneak(event, player, itemInHand));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }

        // 이중 도약(더블 점프) 관련 비행 제어 로직을 제거합니다.
        // 새로운 '공중 대시'는 웅크리기로 발동되므로, onPlayerMove에서 비행 상태를 제어할 필요가 없습니다.
        // 이동 시 발동하는 모든 장비의 특수 능력 처리 (예: 재생, 슈퍼점프 상태 초기화)
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            specialAbilityManager.getAbilityFromItem(armor)
                    .ifPresent(ability -> ability.onPlayerMove(event, player, armor));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 플레이어가 착용한 모든 장비의 능력을 확인 (예: 낙하 데미지 면역)
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            specialAbilityManager.getAbilityFromItem(armor).ifPresent(ability -> {
                ability.onEntityDamage(event, player, armor);
            });
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack rod = null;

        if (mainHand.getType() == Material.FISHING_ROD) rod = mainHand;
        else if (offHand.getType() == Material.FISHING_ROD) rod = offHand;

        if (rod == null) return;

        final ItemStack finalRod = rod;
        specialAbilityManager.getAbilityFromItem(finalRod).ifPresent(ability -> {
            ability.onPlayerFish(event, player, finalRod);
        });
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack bow = event.getBow();
        if (bow == null) return;

        specialAbilityManager.getAbilityFromItem(bow).ifPresent(ability -> {
            ability.onEntityShootBow(event, player, bow);
        });
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.TRIDENT) return;

        specialAbilityManager.getAbilityFromItem(item).ifPresent(ability -> {
            ability.onProjectileLaunch(event, player, item);
        });
    }

    @EventHandler
    public void onPlayerRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        specialAbilityManager.getAbilityFromItem(item).ifPresent(ability -> {
            ability.onPlayerRiptide(event, player, item);
        });
    }
}