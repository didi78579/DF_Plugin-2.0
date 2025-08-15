package cjs.DF_Plugin.settings;

/**
 * config.yml 파일의 경로(키)를 상수로 관리하는 클래스입니다.
 * 문자열을 직접 사용하는 것을 방지하여 오타를 줄이고 유지보수성을 높입니다.
 */
public final class ConfigKeys {

    private ConfigKeys() {} // 인스턴스화 방지

    // --- Pylon Shop ---
    private static final String PYLON_SHOP_BASE = "pylon.shop.";

    public static final String RECON_FIREWORK_COST = PYLON_SHOP_BASE + "recon-firework.cost-level";
    public static final String RETURN_SCROLL_COST = PYLON_SHOP_BASE + "return-scroll.cost-level";
    public static final String MAGIC_STONE_EXCHANGE_COST = PYLON_SHOP_BASE + "magic-stone-exchange.cost-level";
    public static final String MAGIC_STONE_EXCHANGE_GAINED = PYLON_SHOP_BASE + "magic-stone-exchange.gained";
    public static final String UPGRADE_STONE_EXCHANGE_COST = PYLON_SHOP_BASE + "upgrade-stone-exchange.cost-level";
    public static final String UPGRADE_STONE_EXCHANGE_GAINED = PYLON_SHOP_BASE + "upgrade-stone-exchange.gained";
    public static final String AUX_CORE_COST = PYLON_SHOP_BASE + "aux-core.cost-level";

    // --- Pylon Shop Item IDs (for PDC) ---
    public static final String SHOP_ID_RECON = "buy_recon";
    public static final String SHOP_ID_RETURN_SCROLL = "return_scroll";
    public static final String SHOP_ID_MAGIC_STONE = "enchant_scroll_exchange";
    public static final String SHOP_ID_UPGRADE_STONE = "upgrade_stone_exchange";
    public static final String SHOP_ID_AUX_CORE = "aux_core";

    // --- Items ---
    private static final String ITEMS_BASE = "items.";
    public static final String MAGIC_STONE_RECIPE_OUTPUT = ITEMS_BASE + "magic-stone-recipe-output";

    // --- Enchanting System ---
    private static final String ENCHANTING_BASE = "enchanting.";
    public static final String RANDOM_ENCHANTING_EXTRA_CHANCE = ENCHANTING_BASE + "random-enchanting.extra-enchant-chance";
    public static final String RANDOM_ENCHANTING_CURSE_CHANCE = ENCHANTING_BASE + "random-enchanting.curse-chance";
    public static final String RANDOM_ENCHANTING_MAX_ENCHANTS = ENCHANTING_BASE + "random-enchanting.max-enchantments";

    // --- Upgrade System ---
    private static final String UPGRADE_BASE = "upgrade.";
    public static final String UPGRADE_SHOW_CHANCE = UPGRADE_BASE + "show-success-chance";
    public static final String UPGRADE_LEVEL_SETTINGS = UPGRADE_BASE + "level-settings.";
    public static final String UPGRADE_PER_WEAPON_COOLDOWN = UPGRADE_BASE + "cooldown.per-weapon-cooldown";

    // --- Ability Cooldowns & Chances ---
    private static final String ABILITY_COOLDOWN_BASE = UPGRADE_BASE + "ability-cooldowns.";
    private static final String ABILITY_CHANCE_BASE = UPGRADE_BASE + "ability-chances.";
    private static final String ABILITY_ATTR_BASE = UPGRADE_BASE + "ability-attributes.";

    // Super Jump
    public static final String SUPER_JUMP_CHARGE_TIME = ABILITY_ATTR_BASE + "super_jump.charge-time-seconds";
    public static final String SUPER_JUMP_VELOCITY = ABILITY_ATTR_BASE + "super_jump.jump-velocity";
    public static final String SUPER_JUMP_DASH_VELOCITY = ABILITY_ATTR_BASE + "super_jump.dash-velocity";

    // Supercharge
    public static final String SUPERCHARGE_CHARGE_TIME = ABILITY_ATTR_BASE + "supercharge.charge-time-seconds";
    public static final String SUPERCHARGE_CHARGED_DURATION = ABILITY_ATTR_BASE + "supercharge.charged-duration-seconds";
    public static final String SUPERCHARGE_DAMAGE = ABILITY_ATTR_BASE + "supercharge.damage";
    public static final String SUPERCHARGE_KNOCKBACK_BONUS = ABILITY_ATTR_BASE + "supercharge.knockback-bonus";
    public static final String SUPERCHARGE_PASSIVE_DAMAGE_PERCENT = ABILITY_ATTR_BASE + "supercharge.passive-max-health-damage-percent-per-level";
    public static final String SUPERCHARGE_PASSIVE_DAMAGE_MAX_PERCENT = ABILITY_ATTR_BASE + "supercharge.passive-max-health-damage-max-percent";

    // Grab
    public static final String GRAB_PULL_STRENGTH = ABILITY_ATTR_BASE + "grab.pull-strength";
    public static final String GRAB_UPWARD_FORCE = ABILITY_ATTR_BASE + "grab.upward-force";
    public static final String GRAB_MAX_PULL_STRENGTH = ABILITY_ATTR_BASE + "grab.max-pull-strength";
    public static final String GRAB_CAST_VELOCITY_MULTIPLIER = ABILITY_ATTR_BASE + "grab.cast-velocity-multiplier";
    public static final String GRAB_CAST_MAX_VELOCITY = ABILITY_ATTR_BASE + "grab.cast-max-velocity";

    // Laser Shot
    public static final String LASER_SHOT_REQUIRED_LEVEL = ABILITY_ATTR_BASE + "laser_shot.required-level";
    public static final String LASER_SHOT_VELOCITY_MULTIPLIER = ABILITY_ATTR_BASE + "laser_shot.velocity-multiplier";
    public static final String LASER_SHOT_GLOW_DURATION = ABILITY_ATTR_BASE + "laser_shot.glow-duration-seconds";
    public static final String LASER_SHOT_PASSIVE_DAMAGE = ABILITY_ATTR_BASE + "laser_shot.passive-damage-per-level";

    // Damage Negation
    public static final String DAMAGE_NEGATION_COOLDOWN = ABILITY_COOLDOWN_BASE + "damage_negation";
    public static final String DAMAGE_NEGATION_CHANCE = ABILITY_CHANCE_BASE + "damage_negation";

    // Other Cooldowns
    public static final String VOID_RIPTIDE_COOLDOWN = ABILITY_COOLDOWN_BASE + "void_riptide";
    public static final String VAMPIRISM_COOLDOWN = ABILITY_COOLDOWN_BASE + "vampirism";
    public static final String SHIELD_BASH_COOLDOWN = ABILITY_COOLDOWN_BASE + "shield_bash";
    public static final String STUN_COOLDOWN = ABILITY_COOLDOWN_BASE + "stun";
    public static final String GRAPPLING_HOOK_COOLDOWN = ABILITY_COOLDOWN_BASE + "grappling_hook";
    public static final String GRAB_COOLDOWN = ABILITY_COOLDOWN_BASE + "grab";
    public static final String PARRICIDE_COOLDOWN = ABILITY_COOLDOWN_BASE + "parricide";

}