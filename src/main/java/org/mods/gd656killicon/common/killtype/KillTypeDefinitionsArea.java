package org.mods.gd656killicon.common.killtype;

import org.mods.gd656killicon.common.KillType;

import java.util.List;

/**
 * 全部击杀类型定义（9 条）。
 *
 * <p>displayName 用于配置界面行名；format 为 kill_feed 消息的多语言默认格式模板
 * （注入 config 的 formatKey 键，与现状 lang 文本一致，CAPTURE 的 lzh 为补全）。
 * 新增击杀类型 = 在此列表添加一段定义。</p>
 */
public final class KillTypeDefinitionsArea {
    private KillTypeDefinitionsArea() {
    }

    public static List<KillTypeDefinition> all() {
        return List.of(
            KillTypeDefinition.builder()
                .id("NORMAL").type(KillType.NORMAL)
                .displayName("gd656killicon.killtype.NORMAL.name")
                .format("你 使用 <weapon> 击败了 <target>")
                .formatKey("format_normal")
                .placeholderColorKey("color_normal_placeholder")
                .emphasisColorKey("color_normal_emphasis")
                .enableKey("enable_normal_kill")
                .textureKey("default")
                .bf1TextureKey("default")
                .scrollingSoundSlotId("scrolling_default")
                .build(),
            KillTypeDefinition.builder()
                .id("HEADSHOT").type(KillType.HEADSHOT)
                .displayName("gd656killicon.killtype.HEADSHOT.name")
                .format("你 使用 <weapon> 命中头部击败了 <target>")
                .formatKey("format_headshot")
                .placeholderColorKey("color_headshot_placeholder")
                .emphasisColorKey("color_headshot_emphasis")
                .enableKey("enable_headshot_kill")
                .ringEnableKey("enable_ring_effect_headshot")
                .textureKey("headshot")
                .bf1TextureKey("headshot")
                .scrollingSoundSlotId("scrolling_headshot")
                .build(),
            KillTypeDefinition.builder()
                .id("EXPLOSION").type(KillType.EXPLOSION)
                .displayName("gd656killicon.killtype.EXPLOSION.name")
                .format("你 使用 <weapon> 炸死了 <target>")
                .formatKey("format_explosion")
                .placeholderColorKey("color_explosion_placeholder")
                .emphasisColorKey("color_explosion_emphasis")
                .enableKey("enable_explosion_kill")
                .ringEnableKey("enable_ring_effect_explosion")
                .textureKey("explosion")
                .bf1TextureKey("explosion")
                .scrollingSoundSlotId("scrolling_explosion")
                .build(),
            KillTypeDefinition.builder()
                .id("CRIT").type(KillType.CRIT)
                .displayName("gd656killicon.killtype.CRIT.name")
                .format("你 使用 <weapon> 劈死了 <target>")
                .formatKey("format_crit")
                .placeholderColorKey("color_crit_placeholder")
                .emphasisColorKey("color_crit_emphasis")
                .enableKey("enable_crit_kill")
                .ringEnableKey("enable_ring_effect_crit")
                .textureKey("crit")
                .bf1TextureKey("crit")
                .scrollingSoundSlotId("scrolling_crit")
                .build(),
            KillTypeDefinition.builder()
                .id("ASSIST").type(KillType.ASSIST)
                .displayName("gd656killicon.killtype.ASSIST.name")
                .format("你 助攻击败了 <target>")
                .formatKey("format_assist")
                .placeholderColorKey("color_assist_placeholder")
                .emphasisColorKey("color_assist_emphasis")
                .enableKey("enable_assist_kill")
                .textureKey("assist")
                .bf1TextureKey("default")
                .scrollingSoundSlotId("scrolling_assist")
                .build(),
            KillTypeDefinition.builder()
                .id("DESTROY_VEHICLE").type(KillType.DESTROY_VEHICLE)
                .displayName("gd656killicon.killtype.DESTROY_VEHICLE.name")
                .format("你 使用 <weapon> 摧毁了敌方装甲 <target>")
                .formatKey("format_destroy_vehicle")
                .placeholderColorKey("color_destroy_vehicle_placeholder")
                .emphasisColorKey("color_destroy_vehicle_emphasis")
                .enableKey("enable_destroy_vehicle_kill")
                .textureKey("destroy_vehicle")
                .bf1TextureKey("destroy_vehicle")
                .scrollingSoundSlotId("scrolling_vehicle")
                .build(),
            KillTypeDefinition.builder()
                .id("CAPTURE").type(KillType.CAPTURE)
                .displayName("gd656killicon.killtype.CAPTURE.name")
                .format("成功控制并占领据点<weapon> | <target>")
                .formatKey("format_capture")
                .placeholderColorKey("color_capture_placeholder")
                .emphasisColorKey("color_capture_emphasis")
                .enableKey("enable_capture_kill")
                .textureKey("capture")
                .bf1TextureKey("default")
                .scrollingSoundSlotId("scrolling_assist")
                .build(),
            KillTypeDefinition.builder()
                .id("VEHICLE_DESTROY_ASSIST").type(KillType.VEHICLE_DESTROY_ASSIST)
                .displayName("gd656killicon.killtype.VEHICLE_DESTROY_ASSIST.name")
                .format("你 使用 <weapon> 击败了 <target>")
                .formatKey("format_normal")
                .placeholderColorKey("color_normal_placeholder")
                .emphasisColorKey("color_normal_emphasis")
                .textureKey("vehicle_destroy_assist")
                .bf1TextureKey("default")
                .scrollingSoundSlotId("scrolling_assist")
                .build(),
            KillTypeDefinition.builder()
                .id("MEDIC").type(KillType.MEDIC)
                .displayName("gd656killicon.killtype.MEDIC.name")
                .format("你 使用 <weapon> 击败了 <target>")
                .formatKey("format_normal")
                .placeholderColorKey("color_normal_placeholder")
                .emphasisColorKey("color_normal_emphasis")
                .textureKey("medic")
                .bf1TextureKey("default")
                .scrollingSoundSlotId("scrolling_assist")
                .build(),
            KillTypeDefinition.builder()
                .id("SPOT_ASSIST").type(KillType.SPOT_ASSIST)
                .displayName("gd656killicon.killtype.SPOT_ASSIST.name")
                .format("你 标记的 <target> 被队友击杀")
                .formatKey("format_spot_assist")
                .placeholderColorKey("color_spot_assist_placeholder")
                .emphasisColorKey("color_spot_assist_emphasis")
                .enableKey("enable_spot_assist_kill")
                .textureKey("spot_assist")
                .bf1TextureKey("default")
                .scrollingSoundSlotId("scrolling_assist")
                .build(),
            KillTypeDefinition.builder()
                .id("RESCUE").type(KillType.RESCUE)
                .displayName("gd656killicon.killtype.RESCUE.name")
                .format("救援玩家 <target>")
                .formatKey("format_rescue")
                .placeholderColorKey("color_rescue_placeholder")
                .emphasisColorKey("color_rescue_emphasis")
                .enableKey("enable_rescue_kill")
                .textureKey("medic")
                .bf1TextureKey("default")
                .scrollingSoundSlotId("scrolling_assist")
                .build()
        );
    }
}
