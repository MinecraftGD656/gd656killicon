package org.mods.gd656killicon.client.render.impl;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.PreviewRenderTimeContext;
import org.mods.gd656killicon.client.render.ScreenAnchor;
import org.mods.gd656killicon.client.render.effect.TextFadeEffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 命中信息字幕渲染器(元素 id: subtitle/hit_info)。
 * <p>
 * 显示玩家在显示持续时间内对任意生物造成的伤害量累积值：
 * <ul>
 *   <li>默认(单行): 所有实体伤害在显示持续窗口内累积为一个值, 击杀后伤害占位符切换为击杀颜色;</li>
 *   <li>分层模式(enable_entity_layers): 按实体分别堆叠, 每个实体一行(新实体在前, 老实体往后排),
 *       行位移动画/收起补位动画, 每行独立数字滚动, 击杀对应实体行切换击杀颜色;</li>
 *   <li>支持淡入/淡出、闪动淡出、数字滚动、左/右对齐、发光(副本颜色随击杀状态)。</li>
 * </ul>
 * 旋转由 {@code HudElementManager} 按 rotation_angle 统一应用，本类不处理。
 */
public class HitInfoRenderer implements IHudRenderer {
    private static final String ELEMENT_ID = "subtitle/hit_info";
    private static final String DAMAGE_PLACEHOLDER = "<damage>";

    private static HitInfoRenderer instance;

    private boolean visible = true;
    private float scale = 1.0f;
    private int configXOffset = 0;
    private int configYOffset = 80;
    private String screenAnchor = ScreenAnchor.DEFAULT;

    private long displayDurationMs = 3000L;
    private boolean alignLeft = false;
    private boolean alignRight = false;
    private String format = DAMAGE_PLACEHOLDER;
    private int colorDamageDefault = 0xFFFFFFFF;
    private int colorDamageKill = 0xFFD4B800;
    private int colorNormalText = 0xFFFFFFFF;
    private long fadeInDurationMs = 200L;
    private long fadeOutDurationMs = 300L;
    private boolean blinkFadeAnimation = false;
    private boolean enableDamageScroll = true;
    private float scrollDuration = 1.25f;
    private float scrollRefreshRate = 0.02f;
    private boolean enableTextShadow = true;
    private boolean enableGlowEffect = false;
    private float glowIntensity = 0.5f;
    private float glowSize = 0.3f;
    private int glowColorDefaultRgb = 0xFFFFFF;
    private int glowColorKillRgb = 0xFFFFFF;
    private float glowAlphaMultiplier = 1.0f;

    // 分层模式配置
    private boolean enableEntityLayers = false;
    private int maxLayers = 5;
    private float layerSpacing = 12.0f;
    private float layerMoveAnimDuration = 0.2f;
    private float layerCollapseAnimDuration = 0.2f;

    // 单行(非分层)状态
    private boolean displaying = false;
    private float totalDamage = 0.0f;
    private boolean killed = false;
    private long startTime = -1L;
    private long textHideTime = -1L;
    private boolean isFadingOut = false;

    // 单行数字滚动(内联实现, 与 score 的 DigitalScrollEffect 同语义: QUINTIC_OUT 从旧值平滑过渡到新值)
    private float displayDamage = 0.0f;
    private float scrollFrom = 0.0f;
    private float scrollTarget = 0.0f;
    private long scrollStartTime = -1L;
    private boolean isScrolling = false;

    // 分层模式: 每实体一行(新在前)
    private final List<EntityLine> entityLines = new ArrayList<>();
    private long lastLayerRenderTime = 0L;

    public HitInfoRenderer() {
    }

    public static synchronized HitInfoRenderer getInstance() {
        if (instance == null) {
            instance = new HitInfoRenderer();
        }
        return instance;
    }

    /** 客户端收到伤害事件: 在显示持续窗口内累积伤害量并刷新显示窗口。 */
    public void addDamage(int entityId, float amount) {
        JsonObject config = getConfig();
        if (config == null) {
            return;
        }
        if (!(config.has("visible") ? config.get("visible").getAsBoolean() : true)) {
            return;
        }
        loadConfig(config);

        long now = PreviewRenderTimeContext.currentTimeMillis();
        if (this.enableEntityLayers) {
            addLayerDamage(entityId, amount, now);
            return;
        }

        if (!this.displaying) {
            this.totalDamage = amount;
            this.killed = false;
            this.startTime = now;
            this.textHideTime = now + this.displayDurationMs;
            this.isFadingOut = false;
            this.displaying = true;
            beginScroll(0.0f, this.totalDamage, now);
        } else {
            this.totalDamage += amount;
            this.textHideTime = now + this.displayDurationMs;
            // 从当前显示值(断点)重新开始滚动到新累积值 —— 与 score 的 startAnimation(getCurrentValue(), target) 同款:
            // 滚动中断时数字从断点继续, 而非从旧目标/初值跳变
            beginScroll(this.displayDamage, this.totalDamage, now);
        }
    }

    /** 客户端收到击杀事件: 对应实体的伤害占位符切换为击杀颜色。 */
    public void markKill(int entityId) {
        if (this.enableEntityLayers) {
            for (EntityLine line : this.entityLines) {
                if (line.entityId == entityId && !line.isFading) {
                    line.killed = true;
                    line.lastHitTime = PreviewRenderTimeContext.currentTimeMillis();
                    return;
                }
            }
            return;
        }
        this.killed = true;
        if (this.displaying) {
            long now = PreviewRenderTimeContext.currentTimeMillis();
            this.textHideTime = now + this.displayDurationMs;
        }
    }

    /** 分层模式: 指定实体累积伤害; 新实体行插到最前, 老行向后排(位移动画) */
    private void addLayerDamage(int entityId, float amount, long now) {
        for (EntityLine line : this.entityLines) {
            if (line.entityId == entityId && !line.isFading) {
                line.totalDamage += amount;
                line.lastHitTime = now;
                line.beginScroll(line.displayDamage, line.totalDamage, now);
                return;
            }
        }
        EntityLine line = new EntityLine(entityId, amount, now);
        this.entityLines.add(0, line);
        trimToMaxLayers();
    }

    /**
     * 淡出堆积清理: 总列表长度超过 max_layers*2 时强制移除最老的淡出行
     * (防止一次性大量新行导致列表无限增长; 正常超限行由渲染按行号在移动后位置渐隐)。
     */
    private void trimToMaxLayers() {
        while (this.entityLines.size() > this.maxLayers * 2) {
            boolean removed = false;
            for (int i = this.entityLines.size() - 1; i >= 0; i--) {
                if (this.entityLines.get(i).isFading) {
                    this.entityLines.remove(i);
                    removed = true;
                    break;
                }
            }
            if (!removed) {
                break;
            }
        }
    }

    public void reset() {
        this.displaying = false;
        this.totalDamage = 0.0f;
        this.killed = false;
        this.startTime = -1L;
        this.textHideTime = -1L;
        this.isFadingOut = false;
        this.displayDamage = 0.0f;
        this.isScrolling = false;
        this.entityLines.clear();
        this.lastLayerRenderTime = 0L;
    }

    public void resetPreview() {
        reset();
    }

    /** 开始/重开一段数字滚动动画(from → to) */
    private void beginScroll(float from, float to, long now) {
        this.scrollFrom = from;
        this.scrollTarget = to;
        this.scrollStartTime = now;
        this.isScrolling = true;
        this.displayDamage = from;
    }

    /** 每帧推进单行滚动, 返回当前显示伤害值(未启用滚动时直接返回目标值) */
    private float updateScroll(float damage, long now) {
        if (!this.enableDamageScroll) {
            this.displayDamage = this.totalDamage;
            return this.totalDamage;
        }
        if (this.isScrolling) {
            float elapsed = (float) (now - this.scrollStartTime);
            if (elapsed <= 0.0f) {
                return this.displayDamage;
            }
            float progress = Math.min(elapsed / (this.scrollDuration * 1000.0f), 1.0f);
            // QUINTIC ease-out(与 score 同款)
            float eased = 1.0f - (float) Math.pow(1.0f - progress, 5);
            this.displayDamage = this.scrollFrom + (this.scrollTarget - this.scrollFrom) * eased;
            if (progress >= 1.0f) {
                this.displayDamage = this.scrollTarget;
                this.isScrolling = false;
            }
        }
        return this.displayDamage;
    }

    private void loadConfig(JsonObject config) {
        this.scale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
        this.configXOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
        this.configYOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 80;
        this.screenAnchor = config.has("screen_anchor") ? config.get("screen_anchor").getAsString() : ScreenAnchor.DEFAULT;
        this.displayDurationMs = config.has("display_duration")
                ? Math.max(1L, (long) (config.get("display_duration").getAsFloat() * 1000))
                : 3000L;
        this.alignLeft = config.has("align_left") && config.get("align_left").getAsBoolean();
        this.alignRight = config.has("align_right") && config.get("align_right").getAsBoolean();
        this.format = config.has("format_damage") ? config.get("format_damage").getAsString() : DAMAGE_PLACEHOLDER;
        this.colorDamageDefault = parseColorHex(config, "color_damage_default", 0xFFFFFFFF);
        this.colorDamageKill = parseColorHex(config, "color_damage_kill", 0xFFD4B800);
        this.colorNormalText = parseColorHex(config, "color_normal_text", 0xFFFFFFFF);
        this.fadeInDurationMs = config.has("fade_in_duration")
                ? Math.max(1L, (long) (config.get("fade_in_duration").getAsFloat() * 1000))
                : 200L;
        this.fadeOutDurationMs = config.has("fade_out_duration")
                ? Math.max(1L, (long) (config.get("fade_out_duration").getAsFloat() * 1000))
                : 300L;
        this.blinkFadeAnimation = config.has("blink_fade_animation") && config.get("blink_fade_animation").getAsBoolean();
        this.enableDamageScroll = config.has("enable_damage_scroll") && config.get("enable_damage_scroll").getAsBoolean();
        this.scrollDuration = config.has("damage_scroll_duration")
                ? Math.max(0.05f, config.get("damage_scroll_duration").getAsFloat())
                : 1.25f;
        this.scrollRefreshRate = config.has("damage_scroll_refresh_rate") ? config.get("damage_scroll_refresh_rate").getAsFloat() : 0.02f;
        this.enableTextShadow = !config.has("enable_text_shadow") || config.get("enable_text_shadow").getAsBoolean();
        this.enableGlowEffect = config.has("enable_glow_effect") && config.get("enable_glow_effect").getAsBoolean();
        this.glowIntensity = config.has("glow_intensity") ? config.get("glow_intensity").getAsFloat() : 0.5f;
        this.glowSize = config.has("glow_size") ? config.get("glow_size").getAsFloat() : 0.3f;
        this.glowColorDefaultRgb = parseColorHex(config, "glow_color_damage_default", 0xFFFFFF) & 0x00FFFFFF;
        this.glowColorKillRgb = parseColorHex(config, "glow_color_damage_kill", 0xFFFFFF) & 0x00FFFFFF;
        this.glowAlphaMultiplier = config.has("glow_alpha") ? Mth.clamp(config.get("glow_alpha").getAsFloat(), 0.0f, 1.0f) : 1.0f;

        // 分层模式配置
        this.enableEntityLayers = config.has("enable_entity_layers") && config.get("enable_entity_layers").getAsBoolean();
        this.maxLayers = config.has("max_layers") ? Math.max(1, config.get("max_layers").getAsInt()) : 5;
        this.layerSpacing = config.has("layer_spacing") ? Math.max(0.0f, config.get("layer_spacing").getAsFloat()) : 12.0f;
        this.layerMoveAnimDuration = config.has("layer_move_animation_duration")
                ? Math.max(0.05f, config.get("layer_move_animation_duration").getAsFloat())
                : 0.2f;
        this.layerCollapseAnimDuration = config.has("layer_collapse_animation_duration")
                ? Math.max(0.05f, config.get("layer_collapse_animation_duration").getAsFloat())
                : 0.2f;
    }

    @Override
    public void trigger(TriggerContext context) {
        // 触发由 HitInfoPacket 直接驱动(addDamage / markKill)
    }

    private int previewStep = 0;

    /**
     * 配置界面预览触发: 每次加 1~20 随机伤害, 每 5 次模拟一次击杀(演示击杀色切换)。
     * 分层模式: 轮流对 3 个实体造成伤害, 演示多行堆叠。
     */
    public void triggerPreview() {
        this.previewStep++;
        if (this.previewStep % 5 == 0) {
            this.markKill(1000 + (this.previewStep / 5) % 3);
        } else {
            int entityId = 1000 + (this.previewStep % 3);
            this.addDamage(entityId, 1.0f + java.util.concurrent.ThreadLocalRandom.current().nextInt(20));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        if (this.enableEntityLayers) {
            if (this.entityLines.isEmpty()) {
                return;
            }
        } else if (!this.displaying || this.startTime < 0) {
            return;
        }
        // 每帧重载配置(与其它字幕一致: 配置改动立即生效)
        JsonObject config = getConfig();
        if (config != null) {
            if (!(config.has("visible") ? config.get("visible").getAsBoolean() : true)) {
                return;
            }
            loadConfig(config);
        }

        Minecraft mc = Minecraft.getInstance();
        int centerX = ScreenAnchor.resolveCenterX(this.screenAnchor, this.configXOffset, mc.getWindow().getGuiScaledWidth());
        int centerY = ScreenAnchor.resolveCenterY(this.screenAnchor, this.configYOffset, mc.getWindow().getGuiScaledHeight());
        renderAt(guiGraphics, partialTick, centerX, centerY);
    }

    /**
     * 在指定坐标渲染(配置界面预览框复用; 传入坐标即元素中心/锚点位置)。
     */
    public void renderAt(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY) {
        if (this.enableEntityLayers) {
            renderLayered(guiGraphics, partialTick, centerX, centerY);
            return;
        }
        if (!this.displaying || this.startTime < 0) {
            return;
        }
        // 每帧重载配置(与游戏内 render 一致): 预览框内配置改动立即生效(阴影/发光/滚动/颜色等)
        JsonObject liveConfig = getConfig();
        if (liveConfig != null) {
            if (!(liveConfig.has("visible") ? liveConfig.get("visible").getAsBoolean() : true)) {
                return;
            }
            loadConfig(liveConfig);
        }

        long now = PreviewRenderTimeContext.currentTimeMillis();
        // 滚动推进放在最前(不受淡入/淡出 alpha 检查影响, 动画始终推进)
        float displayDamage = updateScroll(this.totalDamage, now);

        if (!this.isFadingOut && now > this.textHideTime) {
            this.isFadingOut = true;
        }

        // 淡出 alpha(闪动同款公式)
        float alpha = 1.0f;
        if (this.isFadingOut) {
            float fadeProgress = (float) (now - this.textHideTime) / this.fadeOutDurationMs;
            alpha = TextFadeEffect.fadeAlpha(Mth.clamp(fadeProgress, 0.0f, 1.0f), this.blinkFadeAnimation);
        }
        if (now >= this.textHideTime + this.fadeOutDurationMs) {
            reset();
            return;
        }

        // 淡入: 前 fadeInDuration 内透明度从 0 平滑升到 1
        long elapsed = now - this.startTime;
        if (elapsed < this.fadeInDurationMs) {
            float t = (float) elapsed / this.fadeInDurationMs;
            float eased = 1.0f - (float) Math.pow(1.0f - t, 3);
            alpha *= eased;
        }
        if (alpha <= 0.05f) {
            return;
        }

        renderDamageLine(guiGraphics, centerX, centerY, displayDamage, this.killed, alpha);
    }

    /** 分层模式渲染: 每实体一行(新在前, 老行向后排), 行位移动画 + 收起补位动画 + 每行独立滚动 */
    private void renderLayered(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY) {
        JsonObject liveConfig = getConfig();
        if (liveConfig != null) {
            if (!(liveConfig.has("visible") ? liveConfig.get("visible").getAsBoolean() : true)) {
                return;
            }
            loadConfig(liveConfig);
        }
        if (this.entityLines.isEmpty()) {
            return;
        }

        long now = PreviewRenderTimeContext.currentTimeMillis();
        float dt = this.lastLayerRenderTime == 0L ? 0.016f : (now - this.lastLayerRenderTime) / 1000.0f;
        this.lastLayerRenderTime = now;
        if (dt > 0.1f) {
            dt = 0.1f;
        }

        boolean removedAny = false;
        Iterator<EntityLine> it = this.entityLines.iterator();
        int index = 0;
        while (it.hasNext()) {
            EntityLine line = it.next();
            // 显示窗口结束 → 开始淡出
            if (!line.isFading && now > line.lastHitTime + this.displayDurationMs) {
                line.startFade(now);
            }
            if (line.isFading && now - line.fadeStartTime >= this.fadeOutDurationMs) {
                it.remove();
                removedAny = true;
                continue;
            }

            // 行位置: 所有行(含淡出中)占 index, 新行插入时老行(含将淡出的)整体下移一行
            float targetY = index * this.layerSpacing;
            float animDur = removedAny ? this.layerCollapseAnimDuration : this.layerMoveAnimDuration;
            float smooth = 1.0f - (float) Math.exp(-dt / Math.max(0.05f, animDur));
            line.currentY += (targetY - line.currentY) * smooth;
            if (Math.abs(line.currentY - targetY) < 0.05f) {
                line.currentY = targetY;
            }

            // 超过 max_layers 的行(被新行顶到超出位置)开始淡出, 在移动后的位置渐隐
            if (!line.isFading && index >= this.maxLayers) {
                line.startFade(now);
            }

            float alpha = 1.0f;
            if (line.isFading) {
                float fadeProgress = (float) (now - line.fadeStartTime) / this.fadeOutDurationMs;
                alpha = TextFadeEffect.fadeAlpha(Mth.clamp(fadeProgress, 0.0f, 1.0f), this.blinkFadeAnimation);
            }
            if (alpha <= 0.05f) {
                index++;
                continue;
            }

            float display = line.updateScroll(now, this.enableDamageScroll, this.scrollDuration);
            renderDamageLine(guiGraphics, centerX, centerY + line.currentY, display, line.killed, alpha);
            index++;
        }
    }

    /** 渲染单行伤害字幕(分色 + 发光 + 阴影 + 对齐) */
    private void renderDamageLine(GuiGraphics guiGraphics, float centerX, float centerY, float displayDamage, boolean lineKilled, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        String damageStr = String.valueOf(Math.round(displayDamage));
        int alphaInt = (int) (alpha * 255.0f) << 24;
        int damageColor = ((lineKilled ? this.colorDamageKill : this.colorDamageDefault) & 0x00FFFFFF) | alphaInt;
        int normalColor = (this.colorNormalText & 0x00FFFFFF) | alphaInt;

        Component text = buildTextComponent(font, damageStr, damageColor, normalColor);

        // 对齐(基于缩放后宽度): 右对齐右边缘=centerX, 居中=centerX, 左对齐左边缘=centerX
        int textWidth = font.width(text);
        int textX;
        if (this.alignLeft && !this.alignRight) {
            textX = Math.round(centerX);
        } else if (this.alignRight && !this.alignLeft) {
            textX = Math.round(centerX - textWidth * this.scale);
        } else {
            textX = Math.round(centerX - textWidth * this.scale / 2.0f);
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(textX, centerY, 0.0f);
        guiGraphics.pose().scale(this.scale, this.scale, 1.0f);
        drawTextWithGlow(guiGraphics, font, text, 0, -font.lineHeight / 2, normalColor, this.enableTextShadow, lineKilled);
        guiGraphics.pose().popPose();
    }

    /**
     * 发光 + 主文本绘制(与 score/bonus_list/kill_feed 同款):
     * 发光开启时先以 8 个 ±glowSize 偏移绘制低透明度副本, 再叠主文本。
     */
    private void drawTextWithGlow(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int color, boolean dropShadow, boolean lineKilled) {
        if (this.enableGlowEffect) {
            int alpha = (color >> 24) & 0xFF;
            int glowAlpha = (int) (alpha * this.glowIntensity * this.glowAlphaMultiplier);
            glowAlpha = Math.max((int) (TextFadeEffect.MIN_ALPHA * 255.0f), Math.min(255, glowAlpha));  // 副本最小透明度 0.1
            // 发光副本颜色跟随击杀状态: 击杀用击杀发光色, 否则默认发光色
            int glowRgb = lineKilled ? this.glowColorKillRgb : this.glowColorDefaultRgb;
            int glowColor = (glowRgb & 0x00FFFFFF) | (glowAlpha << 24);

            float[][] offsets = {
                    {-glowSize, 0}, {glowSize, 0}, {0, -glowSize}, {0, glowSize},
                    {-glowSize, -glowSize}, {glowSize, -glowSize},
                    {-glowSize, glowSize}, {glowSize, glowSize}
            };
            for (float[] offset : offsets) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(offset[0], offset[1], 0);
                // 副本颜色完全来自配置: 递归清除主字符串各段样式色, 渲染颜色全由 drawString 传入
                guiGraphics.drawString(font, stripColor(text), x, y, glowColor, false);
                guiGraphics.pose().popPose();
            }
        }
        guiGraphics.drawString(font, text, x, y, color, dropShadow);
    }

    private Component buildTextComponent(Font font, String damageStr, int damageColor, int normalColor) {
        Component text;
        if (this.format.contains(DAMAGE_PLACEHOLDER)) {
            int idx = this.format.indexOf(DAMAGE_PLACEHOLDER);
            String prefix = this.format.substring(0, idx);
            String suffix = this.format.substring(idx + DAMAGE_PLACEHOLDER.length());
            text = Component.literal(prefix).withStyle(style -> style.withColor(normalColor & 0x00FFFFFF))
                    .append(Component.literal(damageStr).withStyle(style -> style.withColor(damageColor & 0x00FFFFFF)))
                    .append(Component.literal(suffix).withStyle(style -> style.withColor(normalColor & 0x00FFFFFF)));
        } else {
            text = Component.literal(this.format).withStyle(style -> style.withColor(normalColor & 0x00FFFFFF));
        }
        return text;
    }

    /**
     * 分层模式: 单个实体的伤害行。
     * 含独立数字滚动(与单行同款 QUINTIC_OUT 断点续滚)与行位移动画(currentY)。
     */
    private static class EntityLine {
        final int entityId;
        float totalDamage;
        boolean killed;
        long lastHitTime;
        boolean isFading;
        long fadeStartTime;
        float currentY;

        float displayDamage;
        float scrollFrom;
        float scrollTarget;
        long scrollStartTime;
        boolean isScrolling;

        EntityLine(int entityId, float amount, long now) {
            this.entityId = entityId;
            this.totalDamage = amount;
            this.lastHitTime = now;
            beginScroll(0.0f, amount, now);
        }

        void startFade(long now) {
            if (!this.isFading) {
                this.isFading = true;
                this.fadeStartTime = now;
            }
        }

        void beginScroll(float from, float to, long now) {
            this.scrollFrom = from;
            this.scrollTarget = to;
            this.scrollStartTime = now;
            this.isScrolling = true;
            this.displayDamage = from;
        }

        float updateScroll(long now, boolean enabled, float duration) {
            if (!enabled) {
                this.displayDamage = this.totalDamage;
                return this.totalDamage;
            }
            if (this.isScrolling) {
                float elapsed = (float) (now - this.scrollStartTime);
                if (elapsed <= 0.0f) {
                    return this.displayDamage;
                }
                float progress = Math.min(elapsed / (duration * 1000.0f), 1.0f);
                float eased = 1.0f - (float) Math.pow(1.0f - progress, 5);
                this.displayDamage = this.scrollFrom + (this.scrollTarget - this.scrollFrom) * eased;
                if (progress >= 1.0f) {
                    this.displayDamage = this.scrollTarget;
                    this.isScrolling = false;
                }
            }
            return this.displayDamage;
        }
    }

    private JsonObject getConfig() {
        return ElementConfigManager.getElementConfig(ConfigManager.getCurrentPresetId(), ELEMENT_ID);
    }

    /**
     * 递归清除组件所有层级的样式颜色(保留 bold 等其它样式)。
     * 清除后渲染时颜色全部来自 drawString 传入的 color 参数, 不受原字符串各段样式色影响。
     */
    private static Component stripColor(Component component) {
        net.minecraft.network.chat.Style style = component.getStyle();
        net.minecraft.network.chat.Style stripped = (style == null ? net.minecraft.network.chat.Style.EMPTY : style)
                .withColor((net.minecraft.network.chat.TextColor) null);
        net.minecraft.network.chat.MutableComponent result = component.copy();
        result.setStyle(stripped);
        for (int i = 0; i < result.getSiblings().size(); i++) {
            result.getSiblings().set(i, stripColor(result.getSiblings().get(i)));
        }
        return result;
    }

    private static int parseColorHex(JsonObject config, String key, int fallback) {
        if (config == null || !config.has(key)) {
            return fallback;
        }
        try {
            String hex = config.get(key).getAsString();
            return (int) Long.parseLong(hex.startsWith("#") ? hex.substring(1) : hex, 16) | 0xFF000000;
        } catch (Exception e) {
            return fallback;
        }
    }
}
