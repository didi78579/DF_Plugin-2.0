package cjs.DF_Plugin.upgrade.specialability;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.profile.WeaponProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpecialAbilityListener implements Listener {

    private final DF_Main plugin;
    private final SpecialAbilityManager specialAbilityManager;
    private final WeaponProfileManager weaponProfileManager;

    public SpecialAbilityListener(DF_Main plugin) {
        this.plugin = plugin;
        this.specialAbilityManager = plugin.getSpecialAbilityManager();
        this.weaponProfileManager = plugin.getWeaponProfileManager();
    }

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

        ISpecialAbility ability = specialAbilityManager.getAbilityFromItem(item);
        if (ability == null) return;

        // 복합적인 능력(방패, 활, 그래플링 훅 등)은 자체적으로 쿨다운/충전 로직을 관리하므로
        // 리스너에서 일괄적으로 쿨다운을 체크하거나 설정하지 않습니다.
        String internalName = ability.getInternalName();
        boolean isSelfManaged = internalName.equals("shield_bash") || internalName.equals("supercharge_bow") || internalName.equals("grappling_hook");

        if (!isSelfManaged) {
            if (specialAbilityManager.isAbilityOnCooldown(player, ability, item)) return;
            specialAbilityManager.setCooldown(player, ability, item);
        }
        ability.onPlayerInteract(event, player, item);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 공격자 능력 (예: 무적 관통)
        if (event.getDamager() instanceof Player attacker) {
            ItemStack item = attacker.getInventory().getItemInMainHand();
            ISpecialAbility ability = specialAbilityManager.getAbilityFromItem(item);

            if (ability != null) {
                // 'parricide', 'vampirism' 등은 확률적으로 발동하고 자체적으로 쿨다운을 설정하므로,
                // 리스너에서 강제로 쿨다운을 걸지 않습니다.
                String internalName = ability.getInternalName();
                boolean isSelfManaged = internalName.equals("parricide")
                        || internalName.equals("vampirism")
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
            }
        }

        // 삼지창 투사체에 의한 피격 능력
        if (event.getDamager() instanceof Trident trident && trident.getShooter() instanceof Player shooter) {
            // 슈터가 던질 때 사용한 아이템을 기반으로 능력을 확인해야 합니다.
            // 가장 신뢰성 있는 방법은 아니지만, 현재 손에 들고 있는 아이템을 확인합니다.
            ItemStack itemInHand = shooter.getInventory().getItemInMainHand();
            if (itemInHand.getType() == Material.TRIDENT) {
                ISpecialAbility ability = specialAbilityManager.getAbilityFromItem(itemInHand);
                // onDamageByEntity의 'player'는 능력을 사용한 주체(슈터)입니다.
                if (ability != null) {
                    ability.onDamageByEntity(event, shooter, itemInHand);
                }
            }
        }

        // 피격자 능력 (예: 피해 무효화)
        if (event.getEntity() instanceof Player victim) {
            // 갑옷 능력
            for (ItemStack armor : victim.getInventory().getArmorContents()) {
                ISpecialAbility ability = specialAbilityManager.getAbilityFromItem(armor);
                if (ability != null) {
                    // DoubleJumpAbility는 피격 시 항상 onDamageByEntity를 호출하여 쿨다운을 갱신해야 합니다.
                    if (ability.getInternalName().equals("double_jump")) {
                        ability.onDamageByEntity(event, victim, armor);
                    }
                    // 다른 방어 능력들은 쿨다운이 아닐 때만 발동합니다.
                    else if (!specialAbilityManager.isAbilityOnCooldown(victim, ability, armor)) {
                        ability.onDamageByEntity(event, victim, armor);

                        // 피해 무효화 같은 일회성 방어 능력은 발동 후 쿨다운을 설정합니다.
                        if (ability.getInternalName().equals("damage_negation")) {
                            specialAbilityManager.setCooldown(victim, ability, armor);
                        }
                    }
                }
            }
            // 손에 들고 있는 아이템의 능력 확인 (그래플링 훅)
            ItemStack itemInHand = victim.getInventory().getItemInMainHand();
            ISpecialAbility handAbility = specialAbilityManager.getAbilityFromItem(itemInHand);
            if (handAbility != null && handAbility.getInternalName().equals("grappling_hook")) {
                // onDamageByEntity를 호출하여 훅이 고정된 상태라면 취소하도록 합니다.
                handAbility.onDamageByEntity(event, victim, itemInHand);
            }

            // 방패 능력 (도끼 스턴 방지 등)
            ItemStack shield = null;
            if (victim.getInventory().getItemInMainHand().getType() == Material.SHIELD) {
                shield = victim.getInventory().getItemInMainHand();
            } else if (victim.getInventory().getItemInOffHand().getType() == Material.SHIELD) {
                shield = victim.getInventory().getItemInOffHand();
            }
            if (shield != null) {
                ISpecialAbility shieldAbility = specialAbilityManager.getAbilityFromItem(shield);
                if (shieldAbility != null) {
                    shieldAbility.onDamageByEntity(event, victim, shield);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null) return;

        ISpecialAbility ability = specialAbilityManager.getAbilityFromItem(boots);
        if (ability != null) {
            // DoubleJumpAbility 같은 비행 관련 능력은 자체적으로 쿨다운을 확인하고 처리하도록 위임합니다.
            // 리스너는 이벤트를 전달하는 역할만 수행하여 코드 구조를 명확하게 합니다.
            ability.onToggleFlight(event, player, boots);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // 레깅스 능력 (슈퍼 점프)
        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings != null) {
            ISpecialAbility leggingsAbility = specialAbilityManager.getAbilityFromItem(leggings);
            // 슈퍼 점프 능력만 웅크리기 이벤트에 반응하도록 명시
            if (leggingsAbility != null && leggingsAbility.getInternalName().equals("super_jump")) {
                leggingsAbility.onPlayerToggleSneak(event, player, leggings);
            }
        }

        // 손에 든 아이템 능력 (그래플링 훅)
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        ISpecialAbility handAbility = specialAbilityManager.getAbilityFromItem(itemInHand);
        if (handAbility != null && handAbility.getInternalName().equals("grappling_hook")) {
            handAbility.onPlayerToggleSneak(event, player, itemInHand);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 플레이어가 착용한 모든 장비의 능력을 확인 (예: 낙하 데미지 면역)
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            ISpecialAbility ability = specialAbilityManager.getAbilityFromItem(armor);
            if (ability != null) {
                ability.onEntityDamage(event, player, armor);
            }
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

        ISpecialAbility ability = specialAbilityManager.getAbilityFromItem(rod);
        if (ability != null) {
            ability.onPlayerFish(event, player, rod);
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack bow = event.getBow();
        if (bow == null) return;

        ISpecialAbility ability = specialAbilityManager.getAbilityFromItem(bow);
        if (ability == null) return;

        ability.onEntityShootBow(event, player, bow);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.TRIDENT) return;

        ISpecialAbility ability = specialAbilityManager.getAbilityFromItem(item);
        if (ability == null) return;

        ability.onProjectileLaunch(event, player, item);
    }

    @EventHandler
    public void onPlayerRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        ISpecialAbility ability = specialAbilityManager.getAbilityFromItem(item);
        if (ability == null) return;

        ability.onPlayerRiptide(event, player, item);
    }
}