package cjs.DF_Plugin.upgrade.specialability.passive;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;
import java.util.UUID;

/**
 * 부츠 착용으로 인한 모든 패시브 효과(이동 속도, 더블 점프 등)를 관리하는 통합 리스너입니다.
 */
public class BootsPassiveListener implements Listener {

    private final DF_Main plugin;
    private final SpecialAbilityManager specialAbilityManager;
    private static final UUID BOOTS_SPEED_MODIFIER_UUID = UUID.fromString("a8c1b9ab-4e9c-4f1c-9d1a-29f9f734e6ce");

    public BootsPassiveListener(DF_Main plugin) {
        this.plugin = plugin;
        this.specialAbilityManager = plugin.getSpecialAbilityManager();
        startPassiveCheckTask();
    }

    /**
     * 주기적으로 플레이어의 부츠를 확인하여 패시브 효과를 적용/제거하는 작업을 시작합니다.
     */
    private void startPassiveCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkBootPassives(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // 0.1초마다 확인하여 착지 시 즉각적인 반응을 보장합니다.
    }

    /**
     * 플레이어가 지상에서 점프할 때 더블 점프 능력을 활성화합니다.
     */
    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        ItemStack boots = player.getInventory().getBoots();
        Optional<ISpecialAbility> abilityOpt = specialAbilityManager.getAbilityFromItem(boots);
        boolean hasDoubleJump = abilityOpt.isPresent() && "double_jump".equals(abilityOpt.get().getInternalName());

        if (hasDoubleJump) {
            ISpecialAbility ability = abilityOpt.get();
            if (!specialAbilityManager.isOnCooldown(player, ability, boots) && !player.getAllowFlight()) {
                // 1틱 뒤에 비행을 활성화하여, checkBootPassives가 같은 틱에서 즉시 비활성화하는 것을 방지합니다.
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // 플레이어가 여전히 더블 점프 부츠를 신고 있는지 다시 확인합니다.
                        ItemStack currentBoots = player.getInventory().getBoots();
                        if (currentBoots != null && boots.isSimilar(currentBoots)) {
                            player.setAllowFlight(true);
                        }
                    }
                }.runTaskLater(plugin, 1L);
            }
        }
    }

    /**
     * 플레이어의 부츠를 확인하고, 이동 속도 보너스 및 더블 점프 비활성화 등 패시브 효과를 관리합니다.
     * @param player 확인할 플레이어
     */
    private void checkBootPassives(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        // 기존 속도 버프가 있다면 제거
        for (AttributeModifier modifier : speedAttribute.getModifiers()) {
            if (modifier.getUniqueId().equals(BOOTS_SPEED_MODIFIER_UUID)) {
                speedAttribute.removeModifier(modifier);
                break; // UUID는 유일하므로 찾으면 루프 종료
            }
        }

        ItemStack boots = player.getInventory().getBoots();
        Optional<ISpecialAbility> abilityOpt = specialAbilityManager.getAbilityFromItem(boots);
        boolean hasDoubleJump = abilityOpt.isPresent() && "double_jump".equals(abilityOpt.get().getInternalName());

        // 더블 점프 상태 관리 (서바이벌/어드벤처 모드에서만)
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            if (hasDoubleJump) {
                // 플레이어가 착지했다면, 다음 점프를 위해 비행 능력을 비활성화(초기화)합니다.
                if (player.isOnGround() && player.getAllowFlight()) {
                    player.setAllowFlight(false);
                }
            } else {
                // 더블 점프 부츠를 벗었다면, 비행 능력을 비활성화합니다.
                if (player.getAllowFlight()) {
                    player.setAllowFlight(false);
                }
            }
        }

        // 강화 레벨에 따른 속도 증가 적용
        int level = plugin.getUpgradeManager().getUpgradeLevel(boots);
        if (level > 0) {
            double speedBonusPerLevel = plugin.getGameConfigManager().getConfig().getDouble("upgrade.generic-bonuses.boots.speed-multiplier-per-level", 0.05);
            double totalBonus = speedBonusPerLevel * level;

            if (totalBonus > 0) {
                AttributeModifier modifier = new AttributeModifier(BOOTS_SPEED_MODIFIER_UUID, "BootsSpeedBonus", totalBonus, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlot.FEET);
                speedAttribute.addModifier(modifier);
            }
        }
    }
}