package org.mods.gd656killicon.client.render.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.common.killtype.KillTypeDefinition;
import org.mods.gd656killicon.common.killtype.KillTypeRegistry;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.render.PreviewRenderTimeContext;
import org.mods.gd656killicon.client.render.effect.SubtitleEntranceBackground;
import org.mods.gd656killicon.client.render.effect.TextFadeEffect;
import org.mods.gd656killicon.client.util.ClientMessageLogger;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renderer for the kill feed subtitle element.
 * Displays a customizable message when a kill occurs, e.g., "You killed <target> with <weapon>".
 */
public class SubtitleRenderer implements IHudRenderer {

    private static final SubtitleRenderer INSTANCE = new SubtitleRenderer();

    public static SubtitleRenderer getInstance() {
        return INSTANCE;
    }

    /**
     * kill_feed 字幕当前是否有内容显示(队列联动用)。
     */
    public boolean hasVisibleSubtitle() {
        return this.isVisible;
    }

    /**
     * kill_feed 字幕的基准 y(未联动时的位置, 队列联动用, 与 render 中 baseTextY 计算一致)。
     */
    public float getBaseTextY() {
        // 直接进游戏时 kill_feed 未显示, render 提前 return 导致惰性加载从未执行;
        // 联动查询时确保配置已加载(configYOffset 反映玩家配置而非默认 20)
        if (!this.linkageConfigLoaded) {
            JsonObject cfg = ConfigManager.getElementConfig("subtitle", "kill_feed");
            if (cfg != null) {
                loadConfig(cfg);
            }
            this.linkageConfigLoaded = true;
        }
        Minecraft mc = Minecraft.getInstance();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        return org.mods.gd656killicon.client.render.ScreenAnchor.resolveCenterY(configScreenAnchor, configYOffset, screenHeight);
    }

    private static final long FADE_IN_DURATION = 200L;     private static final long FADE_OUT_DURATION = 300L;     private static final int DEFAULT_PLACEHOLDER_COLOR = 0xFF008B8B;
    private static final int DEFAULT_EMPHASIS_COLOR = 0xFFFFFFFF;
    private static final long SCORE_CACHE_WINDOW_MS = 10000L;
    private static final int PREVIEW_SCORE_VICTIM_ID = -9999;
    private static final String RUSH_BOMB_PLANTED_CAPTURE_FORMAT = "format_rush_bomb_planted_capture";
    private static final String RUSH_BOMB_DEFUSED_CAPTURE_FORMAT = "format_rush_bomb_defused_capture";
    private static final String RUSH_OBJECTIVE_DESTROYED_CAPTURE_FORMAT = "format_rush_objective_destroyed_capture";
    private static final Map<Integer, ScoreEntry> RECENT_SCORES = new ConcurrentHashMap<>();
    private static final java.util.Deque<ScoreEntry> RECENT_SCORE_QUEUE = new java.util.ArrayDeque<>();
    private static final int SCORE_QUEUE_MAX = 50;

    
    private int configXOffset = 0;
    private int configYOffset = 20;
    private String configScreenAnchor = "bottom_center";
    private long displayDuration = 3000L;
    private String format = "";
    private int placeholderColor = DEFAULT_PLACEHOLDER_COLOR;
    private boolean enablePlaceholderBold = false;
    private float scale = 1.0f;
    private int emphasisColor = DEFAULT_EMPHASIS_COLOR;
    
    /** 击杀类型启用开关（KillTypeRegistry.enableKey 驱动；无开关键的类型无条目） */
    private final java.util.Map<String, Boolean> killTypeEnableFlags = new java.util.HashMap<>();

    private boolean enableStacking = false;
    private boolean enableTextShadow = true;
    private boolean configBlinkFadeAnimation = false;
    private boolean enableGlowEffect = false;
    private float glowIntensity = 0.5f;
    private float glowSize = 0.3f;
    private int glowColorRgb = 0xFFFFFF;
    private float glowAlphaMultiplier = 1.0f;
    private boolean alignLeft = false;
    private boolean alignRight = false;
    private boolean configEnableFlashIn = true;
    private long fadeInDurationMs = 0L;
    private boolean enableQueueLinkage = false;
    private float queueLinkageScrollSpeed = 0.1f;
    private float queueLinkageIconYOffset = 0.0f;
    private boolean linkageConfigLoaded = false;
    private float linkedYOffset = 0.0f;
    private float linkageFromOffset = 0.0f;
    private long linkageStartTime = 0L;
    private float linkageTargetOffset = 0.0f;
    private float linkageBaseY = 0.0f;
    private boolean configEntranceBackground = false;
    private float configEntranceBgPeakTransparency = 0.2f;
    private long configEntranceBgFadeInMs = 1000L / 15;
    private long configEntranceBgSweepMs = 100L;
    private long configEntranceBgFadeOutMs = 2000L / 15;
    private int configEntranceBgColor = 0xFFFFFF;
    private int maxLines = 5;
    private int lineSpacing = 12;
    private int normalTextColor = 0xFFFFFFFF;

    private long startTime = -1;
    private boolean isVisible = false;
    private boolean isPreview = false;
    private long textHideTime = -1;
    private int currentKillType = KillType.NORMAL;
    private int victimId = -1;
    private int currentVictimId = -1;
    private String currentScoreOverride = null;
    private String victimName = "";
    private ItemStack heldItem = ItemStack.EMPTY;
    private String currentWeaponName = "";
    private String rawFormat = "";     private float currentDistance = 0.0f;

    private final List<SubtitleItem> stackedItems = new ArrayList<>();
    private final java.util.Deque<SubtitleItem> pendingQueue = new java.util.ArrayDeque<>();
    private long lastDequeueTime = 0;

    public SubtitleRenderer() {
    }

    @Override
    public void trigger(TriggerContext context) {
        JsonObject config = ConfigManager.getElementConfig("subtitle", "kill_feed");
        if (config == null) {
            return;
        }
        this.isPreview = false;

        loadConfig(config);

        if (!config.has("visible") || !config.get("visible").getAsBoolean()) {
            this.isVisible = false;
            return;
        }

        if (!isKillTypeEnabled(context.type())) {
            return;
        }

        int type = context.type();
        int entityId = context.entityId();
        Minecraft mc = Minecraft.getInstance();
        String vName;
        
        String rawExtra = context.extraData() == null ? "" : context.extraData();
        String captureWeaponToken = "";
        String captureFormatOverride = null;
        String captureScoreOverride = null;
        if (!rawExtra.isEmpty()) {
            String extra = rawExtra;
            if (type == KillType.CAPTURE) {
                String[] parts = extra.split("\\|", 3);
                captureWeaponToken = parts.length > 0 ? parts[0].trim() : "";
                String captureTarget = parts.length > 1 ? parts[1].trim() : "";
                captureFormatOverride = resolveRushCaptureFormat(captureWeaponToken);
                if (captureFormatOverride != null) {
                    captureScoreOverride = isNumericToken(captureTarget) ? captureTarget : null;
                    captureWeaponToken = "";
                }
                String captureDisplayTarget = parts.length > 2 ? parts[2].trim() : captureTarget;
                if (!captureDisplayTarget.isEmpty() && !isNumericToken(captureDisplayTarget)) {
                    vName = captureDisplayTarget;
                } else if (!captureTarget.isEmpty() && !isNumericToken(captureTarget)) {
                    vName = captureTarget;
                } else if (!captureWeaponToken.isEmpty()) {
                    vName = captureWeaponToken;
                } else {
                    vName = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.unknown");
                }
            } else if (type == KillType.DESTROY_VEHICLE) {
                if (extra.contains("|")) {
                    String[] parts = extra.split("\\|", 2);
                    vName = parts[0];
                } else {
                    vName = extra;
                }
            } else {
                vName = extra;
            }
        } else if (mc.level != null && entityId != -1) {
            net.minecraft.world.entity.Entity entity = mc.level.getEntity(entityId);
            if (entity != null) {
                vName = entity.getDisplayName().getString();
            } else {
                vName = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.unknown");
            }
        } else {
            vName = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.unknown");
        }

        ItemStack itemStack = ItemStack.EMPTY;
        String wName;
        if (type == KillType.CAPTURE) {
            itemStack = ItemStack.EMPTY;
            wName = captureWeaponToken == null || captureWeaponToken.isEmpty()
                ? net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.unknown")
                : org.mods.gd656killicon.client.util.I18nCompat.exists(captureWeaponToken)
                    ? net.minecraft.client.resources.language.I18n.get(captureWeaponToken)
                    : captureWeaponToken;
        } else if (mc.player != null) {
            if (mc.player.getVehicle() != null) {
                itemStack = ItemStack.EMPTY;
                wName = mc.player.getVehicle().getDisplayName().getString();
            } else {
                itemStack = mc.player.getMainHandItem();
                wName = itemStack.isEmpty() 
                    ? net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.bare_hand") 
                    : itemStack.getHoverName().getString();
            }
        } else {
            itemStack = ItemStack.EMPTY;
            wName = "Unknown";
        }
        
        String formatKey = formatKeyForType(type);
        String colorKey = placeholderColorKeyForType(type);
        String emphasisColorKey = emphasisColorKeyForType(type);

        // 配置值即实际文本(所见即所得); 缺键兜底 = 击杀类型注册表当前语言默认(替代已删除的 lang key)
        String normalFormat = config.has("format_normal") ? config.get("format_normal").getAsString()
                : KillTypeRegistry.get(type).format();
        String resolvedFormat = config.has(formatKey) ? config.get(formatKey).getAsString() : normalFormat;
        if (captureFormatOverride != null && config.has(captureFormatOverride)) {
            resolvedFormat = config.get(captureFormatOverride).getAsString();
        }

        String normalColorHex = config.has("color_normal_placeholder") ? config.get("color_normal_placeholder").getAsString() : "#008B8B";
        String chosenColorHex = config.has(colorKey) ? config.get(colorKey).getAsString() : normalColorHex;
        int pColor = parseColorHexOrDefault(chosenColorHex, DEFAULT_PLACEHOLDER_COLOR);
        
        String emphasisHex = config.has(emphasisColorKey) ? config.get(emphasisColorKey).getAsString() : "#FFFFFF";
        int eColor = parseColorHexOrDefault(emphasisHex, DEFAULT_EMPHASIS_COLOR);

        float dist = isNormalKillType(type) ? context.distance() : 0.0f;

        if (this.enableStacking) {
            addItemToStack(resolvedFormat, pColor, eColor, wName, vName, this.displayDuration, dist, entityId, captureScoreOverride);
        } else {
            this.currentKillType = type;
            this.victimId = entityId;
            this.currentVictimId = entityId;
            this.currentScoreOverride = captureScoreOverride;
            this.victimName = vName;
            this.heldItem = itemStack;
            this.currentWeaponName = wName;
            this.format = resolvedFormat;
            this.placeholderColor = pColor;
            this.emphasisColor = eColor;
            this.currentDistance = dist;

            if (this.displayDuration < FADE_IN_DURATION) {
                this.displayDuration = FADE_IN_DURATION;
            }

            this.startTime = PreviewRenderTimeContext.currentTimeMillis();
            this.textHideTime = this.startTime + this.displayDuration;
            this.isVisible = true;
        }
    }

    public void triggerPreview(int killType, String weaponName, String victimName) {
        this.currentKillType = killType;
        this.victimId = PREVIEW_SCORE_VICTIM_ID;
        this.currentVictimId = PREVIEW_SCORE_VICTIM_ID;
        this.isPreview = true;
        this.victimName = victimName != null ? victimName : "";
        this.heldItem = ItemStack.EMPTY;
        this.currentWeaponName = weaponName != null ? weaponName : "Unknown";
        JsonObject config = ConfigManager.getElementConfig("subtitle", "kill_feed");
        if (config == null) {
            return;
        }
        
        loadConfig(config);

        if (!config.has("visible") || !config.get("visible").getAsBoolean()) {
            this.isVisible = false;
            return;
        }

        if (!isKillTypeEnabled(killType)) {
            return;
        }
        
        String formatKey = formatKeyForType(killType);
        String colorKey = placeholderColorKeyForType(killType);
        String emphasisColorKey = emphasisColorKeyForType(killType);

        String normalFormat = config.has("format_normal") ? config.get("format_normal").getAsString()
                : KillTypeRegistry.get(killType).format();
        String resolvedFormat = config.has(formatKey) ? config.get(formatKey).getAsString() : normalFormat;

        String normalColorHex = config.has("color_normal_placeholder") ? config.get("color_normal_placeholder").getAsString() : "#008B8B";
        String chosenColorHex = config.has(colorKey) ? config.get(colorKey).getAsString() : normalColorHex;
        int pColor = parseColorHexOrDefault(chosenColorHex, DEFAULT_PLACEHOLDER_COLOR);
        
        String emphasisHex = config.has(emphasisColorKey) ? config.get(emphasisColorKey).getAsString() : "#FFFFFF";
        int eColor = parseColorHexOrDefault(emphasisHex, DEFAULT_EMPHASIS_COLOR);

        float dist = isNormalKillType(killType) ? 50.0f : 0.0f;

        if (this.enableStacking) {
             addItemToStack(resolvedFormat, pColor, eColor, this.currentWeaponName, this.victimName, this.displayDuration, dist, PREVIEW_SCORE_VICTIM_ID, null);
        } else {
            this.format = resolvedFormat;
            this.placeholderColor = pColor;
            this.emphasisColor = eColor;
            this.currentDistance = dist;
            
            if (this.displayDuration < FADE_IN_DURATION) {
                this.displayDuration = FADE_IN_DURATION;
            }

            this.startTime = PreviewRenderTimeContext.currentTimeMillis();
            this.textHideTime = this.startTime + this.displayDuration;
            this.isVisible = true;
        }
    }

    private void addItemToStack(String format, int pColor, int eColor, String wName, String vName, long duration, float distance, int victimId, String scoreOverride) {
        SubtitleItem newItem = new SubtitleItem(format, pColor, eColor, wName, vName, 0, duration, distance, victimId, scoreOverride);
        if (this.pendingQueue.size() >= 10) {
            return;
        }
        
        this.pendingQueue.add(newItem);
        this.isVisible = true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        if (!this.isVisible) return;
        
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int centerX = org.mods.gd656killicon.client.render.ScreenAnchor.resolveCenterX(configScreenAnchor, configXOffset, screenWidth);
        float baseTextY = org.mods.gd656killicon.client.render.ScreenAnchor.resolveCenterY(configScreenAnchor, configYOffset, screenHeight);
        float textY = baseTextY;

        // 首次渲染时惰性加载一次配置(直接进游戏时联动配置立即生效; 之后由事件/配置界面驱动更新)
        if (!this.linkageConfigLoaded) {
            JsonObject cfg = ConfigManager.getElementConfig("subtitle", "kill_feed");
            if (cfg != null) {
                loadConfig(cfg);
            }
            this.linkageConfigLoaded = true;
        }

        // 队列联动: 击杀图标未显示时, kill_feed 在 x 秒内平滑上移到图标 y; 图标重新出现则按原路径/速度返回
        if (this.enableQueueLinkage) {
            ScrollingIconRenderer iconRenderer = ScrollingIconRenderer.getInstance();
            boolean iconsVisible = iconRenderer.hasVisibleIcons();
            float iconY = iconRenderer.getIconsAnchorY();
            float targetY = iconsVisible ? baseTextY : iconY + this.queueLinkageIconYOffset;
            textY = smoothLinkedY(baseTextY, targetY);
        } else {
            this.linkageStartTime = 0L;
            this.linkedYOffset = 0.0f;
        }

        if (this.enableStacking) {
            renderStacked(guiGraphics, font, centerX, textY);
        } else {
            RenderState state = resolveRenderState();
            if (state == null) return;
            renderInternal(guiGraphics, font, centerX, textY, state, this.format, this.placeholderColor, this.emphasisColor, this.currentWeaponName, this.victimName, this.currentDistance, this.currentVictimId, this.currentScoreOverride, this.startTime);
        }
    }

    public void renderAt(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY) {
        if (!this.isVisible) return;
        
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int resolvedCenterX = Math.round(centerX);
        int resolvedTextY = Math.round(centerY);

        if (this.enableStacking) {
             renderStacked(guiGraphics, font, resolvedCenterX, resolvedTextY);
        } else {
            RenderState state = resolveRenderState();
            if (state == null) return;
            renderInternal(guiGraphics, font, resolvedCenterX, resolvedTextY, state, this.format, this.placeholderColor, this.emphasisColor, this.currentWeaponName, this.victimName, this.currentDistance, this.currentVictimId, this.currentScoreOverride, this.startTime);
        }
    }

    /**
     * 队列联动的平滑移动: 参考加分项字幕行的平滑, 但使用 x 秒内完成的时间线插值(easeOutCubic),
     * 全程浮点无取整(避免颗粒感), 到时长即锁定目标(不会无限渐近超时)。
     */
    private float smoothLinkedY(float baseY, float targetY) {
        float targetOffset = targetY - baseY;
        long now = System.currentTimeMillis();
        long durationMs = Math.max(1L, (long) (this.queueLinkageScrollSpeed * 1000.0f));

        // 目标未变化: 继续当前时间线
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

        // 目标变化或首次: 从当前值开始新的时间线
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

    private void renderStacked(GuiGraphics guiGraphics, Font font, int centerX, float startY) {
        long now = PreviewRenderTimeContext.currentTimeMillis();
        
        if (!pendingQueue.isEmpty()) {
            if (now - lastDequeueTime >= 200) {
                SubtitleItem newItem = pendingQueue.poll();
                if (newItem != null) {
                    newItem.spawnTime = now;                     this.stackedItems.add(newItem);
                    
                    while (this.stackedItems.size() > this.maxLines) {
                         this.stackedItems.remove(0);                     }
                    
                    lastDequeueTime = now;
                }
            }
        }
        
        if (stackedItems.isEmpty()) {
            this.isVisible = false;
            return;
        }

        
        boolean hasVisibleItems = false;
        
        Iterator<SubtitleItem> iterator = stackedItems.iterator();
        while (iterator.hasNext()) {
            SubtitleItem item = iterator.next();
            long hideTime = item.spawnTime + item.duration;
            if (now >= hideTime + FADE_OUT_DURATION) {
                iterator.remove();
            } else {
                hasVisibleItems = true;
            }
        }
        
        if (!hasVisibleItems && stackedItems.isEmpty()) {
            this.isVisible = false;
            return;
        }

        renderStackItems(guiGraphics, font, centerX, startY);
    }

    private void renderStackItems(GuiGraphics guiGraphics, Font font, int centerX, float startY) {
        long now = PreviewRenderTimeContext.currentTimeMillis();
        
        for (int i = 0; i < stackedItems.size(); i++) {
            SubtitleItem item = stackedItems.get(i);
            
            int posFromBottom = stackedItems.size() - 1 - i;
            float targetRelY = - (posFromBottom * this.lineSpacing);
            
            float smooth = 0.2f;             item.currentRelY = Mth.lerp(smooth, item.currentRelY, targetRelY);
            
            if (Math.abs(item.currentRelY - targetRelY) < 0.5f) item.currentRelY = targetRelY;
            
            float itemAlpha = 1.0f;
            
            long hideTime = item.spawnTime + item.duration;
            if (now >= hideTime) {
                long fadeElapsed = now - hideTime;
                itemAlpha = Math.max(0.0f, 1.0f - (float) fadeElapsed / FADE_OUT_DURATION);
            }
            
            if (i == stackedItems.size() - 1) {
                long elapsed = now - item.spawnTime;
                if (elapsed < FADE_IN_DURATION) {
                     float fadeIn = (float) elapsed / FADE_IN_DURATION;
                     itemAlpha = Math.min(itemAlpha, fadeIn);
                }
            }
            
            if (this.maxLines > 1) {
                float posAlpha = Math.max(0.0f, 1.0f - (float) posFromBottom / (this.maxLines - 1));
                itemAlpha *= posAlpha;
            }
            
            if (itemAlpha <= 0.05f) continue;

            float drawY = startY + item.currentRelY;
            
            RenderState state = new RenderState(now - item.spawnTime, itemAlpha, this.scale);
            
            renderInternal(guiGraphics, font, centerX, drawY, state, item.format, item.pColor, item.eColor, item.wName, item.vName, item.distance, item.victimId, item.scoreOverride, item.spawnTime);
        }
    }

    private RenderState resolveRenderState() {
        if (!isVisible || startTime == -1) return null;

        long currentTime = PreviewRenderTimeContext.currentTimeMillis();
        long elapsed = currentTime - startTime;

        float alpha = calculateAlpha(currentTime);
        // 渐入阶段(elapsed < fadeInDurationMs)alpha 从 0 平滑上升, 不能因低 alpha 被误判隐藏;
        // 仅渐入结束后 alpha 仍 ≤0.05 才视为隐藏/移除
        boolean fadingIn = this.startTime >= 0 && this.fadeInDurationMs > 0
                && (currentTime - this.startTime) < this.fadeInDurationMs;
        if ((alpha <= 0.05f && !fadingIn)
                || (this.textHideTime > 0 && currentTime >= this.textHideTime + FADE_OUT_DURATION)) {
            isVisible = false;
            startTime = -1;
            return null;
        }

        float currentScale = this.scale;
        /*
        if (elapsed < FADE_IN_DURATION) {
            float progress = (float) elapsed / FADE_IN_DURATION;
            float easedProgress = 1.0f - (float) Math.pow(1.0f - progress, 3);
            currentScale = Mth.lerp(easedProgress, 1.5f, this.scale);
        }
        */

        return new RenderState(elapsed, alpha, currentScale);
    }
    
    private boolean enableScaleAnimation = false;

    private void renderInternal(GuiGraphics guiGraphics, Font font, int centerX, float textY, RenderState state, 
                              String fmt, int pColor, int eColor, String wName, String vName, float distance, int victimId, String scoreOverride, long referenceTime) {
        // 亚像素平滑: 文本 y 的小数部分通过 pose 平移注入(MC drawString 坐标为 int),
        // 避免联动/动画时每帧整像素跳变产生颗粒感
        float textYFrac = textY - (float) Math.floor(textY);
        int textYInt = (int) Math.floor(textY);

        float colorProgress = configEnableFlashIn ? getColorProgress(state.elapsed) : 1.0f;
        String scoreStr = scoreOverride == null || scoreOverride.isBlank()
            ? resolveScoreString(victimId, referenceTime)
            : scoreOverride;
        Component fullText = buildFullText(fmt, pColor, eColor, wName, vName, scoreStr, colorProgress, distance);

        int textWidth = font.width(fullText);
        int textX;
        if (alignLeft && !alignRight) {
            textX = centerX;
        } else if (alignRight && !alignLeft) {
            textX = centerX - textWidth;
        } else {
            textX = centerX - textWidth / 2;
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        float pivotX;
        if (alignLeft && !alignRight) {
            pivotX = textX;
        } else if (alignRight && !alignLeft) {
            pivotX = textX + textWidth;
        } else {
            pivotX = textX + textWidth / 2.0f;
        }
        float pivotY = textY + font.lineHeight / 2.0f;

        // 字幕入场背景(位于文本之下): 屏幕坐标, 与字幕 pose 缩放对齐; 每次字幕出现(t0)触发;
        // 右对齐时完全镜像(从右侧显示往左侧扫), 左对齐/居中为现有方向(左边框固定向右扫)
        if (configEntranceBackground) {
            float scale = state.currentScale;
            float textLeftScreen = pivotX + (textX - pivotX) * scale;
            float textRightScreen = pivotX + (textX + textWidth - pivotX) * scale;
            float midYScreen = pivotY;
            float textHeightScreen = font.lineHeight * scale;
            boolean mirror = alignRight && !alignLeft;
            SubtitleEntranceBackground.draw(guiGraphics, state.elapsed,
                    textLeftScreen, textRightScreen, midYScreen, textHeightScreen,
                    configEntranceBgFadeInMs, configEntranceBgSweepMs, configEntranceBgFadeOutMs,
                    configEntranceBgPeakTransparency, configEntranceBgColor, mirror);
        }

        poseStack.translate(pivotX, pivotY, 0);
        
        float s = state.currentScale;
        if (this.enableScaleAnimation && state.elapsed < FADE_IN_DURATION) {
             float progress = (float) state.elapsed / FADE_IN_DURATION;
             float easedProgress = 1.0f - (float) Math.pow(1.0f - progress, 3);
             s = Mth.lerp(easedProgress, this.scale * 1.5f, this.scale);
        } else {
             s = this.scale;
        }
        
        poseStack.scale(s, s, 1.0f);
        poseStack.translate(-pivotX, -pivotY, 0);
        // 亚像素偏移: 文本 y 小数部分在 scale 后注入, 使最终绘制位置 = 浮点 textY(平滑无颗粒)
        if (textYFrac > 0.001f) {
            poseStack.translate(0.0f, textYFrac, 0.0f);
        }

        int alphaInt = (int) (state.alpha * 255.0f) << 24;
        int colorWithAlpha = (normalTextColor & 0x00FFFFFF) | alphaInt;
        // 文本发光(与 score/bonus_list 同款): 8 个 ±glowSize 偏移绘制低透明度文本, 再叠主文本
        if (this.enableGlowEffect) {
            int glowAlpha = (int) (state.alpha * this.glowIntensity * this.glowAlphaMultiplier * 255.0f);
            glowAlpha = Math.max((int) (TextFadeEffect.MIN_ALPHA * 255.0f), Math.min(255, glowAlpha));  // 副本最小透明度 0.1
            glowAlpha = Math.max(0, Math.min(255, glowAlpha));
            int glowColor = (this.glowColorRgb & 0x00FFFFFF) | (glowAlpha << 24);
            // 发光副本一律显示配置的发光色: 递归清除各段样式颜色(null),
            // 渲染时 RGB/alpha 全部来自 drawString 传入的 glowColor(不受主字幕样式色影响)
            Component glowComponent = stripColor(fullText);
            float[][] offsets = {
                {-glowSize, 0}, {glowSize, 0}, {0, -glowSize}, {0, glowSize},
                {-glowSize, -glowSize}, {glowSize, -glowSize},
                {-glowSize, glowSize}, {glowSize, glowSize}
            };
            for (float[] offset : offsets) {
                poseStack.pushPose();
                poseStack.translate(offset[0], offset[1], 0);
                guiGraphics.drawString(font, glowComponent, textX, textYInt, glowColor, false);
                poseStack.popPose();
            }
        }
        guiGraphics.drawString(font, fullText, textX, textYInt, colorWithAlpha, this.enableTextShadow);

        poseStack.popPose();
    }

    private static final class RenderState {
        private final long elapsed;
        private final float alpha;
        private final float currentScale;

        private RenderState(long elapsed, float alpha, float currentScale) {
            this.elapsed = elapsed;
            this.alpha = alpha;
            this.currentScale = currentScale;
        }
    }
    
    private static class SubtitleItem {
        String format;
        int pColor;
        int eColor;
        String wName;
        String vName;
        long spawnTime;
        long duration;
        float currentRelY;         float distance;
        int victimId;
        String scoreOverride;
        
        public SubtitleItem(String format, int pColor, int eColor, String wName, String vName, long spawnTime, long duration, float distance, int victimId, String scoreOverride) {
            this.format = format;
            this.pColor = pColor;
            this.eColor = eColor;
            this.wName = wName;
            this.vName = vName;
            this.spawnTime = spawnTime;
            this.duration = duration;
            this.currentRelY = 0;             this.distance = distance;
            this.victimId = victimId;
            this.scoreOverride = scoreOverride;
        }
    }

    public static void recordBonusScore(int bonusType, float score, int victimId) {
        if (victimId == -1) return;
        long now = PreviewRenderTimeContext.currentTimeMillis();
        ScoreEntry entry = new ScoreEntry(victimId, score, now);
        RECENT_SCORES.put(victimId, entry);
        RECENT_SCORE_QUEUE.addLast(entry);
        while (RECENT_SCORE_QUEUE.size() > SCORE_QUEUE_MAX) {
            RECENT_SCORE_QUEUE.removeFirst();
        }
        while (!RECENT_SCORE_QUEUE.isEmpty() && now - RECENT_SCORE_QUEUE.peekFirst().timestamp > SCORE_CACHE_WINDOW_MS) {
            RECENT_SCORE_QUEUE.removeFirst();
        }
    }

    private static String resolveScoreString(int victimId, long referenceTime) {
        if (victimId == PREVIEW_SCORE_VICTIM_ID) return "20";
        long now = PreviewRenderTimeContext.currentTimeMillis();
        if (victimId != -1) {
            ScoreEntry entry = RECENT_SCORES.get(victimId);
            if (entry != null) {
                if (now - entry.timestamp <= SCORE_CACHE_WINDOW_MS) {
                    return formatScore(entry.score);
                }
                RECENT_SCORES.remove(victimId);
            }
        }
        ScoreEntry closest = null;
        long closestDelta = Long.MAX_VALUE;
        Iterator<ScoreEntry> iterator = RECENT_SCORE_QUEUE.iterator();
        while (iterator.hasNext()) {
            ScoreEntry entry = iterator.next();
            if (now - entry.timestamp > SCORE_CACHE_WINDOW_MS) {
                iterator.remove();
                continue;
            }
            long delta = Math.abs(entry.timestamp - referenceTime);
            if (delta < closestDelta) {
                closestDelta = delta;
                closest = entry;
            }
        }
        if (closest != null) {
            return formatScore(closest.score);
        }
        return "0";
    }

    private static String formatScore(float score) {
        if (score < 1.0f && score > 0.0f) {
            return String.format("%.1f", score);
        }
        return String.valueOf(Math.round(score));
    }

    private record ScoreEntry(int victimId, float score, long timestamp) {}


    /**
     * Loads configuration from the JSON object.
     * @param config The configuration JSON object.
     */
    private void loadConfig(JsonObject config) {
        try {
            this.configXOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
            this.configYOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 100;
            this.configScreenAnchor = config.has("screen_anchor") ? config.get("screen_anchor").getAsString() : "bottom_center";
            this.displayDuration = config.has("display_duration")
                ? (long)(config.get("display_duration").getAsFloat() * 1000)
                : 3000L;
            this.scale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
            this.enableScaleAnimation = !config.has("enable_scale_animation") || config.get("enable_scale_animation").getAsBoolean();

            this.killTypeEnableFlags.clear();
            this.killTypeEnableFlags.put("enable_normal_kill", !config.has("enable_normal_kill") || config.get("enable_normal_kill").getAsBoolean());
            this.killTypeEnableFlags.put("enable_headshot_kill", !config.has("enable_headshot_kill") || config.get("enable_headshot_kill").getAsBoolean());
            this.killTypeEnableFlags.put("enable_explosion_kill", !config.has("enable_explosion_kill") || config.get("enable_explosion_kill").getAsBoolean());
            this.killTypeEnableFlags.put("enable_crit_kill", !config.has("enable_crit_kill") || config.get("enable_crit_kill").getAsBoolean());
            this.killTypeEnableFlags.put("enable_assist_kill", !config.has("enable_assist_kill") || config.get("enable_assist_kill").getAsBoolean());
            this.killTypeEnableFlags.put("enable_destroy_vehicle_kill", !config.has("enable_destroy_vehicle_kill") || config.get("enable_destroy_vehicle_kill").getAsBoolean());
            this.killTypeEnableFlags.put("enable_capture_kill", !config.has("enable_capture_kill") || config.get("enable_capture_kill").getAsBoolean());

            this.enableStacking = config.has("enable_stacking") && config.get("enable_stacking").getAsBoolean();
            this.maxLines = config.has("max_lines") ? config.get("max_lines").getAsInt() : 5;
            this.lineSpacing = config.has("line_spacing") ? config.get("line_spacing").getAsInt() : 12;

            String normalFormat = config.has("format_normal")
                    ? config.get("format_normal").getAsString()
                    : KillTypeRegistry.get(KillType.NORMAL).format();
            String normalColorHex = config.has("color_normal_placeholder")
                    ? config.get("color_normal_placeholder").getAsString()
                    : "#008B8B";
            
            this.format = normalFormat;

            this.placeholderColor = parseColorHexOrDefault(normalColorHex, DEFAULT_PLACEHOLDER_COLOR);
            this.enablePlaceholderBold = config.has("enable_placeholder_bold") && config.get("enable_placeholder_bold").getAsBoolean();
            this.enableTextShadow = !config.has("enable_text_shadow") || config.get("enable_text_shadow").getAsBoolean();
            this.configBlinkFadeAnimation = config.has("blink_fade_animation") && config.get("blink_fade_animation").getAsBoolean();
            this.enableGlowEffect = config.has("enable_glow_effect") && config.get("enable_glow_effect").getAsBoolean();
            this.glowIntensity = config.has("glow_intensity") ? config.get("glow_intensity").getAsFloat() : 0.5f;
            this.glowSize = config.has("glow_size") ? config.get("glow_size").getAsFloat() : 0.3f;
            this.glowColorRgb = parseColorHexOrDefault(config.has("glow_color") ? config.get("glow_color").getAsString() : "#FFFFFF", 0xFFFFFF) & 0x00FFFFFF;
            this.glowAlphaMultiplier = config.has("glow_alpha") ? Mth.clamp(config.get("glow_alpha").getAsFloat(), 0.0f, 1.0f) : 1.0f;
            this.alignLeft = config.has("align_left") ? config.get("align_left").getAsBoolean() : false;
            this.alignRight = config.has("align_right") ? config.get("align_right").getAsBoolean() : false;
            this.configEnableFlashIn = !config.has("enable_flash_in") || config.get("enable_flash_in").getAsBoolean();
            this.fadeInDurationMs = config.has("fade_in_duration")
                    ? Math.max(0L, (long) (config.get("fade_in_duration").getAsFloat() * 1000))
                    : 0L;
            this.enableQueueLinkage = config.has("enable_queue_linkage") && config.get("enable_queue_linkage").getAsBoolean();
            this.queueLinkageScrollSpeed = config.has("queue_linkage_scroll_speed")
                    ? Math.max(0.01f, config.get("queue_linkage_scroll_speed").getAsFloat())
                    : 0.1f;
            this.queueLinkageIconYOffset = config.has("queue_linkage_icon_y_offset")
                    ? config.get("queue_linkage_icon_y_offset").getAsFloat()
                    : 0.0f;
            this.configEntranceBackground = config.has("entrance_background") && config.get("entrance_background").getAsBoolean();
            this.configEntranceBgPeakTransparency = config.has("entrance_background_alpha")
                    ? Mth.clamp(config.get("entrance_background_alpha").getAsFloat(), 0.0f, 1.0f)
                    : 0.2f;
            this.configEntranceBgFadeInMs = config.has("entrance_background_fade_in")
                    ? Math.max(1L, (long) (config.get("entrance_background_fade_in").getAsFloat() * 1000))
                    : 1000L / 15;
            this.configEntranceBgSweepMs = config.has("entrance_background_sweep_duration")
                    ? Math.max(1L, (long) (config.get("entrance_background_sweep_duration").getAsFloat() * 1000))
                    : 100L;
            this.configEntranceBgFadeOutMs = config.has("entrance_background_fade_out")
                    ? Math.max(1L, (long) (config.get("entrance_background_fade_out").getAsFloat() * 1000))
                    : 2000L / 15;
            this.configEntranceBgColor = config.has("entrance_background_color")
                    ? SubtitleEntranceBackground.parseColor(config.get("entrance_background_color").getAsString(), 0xFFFFFF)
                    : 0xFFFFFF;
            this.normalTextColor = parseColorHexOrDefault(config.has("color_normal_text") ? config.get("color_normal_text").getAsString() : "#FFFFFF", 0xFFFFFFFF);
            
        } catch (Exception e) {
            ClientMessageLogger.chatWarn("gd656killicon.client.subtitle.config_error");
            this.configXOffset = 0;
            this.configYOffset = 100;
            this.configScreenAnchor = "bottom_center";
            this.displayDuration = 3000L;
            this.scale = 1.0f;
            this.format = KillTypeRegistry.get(KillType.NORMAL).format();
            this.placeholderColor = DEFAULT_PLACEHOLDER_COLOR;
            this.enablePlaceholderBold = false;
            this.enableScaleAnimation = true;
            this.killTypeEnableFlags.clear();
            this.killTypeEnableFlags.put("enable_normal_kill", true);
            this.killTypeEnableFlags.put("enable_capture_kill", true);
            this.enableStacking = false;
            this.normalTextColor = 0xFFFFFFFF;
        }
    }

    private boolean isKillTypeEnabled(int type) {
        KillTypeDefinition def = KillTypeRegistry.get(type);
        if (def == null || def.enableKey() == null) {
            return true;
        }
        return killTypeEnableFlags.getOrDefault(def.enableKey(), true);
    }

    /**
     * Calculates the alpha transparency based on fade-out duration.
     * @param currentTime Current system time.
     * @return Alpha value between 0.0 and 1.0.
     */
    private float calculateAlpha(long currentTime) {
        // 字幕渐入: t0 之后 fadeInDurationMs 内透明度从 100%(完全不显示)平滑变为正常透明度
        if (this.startTime >= 0 && this.fadeInDurationMs > 0) {
            long elapsed = currentTime - this.startTime;
            if (elapsed < this.fadeInDurationMs) {
                return Math.max(0.0f, (float) elapsed / (float) this.fadeInDurationMs);
            }
        }
        if (this.textHideTime > 0) {
            if (currentTime < this.textHideTime) {
                return 1.0f;
            } else {
                long fadeElapsed = currentTime - this.textHideTime;
                float fadeProgress = (float) fadeElapsed / FADE_OUT_DURATION;
                return TextFadeEffect.fadeAlpha(fadeProgress, configBlinkFadeAnimation);
            }
        }
        return 1.0f;
    }

    /**
     * Builds the full text component by replacing placeholders and applying styles.
     * @param colorProgress Progress of the color transition (0.0 to 1.0).
     * @return The formatted text component.
     */
    private Component buildFullText(String fmt, int pColor, int eColor, String wName, String vName, String scoreStr, float colorProgress, float distance) {
        Component fullText = Component.empty();
        String tempFormat = fmt;

        
        while (!tempFormat.isEmpty()) {
            int weaponIdx = tempFormat.indexOf("<weapon>");
            int targetIdx = tempFormat.indexOf("<target>");
            int distanceIdx = tempFormat.indexOf("<distance>");
            int scoreIdx = tempFormat.indexOf("<score>");
            int emphasisStart = tempFormat.indexOf("/");
            int emphasisEnd = -1;
            
            if (emphasisStart != -1) {
                emphasisEnd = tempFormat.indexOf("\\", emphasisStart + 1);
                if (emphasisEnd == -1) emphasisStart = -1;             }

            int firstIdx = -1;
            String type = "";

            if (weaponIdx != -1) {
                firstIdx = weaponIdx;
                type = "weapon";
            }
            if (targetIdx != -1 && (firstIdx == -1 || targetIdx < firstIdx)) {
                firstIdx = targetIdx;
                type = "target";
            }
            if (distanceIdx != -1 && (firstIdx == -1 || distanceIdx < firstIdx)) {
                firstIdx = distanceIdx;
                type = "distance";
            }
            if (scoreIdx != -1 && (firstIdx == -1 || scoreIdx < firstIdx)) {
                firstIdx = scoreIdx;
                type = "score";
            }
            if (emphasisStart != -1 && (firstIdx == -1 || emphasisStart < firstIdx)) {
                firstIdx = emphasisStart;
                type = "emphasis";
            }

            if (firstIdx == -1) {
                int targetColor = this.normalTextColor;
                int interpolatedColor = interpolateFromWhite(targetColor, colorProgress);
                fullText.getSiblings().add(Component.literal(tempFormat).withStyle(style -> 
                    style.withColor(interpolatedColor & 0x00FFFFFF)));
                break;
            }

            if (firstIdx > 0) {
                String prefix = tempFormat.substring(0, firstIdx);
                int targetColor = this.normalTextColor;
                int interpolatedColor = interpolateFromWhite(targetColor, colorProgress);
                fullText.getSiblings().add(Component.literal(prefix).withStyle(style -> 
                    style.withColor(interpolatedColor & 0x00FFFFFF)));
            }

            if (type.equals("weapon")) {
                int targetColor = pColor & 0x00FFFFFF;
                int interpolatedColor = interpolateFromWhite(targetColor, colorProgress);
                fullText.getSiblings().add(Component.literal(wName).withStyle(style -> 
                    style.withColor(interpolatedColor & 0x00FFFFFF).withBold(this.enablePlaceholderBold)));
                tempFormat = tempFormat.substring(firstIdx + "<weapon>".length());
            } else if (type.equals("target")) {
                int targetColor = pColor & 0x00FFFFFF;
                int interpolatedColor = interpolateFromWhite(targetColor, colorProgress);
                String translatedVName = org.mods.gd656killicon.client.util.I18nCompat.exists(vName)
                    ? net.minecraft.client.resources.language.I18n.get(vName)
                    : vName;
                fullText.getSiblings().add(Component.literal(translatedVName).withStyle(style -> 
                    style.withColor(interpolatedColor & 0x00FFFFFF).withBold(this.enablePlaceholderBold)));
                tempFormat = tempFormat.substring(firstIdx + "<target>".length());
            } else if (type.equals("distance")) {
                if (distance >= 20.0f) {
                     String meterText = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.meter");
                     String content = String.format("(%d%s)", (int)distance, meterText);
                     
                     int targetColor = pColor & 0x00FFFFFF;
                     int interpolatedColor = interpolateFromWhite(targetColor, colorProgress);
                     fullText.getSiblings().add(Component.literal(content).withStyle(style -> 
                        style.withColor(interpolatedColor & 0x00FFFFFF).withBold(this.enablePlaceholderBold)));
                }
                tempFormat = tempFormat.substring(firstIdx + "<distance>".length());
            } else if (type.equals("score")) {
                int targetColor = pColor & 0x00FFFFFF;
                int interpolatedColor = interpolateFromWhite(targetColor, colorProgress);
                fullText.getSiblings().add(Component.literal(scoreStr).withStyle(style -> 
                    style.withColor(interpolatedColor & 0x00FFFFFF).withBold(this.enablePlaceholderBold)));
                tempFormat = tempFormat.substring(firstIdx + "<score>".length());
            } else if (type.equals("emphasis")) {
                String content = tempFormat.substring(emphasisStart + 1, emphasisEnd);
                int targetColor = eColor & 0x00FFFFFF;
                int interpolatedColor = interpolateFromWhite(targetColor, colorProgress);
                fullText.getSiblings().add(Component.literal(content).withStyle(style -> 
                    style.withColor(interpolatedColor & 0x00FFFFFF)));
                tempFormat = tempFormat.substring(emphasisEnd + 1);
            }
        }
        
        return fullText;
    }

    /**
     * Returns the config key for the format string based on kill type.
     */
    private static String formatKeyForType(int killType) {
        return KillTypeRegistry.get(killType).formatKey();
    }

    private static String resolveRushCaptureFormat(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return switch (token) {
            case "rush_bomb_planted" -> RUSH_BOMB_PLANTED_CAPTURE_FORMAT;
            case "rush_bomb_defused" -> RUSH_BOMB_DEFUSED_CAPTURE_FORMAT;
            case "rush_objective_destroyed" -> RUSH_OBJECTIVE_DESTROYED_CAPTURE_FORMAT;
            default -> null;
        };
    }

    private static boolean isNumericToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * Returns the config key for the placeholder color based on kill type.
     */
    private static String placeholderColorKeyForType(int killType) {
        return KillTypeRegistry.get(killType).placeholderColorKey();
    }

    private static String emphasisColorKeyForType(int killType) {
        return KillTypeRegistry.get(killType).emphasisColorKey();
    }

    private boolean isNormalKillType(int type) {
        return type == KillType.NORMAL || type == KillType.HEADSHOT || type == KillType.EXPLOSION || type == KillType.CRIT;
    }

    /**
     * Parses a hex color string or returns a default value.
     */
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

    private static int parseColorHexOrDefault(String hex, int fallbackArgb) {
        if (hex == null || hex.isEmpty()) {
            return fallbackArgb;
        }
        try {
            int rgb = Integer.parseInt(hex.replace("#", ""), 16);
            return (rgb & 0x00FFFFFF) | 0xFF000000;
        } catch (NumberFormatException e) {
            return fallbackArgb;
        }
    }

    /**
     * Calculates the progress of the color transition (white to target).
     */
    private float getColorProgress(long elapsed) {
        if (elapsed < FADE_IN_DURATION) {
            return (float) elapsed / FADE_IN_DURATION;
        }
        return 1.0f;
    }

    /**
     * Interpolates color from white to target color.
     * @param targetColor Target color (ARGB).
     * @param progress Interpolation progress (0.0 to 1.0).
     * @return Interpolated color (ARGB).
     */
    private static int interpolateFromWhite(int targetColor, float progress) {
        if (progress >= 1.0f) {
            return targetColor;
        }
        int white = 0x00FFFFFF;
        int targetRGB = targetColor & 0x00FFFFFF;
        int alpha = targetColor & 0xFF000000;

        int r1 = (white >> 16) & 0xFF;
        int g1 = (white >> 8) & 0xFF;
        int b1 = white & 0xFF;
        int r2 = (targetRGB >> 16) & 0xFF;
        int g2 = (targetRGB >> 8) & 0xFF;
        int b2 = targetRGB & 0xFF;

        int r = (int)(r1 + (r2 - r1) * progress);
        int g = (int)(g1 + (g2 - g1) * progress);
        int b = (int)(b1 + (b2 - b1) * progress);

        return (alpha) | (r << 16) | (g << 8) | b;
    }

}
