package cjs.DF_Plugin.upgrade.specialability;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
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
            // 복합적인 능력(방패, 활, 그래플링 훅 등)은 자체적으로 쿨다운/충전 로직을 관리하므로
            // 리스너에서 일괄적으로 쿨다운을 체크하거나 설정하지 않습니다.
            String internalName = ability.getInternalName();
            boolean isSelfManaged = internalName.equals("shield_bash")
                    || internalName.equals("supercharge")
                    || internalName.equals("grappling_hook")
                    || internalName.equals("grab");

            if (!isSelfManaged) {
                if (specialAbilityManager.isAbilityOnCooldown(player, ability, item)) return;
                specialAbilityManager.setCooldown(player, ability, item);
            }
            ability.onPlayerInteract(event, player, item);
        });
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 공격자 능력 (예: 무적 관통)
        if (event.getDamager() instanceof Player attacker) {
            ItemStack item = attacker.getInventory().getItemInMainHand();
            specialAbilityManager.getAbilityFromItem(item).ifPresent(ability -> {
                // 'vampirism', 'stun' 등은 확률적으로 발동하거나 자체적으로 쿨다운을 설정하므로,
                // 리스너에서 강제로 쿨다운을 걸지 않습니다.
                String internalName = ability.getInternalName();
                boolean isSelfManaged = internalName.equals("vampirism")
                        || internalName.equals("stun");

                if (isSelfManaged) {
                        // 능력 클래스가 자체적으로 쿨다운 확인 및 설정을 처리합니다.
                        ability.onDamageByEntity(event, attacker, item);
                    } else {
                        // 리스너가 쿨다운을 관리합니다.
                        if (!specialAbilityManager.isAbilityOnCooldown(attacker, ability, item)) {
                            ability.onDamageByEntity(event, attacker, item);
                            specialAbilityManager.setCooldown(attacker, ability, item);
                        }
                    }
            });
        }

        // 삼지창 투사체에 의한 피격 능력
        if (event.getDamager() instanceof Trident trident && trident.getShooter() instanceof Player shooter) {
            // 슈터가 던질 때 사용한 아이템을 기반으로 능력을 확인해야 합니다.
            // 가장 신뢰성 있는 방법은 아니지만, 현재 손에 들고 있는 아이템을 확인합니다.
            ItemStack itemInHand = shooter.getInventory().getItemInMainHand();
            if (itemInHand.getType() == Material.TRIDENT) {
                specialAbilityManager.getAbilityFromItem(itemInHand).ifPresent(ability -> {
                    // onDamageByEntity의 'player'는 능력을 사용한 주체(슈터)입니다.
                    ability.onDamageByEntity(event, shooter, itemInHand);
                });
            }
        }

        // 화살 투사체에 의한 피격 능력 (슈퍼차지, 레이저샷 등)
        if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player shooter) {
            // 슈터가 활/쇠뇌를 쏠 때 사용했던 무기를 기반으로 능력을 확인해야 합니다.
            // 가장 신뢰성 있는 방법은 아니지만, 현재 손에 들고 있는 아이템을 확인합니다.
            ItemStack weapon = shooter.getInventory().getItemInMainHand();
            // 주 손에 무기가 없으면 다른 손도 확인
            if (weapon.getType() != Material.BOW && weapon.getType() != Material.CROSSBOW) {
                weapon = shooter.getInventory().getItemInOffHand();
            }

            final ItemStack finalWeapon = weapon;
            if (finalWeapon.getType() == Material.BOW || finalWeapon.getType() == Material.CROSSBOW) {
                specialAbilityManager.getAbilityFromItem(finalWeapon).ifPresent(ability -> {
                    // onDamageByEntity의 'player'는 능력을 사용한 주체(슈터)입니다.
                    ability.onDamageByEntity(event, shooter, finalWeapon);
                });
            }
        }

        // 피격자 능력 (예: 피해 무효화)
        if (event.getEntity() instanceof Player victim) {
            // 갑옷 능력
            for (ItemStack armor : victim.getInventory().getArmorContents()) {
                specialAbilityManager.getAbilityFromItem(armor).ifPresent(ability -> {
                    String internalName = ability.getInternalName();
                    // 자체 관리 능력들은 스스로 쿨다운을 확인하고 로직을 처리합니다.
                    boolean isSelfManaged = internalName.equals("double_jump") || internalName.equals("damage_negation");

                    if (isSelfManaged) {
                        ability.onDamageByEntity(event, victim, armor);
                    } else {
                        // 그 외 간단한 방어 능력들은 리스너가 쿨다운을 확인합니다.
                        if (!specialAbilityManager.isAbilityOnCooldown(victim, ability, armor)) {
                            ability.onDamageByEntity(event, victim, armor);
                        }
                    }
                });
            }
            // 손에 들고 있는 아이템의 능력 확인 (그래플링 훅)
            ItemStack itemInHand = victim.getInventory().getItemInMainHand();
            specialAbilityManager.getAbilityFromItem(itemInHand).ifPresent(handAbility -> {
                if (handAbility.getInternalName().equals("grappling_hook")) {
                    // onDamageByEntity를 호출하여 훅이 고정된 상태라면 취소하도록 합니다.
                    handAbility.onDamageByEntity(event, victim, itemInHand);
                }
            });

            // 방패 능력 (도끼 스턴 방지 등)
            ItemStack shield = null;
            if (victim.getInventory().getItemInMainHand().getType() == Material.SHIELD) {
                shield = victim.getInventory().getItemInMainHand();
            } else if (victim.getInventory().getItemInOffHand().getType() == Material.SHIELD) {
                shield = victim.getInventory().getItemInOffHand();
            }
            final ItemStack finalShield = shield;
            if (finalShield != null) {
                specialAbilityManager.getAbilityFromItem(finalShield).ifPresent(shieldAbility -> {
                    shieldAbility.onDamageByEntity(event, victim, finalShield);
                });
            }
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null) return;
        
        specialAbilityManager.getAbilityFromItem(boots).ifPresent(ability -> {
            // DoubleJumpAbility 같은 비행 관련 능력은 자체적으로 쿨다운을 확인하고 처리하도록 위임합니다.
            // 리스너는 이벤트를 전달하는 역할만 수행하여 코드 구조를 명확하게 합니다.
            ability.onToggleFlight(event, player, boots);
        });
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // 레깅스 능력 (슈퍼 점프)
        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings != null) {
            specialAbilityManager.getAbilityFromItem(leggings).ifPresent(leggingsAbility -> {
                // 슈퍼 점프 능력만 웅크리기 이벤트에 반응하도록 명시
                if (leggingsAbility.getInternalName().equals("super_jump")) {
                    leggingsAbility.onPlayerToggleSneak(event, player, leggings);
                }
            });
        }

        // 손에 든 아이템 능력 (그래플링 훅)
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        specialAbilityManager.getAbilityFromItem(itemInHand).ifPresent(handAbility -> {
            if (handAbility.getInternalName().equals("grappling_hook")) {
                handAbility.onPlayerToggleSneak(event, player, itemInHand);
            }
        });
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }

        // 이중 도약 능력의 비행 활성화/비활성화 처리
        ItemStack boots = player.getInventory().getBoots();

        boolean hasDoubleJump = specialAbilityManager.getAbilityFromItem(boots)
                .filter(ability -> ability.getInternalName().equals("double_jump"))
                .isPresent();

        if (hasDoubleJump) {
            if (player.isOnGround() && !player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
        } else {
            // 이중 도약 부츠를 신고 있지 않으면 비행을 비활성화
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
            }
        }

        // 이동 시 발동하는 모든 장비의 특수 능력 처리 (예: 재생)
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            specialAbilityManager.getAbilityFromItem(armor).ifPresent(moveAbility -> {
                if (!specialAbilityManager.isAbilityOnCooldown(player, moveAbility, armor)) {
                    moveAbility.onPlayerMove(event, player, armor);
                }
            });
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