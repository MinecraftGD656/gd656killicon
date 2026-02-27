package org.mods.gd656killicon.client.render.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.effect.DigitalScrollEffect;
import org.mods.gd656killicon.client.render.effect.TextScrambleEffect;
import org.mods.gd656killicon.common.BonusType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderer for the bonus list element.
 * Displays a list of bonus scores with support for merging identical items,
 * digital scroll animation, and smooth layout transitions.
 * Implements the Singleton pattern.
 */
public class BonusListRenderer implements IHudRenderer {

    private static final float COMBO_ANIMATION_DURATION_MULTIPLIER = 1.25f;
    private static final float STREAK_ANIMATION_DURATION_MULTIPLIER = 5.0f;     private static final float ALPHA_THRESHOLD = 0.05f;
    private static final float GLOW_OFFSET = 0.3f;
    private static final long KILL_FEED_ENTRY_ANIMATION_DURATION = 350L;
    private static final float KILL_FEED_ENTRY_SCALE_START = 1.8f;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("<(\\w+)>");

    private static final BonusListRenderer INSTANCE = new BonusListRenderer();
    
    private static final Map<Integer, FormatConfig> TYPE_CONFIGS = new HashMap<>();
    
    private record FormatConfig(String configKey, String defaultLangKey) {}
    
    static {
        registerConfig(BonusType.DAMAGE, "format_damage", "gd656killicon.client.format.bonus_damage");
        registerConfig(BonusType.KILL, "format_kill", "gd656killicon.client.format.bonus_kill");
        registerConfig(BonusType.EXPLOSION, "format_explosion_damage", "gd656killicon.client.format.bonus_explosion");
        registerConfig(BonusType.HEADSHOT, "format_headshot_damage", "gd656killicon.client.format.bonus_headshot");
        registerConfig(BonusType.CRIT, "format_crit_damage", "gd656killicon.client.format.bonus_crit");
        registerConfig(BonusType.KILL_EXPLOSION, "format_kill_explosion", "gd656killicon.client.format.bonus_kill_explosion");
        registerConfig(BonusType.KILL_HEADSHOT, "format_kill_headshot", "gd656killicon.client.format.bonus_kill_headshot");
        registerConfig(BonusType.KILL_CRIT, "format_kill_crit", "gd656killicon.client.format.bonus_kill_crit");
        registerConfig(BonusType.KILL_COMBO, "format_combo", "gd656killicon.client.format.bonus_combo");
        registerConfig(BonusType.KILL_LONG_DISTANCE, "format_kill_long_distance", "gd656killicon.client.format.bonus_kill_long_distance");
        registerConfig(BonusType.KILL_INVISIBLE, "format_kill_invisible", "gd656killicon.client.format.bonus_kill_invisible");
        registerConfig(BonusType.ASSIST, "format_assist", "gd656killicon.client.format.bonus_assist");
        registerConfig(BonusType.DESPERATE_COUNTERATTACK, "format_desperate_counterattack", "gd656killicon.client.format.bonus_desperate_counterattack");
        registerConfig(BonusType.AVENGE, "format_avenge", "gd656killicon.client.format.bonus_avenge");
        registerConfig(BonusType.SHOCKWAVE, "format_shockwave", "gd656killicon.client.format.bonus_shockwave");
        registerConfig(BonusType.BLIND_KILL, "format_blind_kill", "gd656killicon.client.format.bonus_blind_kill");
        registerConfig(BonusType.BUFF_KILL, "format_buff_kill", "gd656killicon.client.format.bonus_buff_kill");
        registerConfig(BonusType.DEBUFF_KILL, "format_debuff_kill", "gd656killicon.client.format.bonus_debuff_kill");
        registerConfig(BonusType.BOTH_BUFF_DEBUFF_KILL, "format_both_buff_debuff_kill", "gd656killicon.client.format.bonus_both_buff_debuff_kill");
        registerConfig(BonusType.LAST_BULLET_KILL, "format_last_bullet_kill", "gd656killicon.client.format.bonus_last_bullet_kill");
        registerConfig(BonusType.ONE_BULLET_MULTI_KILL, "format_one_bullet_multi_kill", "gd656killicon.client.format.bonus_one_bullet_multi_kill");
        registerConfig(BonusType.EFFORTLESS_KILL, "format_effortless_kill", "gd656killicon.client.format.bonus_effortless_kill");
        registerConfig(BonusType.BACKSTAB_KILL, "format_backstab", "gd656killicon.client.format.bonus_backstab");
        registerConfig(BonusType.BACKSTAB_MELEE_KILL, "format_backstab_melee", "gd656killicon.client.format.bonus_backstab_melee");
        registerConfig(BonusType.BRAVE_RETURN, "format_brave_return", "gd656killicon.client.format.bonus_brave_return");
        registerConfig(BonusType.BERSERKER, "format_berserker", "gd656killicon.client.format.bonus_berserker");
        registerConfig(BonusType.INTERRUPTED_STREAK, "format_interrupted_streak", "gd656killicon.client.format.bonus_interrupted_streak");
        registerConfig(BonusType.LEAVE_IT_TO_ME, "format_leave_it_to_me", "gd656killicon.client.format.bonus_leave_it_to_me");
        registerConfig(BonusType.JUSTICE_FROM_ABOVE, "format_justice_from_above", "gd656killicon.client.format.bonus_justice_from_above");
        registerConfig(BonusType.ABSOLUTE_AIR_CONTROL, "format_absolute_air_control", "gd656killicon.client.format.bonus_absolute_air_control");
        registerConfig(BonusType.SAVIOR, "format_savior", "gd656killicon.client.format.bonus_savior");
        registerConfig(BonusType.SLAY_THE_LEADER, "format_slay_the_leader", "gd656killicon.client.format.bonus_slay_the_leader");
        registerConfig(BonusType.PURGE, "format_purge", "gd656killicon.client.format.bonus_purge");
        registerConfig(BonusType.QUICK_SWITCH, "format_quick_switch", "gd656killicon.client.format.bonus_quick_switch");
        registerConfig(BonusType.SEIZE_OPPORTUNITY, "format_seize_opportunity", "gd656killicon.client.format.bonus_seize_opportunity");
        registerConfig(BonusType.BLOODTHIRSTY, "format_bloodthirsty", "gd656killicon.client.format.bonus_bloodthirsty");
        registerConfig(BonusType.MERCILESS, "format_merciless", "gd656killicon.client.format.bonus_merciless");
        registerConfig(BonusType.VALIANT, "format_valiant", "gd656killicon.client.format.bonus_valiant");
        registerConfig(BonusType.FIERCE, "format_fierce", "gd656killicon.client.format.bonus_fierce");
        registerConfig(BonusType.SAVAGE, "format_savage", "gd656killicon.client.format.bonus_savage");
        registerConfig(BonusType.POTATO_AIM, "format_potato_aim", "gd656killicon.client.format.bonus_potato_aim");
        registerConfig(BonusType.HIT_VEHICLE_ARMOR, "format_hit_vehicle_armor", "gd656killicon.client.format.bonus_hit_vehicle_armor");
        registerConfig(BonusType.DESTROY_VEHICLE, "format_destroy_vehicle", "gd656killicon.client.format.bonus_destroy_vehicle");
        registerConfig(BonusType.VALUE_TARGET_DESTROYED, "format_value_target_destroyed", "gd656killicon.client.format.bonus_value_target_destroyed");
        registerConfig(BonusType.VEHICLE_REPAIR, "format_vehicle_repair", "gd656killicon.client.format.bonus_vehicle_repair");
        registerConfig(BonusType.LOCKED_TARGET, "format_locked_target", "gd656killicon.client.format.bonus_locked_target");
        registerConfig(BonusType.HOLD_POSITION, "format_hold_position", "gd656killicon.client.format.bonus_hold_position");
        registerConfig(BonusType.CHARGE_ASSAULT, "format_charge_assault", "gd656killicon.client.format.bonus_charge_assault");
        registerConfig(BonusType.FIRE_SUPPRESSION, "format_fire_suppression", "gd656killicon.client.format.bonus_fire_suppression");
        registerConfig(BonusType.DESTROY_BLOCK, "format_destroy_block", "gd656killicon.client.format.bonus_destroy_block");
        registerConfig(BonusType.SPOTTING, "format_spotting", "gd656killicon.client.format.bonus_spotting");
        registerConfig(BonusType.SPOTTING_KILL, "format_spotting_kill", "gd656killicon.client.format.bonus_spotting_kill");
        registerConfig(BonusType.SPOTTING_TEAM_ASSIST, "format_spotting_team_assist", "gd656killicon.client.format.bonus_spotting_team_assist");
    }

    private static void registerConfig(int type, String configKey, String defaultFormatKey) {
        TYPE_CONFIGS.put(type, new FormatConfig(configKey, defaultFormatKey));
    }

    private final List<BonusItem> items = new ArrayList<>();
    private final Deque<BonusItem> pendingQueue = new ArrayDeque<>();
    
    private float animationDuration = 0.5f;
    private float animationRefreshRate = 0.01f;
    private boolean enableTextScrolling = false;
    private float textScrollingDurationMultiplier = 1.5f;
    private float textScrollingRefreshRate = 0.05f;
    private long enterAnimationDuration = 200L;
    private boolean enableTextSweepAnimation = false;
    private long mergeWindowDuration = 500L;
    private float animationSpeed = 10.0f;
    private float killBonusScale = 1.0f;
    private boolean enableKillFeed = false;
    private String killFeedFormat = "[<weapon>] <target> +<score>";
    private String killFeedVictimColor = "#FF0000";
    private boolean enableDigitalScroll = true;
    private boolean enableGlowEffect = false;
    private float glowIntensity = 0.5f;
    private int normalTextColor = 0xFFFFFF;
    
    private long lastProcessTime = 0;
    private long lastRenderTime = 0;
    private long nextFadeTriggerTime = 0;

    private BonusListRenderer() {}

    public static BonusListRenderer getInstance() {
        return INSTANCE;
    }

    public static String getEffectiveFormat(int type, String extraData) {
        JsonObject config = ElementConfigManager.getElementConfig(ConfigManager.getCurrentPresetId(), "subtitle/bonus_list");
        return getEffectiveFormat(type, extraData, config);
    }

    public static String getEffectiveFormat(int type, String extraData, JsonObject config) {
        if (type == BonusType.KILL_COMBO && config != null && config.has("enable_special_streak_subtitles") && config.get("enable_special_streak_subtitles").getAsBoolean()) {
            try {
                int combo = Integer.parseInt(extraData);
                if (combo >= 2 && combo <= 8) {
                    String format = "gd656killicon.client.format.bonus_combo_" + combo;
                    if (net.minecraft.client.resources.language.I18n.exists(format)) {
                        return net.minecraft.client.resources.language.I18n.get(format);
                    }
                    return format;
                }
            } catch (NumberFormatException ignored) {}
        }

        String format;
        FormatConfig fmtConfig = TYPE_CONFIGS.get(type);
        if (fmtConfig != null) {
            format = (config != null && config.has(fmtConfig.configKey)) ? config.get(fmtConfig.configKey).getAsString() : fmtConfig.defaultLangKey;
        } else {
            format = "Bonus +<score>";
        }

        if (net.minecraft.client.resources.language.I18n.exists(format)) {
            format = net.minecraft.client.resources.language.I18n.get(format);
        }
        return format;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        JsonObject config = ElementConfigManager.getElementConfig(ConfigManager.getCurrentPresetId(), "subtitle/bonus_list");
        if (config == null || !config.get("visible").getAsBoolean()) return;
        int xOffset = config.get("x_offset").getAsInt();
        int yOffset = config.get("y_offset").getAsInt();
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float centerX = screenWidth / 2.0f + xOffset;
        float startY = screenHeight - yOffset;
        renderInternal(guiGraphics, config, centerX, startY);
    }

    public void renderAt(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY) {
        JsonObject config = ElementConfigManager.getElementConfig(ConfigManager.getCurrentPresetId(), "subtitle/bonus_list");
        if (config == null || !config.get("visible").getAsBoolean()) return;
        renderInternal(guiGraphics, config, centerX, centerY);
    }

    @Override
    public void trigger(TriggerContext context) {
        JsonObject config = ElementConfigManager.getElementConfig(ConfigManager.getCurrentPresetId(), "subtitle/bonus_list");
        if (config == null || !config.has("visible") || !config.get("visible").getAsBoolean()) return;

        loadConfig(config);

        ParsedData parsed = ParsedData.parse(context.extraData());
        String extraData = parsed.extraData;
        
        String format = getEffectiveFormat(context.type(), extraData);

        int specialColor = parseSpecialColor(config);

        long now = System.currentTimeMillis();
        
        String weaponName = "";
        String victimName = "";
        
        Minecraft mc = Minecraft.getInstance();
        
        if (parsed.victimName != null && !parsed.victimName.isEmpty()) {
            if (net.minecraft.client.resources.language.I18n.exists(parsed.victimName)) {
                victimName = net.minecraft.client.resources.language.I18n.get(parsed.victimName);
            } else {
                victimName = parsed.victimName;
            }
        } else if (mc.level != null && context.entityId() != -1) {
            Entity entity = mc.level.getEntity(context.entityId());
            if (entity != null) {
                victimName = entity.getDisplayName().getString();
            } else {
                victimName = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.unknown");
            }
        } else {
            victimName = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.unknown");
        }
        
        if (mc.player != null) {
            if (mc.player.getVehicle() != null) {
                weaponName = mc.player.getVehicle().getDisplayName().getString();
            } else {
                ItemStack held = mc.player.getMainHandItem();
                if (held.isEmpty()) {
                    if (net.minecraft.client.resources.language.I18n.exists("gd656killicon.client.text.bare_hand")) {
                        weaponName = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.bare_hand");
                    } else {
                        weaponName = "Bare Hand";
                    }
                } else {
                    weaponName = held.getHoverName().getString();
                }
            }
        }

        synchronized (items) {
            boolean isComboFormat = format.contains("<combo>");

            for (BonusItem item : items) {
                if (canMerge(item, format, isComboFormat, extraData, now)) {
                    item.merge(parsed.score, extraData, isComboFormat, format);
                    nextFadeTriggerTime = now + (long)(config.get("display_duration").getAsFloat() * 1000);
                    return;
                }
            }
            
            BonusItem newItem = new BonusItem(format, parsed.score, extraData, context.type(), specialColor, weaponName, victimName);
            pendingQueue.add(newItem);
        }
    }

    public void triggerPreview(int type, float score, String extraData, String weaponName, String victimName, JsonObject config) {
        if (config == null || !config.has("visible") || !config.get("visible").getAsBoolean()) return;

        loadConfig(config);

        String resolvedExtraData = extraData != null ? extraData : "";
        String format = getEffectiveFormat(type, resolvedExtraData, config);
        int specialColor = parseSpecialColor(config);
        long now = System.currentTimeMillis();

        synchronized (items) {
            boolean isComboFormat = format.contains("<combo>");

            for (BonusItem item : items) {
                if (canMerge(item, format, isComboFormat, resolvedExtraData, now)) {
                    item.merge(score, resolvedExtraData, isComboFormat, format);
                    nextFadeTriggerTime = now + (long)(config.get("display_duration").getAsFloat() * 1000);
                    return;
                }
            }

            BonusItem newItem = new BonusItem(format, score, resolvedExtraData, type, specialColor, weaponName, victimName);
            pendingQueue.add(newItem);
        }
    }

    public void resetPreview() {
        synchronized (items) {
            items.clear();
            pendingQueue.clear();
        }
        lastProcessTime = 0;
        lastRenderTime = 0;
        nextFadeTriggerTime = 0;
    }

    
    private void loadConfig(JsonObject config) {
        try {
            this.animationDuration = config.has("animation_duration") ? config.get("animation_duration").getAsFloat() : 0.5f;
            this.animationRefreshRate = config.has("animation_refresh_rate") ? config.get("animation_refresh_rate").getAsFloat() : 0.01f;
            this.enableTextScrolling = config.has("enable_text_scrolling") && config.get("enable_text_scrolling").getAsBoolean();
            this.textScrollingDurationMultiplier = config.has("text_scrolling_duration_multiplier") ? config.get("text_scrolling_duration_multiplier").getAsFloat() : 1.5f;
            this.textScrollingRefreshRate = config.has("text_scrolling_refresh_rate") ? config.get("text_scrolling_refresh_rate").getAsFloat() : 0.05f;
            this.enterAnimationDuration = config.has("enter_animation_duration") ? (long)(config.get("enter_animation_duration").getAsFloat() * 1000) : 200L;
            this.enableTextSweepAnimation = config.has("enable_text_sweep_animation") && config.get("enable_text_sweep_animation").getAsBoolean();
            this.mergeWindowDuration = config.has("merge_window_duration") ? (long)(config.get("merge_window_duration").getAsFloat() * 1000) : 500L;
            this.animationSpeed = config.has("animation_speed") ? config.get("animation_speed").getAsFloat() : 10.0f;
            this.killBonusScale = config.has("kill_bonus_scale") ? config.get("kill_bonus_scale").getAsFloat() : 1.0f;
            this.enableKillFeed = config.has("enable_kill_feed") && config.get("enable_kill_feed").getAsBoolean();
            this.killFeedFormat = config.has("kill_feed_format") ? config.get("kill_feed_format").getAsString() : "[<weapon>] <target> +<score>";
            this.killFeedVictimColor = config.has("kill_feed_victim_color") ? config.get("kill_feed_victim_color").getAsString() : "#FF0000";
            this.enableDigitalScroll = !config.has("enable_digital_scroll") || config.get("enable_digital_scroll").getAsBoolean();
            this.enableGlowEffect = config.has("enable_glow_effect") && config.get("enable_glow_effect").getAsBoolean();
            this.glowIntensity = config.has("glow_intensity") ? config.get("glow_intensity").getAsFloat() : 0.5f;
            this.normalTextColor = parseHexColor(config, "color_normal_text", 0xFFFFFF);
        } catch (Exception e) {
            this.animationDuration = 0.5f;
            this.animationRefreshRate = 0.01f;
            this.enableTextScrolling = false;
            this.enterAnimationDuration = 200L;
            this.enableTextSweepAnimation = false;
            this.mergeWindowDuration = 500L;
            this.animationSpeed = 10.0f;
            this.killBonusScale = 1.0f;
            this.enableKillFeed = false;
            this.enableDigitalScroll = true;
            this.enableGlowEffect = false;
            this.glowIntensity = 0.5f;
            this.normalTextColor = 0xFFFFFF;
        }
    }

    private void renderInternal(GuiGraphics guiGraphics, JsonObject config, float baseCenterX, float baseBottomY) {
        loadConfig(config);
        float scale = config.get("scale").getAsFloat();
        int lineSpacing = config.get("line_spacing").getAsInt();
        int maxLines = config.get("max_lines").getAsInt();
        float displayDuration = config.get("display_duration").getAsFloat() * 1000;
        float fadeOutInterval = config.get("fade_out_interval").getAsFloat() * 1000;

        Minecraft mc = Minecraft.getInstance();
        int centerX = Math.round(baseCenterX);
        int startY = Math.round(baseBottomY);

        long now = System.currentTimeMillis();

        if (lastRenderTime == 0) lastRenderTime = now;
        float dt = (now - lastRenderTime) / 1000.0f;
        lastRenderTime = now;

        processPendingQueue(now, displayDuration);
        processIdleFade(now, fadeOutInterval);
        renderItems(guiGraphics, mc, scale, centerX, startY, lineSpacing, maxLines, now, dt);
    }
    
    private int parseSpecialColor(JsonObject config) {
        if (config.has("color_special_placeholder")) {
            String colorStr = config.get("color_special_placeholder").getAsString();
            try {
                if (colorStr.startsWith("#")) {
                    return Integer.parseInt(colorStr.substring(1), 16);
                } else {
                    return Integer.parseInt(colorStr, 16);
                }
            } catch (NumberFormatException ignored) {}
        }
        return 0xD4B800;     }

    private int parseHexColor(JsonObject config, String key, int fallback) {
        if (config.has(key)) {
            String colorStr = config.get(key).getAsString();
            try {
                String hex = colorStr.startsWith("#") ? colorStr.substring(1) : colorStr;
                return Integer.parseInt(hex, 16);
            } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    private boolean canMerge(BonusItem item, String format, boolean isComboFormat, String extraData, long now) {
        if (item.isFading || !item.formatString.equals(format) || !item.hasPlaceholder) {
            return false;
        }

        if (isComboFormat) {
            return true;
        }

        boolean extraDataMatches = (item.extraData == null || item.extraData.isEmpty()) && 
                                  (extraData == null || extraData.isEmpty()) ||
                                  (item.extraData != null && item.extraData.equals(extraData));
        if (!extraDataMatches) return false;

        return now - item.spawnTime <= this.mergeWindowDuration;
    }

    private void processPendingQueue(long now, float displayDuration) {
        if (!pendingQueue.isEmpty()) {
            if (now - lastProcessTime >= 100) { 
                synchronized (items) {
                    BonusItem newItem = pendingQueue.poll();
                    if (newItem != null) {
                        newItem.spawnTime = now;                         items.add(0, newItem);
                    }
                }
                lastProcessTime = now;
                nextFadeTriggerTime = now + (long)displayDuration;
            }
        }
    }

    private void processIdleFade(long now, float fadeOutInterval) {
        if (pendingQueue.isEmpty() && now > nextFadeTriggerTime && !items.isEmpty()) {
            synchronized (items) {
                for (int i = items.size() - 1; i >= 0; i--) {
                    BonusItem item = items.get(i);
                    if (!item.isFading) {
                        item.isFading = true;
                        item.fadeStartTime = now;
                        nextFadeTriggerTime += (long)fadeOutInterval;
                        return;
                    }
                }
                nextFadeTriggerTime = now + (long)fadeOutInterval;
            }
        }
    }

    private void renderItems(GuiGraphics guiGraphics, Minecraft mc, float scale, int centerX, int startY, 
                             int lineSpacing, int maxLines, long now, float dt) {
        JsonObject config = ElementConfigManager.getElementConfig(ConfigManager.getCurrentPresetId(), "subtitle/bonus_list");
        boolean alignLeft = config != null && config.has("align_left") && config.get("align_left").getAsBoolean();
        boolean alignRight = config != null && config.has("align_right") && config.get("align_right").getAsBoolean();
        
        boolean effectiveAlignLeft = alignLeft && !alignRight;
        boolean effectiveAlignRight = !alignLeft && alignRight;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);
        float scaledCenterX = centerX / scale;
        float scaledStartY = startY / scale;

        synchronized (items) {
            Iterator<BonusItem> iterator = items.iterator();
            int index = 0;

            while (iterator.hasNext()) {
                BonusItem item = iterator.next();
                
                item.update(now, dt, index * lineSpacing);
                
                float alpha = calculateAlpha(item, now, maxLines, lineSpacing);
                if (alpha > ALPHA_THRESHOLD) {
                    item.render(guiGraphics, mc, scaledCenterX, scaledStartY, alpha, effectiveAlignLeft, effectiveAlignRight, mc.getWindow().getGuiScaledWidth() / scale, scale);
                }

                if (shouldRemove(item, alpha, maxLines, lineSpacing)) {
                    iterator.remove();
                    continue;
                }
                index++;
            }
        }

        guiGraphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private float calculateAlpha(BonusItem item, long now, int maxLines, int lineSpacing) {
        float alpha = 1.0f;
        
        if (!this.enableTextSweepAnimation) {
            long timeSinceSpawn = now - item.spawnTime;
            float fadeInProgress = timeSinceSpawn / (float)this.enterAnimationDuration;
            alpha *= Math.min(1.0f, fadeInProgress);
        }
        
        float lineIndex = item.currentY / lineSpacing;
        float fadeRange = Math.max(1.0f, (float)maxLines - 1.0f);
        float posFadeProgress = lineIndex / fadeRange;
        alpha *= Math.max(0.0f, 1.0f - posFadeProgress);

        if (item.isFading) {
            long fadeElapsed = now - item.fadeStartTime;
            float fadeDuration = 300.0f;
            float fadeProgress = fadeElapsed / fadeDuration;
            alpha *= Math.max(0.0f, 1.0f - fadeProgress);
        }

        return alpha;
    }
    
    private boolean shouldRemove(BonusItem item, float alpha, int maxLines, int lineSpacing) {
        float lineIndex = item.currentY / lineSpacing;
        boolean positionHidden = lineIndex >= maxLines;
        boolean fadeHidden = item.isFading && alpha <= 0.01f;
        return positionHidden || fadeHidden;
    }

    private void drawComponentWithGlow(GuiGraphics guiGraphics, Font font, Component component, int x, int y, int alphaInt) {
        if (this.enableGlowEffect) {
            int glowAlpha = (int)(alphaInt * this.glowIntensity);
            glowAlpha = Math.max(0, Math.min(255, glowAlpha));
            int glowColor = (glowAlpha << 24) | (this.normalTextColor & 0xFFFFFF);
            
            PoseStack poseStack = guiGraphics.pose();
            
            float[][] offsets = {
                {-GLOW_OFFSET, 0}, {GLOW_OFFSET, 0}, {0, -GLOW_OFFSET}, {0, GLOW_OFFSET},
                {-GLOW_OFFSET, -GLOW_OFFSET}, {GLOW_OFFSET, -GLOW_OFFSET},
                {-GLOW_OFFSET, GLOW_OFFSET}, {GLOW_OFFSET, GLOW_OFFSET}
            };
            
            for (float[] offset : offsets) {
                poseStack.pushPose();
                poseStack.translate(offset[0], offset[1], 0);
                guiGraphics.drawString(font, component, x, y, glowColor, false);
                poseStack.popPose();
            }
        }
        int color = (alphaInt << 24) | (this.normalTextColor & 0xFFFFFF);
        guiGraphics.drawString(font, component, x, y, color, true);
    }

    
    private record ParsedData(float score, String extraData, String victimName) {
        static ParsedData parse(String data) {
            float score = 0;
            String extraData = "";
            String victimName = null;
            
            if (data != null && !data.isEmpty()) {
                String[] parts = data.split("\\|", -1);
                if (parts.length > 0) {
                    try {
                        score = Float.parseFloat(parts[0]);
                    } catch (NumberFormatException e) {
                        score = parseScore(parts[0]);
                    }
                }
                if (parts.length > 1) {
                    extraData = parts[1];
                }
                if (parts.length > 2) {
                    victimName = parts[2];
                }
            }
            return new ParsedData(score, extraData, victimName);
        }
        
        private static float parseScore(String data) {
            try {
                Matcher matcher = Pattern.compile("\\+([\\d.]+)").matcher(data);
                if (matcher.find()) return Float.parseFloat(matcher.group(1));
                
                matcher = Pattern.compile("([\\d.]+)").matcher(data);
                String lastMatch = null;
                while (matcher.find()) lastMatch = matcher.group(1);
                
                if (lastMatch != null) return Float.parseFloat(lastMatch);
            } catch (NumberFormatException ignored) {}
            return 0;
        }
    }

    /**
     * Encapsulates a value that can be animated (score, combo, etc.)
     */
    private static class AnimatedStat {
        final DigitalScrollEffect effect;
        float targetValue;
        float flashAlpha;
        float lastDisplayed;
        
        AnimatedStat(float initial, float duration, float refreshRate) {
            this.effect = new DigitalScrollEffect(duration, refreshRate, DigitalScrollEffect.Easing.CUBIC_OUT);
            this.targetValue = initial;
            this.effect.startAnimation(0, initial);
            this.lastDisplayed = 0;
            this.flashAlpha = 0.0f;
        }

        void add(float amount) {
            targetValue += amount;
            effect.startAnimation(effect.getCurrentValue(), targetValue);
        }
        
        void updateMax(float newVal) {
            if (newVal > targetValue) {
                targetValue = newVal;
                effect.startAnimation(effect.getCurrentValue(), targetValue);
            }
        }
        
        void update(long now, float dt) {
            effect.update(now);
            float current = effect.getCurrentValue();
            if (Math.abs(current - lastDisplayed) >= 0.1f) {
                flashAlpha = 1.0f;
                lastDisplayed = current;
            }
            if (flashAlpha > 0) flashAlpha = Math.max(0, flashAlpha - dt * 5.0f);
        }
        
        float getValue(boolean enableScroll) {
            return enableScroll ? effect.getCurrentValue() : targetValue;
        }
    }

    private class BonusItem {
        final String formatString;
        final boolean hasPlaceholder;
        String extraData;
        
        float currentY;
        boolean isFading;
        long fadeStartTime;
        long spawnTime;
        
        final AnimatedStat scoreStat;
        AnimatedStat comboStat;
        AnimatedStat mkStat;
        AnimatedStat distanceStat;
        AnimatedStat streakStat;
        
        int specialColor;
        
        final boolean isKillBonus;
        final float itemScale;
        final boolean showKillFeed;
        final String weaponName;
        final String victimName;
        final String killFeedFormatStr;
        final int killFeedVictimColorVal;
        
        private final List<TextScrambleEffect> scrambleEffects = new ArrayList<>();

        public BonusItem(String format, float initialScore, String extraData, int type, int specialColor, 
                         String weaponName, String victimName) {
            this.formatString = format;
            this.extraData = extraData != null ? extraData : "";
            this.currentY = 0; 
            this.isFading = false;
            this.spawnTime = System.currentTimeMillis();
            this.specialColor = specialColor;
            
            this.isKillBonus = type == BonusType.KILL || type == BonusType.KILL_HEADSHOT || 
                               type == BonusType.KILL_CRIT || type == BonusType.KILL_EXPLOSION;
            
            this.itemScale = this.isKillBonus ? BonusListRenderer.this.killBonusScale : 1.0f;
            this.showKillFeed = this.isKillBonus && BonusListRenderer.this.enableKillFeed;
            
            this.weaponName = weaponName;
            this.victimName = victimName;
            this.killFeedFormatStr = BonusListRenderer.this.killFeedFormat;
            
            int vColor = 0xFF0000;
            try {
                String c = BonusListRenderer.this.killFeedVictimColor;
                if (c.startsWith("#")) c = c.substring(1);
                vColor = Integer.parseInt(c, 16);
            } catch (Exception ignored) {}
            this.killFeedVictimColorVal = vColor;
            
            this.hasPlaceholder = format.contains("<score>");
            this.scoreStat = hasPlaceholder ? new AnimatedStat(initialScore, animationDuration, animationRefreshRate) : null;
            
            initStats(format, extraData);
            initScrambleEffects(format);
        }
        
        private void initScrambleEffects(String format) {
            if (!enableTextScrolling) return;

            Matcher matcher = PLACEHOLDER_PATTERN.matcher(format);
            int lastEnd = 0;
            long duration = (long) (animationDuration * textScrollingDurationMultiplier * 1000);
            long refresh = (long) (textScrollingRefreshRate * 1000);

            while (matcher.find()) {
                String staticPart = format.substring(lastEnd, matcher.start());
                scrambleEffects.add(new TextScrambleEffect(staticPart, duration, refresh, true));
                lastEnd = matcher.end();
            }
            scrambleEffects.add(new TextScrambleEffect(format.substring(lastEnd), duration, refresh, true));
        }
        
        private void initStats(String format, String extraData) {
            if (extraData == null || extraData.isEmpty()) return;
            
            if (format.contains("<combo>")) {
                comboStat = createStat(extraData, COMBO_ANIMATION_DURATION_MULTIPLIER);
            }
            if (format.contains("<multi_kill>")) {
                mkStat = createStat(extraData, STREAK_ANIMATION_DURATION_MULTIPLIER);
            }
            if (format.contains("<distance>")) {
                distanceStat = createStat(extraData, STREAK_ANIMATION_DURATION_MULTIPLIER);
            }
            if (format.contains("<streak>")) {
                streakStat = createStat(extraData, STREAK_ANIMATION_DURATION_MULTIPLIER);
            }
        }
        
        private AnimatedStat createStat(String data, float durationMult) {
            try {
                int val = Integer.parseInt(data);
                return new AnimatedStat(val, animationDuration * durationMult, animationRefreshRate);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public void merge(float score, String newExtraData, boolean isComboFormat, String format) {
            if (scoreStat != null) scoreStat.add(score);
            
            if (newExtraData == null) return;
            
            if (isComboFormat && comboStat != null) {
                try {
                    comboStat.updateMax(Integer.parseInt(newExtraData));
                    this.extraData = String.valueOf((int)comboStat.targetValue);
                } catch (NumberFormatException ignored) {}
            }
            
            if (format.contains("<multi_kill>") && mkStat != null) {
                try {
                    mkStat.updateMax(Integer.parseInt(newExtraData));
                    this.extraData = String.valueOf((int)mkStat.targetValue);
                } catch (NumberFormatException ignored) {}
            }
            
            if (format.contains("<distance>") && distanceStat != null) {
                 try {
                     int dist = Integer.parseInt(newExtraData);
                     if (dist != distanceStat.targetValue) {
                         distanceStat.targetValue = dist;
                         distanceStat.effect.startAnimation(distanceStat.effect.getCurrentValue(), dist);
                         this.extraData = String.valueOf(dist);
                     }
                 } catch (NumberFormatException ignored) {}
            }

            if (format.contains("<streak>") && streakStat != null) {
                try {
                    streakStat.updateMax(Integer.parseInt(newExtraData));
                    this.extraData = String.valueOf((int)streakStat.targetValue);
                } catch (NumberFormatException ignored) {}
            }
        }

        public void update(long now, float dt, float targetY) {
            if (scoreStat != null) scoreStat.update(now, dt);
            if (comboStat != null) comboStat.update(now, dt);
            if (mkStat != null) mkStat.update(now, dt);
            if (distanceStat != null) distanceStat.update(now, dt);
            if (streakStat != null) streakStat.update(now, dt);
            
            float smoothFactor = 1.0f - (float)Math.exp(-BonusListRenderer.this.animationSpeed * dt);
            this.currentY = this.currentY + (targetY - this.currentY) * smoothFactor;
        }

        public void render(GuiGraphics guiGraphics, Minecraft mc, float x, float y, float alpha, boolean alignLeft, boolean alignRight, float screenWidth, float globalScale) {
            Component component = getDisplayComponent();
            
            Component killFeedComponent = null;
            if (this.showKillFeed) {
                killFeedComponent = buildKillFeedComponent();
            }

            int alphaInt = (int)(alpha * 255);
            alphaInt = Math.max(0, Math.min(255, alphaInt));
            int textColor = (alphaInt << 24) | 0xFFFFFF;
            
            int baseTextWidth = mc.font.width(component);
            int feedTextWidth = killFeedComponent != null ? mc.font.width(killFeedComponent) : 0;
            
            long now = System.currentTimeMillis();
            long elapsed = now - this.spawnTime;
            long enterDuration = BonusListRenderer.this.enterAnimationDuration;
            boolean sweepEnabled = BonusListRenderer.this.enableTextSweepAnimation;
            
            guiGraphics.pose().pushPose();
            
            float scaleOriginX = x;
            float scaleOriginY = y + this.currentY + (mc.font.lineHeight / 2.0f);
            
            float currentScale = this.itemScale;
            if (this.showKillFeed && elapsed < KILL_FEED_ENTRY_ANIMATION_DURATION) {
                float progress = (float) elapsed / KILL_FEED_ENTRY_ANIMATION_DURATION;
                float easedProgress = 1.0f - (float) Math.pow(1.0f - progress, 3);                 float animationScaleMultiplier = net.minecraft.util.Mth.lerp(easedProgress, KILL_FEED_ENTRY_SCALE_START, 1.0f);
                currentScale *= animationScaleMultiplier;
            }

            guiGraphics.pose().translate(scaleOriginX, scaleOriginY, 0);
            guiGraphics.pose().scale(currentScale, currentScale, 1.0f);
            guiGraphics.pose().translate(-scaleOriginX, -scaleOriginY, 0);
            
            boolean renderOriginal = true;
            boolean renderFeed = false;
            float feedProgress = 0.0f;
            
            if (this.showKillFeed) {
                long feedStart = enterDuration * 4;
                if (elapsed > feedStart) {
                    feedProgress = (elapsed - feedStart) / (float)enterDuration;
                    feedProgress = Math.max(0.0f, Math.min(1.0f, feedProgress));
                    renderFeed = true;
                    if (feedProgress >= 1.0f) renderOriginal = false;
                }
            }
            
            float totalScale = globalScale * this.itemScale;
            float screenAnchorX = x * globalScale;
            float pivotYScreen = scaleOriginY * globalScale;
            
            if (renderOriginal) {
                float drawX = calculateDrawX(x, baseTextWidth, alignLeft, alignRight);
                float drawY = y + this.currentY;
                
                if (sweepEnabled) {
                    float entryProgress = elapsed / (float)enterDuration;
                    entryProgress = Math.max(0.0f, Math.min(1.0f, entryProgress));
                    
                    float screenWidthPx = baseTextWidth * totalScale;
                    float screenLeft = calculateScreenLeft(screenAnchorX, screenWidthPx, alignLeft, alignRight);
                    float screenRight = screenLeft + screenWidthPx;
                    
                    float entryLeft = screenRight - (screenWidthPx * entryProgress);
                    
                    float exitLeft = screenLeft + (screenWidthPx * feedProgress);
                    
                    int scLeft = (int)Math.max(entryLeft, exitLeft);
                    int scRight = (int)screenRight;
                    int scY = (int)(pivotYScreen - (mc.font.lineHeight / 2.0f * totalScale));
                    int scH = (int)(mc.font.lineHeight * totalScale);
                    
                    if (scRight > scLeft) {
                        guiGraphics.enableScissor(Math.max(0, scLeft), Math.max(0, scY), scRight, scY + scH + 2);
                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(drawX, drawY, 0);
                        BonusListRenderer.this.drawComponentWithGlow(guiGraphics, mc.font, component, 0, 0, alphaInt);
                        guiGraphics.pose().popPose();
                        guiGraphics.disableScissor();
                    }
                } else {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(drawX, drawY, 0);
                    BonusListRenderer.this.drawComponentWithGlow(guiGraphics, mc.font, component, 0, 0, alphaInt);
                    guiGraphics.pose().popPose();
                }
            }
            
            if (renderFeed && killFeedComponent != null) {
                float drawX = calculateDrawX(x, feedTextWidth, alignLeft, alignRight);
                float drawY = y + this.currentY;
                
                if (sweepEnabled) {
                    float screenWidthPx = feedTextWidth * totalScale;
                    float screenLeft = calculateScreenLeft(screenAnchorX, screenWidthPx, alignLeft, alignRight);
                    float revealRight = screenLeft + (screenWidthPx * feedProgress);
                    
                    int scLeft = (int)screenLeft;
                    int scRight = (int)revealRight;
                    int scY = (int)(pivotYScreen - (mc.font.lineHeight / 2.0f * totalScale));
                    int scH = (int)(mc.font.lineHeight * totalScale);
                    
                    if (scRight > scLeft) {
                        guiGraphics.enableScissor(Math.max(0, scLeft), Math.max(0, scY), scRight, scY + scH + 2);
                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(drawX, drawY, 0);
                        BonusListRenderer.this.drawComponentWithGlow(guiGraphics, mc.font, killFeedComponent, 0, 0, alphaInt);
                        guiGraphics.pose().popPose();
                        guiGraphics.disableScissor();
                    }
                } else {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(drawX, drawY, 0);
                    BonusListRenderer.this.drawComponentWithGlow(guiGraphics, mc.font, killFeedComponent, 0, 0, alphaInt);
                    guiGraphics.pose().popPose();
                }
            }
            
            guiGraphics.pose().popPose();
        }

        private float calculateDrawX(float x, int width, boolean alignLeft, boolean alignRight) {
            if (alignLeft) return x;
            if (alignRight) return x - width;
            return x - width / 2.0f;
        }
        
        private float calculateScreenLeft(float anchorX, float widthPx, boolean alignLeft, boolean alignRight) {
            if (alignLeft) return anchorX;
            if (alignRight) return anchorX - widthPx;
            return anchorX - widthPx / 2.0f;
        }

        private Component buildKillFeedComponent() {
            MutableComponent root = Component.empty();
            String fmt = this.killFeedFormatStr;
            float score = scoreStat != null ? scoreStat.getValue(BonusListRenderer.this.enableDigitalScroll) : 0;
            
            
            Matcher m = Pattern.compile("(<weapon>|<target>|<score>)").matcher(fmt);
            int lastEnd = 0;
            
            while (m.find()) {
                String staticPart = fmt.substring(lastEnd, m.start());
                if (!staticPart.isEmpty()) {
                    root.append(Component.literal(staticPart).withStyle(Style.EMPTY.withColor(BonusListRenderer.this.normalTextColor)));
                }
                
                String tag = m.group(1);
                if ("<weapon>".equals(tag)) {
                    root.append(Component.literal(this.weaponName).withStyle(Style.EMPTY.withColor(BonusListRenderer.this.normalTextColor)));
                } else if ("<target>".equals(tag)) {
                    String translatedVName = net.minecraft.client.resources.language.I18n.get(this.victimName);
                    root.append(Component.literal(translatedVName).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.killFeedVictimColorVal))));
                } else if ("<score>".equals(tag)) {
                    String scoreStr;
                    if (Math.abs(score) < 1.0f && Math.abs(score) > 0.001f) {
                        scoreStr = String.format("%.1f", score);
                    } else {
                        scoreStr = String.valueOf(Math.round(score));
                    }
                    root.append(Component.literal(scoreStr).withStyle(Style.EMPTY.withColor(BonusListRenderer.this.normalTextColor)));
                }
                
                lastEnd = m.end();
            }
            
            String tail = fmt.substring(lastEnd);
            if (!tail.isEmpty()) {
                root.append(Component.literal(tail).withStyle(Style.EMPTY.withColor(BonusListRenderer.this.normalTextColor)));
            }
            
            return root;
        }

        private Component getDisplayComponent() {
            MutableComponent root = Component.empty();
            
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(formatString);
            int lastEnd = 0;
            int scrambleIdx = 0;
            
            while (matcher.find()) {
                String staticPart = formatString.substring(lastEnd, matcher.start());
                if (enableTextScrolling && scrambleIdx < scrambleEffects.size()) {
                    TextScrambleEffect effect = scrambleEffects.get(scrambleIdx);
                    root.append(effect != null ? effect.getCurrentText() : staticPart);
                } else {
                    root.append(staticPart);
                }
                scrambleIdx++;

                String type = matcher.group(1);
                
                switch (type) {
                    case "score" -> {
                        float score = scoreStat != null ? scoreStat.getValue(BonusListRenderer.this.enableDigitalScroll) : 0;
                        if (Math.abs(score) < 1.0f && Math.abs(score) > 0.001f) {
                            root.append(String.format("%.1f", score));
                        } else {
                            root.append(String.valueOf(Math.round(score)));
                        }
                    }
                    case "combo" -> {
                        int val = comboStat != null ? (int)comboStat.getValue(BonusListRenderer.this.enableDigitalScroll) : tryParse(extraData);
                        root.append(createStyledComponent(String.valueOf(val), comboStat));
                    }
                    case "multi_kill" -> {
                        int val = mkStat != null ? (int)mkStat.getValue(BonusListRenderer.this.enableDigitalScroll) : tryParse(extraData);
                        String text = getLocalizedNumber(val);
                        root.append(createStyledComponent(text, mkStat));
                    }
                    case "distance" -> {
                        int val = distanceStat != null ? (int)distanceStat.getValue(BonusListRenderer.this.enableDigitalScroll) : tryParse(extraData);
                        MutableComponent c = createStyledComponent(String.valueOf(val), distanceStat);
                        root.append(c);
                        MutableComponent unit = Component.literal("m");
                        if (c.getStyle().getColor() != null) unit.setStyle(c.getStyle());
                        root.append(unit);
                    }
                    case "streak" -> {
                        int val = streakStat != null ? (int)streakStat.getValue(BonusListRenderer.this.enableDigitalScroll) : tryParse(extraData);
                        root.append(createStyledComponent(String.valueOf(val), streakStat));
                    }
                    case "extra" -> root.append(extraData);
                }
                lastEnd = matcher.end();
            }
            
            String lastPart = formatString.substring(lastEnd);
            if (enableTextScrolling && scrambleIdx < scrambleEffects.size()) {
                TextScrambleEffect effect = scrambleEffects.get(scrambleIdx);
                root.append(effect != null ? effect.getCurrentText() : lastPart);
            } else {
                root.append(lastPart);
            }
            
            return root;
        }
        
        private MutableComponent createStyledComponent(String text, AnimatedStat stat) {
            MutableComponent comp = Component.literal(text);
            int baseColor = (specialColor != 0) ? specialColor : BonusListRenderer.this.normalTextColor;
            
            float flash = stat != null ? stat.flashAlpha : 0;
            if (flash > 0) {
                int color = interpolateColor(baseColor, 0xFFFFFF, flash);
                comp.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
            } else if (baseColor != 0xFFFFFF) {
                comp.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(baseColor)));
            }
            return comp;
        }

        
        private int tryParse(String s) {
            try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
        }
    }
    
    public static String getLocalizedNumber(int n) {
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        boolean isChinese = lang != null && (lang.startsWith("zh_"));
        if (isChinese) {
             return switch (n) {
                case 1 -> "一";
                case 2 -> "双";
                case 3 -> "三";
                case 4 -> "四";
                case 5 -> "五";
                case 6 -> "六";
                case 7 -> "七";
                case 8 -> "八";
                case 9 -> "九";
                case 10 -> "十";
                default -> String.valueOf(n);
            };
        }
        return String.valueOf(n);
    }
    
    private int interpolateColor(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        
        return (r << 16) | (g << 8) | b;
    }
}
