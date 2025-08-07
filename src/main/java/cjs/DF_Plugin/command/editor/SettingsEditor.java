package cjs.DF_Plugin.command.editor;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.settings.GameConfigManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SettingsEditor {

    private final GameConfigManager configManager;

    public SettingsEditor(DF_Main plugin) {
        this.configManager = plugin.getGameConfigManager();
    }

    /**
     * 메인 설정 카테고리 메뉴를 엽니다.
     */
    public void openMainMenu(Player player) {
        player.sendMessage("§e======== §f[DarkForest 설정] §e========");
        player.sendMessage("§7수정할 설정 카테고리를 선택하세요.");

        sendCategoryButton(player, "사망 타이머", "death", "§e사망 시 부활 대기 관련 설정을 엽니다.");
        sendCategoryButton(player, "파일런 기능", "pylon", "§e파일런 관련 세부 기능 설정을 엽니다.");
        sendCategoryButton(player, "월드 보더", "worldborder", "§e월드 보더 크기 및 활성화 설정을 엽니다.");
        sendCategoryButton(player, "유틸리티", "utility", "§e게임 편의성 관련 설정을 엽니다.");
        sendCategoryButton(player, "OP 인챈트", "openchant", "§e밸런스에 영향을 주는 인챈트 활성화 여부를 설정합니다.");
        sendCategoryButton(player, "보스몹 강화", "bossmobstrength", "§e보스 몬스터의 능력치 배율을 설정합니다.");
        sendCategoryButton(player, "세부 정보", "detailsettings", "§e설정 파일 직접 수정 등 세부 정보를 안내합니다.");
        sendCategoryButton(player, "§c설정 초기화", "resetsettings", "§c모든 설정을 기본값으로 되돌립니다.");

        player.sendMessage("§e===================================");
    }

    /**
     * 사망 타이머 설정 UI를 엽니다.
     */
    public void openDeathTimerSettings(Player player) {
        player.sendMessage("§e======== §f[사망 타이머 설정] §e========");
        sendNumberSetting(player, "부활 비용(개)", "death-timer.price", 1);
        sendNumberSetting(player, "기본 데스 카운트", "death-timer.death-count", 1);
        sendNumberSetting(player, "부활 대기시간(초)", "death-timer.time", 10);
        player.sendMessage("§e===================================");
        sendBackButton(player);
    }

    /**
     * 파일런 기능 설정 UI를 엽니다.
     */
    public void openPylonFeaturesSettings(Player player) {
        player.sendMessage("§e======== §f[파일런 기능 설정] §e========");
        sendBooleanSetting(player, "개인 창고", "pylon-features.storage");
        sendBooleanSetting(player, "귀환 주문서", "pylon-features.return-scroll");
        sendBooleanSetting(player, "멀티 코어", "pylon-features.multi-core");
        sendBooleanSetting(player, "클랜 지옥", "pylon-features.clan-nether");
        player.sendMessage("§e===================================");
        sendBackButton(player);
    }

    /**
     * 월드보더 설정 UI를 엽니다.
     */
    public void openWorldBorderSettings(Player player) {
        player.sendMessage("§e======== §f[월드 보더 설정] §e========");
        sendNumberSetting(player, "오버월드 크기", "worldborder.overworld-size", 1000);
        sendBooleanSetting(player, "엔드 월드보더", "worldborder.end-enabled");
        player.sendMessage("§e===================================");
        sendBackButton(player);
    }

    /**
     * 유틸리티 설정 UI를 엽니다.
     */
    public void openUtilitySettings(Player player) {
        player.sendMessage("§e======== §f[유틸리티 설정] §e========");
        sendBooleanSetting(player, "인벤토리 유지", "utility.keep-inventory");
        sendBooleanSetting(player, "좌표 비활성화", "utility.location-info-disabled");
        sendBooleanSetting(player, "팬텀 비활성화", "utility.phantom-disabled");
        sendBooleanSetting(player, "마법 황금사과 조합", "utility.notched-apple-recipe");
        sendBooleanSetting(player, "불사의 토템", "utility.totem-enabled");
        sendBooleanSetting(player, "엔더 상자", "utility.enderchest-enabled");
        sendNumberSetting(player, "포션 소지 제한", "utility.potion-limit", 1);
        sendBooleanSetting(player, "보급 활성화", "utility.supply-drop-enabled");
        player.sendMessage("§e===================================");
        sendBackButton(player);
    }

    /**
     * OP 인챈트 설정 UI를 엽니다.
     */
    public void openOpEnchantSettings(Player player) {
        player.sendMessage("§e======== §f[OP 인챈트 설정] §e========");
        sendBooleanSetting(player, "격파(Breach)", "op-enchant.breach-enabled");
        sendBooleanSetting(player, "가시(Thorns)", "op-enchant.thorns-enabled");
        player.sendMessage("§e===================================");
        sendBackButton(player);
    }

    /**
     * 보스몹 강화 설정 UI를 엽니다.
     */
    public void openBossMobStrengthSettings(Player player) {
        player.sendMessage("§e======== §f[보스몹 강화 설정] §e========");
        sendDecimalSetting(player, "엔더 드래곤 배율", "boss-mob-strength.ender_dragon", 0.5);
        sendDecimalSetting(player, "위더 배율", "boss-mob-strength.wither", 0.5);
        player.sendMessage("§e===================================");
        sendBackButton(player);
    }

    /**
     * 세부 설정 안내 메시지를 표시합니다.
     */
    public void openDetailSettingsInfo(Player player) {
        player.sendMessage("§e======== §f[세부 설정 정보] §e========");
        player.sendMessage("§7이 플러그인의 일부 세부 설정(강화 확률, 아이템 능력치 등)은");
        player.sendMessage("§7플러그인 폴더 내의 §eupgrade.yml§7, §epylon.yml§7 등");
        player.sendMessage("§7각 기능별 설정 파일에서 직접 수정할 수 있습니다.");
        player.sendMessage("§7잘못된 수정은 오류를 유발할 수 있으니 주의하세요.");
        player.sendMessage("§e===================================");
        sendBackButton(player);
    }


    // --- UI 컴포넌트 생성 메소드 ---

    private void sendCategoryButton(Player player, String name, String command, String hoverText) {
        TextComponent button = new TextComponent("  §a▶ " + name);
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/df settings " + command));
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));
        player.spigot().sendMessage(button);
    }

    private void sendNumberSetting(Player player, String name, String key, int increment) {
        int value = configManager.getConfig().getInt(key);
        TextComponent message = new TextComponent("§7- " + name + ": ");

        TextComponent minusButton = new TextComponent("§c[-]");
        minusButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/df set " + key + " " + (value - increment)));
        minusButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§c-" + increment + " 감소").create()));
        message.addExtra(minusButton);

        message.addExtra(" ");

        TextComponent valueComponent = new TextComponent("§b[" + value + "]");
        valueComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/df set " + key + " "));
        valueComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§e클릭하여 직접 값 입력").create()));
        message.addExtra(valueComponent);

        message.addExtra(" ");

        TextComponent plusButton = new TextComponent("§a[+]");
        plusButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/df set " + key + " " + (value + increment)));
        plusButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§a+" + increment + " 증가").create()));
        message.addExtra(plusButton);

        player.spigot().sendMessage(message);
    }

    private void sendDecimalSetting(Player player, String name, String key, double increment) {
        double value = configManager.getConfig().getDouble(key);
        BigDecimal bdValue = BigDecimal.valueOf(value);
        BigDecimal bdIncrement = BigDecimal.valueOf(increment);

        String displayValue = bdValue.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String minusValue = bdValue.subtract(bdIncrement).setScale(2, RoundingMode.HALF_UP).toPlainString();
        String plusValue = bdValue.add(bdIncrement).setScale(2, RoundingMode.HALF_UP).toPlainString();

        TextComponent message = new TextComponent("§7- " + name + ": ");

        TextComponent minusButton = new TextComponent("§c[-]");
        minusButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/df set " + key + " " + minusValue));
        minusButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§c-" + increment + " 감소").create()));
        message.addExtra(minusButton);

        message.addExtra(" ");

        TextComponent valueComponent = new TextComponent("§b[" + displayValue + "]");
        valueComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/df set " + key + " "));
        valueComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§e클릭하여 직접 값 입력").create()));
        message.addExtra(valueComponent);

        message.addExtra(" ");

        TextComponent plusButton = new TextComponent("§a[+]");
        plusButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/df set " + key + " " + plusValue));
        plusButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§a+" + increment + " 증가").create()));
        message.addExtra(plusButton);

        player.spigot().sendMessage(message);
    }

    private void sendBooleanSetting(Player player, String name, String key) {
        boolean value = configManager.getConfig().getBoolean(key);
        String displayValue = value ? "§a활성화" : "§c비활성화";
        String command = "/df set " + key + " " + !value;

        TextComponent message = new TextComponent("§7- " + name + ": ");
        TextComponent valueComponent = new TextComponent("[" + displayValue + "§r]");
        valueComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        valueComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§e클릭하여 토글").create()));
        message.addExtra(valueComponent);
        player.spigot().sendMessage(message);
    }

    private void sendBackButton(Player player) {
        TextComponent backButton = new TextComponent("§7« 뒤로가기");
        backButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/df settings"));
        backButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7설정 카테고리 선택으로 돌아갑니다.").create()));
        player.spigot().sendMessage(backButton);
    }
}