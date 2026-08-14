package org.mods.gd656killicon.client.render.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import org.mods.gd656killicon.client.render.effect.BonusTextBox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.PreviewRenderTimeContext;
import org.mods.gd656killicon.client.render.effect.DigitalScrollEffect;
import org.mods.gd656killicon.client.render.effect.TextFadeEffect;
import org.mods.gd656killicon.client.render.effect.TextScrambleEffect;
import org.mods.gd656killicon.common.bonus.BonusDefinition;
import org.mods.gd656killicon.common.bonus.BonusRegistry;
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
    private boolean enableStackMultiplier = false;
    private boolean enableGlowEffect = false;
    private float glowIntensity = 0.5f;
    private float glowSize = GLOW_OFFSET;
    private int glowColorRgb = 0xFFFFFF;
    private float glowAlphaMultiplier = 1.0f;
    private boolean enableTextBox = false;
    private float textBoxBorderWidth = 1.0f;
    private int textBoxColorRgb = 0xFFFFFF;
    private boolean cleanSubtitleContent = false;
    private long fadeOutDurationMs = 300L;
    private float fadeStartLineRatio = 0.0f;
    private boolean enableQueueLinkage = false;
    private float queueLinkageScrollSpeed = 0.1f;
    private float queueLinkageIconYOffset = 0.0f;
    private boolean linkageConfigLoaded = false;
    private float linkedYOffset = 0.0f;
    private float linkageFromOffset = 0.0f;
    private long linkageStartTime = 0L;
    private float linkageTargetOffset = 0.0f;
    private float linkageBaseY = 0.0f;
    private int normalTextColor = 0xFFFFFF;
    private boolean enableTextShadow = true;
    private boolean configBlinkFadeAnimation = false;
    
    private long lastProcessTime = 0;
    private long lastRenderTime = 0;
    private long nextFadeTriggerTime = 0;

    private BonusListRenderer() {}

    public static BonusListRenderer getInstance() {
        return INSTANCE;
    }

    /**
     * 玩家是否将该加分项的客户端元素配置内容改为空(配置键存在且值为空字符串)。
     * 为空时服务端显示请求到达后不显示该加分项。
     */
    public static boolean isBonusConfigEmpty(int type, JsonObject config) {
        BonusDefinition def = BonusRegistry.get(type);
        if (def == null || config == null || !config.has(def.formatConfigKey())) {
            return false;
        }
        return config.get(def.formatConfigKey()).getAsString().isEmpty();
    }

    public static String getEffectiveFormat(int type, String extraData) {
        JsonObject config = ElementConfigManager.getElementConfig(ConfigManager.getCurrentPresetId(), "subtitle/bonus_list");
        return getEffectiveFormat(type, extraData, config);
    }

    public static String getEffectiveFormat(int type, String extraData, JsonObject config) {
        if (type == BonusType.KILL_COMBO && config != null && config.has("enable_special_streak_subtitles") && config.get("enable_special_streak_subtitles").getAsBoolean()) {
            try {
                int combo = Integer.parseInt(extraData);
                BonusDefinition def = BonusRegistry.get(type);
                if (def != null) {
                    String subtitle = def.streakSubtitle(combo);
                    if (subtitle != null) {
                        return I18n.get(subtitle);
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        String configValue = null;
        BonusDefinition def = BonusRegistry.get(type);
        if (def != null && config != null && config.has(def.formatConfigKey())) {
            configValue = config.get(def.formatConfigKey()).getAsString();
        }
        if (def == null) {
            return "";
        }
        // 配置值空/缺键 → 语言默认(统一走 formats json, 注册表 format 硬编码已根除)
        if (configValue == null || configValue.isEmpty()) {
            return org.mods.gd656killicon.client.config.FormatDefaultsManager.getDefault("subtitle/bonus_list", def.formatConfigKey());
        }
        return configValue;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        JsonObject config = ElementConfigManager.getElementConfig(ConfigManager.getCurrentPresetId(), "subtitle/bonus_list");
        if (config == null || !config.get("visible").getAsBoolean()) return;
        // 首次渲染时惰性加载一次配置(直接进游戏时联动配置立即生效; 之后由事件/配置界面驱动更新)
        if (!this.linkageConfigLoaded) {
            loadConfig(config);
            this.linkageConfigLoaded = true;
        }
        int xOffset = config.get("x_offset").getAsInt();
        int yOffset = config.get("y_offset").getAsInt();
        String screenAnchor = config.has("screen_anchor") ? config.get("screen_anchor").getAsString() : "bottom_center";
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float centerX = org.mods.gd656killicon.client.render.ScreenAnchor.resolveCenterX(screenAnchor, xOffset, screenWidth);
        float startY = org.mods.gd656killicon.client.render.ScreenAnchor.resolveCenterY(screenAnchor, yOffset, screenHeight);
        startY = applyQueueLinkage(startY);
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

        // 玩家将该加分项客户端元素配置内容改为空 → 不显示此加分项
        if (isBonusConfigEmpty(context.type(), config)) {
            return;
        }

        String format = getEffectiveFormat(context.type(), extraData);

        int specialColor = parseSpecialColor(config);

        long now = PreviewRenderTimeContext.currentTimeMillis();
        
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
        // 玩家将该加分项客户端元素配置内容改为空 → 不显示此加分项
        if (isBonusConfigEmpty(type, config)) {
            return;
        }
        String format = getEffectiveFormat(type, resolvedExtraData, config);
        int specialColor = parseSpecialColor(config);
        long now = PreviewRenderTimeContext.currentTimeMillis();

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
            this.enableStackMultiplier = config.has("enable_stack_multiplier") && config.get("enable_stack_multiplier").getAsBoolean();
            this.enableGlowEffect = config.has("enable_glow_effect") && config.get("enable_glow_effect").getAsBoolean();
            this.glowIntensity = config.has("glow_intensity") ? config.get("glow_intensity").getAsFloat() : 0.5f;
            this.glowSize = config.has("glow_size") ? config.get("glow_size").getAsFloat() : GLOW_OFFSET;
            this.glowColorRgb = parseHexColor(config, "glow_color", 0xFFFFFF);
            this.glowAlphaMultiplier = config.has("glow_alpha") ? Mth.clamp(config.get("glow_alpha").getAsFloat(), 0.0f, 1.0f) : 1.0f;
            this.enableTextBox = config.has("text_box") && config.get("text_box").getAsBoolean();
            this.textBoxBorderWidth = config.has("text_box_border_width") ? Math.max(0.0f, config.get("text_box_border_width").getAsFloat()) : 1.0f;
            this.textBoxColorRgb = parseHexColor(config, "text_box_color", 0xFFFFFF);
            this.cleanSubtitleContent = config.has("clean_subtitle_content") && config.get("clean_subtitle_content").getAsBoolean();
            this.fadeOutDurationMs = config.has("fade_out_duration")
                    ? Math.max(1L, (long) (config.get("fade_out_duration").getAsFloat() * 1000))
                    : 300L;
            this.fadeStartLineRatio = config.has("fade_start_line_ratio")
                    ? Mth.clamp(config.get("fade_start_line_ratio").getAsFloat(), 0.0f, 1.0f)
                    : 0.0f;
            this.enableQueueLinkage = config.has("enable_queue_linkage") && config.get("enable_queue_linkage").getAsBoolean();
            this.queueLinkageScrollSpeed = config.has("queue_linkage_scroll_speed")
                    ? Math.max(0.01f, config.get("queue_linkage_scroll_speed").getAsFloat())
                    : 0.1f;
            this.queueLinkageIconYOffset = config.has("queue_linkage_icon_y_offset")
                    ? config.get("queue_linkage_icon_y_offset").getAsFloat()
                    : 0.0f;
            this.enableTextShadow = !config.has("enable_text_shadow") || config.get("enable_text_shadow").getAsBoolean();
            this.configBlinkFadeAnimation = config.has("blink_fade_animation") && config.get("blink_fade_animation").getAsBoolean();
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
            this.glowSize = GLOW_OFFSET;
            this.glowColorRgb = 0xFFFFFF;
            this.glowAlphaMultiplier = 1.0f;
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

        long now = PreviewRenderTimeContext.currentTimeMillis();

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

    /**
     * 干净字幕内容: 排除空格与 '+' 字符(占位符由调用方跳过)。
     */
    private static String cleanSubtitleText(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ' || c == '+') {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 递归清除组件所有层级的样式颜色(保留 bold 等其它样式)。
     * 清除后渲染时颜色全部来自 drawString 传入的 color 参数, 不受原字符串各段样式色影响。
     */
    private static Component stripColor(Component component) {
        Style style = component.getStyle();
        Style stripped = (style == null ? Style.EMPTY : style).withColor((TextColor) null);
        MutableComponent result = component.copy();
        result.setStyle(stripped);
        for (int i = 0; i < result.getSiblings().size(); i++) {
            result.getSiblings().set(i, stripColor(result.getSiblings().get(i)));
        }
        return result;
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
        // 横向排列仅在设置了左对齐或右对齐后可用: 向右对齐往左顶, 向左对齐往右顶
        boolean horizontal = config != null && config.has("enable_horizontal_layout")
                && config.get("enable_horizontal_layout").getAsBoolean()
                && (effectiveAlignLeft || effectiveAlignRight);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);
        float scaledCenterX = centerX / scale;
        float scaledStartY = startY / scale;

        synchronized (items) {
            Iterator<BonusItem> iterator = items.iterator();
            int index = 0;
            float accumulatedX = 0.0f;   // 横向: 从最新项(0)开始累计, 每项占位 = 自身文本宽 + 字幕间距

            while (iterator.hasNext()) {
                BonusItem item = iterator.next();
                
                if (horizontal) {
                    // 向右对齐往左顶(负方向), 向左对齐往右顶(正方向);
                    // 老字幕的右(左)边缘距新字幕的左(右)边缘 = 字幕间距(lineSpacing)
                    float itemWidth = mc.font.width(item.getDisplayComponent());
                    float dir = effectiveAlignRight ? -1.0f : 1.0f;
                    item.updateX(now, dt, dir * accumulatedX);
                    accumulatedX += itemWidth + lineSpacing;
                } else {
                    item.update(now, dt, index * lineSpacing);
                }
                
                float alpha = calculateAlpha(item, now, maxLines, lineSpacing, horizontal, index);
                if (alpha > ALPHA_THRESHOLD) {
                    item.render(guiGraphics, mc, scaledCenterX, scaledStartY, alpha, effectiveAlignLeft, effectiveAlignRight, mc.getWindow().getGuiScaledWidth() / scale, scale, horizontal);
                }

                if (shouldRemove(item, alpha, now, maxLines, lineSpacing, horizontal, index)) {
                    iterator.remove();
                    continue;
                }
                index++;
            }
        }

        guiGraphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private float calculateAlpha(BonusItem item, long now, int maxLines, int lineSpacing, boolean horizontal, int index) {
        float alpha = 1.0f;
        
        if (!this.enableTextSweepAnimation) {
            long timeSinceSpawn = now - item.spawnTime;
            float fadeInProgress = timeSinceSpawn / (float)this.enterAnimationDuration;
            alpha *= Math.min(1.0f, fadeInProgress);
        }
        
        // 位置: 竖向用 currentY/行距(每行一个单位); 横向用队列序号(被顶开第几格),
        // 不能按像素偏移/行距换算(每项文本宽不同, 会立即超过 maxLines 导致老项瞬间消失)
        float lineIndex = horizontal ? index : item.currentY / lineSpacing;
        float fadeRange = Math.max(1.0f, (float)maxLines - 1.0f);
        // 开始渐隐行数比: 前 ratio×fadeRange 行保持全显, 之后线性衰减到 0
        float fadeStartLine = this.fadeStartLineRatio * fadeRange;
        float fadeSpan = fadeRange - fadeStartLine;
        float posFadeProgress = fadeSpan <= 0.0f ? 0.0f : (lineIndex - fadeStartLine) / fadeSpan;
        float posAlpha = Math.max(0.0f, 1.0f - Math.max(0.0f, posFadeProgress));
        alpha *= posAlpha;
        // 位置完全隐藏(被顶出显示区): 直接不可见; 回收动画的 0.1 下限只作用于回收渐隐本身,
        // 不能把位置淡出到 0 的行(如 maxLines=4 时的第 4 行)抬回 0.1 导致它很淡地出现
        if (posAlpha <= 0.0f) {
            return 0.0f;
        }

        if (item.isFading) {
            long fadeElapsed = now - item.fadeStartTime;
            float fadeDuration = this.fadeOutDurationMs;   // 出场动画时长(闪烁出场动画同用此值)
            float fadeProgress = fadeElapsed / fadeDuration;
            // 闪出动画用于单行字幕的渐隐(与滚动图标同款公式); 渐隐期整体透明度不低于 0.1
            alpha *= TextFadeEffect.fadeAlpha(fadeProgress, configBlinkFadeAnimation);
            alpha = Math.max(TextFadeEffect.MIN_ALPHA, alpha);
        }

        return alpha;
    }
    
    private boolean shouldRemove(BonusItem item, float alpha, long now, int maxLines, int lineSpacing, boolean horizontal, int index) {
        float lineIndex = horizontal ? index : item.currentY / lineSpacing;
        boolean positionHidden = lineIndex >= maxLines;
        boolean fadeHidden = item.isFading && (now - item.fadeStartTime) >= this.fadeOutDurationMs;
        return positionHidden || fadeHidden;
    }

    /**
     * 队列联动(逐级补位): 检查 kill_feed 与击杀图标的 y, 有上方空位就补位, 可多级上移。
     * <ul>
     *   <li>图标、kill_feed 都显示 → 原位;</li>
     *   <li>图标未显示(kill_feed 显示)→ kill_feed 会补图标位, 加分项补到 kill_feed 位;</li>
     *   <li>kill_feed 未显示(图标显示)→ 加分项补到 kill_feed 位;</li>
     *   <li>两者都未显示 → 加分项一路补到最上(击杀图标位)。</li>
     * </ul>
     * 平滑: x 秒内 easeOutCubic 时间线插值(参考加分项行平滑, 浮点无颗粒感)。
     */
    private float applyQueueLinkage(float baseY) {
        if (!this.enableQueueLinkage) {
            this.linkageStartTime = 0L;
            this.linkedYOffset = 0.0f;
            return baseY;
        }
        SubtitleRenderer subtitleRenderer = SubtitleRenderer.getInstance();
        ScrollingIconRenderer iconRenderer = ScrollingIconRenderer.getInstance();
        boolean kfVisible = subtitleRenderer.hasVisibleSubtitle();
        boolean iconsVisible = iconRenderer.hasVisibleIcons();
        float kfBaseY = subtitleRenderer.getBaseTextY();
        float iconY = iconRenderer.getIconsAnchorY();

        float targetY;
        if (iconsVisible && kfVisible) {
            targetY = baseY;                              // 全显示: 原位
        } else if (kfVisible) {
            targetY = kfBaseY;                            // 图标空: kill_feed 补图标位, 加分项补 kill_feed 位
        } else if (iconsVisible) {
            targetY = kfBaseY;                            // kill_feed 空: 加分项补 kill_feed 位
        } else {
            targetY = iconY + this.queueLinkageIconYOffset;   // 都空: 一路补到最上(击杀图标位) + 额外偏移
        }
        // 目标必须在自身之上(正常组合 kfBaseY < baseY, iconY < kfBaseY), 否则不联动
        if (targetY >= baseY) {
            targetY = baseY;
        }
        return smoothLinkedY(baseY, targetY);
    }

    /**
     * 队列联动的平滑移动: x 秒内完成的时间线插值(easeOutCubic), 全程浮点无取整。
     */
    private float smoothLinkedY(float baseY, float targetY) {
        float targetOffset = targetY - baseY;
        long now = System.currentTimeMillis();
        long durationMs = Math.max(1L, (long) (this.queueLinkageScrollSpeed * 1000.0f));

        if (this.linkageStartTime != 0L && this.linkageTargetOffset == targetOffset && this.linkageBaseY == baseY) {
            float progress = (now - this.linkageStartTime) / (float) durationMs;
            progress = Math.min(1.0f, Math.max(0.0f, progress));
            float eased = 1.0f - (float) Math.pow(1.0f - progress, 3);
            this.linkedYOffset = this.linkageFromOffset + (targetOffset - this.linkageFromOffset) * eased;
            if (progress >= 1.0f) {
                this.linkageFromOffset = targetOffset;
            }
            return baseY + this.linkedYOffset;
        }

        this.linkageFromOffset = this.linkedYOffset;
        this.linkageStartTime = now;
        this.linkageTargetOffset = targetOffset;
        this.linkageBaseY = baseY;
        if (Math.abs(targetOffset - this.linkedYOffset) < 0.001f) {
            this.linkageFromOffset = targetOffset;
            this.linkedYOffset = targetOffset;
        }
        return baseY + this.linkedYOffset;
    }

    private void drawComponentWithGlow(GuiGraphics guiGraphics, Font font, Component component, int x, int y, int alphaInt) {
        if (this.enableGlowEffect) {
            int glowAlpha = (int)(alphaInt * this.glowIntensity * this.glowAlphaMultiplier);
            glowAlpha = Math.max((int) (TextFadeEffect.MIN_ALPHA * 255.0f), Math.min(255, glowAlpha));  // 副本最小透明度 0.1
            glowAlpha = Math.max(0, Math.min(255, glowAlpha));
            int glowColor = (glowAlpha << 24) | (this.glowColorRgb & 0xFFFFFF);
            // 发光副本一律显示配置的发光色: 递归清除各段样式颜色(null),
            // 渲染时 RGB/alpha 全部来自 drawString 传入的 glowColor(不受主字幕样式色影响)
            Component glowComponent = stripColor(component);
            
            PoseStack poseStack = guiGraphics.pose();
            
            float[][] offsets = {
                {-glowSize, 0}, {glowSize, 0}, {0, -glowSize}, {0, glowSize},
                {-glowSize, -glowSize}, {glowSize, -glowSize},
                {-glowSize, glowSize}, {glowSize, glowSize}
            };
            
            for (float[] offset : offsets) {
                poseStack.pushPose();
                poseStack.translate(offset[0], offset[1], 0);
                guiGraphics.drawString(font, glowComponent, x, y, glowColor, false);
                poseStack.popPose();
            }
        }
        int color = (alphaInt << 24) | (this.normalTextColor & 0xFFFFFF);
        guiGraphics.drawString(font, component, x, y, color, this.enableTextShadow);
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
        int stackCount = 1;
        
        float currentY;
        float currentXOffset;
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
            this.currentXOffset = 0;
            this.isFading = false;
            this.spawnTime = PreviewRenderTimeContext.currentTimeMillis();
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
            this.stackCount++;
            if (scoreStat != null) {
                // 启用叠加倍数显示: score 保持单次值不累加, 堆叠次数由 stackCount 记录(渲染时显示 ×N)
                if (!BonusListRenderer.this.enableStackMultiplier) {
                    scoreStat.add(score);
                }
            }
            
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

        /**
         * 横向排列的平移动画: 与竖向 {@link #update} 完全相同的平滑曲线与时序,
         * 但目标/当前值作用于水平偏移 currentXOffset(新加分项为 0, 老加分项被顶向两侧)。
         */
        public void updateX(long now, float dt, float targetX) {
            if (scoreStat != null) scoreStat.update(now, dt);
            if (comboStat != null) comboStat.update(now, dt);
            if (mkStat != null) mkStat.update(now, dt);
            if (distanceStat != null) distanceStat.update(now, dt);
            if (streakStat != null) streakStat.update(now, dt);
            
            float smoothFactor = 1.0f - (float)Math.exp(-BonusListRenderer.this.animationSpeed * dt);
            this.currentXOffset = this.currentXOffset + (targetX - this.currentXOffset) * smoothFactor;
        }

        public void render(GuiGraphics guiGraphics, Minecraft mc, float x, float y, float alpha, boolean alignLeft, boolean alignRight, float screenWidth, float globalScale, boolean horizontal) {
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
            
            long now = PreviewRenderTimeContext.currentTimeMillis();
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
                if (horizontal) drawX += this.currentXOffset;
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
                        if (BonusListRenderer.this.enableTextBox) {
                            BonusTextBox.draw(guiGraphics, baseTextWidth, mc.font.lineHeight, BonusListRenderer.this.textBoxBorderWidth, BonusListRenderer.this.textBoxColorRgb, alphaInt);
                        }
                        BonusListRenderer.this.drawComponentWithGlow(guiGraphics, mc.font, component, 0, 0, alphaInt);
                        guiGraphics.pose().popPose();
                        guiGraphics.disableScissor();
                    }
                } else {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(drawX, drawY, 0);
                    if (BonusListRenderer.this.enableTextBox) {
                        BonusTextBox.draw(guiGraphics, baseTextWidth, mc.font.lineHeight, BonusListRenderer.this.textBoxBorderWidth, BonusListRenderer.this.textBoxColorRgb, alphaInt);
                    }
                    BonusListRenderer.this.drawComponentWithGlow(guiGraphics, mc.font, component, 0, 0, alphaInt);
                    guiGraphics.pose().popPose();
                }
            }
            
            if (renderFeed && killFeedComponent != null) {
                float drawX = calculateDrawX(x, feedTextWidth, alignLeft, alignRight);
                if (horizontal) drawX += this.currentXOffset;
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
                        if (BonusListRenderer.this.enableTextBox) {
                            BonusTextBox.draw(guiGraphics, feedTextWidth, mc.font.lineHeight, BonusListRenderer.this.textBoxBorderWidth, BonusListRenderer.this.textBoxColorRgb, alphaInt);
                        }
                        BonusListRenderer.this.drawComponentWithGlow(guiGraphics, mc.font, killFeedComponent, 0, 0, alphaInt);
                        guiGraphics.pose().popPose();
                        guiGraphics.disableScissor();
                    }
                } else {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(drawX, drawY, 0);
                    if (BonusListRenderer.this.enableTextBox) {
                        BonusTextBox.draw(guiGraphics, feedTextWidth, mc.font.lineHeight, BonusListRenderer.this.textBoxBorderWidth, BonusListRenderer.this.textBoxColorRgb, alphaInt);
                    }
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
            
            // 只匹配 score/weapon/target 占位符(与渲染分支一致): clean 模式只排除 <score>
            Matcher m = Pattern.compile("(<weapon>|<target>|<score>)").matcher(fmt);
            int lastEnd = 0;
            
            while (m.find()) {
                String staticPart = fmt.substring(lastEnd, m.start());
                if (BonusListRenderer.this.cleanSubtitleContent) {
                    staticPart = cleanSubtitleText(staticPart);
                }
                if (!staticPart.isEmpty()) {
                    root.append(Component.literal(staticPart).withStyle(Style.EMPTY.withColor(BonusListRenderer.this.normalTextColor)));
                }
                
                String tag = m.group(1);
                if (BonusListRenderer.this.cleanSubtitleContent) {
                    // 干净模式: 只排除 <score> 占位符(不显示其值), <weapon>/<target> 正常显示
                    if ("<score>".equals(tag)) {
                        lastEnd = m.end();
                        continue;
                    }
                }
                if ("<weapon>".equals(tag)) {
                    root.append(Component.literal(this.weaponName).withStyle(Style.EMPTY.withColor(BonusListRenderer.this.normalTextColor)));
                } else if ("<target>".equals(tag)) {
                    String displayVName = this.victimName;
                    if (org.mods.gd656killicon.client.util.I18nCompat.exists(displayVName)) {
                        displayVName = net.minecraft.client.resources.language.I18n.get(displayVName);
                    }
                    root.append(Component.literal(displayVName).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.killFeedVictimColorVal))));
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
            if (BonusListRenderer.this.cleanSubtitleContent) {
                tail = cleanSubtitleText(tail);
            }
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
                if (BonusListRenderer.this.cleanSubtitleContent) {
                    staticPart = cleanSubtitleText(staticPart);
                }
                if (enableTextScrolling && scrambleIdx < scrambleEffects.size()) {
                    TextScrambleEffect effect = scrambleEffects.get(scrambleIdx);
                    root.append(effect != null ? effect.getCurrentText() : staticPart);
                } else {
                    root.append(staticPart);
                }
                scrambleIdx++;

                String type = matcher.group(1);

                if (BonusListRenderer.this.cleanSubtitleContent) {
                    // 干净模式: 只排除 <score> 占位符(不显示其值), 其它占位符正常显示
                    if ("score".equals(type)) {
                        lastEnd = matcher.end();
                        continue;
                    }
                }
                
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
            if (BonusListRenderer.this.cleanSubtitleContent) {
                lastPart = cleanSubtitleText(lastPart);
            }
            if (enableTextScrolling && scrambleIdx < scrambleEffects.size()) {
                TextScrambleEffect effect = scrambleEffects.get(scrambleIdx);
                root.append(effect != null ? effect.getCurrentText() : lastPart);
            } else {
                root.append(lastPart);
            }

            // 叠加倍数显示: 启用且堆叠次数 > 1 时追加 " ×N"(score 已保持单次值)
            if (BonusListRenderer.this.enableStackMultiplier && this.stackCount > 1) {
                root.append(Component.literal(" ×" + this.stackCount)
                        .withStyle(Style.EMPTY.withColor(BonusListRenderer.this.normalTextColor)));
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
                case 2 -> "二";
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
