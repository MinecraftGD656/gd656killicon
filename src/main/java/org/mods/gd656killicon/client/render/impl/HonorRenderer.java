package org.mods.gd656killicon.client.render.impl;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.effect.IconTextureFilterEffect;
import org.mods.gd656killicon.client.render.ScreenAnchor;

/**
 * Renderer for the Honor element (kill_icon/honor).
 * <p>
 * 荣誉(勋章)元素渲染: 收到服务端显示包(经 {@link org.mods.gd656killicon.network.packet.HonorPacket}
 * 触发)后, 按配置时间线播放:
 * <pre>
 * 触发 ──(延迟显示秒数 display_delay)──► t0 ──(入场动画 enter_animation_duration,
 *                                           大小从 enter_start_scale 平滑到正常)──►
 * 保持显示(display_duration) ──► 出场淡出(fade_out_duration, 可选闪动) ──► 移除
 * </pre>
 * 纹理直接使用 jar 内资源 textures/honor/honor_&lt;id&gt;.png, 不接入自定义纹理系统。
 * </p>
 */
public class HonorRenderer implements IHudRenderer {

    private static final HonorRenderer INSTANCE = new HonorRenderer();

    public static HonorRenderer getInstance() {
        return INSTANCE;
    }

    /** 徽章纹理原始尺寸(与 79 张截图输出一致)。 */
    private static final int TEXTURE_W = 110;
    private static final int TEXTURE_H = 61;

    /** 显示缩小系数: honor 图标在代码中固定缩小显示(不改配置 scale, 不改预览框大小)。 */
    private static final float HONOR_DISPLAY_SCALE = 0.4f;

    // ==================== 配置字段 ====================

    private boolean configVisible = true;
    private float configScale = 1.0f;
    private String configDirection = "right";
    private int configXOffset = 0;
    private int configYOffset = 100;
    private String configScreenAnchor = "bottom_center";
    private long configDisplayDelayMs = 200L;
    private long configEnterAnimationMs = 200L;
    private float configEnterStartScale = 0.0f;
    private long configDisplayDurationMs = 3000L;
    private long configFadeOutMs = 200L;
    private boolean configBlinkFadeAnimation = false;
    // 荣誉提示框
    private boolean configHintBoxEnabled = true;
    private int configHintBoxXOffset = -30;
    private int configHintBoxHeight = 15;
    private long configHintBoxEnterMs = 600L;
    private int configHintBoxColor = 0xFFFFFF;
    private float configHintBoxMaxAlpha = 0.0f;
    private int configHintBoxTextColor = 0x000000;
    private int configHintBoxTextPadding = 3;
    private int configHintBoxTextGap = 5;
    private float configHintBoxTextScale = 0.8f;
    private boolean configHintBoxTextShadow = true;
    private float configMainIconMaxAlpha = 20.0f;
    private long configMinDisplayMs = 1000L;
    private boolean configShakeEnabled = false;
    private int configShakeCount = 2;
    private int configShakeRange = 2;

    // ==================== 显示条目 ====================

    /** 当前显示的荣誉(单条目, 新触发覆盖旧的)。 */
    private static final class Display {
        final String honorId;
        final long triggerTime;

        Display(String honorId, long triggerTime) {
            this.honorId = honorId;
            this.triggerTime = triggerTime;
        }
    }

    private Display currentDisplay;
    /** 等待显示的荣誉队列(当前显示结束后接替)。 */
    private final java.util.ArrayDeque<String> pendingHonorIds = new java.util.ArrayDeque<>();
    /** 当前显示是否已播放音效(仅在 t0 实际显示开始时播放一次, 不随收到包播放)。 */
    private boolean displaySoundPlayed = false;

    /** 是否有正在播放的荣誉显示(预览轮换用)。 */
    public boolean isDisplaying() {
        return currentDisplay != null;
    }

    @Override
    public void resetPreview() {
        currentDisplay = null;
        pendingHonorIds.clear();
        displaySoundPlayed = false;
    }

    // 抗锯齿过滤缓存(避免每帧重复 setFilter / bind)
    private boolean lastAppliedFilter = false;
    private boolean hasAppliedFilter = false;

    // 入场抖动状态
    private final java.util.Random shakeRandom = new java.util.Random();
    private float shakeOffsetX = 0.0f;
    private float shakeOffsetY = 0.0f;
    private int shakeSegment = -1;

    // ==================== 配置加载 ====================

    public void loadConfig(JsonObject config) {
        if (config == null) {
            return;
        }
        this.configVisible = !config.has("visible") || config.get("visible").getAsBoolean();
        this.configScale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
        this.configDirection = config.has("direction") ? config.get("direction").getAsString() : "right";
        this.configXOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
        this.configYOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 100;
        this.configScreenAnchor = config.has("screen_anchor") ? config.get("screen_anchor").getAsString() : "bottom_center";
        this.configDisplayDelayMs = (long) (config.has("display_delay") ? config.get("display_delay").getAsFloat() * 1000 : 200);
        this.configEnterAnimationMs = Math.max(1L, (long) (config.has("enter_animation_duration") ? config.get("enter_animation_duration").getAsFloat() * 1000 : 200));
        this.configEnterStartScale = config.has("enter_start_scale") ? config.get("enter_start_scale").getAsFloat() : 0.0f;
        this.configDisplayDurationMs = (long) (config.has("display_duration") ? config.get("display_duration").getAsFloat() * 1000 : 3000);
        this.configFadeOutMs = Math.max(1L, (long) (config.has("fade_out_duration") ? config.get("fade_out_duration").getAsFloat() * 1000 : 200));
        this.configBlinkFadeAnimation = config.has("blink_fade_animation") && config.get("blink_fade_animation").getAsBoolean();
        this.configHintBoxEnabled = !config.has("hint_box_enabled") || config.get("hint_box_enabled").getAsBoolean();
        this.configHintBoxXOffset = config.has("hint_box_x_offset") ? config.get("hint_box_x_offset").getAsInt() : -30;
        this.configHintBoxHeight = config.has("hint_box_height") ? config.get("hint_box_height").getAsInt() : 15;
        this.configHintBoxEnterMs = Math.max(1L, (long) (config.has("hint_box_enter_duration") ? config.get("hint_box_enter_duration").getAsFloat() * 1000 : 600));
        this.configHintBoxColor = parseColor(config, "hint_box_color", 0xFFFFFF);
        this.configHintBoxMaxAlpha = config.has("hint_box_max_alpha") ? config.get("hint_box_max_alpha").getAsFloat() : 0.0f;
        this.configHintBoxTextColor = parseColor(config, "hint_box_text_color", 0x000000);
        this.configHintBoxTextPadding = config.has("hint_box_text_padding") ? config.get("hint_box_text_padding").getAsInt() : 3;
        this.configHintBoxTextGap = config.has("hint_box_text_gap") ? config.get("hint_box_text_gap").getAsInt() : 5;
        this.configHintBoxTextScale = config.has("hint_box_text_scale") ? config.get("hint_box_text_scale").getAsFloat() : 0.8f;
        this.configHintBoxTextShadow = !config.has("hint_box_text_shadow") || config.get("hint_box_text_shadow").getAsBoolean();
        this.configMainIconMaxAlpha = config.has("main_icon_max_alpha") ? config.get("main_icon_max_alpha").getAsFloat() : 20.0f;
        this.configMinDisplayMs = Math.max(1L, (long) (config.has("min_display_duration") ? config.get("min_display_duration").getAsFloat() * 1000 : 1000));
        this.configShakeEnabled = config.has("shake_enabled") && config.get("shake_enabled").getAsBoolean();
        this.configShakeCount = config.has("shake_count") ? config.get("shake_count").getAsInt() : 2;
        this.configShakeRange = config.has("shake_range") ? config.get("shake_range").getAsInt() : 2;
    }

    /** 解析颜色配置(#RRGGBB 或 0x), 失败回退默认。 */
    private static int parseColor(JsonObject config, String key, int fallback) {
        if (config == null || !config.has(key)) {
            return fallback;
        }
        String raw = config.get(key).getAsString();
        try {
            if (raw.startsWith("#")) {
                return (int) Long.parseLong(raw.substring(1), 16);
            }
            if (raw.startsWith("0x") || raw.startsWith("0X")) {
                return (int) Long.parseLong(raw.substring(2), 16);
            }
            return (int) Long.parseLong(raw, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ==================== 触发 ====================

    @Override
    public void trigger(TriggerContext context) {
        // 预设语义: 当前预设未包含 kill_icon/honor 元素时, 荣誉不显示(不入队)
        if (ConfigManager.getElementConfig("kill_icon", "honor") == null) {
            return;
        }
        String extra = context.extraData();
        if (extra == null || extra.isEmpty()) {
            return;
        }
        // extraData 格式: <honorId>[:<附加数据>]
        String honorId = extra;
        int colon = extra.indexOf(':');
        if (colon > 0) {
            honorId = extra.substring(0, colon);
        }
        if (honorId.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (currentDisplay == null) {
            currentDisplay = new Display(honorId, now);
            displaySoundPlayed = false;
        } else {
            // 有显示进行中: 排队, 当前显示满最小显示时长后渐隐再接替
            pendingHonorIds.addLast(honorId);
        }
    }

    // ==================== 渲染 ====================

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        JsonObject config = ConfigManager.getElementConfig("kill_icon", "honor");
        // 预设语义: 当前预设未包含 kill_icon/honor 元素时, 荣誉不渲染
        if (config == null) {
            return;
        }
        loadConfig(config);
        if (!configVisible || currentDisplay == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long[] timeline = advanceTimeline(now, true);
        if (timeline == null) {
            return; // 延迟中 / 播放完毕 / 队列接替(下一帧渲染新显示)
        }
        long elapsed = timeline[0];
        long fadeStartMs = timeline[1];

        // 入场动画: 大小从入场初始大小平滑变为正常, 同步渐入
        float scale = configScale;
        float peakAlpha = 1.0f - Mth.clamp(configMainIconMaxAlpha, 0.0f, 100.0f) / 100.0f;
        float alpha = peakAlpha;
        float fadeProgress = 0.0f;
        if (elapsed < configEnterAnimationMs) {
            float progress = Math.min(1.0f, (float) elapsed / configEnterAnimationMs);
            progress = easeOutCubic(progress);
            scale = Mth.lerp(progress, configEnterStartScale, configScale);
            alpha = peakAlpha * progress;
        }

        // 出场淡出(无排队时起点 = display_duration; 有排队时起点 = 最小显示时长)
        if (elapsed >= fadeStartMs) {
            fadeProgress = Math.min(1.0f, (float) (elapsed - fadeStartMs) / configFadeOutMs);
            alpha = peakAlpha * resolveFadeAlpha(fadeProgress);
        }

        // 入场抖动(入场期间 x 次 ±y 像素随机位移)
        float shakeX = 0.0f;
        float shakeY = 0.0f;
        if (elapsed < configEnterAnimationMs && configShakeEnabled) {
            float[] shake = computeShake(elapsed);
            shakeX = shake[0];
            shakeY = shake[1];
        }

        // 位置(以屏幕锚点为参考)
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int centerX = ScreenAnchor.resolveCenterX(configScreenAnchor, configXOffset, screenWidth);
        int centerY = ScreenAnchor.resolveCenterY(configScreenAnchor, configYOffset, screenHeight);

        ResourceLocation texture =new ResourceLocation("gd656killicon",
                "textures/honor/honor_" + currentDisplay.honorId + ".png");

        // 主图标: 顶点带 alpha 自绘(亚像素, 立即提交, 不依赖全局 shader color 时序, 根除击杀帧透明度失效)
        float iconScale = scale * HONOR_DISPLAY_SCALE;
        applyTextureFilter(texture);
        drawIcon(guiGraphics, texture, centerX + shakeX, centerY + shakeY, iconScale, alpha);

        // 荣誉提示框与字幕(淡出与主图标同步, 跟随抖动)
        float hintFadeAlpha = (elapsed >= fadeStartMs) ? resolveFadeAlpha(fadeProgress) : 1.0f;
        drawHintBox(guiGraphics, centerX + shakeX, centerY + shakeY, TEXTURE_W * iconScale, elapsed, hintFadeAlpha);
    }

    /** 入场抖动: 入场时长按抖动次数分段, 每段随机 ±抖动范围位移(段内保持, 段切换时更新)。 */
    private float[] computeShake(long elapsed) {
        if (configShakeCount <= 0 || configShakeRange <= 0) {
            return new float[]{0.0f, 0.0f};
        }
        long segMs = Math.max(1L, configEnterAnimationMs / configShakeCount);
        int seg = (int) Math.min(configShakeCount - 1, elapsed / segMs);
        if (seg != shakeSegment) {
            shakeSegment = seg;
            shakeOffsetX = (shakeRandom.nextFloat() * 2.0f - 1.0f) * configShakeRange;
            shakeOffsetY = (shakeRandom.nextFloat() * 2.0f - 1.0f) * configShakeRange;
        }
        return new float[]{shakeOffsetX, shakeOffsetY};
    }

    // ==================== 预览渲染(元素配置界面左侧预览区域用) ====================

    /**
     * 以屏幕坐标 (originX, originY) 为中心绘制当前荣誉的完整时间线动画。
     * 供元素配置界面预览区域调用; 若当前无显示条目则跳过。
     *
     * @param baseW 预览基础宽度(未入场缩放的完整尺寸, 按预览区域缩小)
     * @param baseH 预览基础高度
     */
    public void renderAt(GuiGraphics guiGraphics, float partialTick, float originX, float originY, float baseW, float baseH) {
        loadConfig(ConfigManager.getElementConfig("kill_icon", "honor"));
        if (currentDisplay == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long[] timeline = advanceTimeline(now, false); // 预览渲染不播放荣誉弹出音效
        if (timeline == null) {
            return;
        }
        long elapsed = timeline[0];
        long fadeStartMs = timeline[1];

        float scaleFactor = 1.0f;
        float peakAlpha = 1.0f - Mth.clamp(configMainIconMaxAlpha, 0.0f, 100.0f) / 100.0f;
        float alpha = peakAlpha;
        if (elapsed < configEnterAnimationMs) {
            float progress = Math.min(1.0f, (float) elapsed / configEnterAnimationMs);
            progress = easeOutCubic(progress);
            scaleFactor = Mth.lerp(progress, configEnterStartScale, 1.0f);
            alpha = peakAlpha * progress;
        }
        float fadeProgress = 0.0f;
        if (elapsed >= fadeStartMs) {
            fadeProgress = Math.min(1.0f, (float) (elapsed - fadeStartMs) / configFadeOutMs);
            alpha = peakAlpha * resolveFadeAlpha(fadeProgress);
        }

        // 入场抖动
        float shakeX = 0.0f;
        float shakeY = 0.0f;
        if (elapsed < configEnterAnimationMs && configShakeEnabled) {
            float[] shake = computeShake(elapsed);
            shakeX = shake[0];
            shakeY = shake[1];
        }

        ResourceLocation texture = new ResourceLocation("gd656killicon",
                "textures/honor/honor_" + currentDisplay.honorId + ".png");

        // 主图标: 顶点带 alpha 自绘(亚像素, 立即提交)
        float iconScale = scaleFactor * HONOR_DISPLAY_SCALE;
        applyTextureFilter(texture);
        drawIcon(guiGraphics, texture, originX + shakeX, originY + shakeY, iconScale, alpha);

        // 荣誉提示框与字幕(淡出与主图标同步, 跟随抖动)
        float hintFadeAlpha = (elapsed >= fadeStartMs) ? resolveFadeAlpha(fadeProgress) : 1.0f;
        drawHintBox(guiGraphics, originX + shakeX, originY + shakeY, TEXTURE_W * iconScale, elapsed, hintFadeAlpha);
    }

    // ==================== 荣誉提示框与字幕 ====================

    /**
     * 绘制荣誉提示框与内部字幕。
     * 提示框 = 一个从初始位置(x 轴 + a)展开到字幕两侧(±g)的长方形,
     * 入场 c 秒内透明度从完全透明平滑变为配置最大透明度; 淡出与主图标同曲线。
     * 字幕在 1/2 c 时显示, 右侧始终与主图标左边缘相距 h, 显示后 1/2 c 内渐显。
     */
    private void drawHintBox(GuiGraphics guiGraphics, float centerX, float centerY, float iconW, long elapsed, float fadeAlpha) {
        if (!configHintBoxEnabled || currentDisplay == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String subtitle = resolveSubtitle();
        if (subtitle == null) {
            return; // 字幕被清空: 提示框与字幕都不显示(与加分项"空配置不显示"一致)
        }
        float textW = mc.font.width(subtitle) * configHintBoxTextScale;
        float iconLeft = centerX - iconW / 2f;
        float textRight = iconLeft - configHintBoxTextGap;      // 字幕右 = 主图标左 - h
        float textLeft = textRight - textW;
        float boxStart = centerX + configHintBoxXOffset;         // 初始重叠位置(两线段起点)
        float boxLeftFinal = textLeft - configHintBoxTextPadding;
        float boxRightFinal = textRight + configHintBoxTextPadding;
        float p = Math.min(1.0f, (float) elapsed / configHintBoxEnterMs);
        float eased = easeOutCubic(p);
        float boxLeft = Mth.lerp(eased, boxStart, boxLeftFinal);
        float boxRight = Mth.lerp(eased, boxStart, boxRightFinal);
        float maxAlpha = 1.0f - Mth.clamp(configHintBoxMaxAlpha, 0.0f, 100.0f) / 100.0f;
        float alpha = Math.min(maxAlpha * eased, fadeAlpha);
        if (alpha <= 0.01f) {
            return;
        }
        // 提示框(单个长方形, 左/右边界即"两条线段"的展开动画)
        int boxColor = ((int) (alpha * 255.0f) << 24) | (configHintBoxColor & 0xFFFFFF);
        float boxTop = centerY - configHintBoxHeight / 2f;
        // 亚像素填充(浮点顶点, 展开动画连续平滑, 无 1px 步进卡顿)
        fillRectF(guiGraphics, boxLeft, boxTop, boxRight, boxTop + configHintBoxHeight, boxColor);
        // 字幕: 1/2 c 后显示, 之后 1/2 c 内 alpha 0 → maxAlpha
        if (elapsed >= configHintBoxEnterMs / 2) {
            float tp = Math.min(1.0f, (elapsed - configHintBoxEnterMs / 2f) / Math.max(1f, configHintBoxEnterMs / 2f));
            float textAlpha = Math.min(maxAlpha * easeOutCubic(tp), fadeAlpha);
            if (textAlpha > 0.01f) {
                int textColor = ((int) (textAlpha * 255.0f) << 24) | (configHintBoxTextColor & 0xFFFFFF);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(textLeft, centerY, 0.0f);
                guiGraphics.pose().scale(configHintBoxTextScale, configHintBoxTextScale, 1.0f);
                guiGraphics.drawString(mc.font, subtitle, 0.0f, -mc.font.lineHeight / 2f, textColor, configHintBoxTextShadow);
                guiGraphics.pose().popPose();
            }
        }
    }

    /**
     * 主图标绘制: 顶点带 alpha 的自绘四边形(POSITION_COLOR_TEX)。
     * 立即提交, 不依赖 RenderSystem.setShaderColor 的批次时序 —— 根除击杀瞬间
     * 其它渲染器增多导致 honor 的 blit 延迟提交、alpha 被全局 color 覆盖为不透明的 bug。
     */
    private static void drawIcon(GuiGraphics guiGraphics, ResourceLocation texture,
                                 float centerX, float centerY, float iconScale, float alpha) {
        if (alpha <= 0.001f || iconScale <= 0.001f) {
            return;
        }
        int a = (int) (Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
        float w = TEXTURE_W * iconScale;
        float h = TEXTURE_H * iconScale;
        float x1 = centerX - w / 2f;
        float y1 = centerY - h / 2f;
        float x2 = centerX + w / 2f;
        float y2 = centerY + h / 2f;
        Matrix4f matrix = guiGraphics.pose().last().pose();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        builder.vertex(matrix, x1, y1, 0).color(255, 255, 255, a).uv(0.0f, 0.0f).endVertex();
        builder.vertex(matrix, x2, y1, 0).color(255, 255, 255, a).uv(1.0f, 0.0f).endVertex();
        builder.vertex(matrix, x2, y2, 0).color(255, 255, 255, a).uv(1.0f, 1.0f).endVertex();
        builder.vertex(matrix, x1, y2, 0).color(255, 255, 255, a).uv(0.0f, 1.0f).endVertex();
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /** 应用全局抗锯齿过滤(配置值缓存, 相同则跳过, 避免每帧 bind/setFilter GL 操作)。 */
    private void applyTextureFilter(ResourceLocation texture) {
        boolean filter = ClientConfigManager.isEnableIconAntialiasing();
        if (hasAppliedFilter && filter == lastAppliedFilter) {
            return;
        }
        IconTextureFilterEffect.apply(texture);
        lastAppliedFilter = filter;
        hasAppliedFilter = true;
    }

    /**
     * 字幕文本解析(与加分项 format 同款, 所见即所得):
     * 配置键 format_<honor_id> 有值 → 玩家自定义文本;
     * 否则 → 注册表 format(若提供); 再否则 → 荣誉显示名(I18n lang 键)。
     */
    /**
     * 字幕文本解析(与加分项 format 同款, 所见即所得):
     * 配置键 format_<honor_id> 存在且为空(玩家清空) → 返回 null, 不显示字幕与提示框;
     * 配置存在且非空 → 玩家自定义文本;
     * 配置不存在 → 注册表 format(若提供); 再否则 → 荣誉显示名(I18n lang 键)。
     */
    private String resolveSubtitle() {
        String formatKey = "format_" + currentDisplay.honorId;
        JsonObject config = ConfigManager.getElementConfig("kill_icon", "honor");
        if (config != null && config.has(formatKey)) {
            String custom = config.get(formatKey).getAsString();
            if (custom.isEmpty()) {
                return null; // 玩家清空 → 不显示(与加分项一致)
            }
            return custom;
        }
        org.mods.gd656killicon.common.honor.HonorDefinition def =
                org.mods.gd656killicon.common.honor.HonorRegistry.get(currentDisplay.honorId);
        if (def != null) {
            // 语言驱动默认字幕: 优先 lang 键 gd656killicon.honor.<id>.format(多语言, 与 bonus 语言默认机制一致)
            String langFormatKey = def.formatLangKey();
            String langFormat = null;
            if (net.minecraft.client.resources.language.I18n.exists(langFormatKey)) {
                String resolved = net.minecraft.client.resources.language.I18n.get(langFormatKey);
                if (resolved != null && !resolved.isEmpty() && !resolved.equals(langFormatKey)) {
                    langFormat = resolved;
                }
            }
            return org.mods.gd656killicon.common.honor.HonorRegistry.resolveFormat(
                    currentDisplay.honorId, null, langFormat);
        }
        return currentDisplay.honorId;
    }

    /**
     * 亚像素填充矩形(浮点顶点, 连续平滑无 1px 步进)。
     * 参照 IconEntranceBackground 的浮点四边形方案, 避免展开动画按整像素跳变卡顿。
     */
    private static void fillRectF(GuiGraphics guiGraphics, float x1, float y1, float x2, float y2, int argb) {
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        Matrix4f matrix = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        quad(builder, matrix, x1, y1, x2, y2, r, g, b, a);
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /** 浮点四边形顶点(与 IconEntranceBackground.quad 同款, 1.20.1 BufferBuilder 签名)。 */
    private static void quad(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float x2, float y2, int r, int g, int b, int a) {
        builder.vertex(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y1, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y2, 0).color(r, g, b, a).endVertex();
    }

    // ==================== 时序推进 ====================

    /**
     * 推进显示时序(render / renderAt 共用):
     * 延迟等待(display_delay)→ 音频随显示播放 → 显示/淡出(有排队时满最小显示时长后渐隐)→ 队列接替。
     * 返回 [elapsed, fadeStartMs]; 无显示/延迟中/本帧接替返回 null。
     *
     * @param playSound 是否在显示开始时播放荣誉音效(预览渲染传 false, 不触发弹出音效)
     */
    private long[] advanceTimeline(long now, boolean playSound) {
        if (currentDisplay == null) {
            return null;
        }
        long t0 = currentDisplay.triggerTime + configDisplayDelayMs; // 显示开始时刻(收到请求 + 入场显示延迟)
        long elapsed = now - t0;
        if (elapsed < 0) {
            return null; // 延迟等待中
        }
        // 音频: 随图标实际显示播放(而非收到服务端包时); 预览渲染不播放音效
        if (playSound && !displaySoundPlayed) {
            playDisplaySound();
            displaySoundPlayed = true;
        }
        boolean hasNext = !pendingHonorIds.isEmpty();
        // 淡出起点: 有排队 → 满最小显示时长后渐隐; 无排队 → 正常显示时长
        long fadeStartMs = hasNext ? configMinDisplayMs : configDisplayDurationMs;
        long total = fadeStartMs + configFadeOutMs;
        if (elapsed >= total) {
            if (hasNext) {
                currentDisplay = new Display(pendingHonorIds.pollFirst(), now);
                displaySoundPlayed = false;
            } else {
                currentDisplay = null;
                displaySoundPlayed = false;
            }
            return null; // 本帧不渲染, 下一帧渲染新显示
        }
        return new long[]{elapsed, fadeStartMs};
    }

    /** 荣誉图标显示时播放普通成就音效(高级槽位已注册但不触发)。 */
    private void playDisplaySound() {
        try {
            org.mods.gd656killicon.client.sounds.ExternalSoundManager.playConfiguredSound(
                    ConfigManager.getCurrentPresetId(),
                    org.mods.gd656killicon.client.sounds.ExternalSoundManager.SLOT_HONOR_NORMAL);
        } catch (Exception ignored) {
        }
    }

    // ==================== 时间曲线 ====================

    private static float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3);
    }

    /**
     * 淡出阶段透明度曲线(与滚动元素一致)。
     * 未启用闪动: 线性 1 → 0。
     * 启用闪动: 前 1/3 alpha 1 → 0.2, 中 1/3 0.2 → 1, 后 1/3 1 → 0。
     */
    private float resolveFadeAlpha(float fadeProgress) {
        if (configBlinkFadeAnimation) {
            if (fadeProgress < 1.0f / 3.0f) {
                return 1.0f - 2.4f * fadeProgress;
            } else if (fadeProgress < 2.0f / 3.0f) {
                return 2.4f * fadeProgress - 0.6f;
            }
            return 3.0f * (1.0f - fadeProgress);
        }
        return 1.0f - fadeProgress;
    }
}
