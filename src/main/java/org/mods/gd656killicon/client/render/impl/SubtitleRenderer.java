package org.mods.gd656killicon.client.render.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.mods.gd656killicon.common.KillType;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
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

    private static final long FADE_IN_DURATION = 200L;     private static final long FADE_OUT_DURATION = 300L;     private static final int DEFAULT_PLACEHOLDER_COLOR = 0xFF008B8B;
    private static final int DEFAULT_EMPHASIS_COLOR = 0xFFFFFFFF;
    private static final long SCORE_WAIT_WINDOW_MS = 2000L;
    private static final long SCORE_CACHE_WINDOW_MS = 10000L;
    private static final int PREVIEW_SCORE_VICTIM_ID = -9999;
    private static final Map<Integer, ScoreEntry> RECENT_SCORES = new ConcurrentHashMap<>();

    
    private int configXOffset = 0;
    private int configYOffset = 20;
    private long displayDuration = 3000L;
    private String format = "gd656killicon.client.format.normal";
    private int placeholderColor = DEFAULT_PLACEHOLDER_COLOR;
    private boolean enablePlaceholderBold = false;
    private float scale = 1.0f;
    private int emphasisColor = DEFAULT_EMPHASIS_COLOR;
    
    private boolean enableNormalKill = true;
    private boolean enableHeadshotKill = true;
    private boolean enableExplosionKill = true;
    private boolean enableCritKill = true;
    private boolean enableAssistKill = true;
    private boolean enableDestroyVehicleKill = true;

    private boolean enableStacking = false;
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
        
        if (context.extraData() != null && !context.extraData().isEmpty()) {
            String extra = context.extraData();
            if (type == KillType.DESTROY_VEHICLE) {
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
        if (mc.player != null) {
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

        String normalFormat = config.has("format_normal") ? config.get("format_normal").getAsString() : "gd656killicon.client.format.normal";
        String resolvedFormat = config.has(formatKey) ? config.get(formatKey).getAsString() : normalFormat;
        if (net.minecraft.client.resources.language.I18n.exists(resolvedFormat)) {
            resolvedFormat = net.minecraft.client.resources.language.I18n.get(resolvedFormat);
        }

        String normalColorHex = config.has("color_normal_placeholder") ? config.get("color_normal_placeholder").getAsString() : "#008B8B";
        String chosenColorHex = config.has(colorKey) ? config.get(colorKey).getAsString() : normalColorHex;
        int pColor = parseColorHexOrDefault(chosenColorHex, DEFAULT_PLACEHOLDER_COLOR);
        
        String emphasisHex = config.has(emphasisColorKey) ? config.get(emphasisColorKey).getAsString() : "#FFFFFF";
        int eColor = parseColorHexOrDefault(emphasisHex, DEFAULT_EMPHASIS_COLOR);

        float dist = isNormalKillType(type) ? context.distance() : 0.0f;

        if (this.enableStacking) {
            addItemToStack(resolvedFormat, pColor, eColor, wName, vName, this.displayDuration, dist, entityId);
        } else {
            this.currentKillType = type;
            this.victimId = entityId;
            this.currentVictimId = entityId;
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

            this.startTime = System.currentTimeMillis();
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

        String normalFormat = config.has("format_normal") ? config.get("format_normal").getAsString() : "gd656killicon.client.format.normal";
        String resolvedFormat = config.has(formatKey) ? config.get(formatKey).getAsString() : normalFormat;
        if (net.minecraft.client.resources.language.I18n.exists(resolvedFormat)) {
            resolvedFormat = net.minecraft.client.resources.language.I18n.get(resolvedFormat);
        }

        String normalColorHex = config.has("color_normal_placeholder") ? config.get("color_normal_placeholder").getAsString() : "#008B8B";
        String chosenColorHex = config.has(colorKey) ? config.get(colorKey).getAsString() : normalColorHex;
        int pColor = parseColorHexOrDefault(chosenColorHex, DEFAULT_PLACEHOLDER_COLOR);
        
        String emphasisHex = config.has(emphasisColorKey) ? config.get(emphasisColorKey).getAsString() : "#FFFFFF";
        int eColor = parseColorHexOrDefault(emphasisHex, DEFAULT_EMPHASIS_COLOR);

        float dist = isNormalKillType(killType) ? 50.0f : 0.0f;

        if (this.enableStacking) {
             addItemToStack(resolvedFormat, pColor, eColor, this.currentWeaponName, this.victimName, this.displayDuration, dist, PREVIEW_SCORE_VICTIM_ID);
        } else {
            this.format = resolvedFormat;
            this.placeholderColor = pColor;
            this.emphasisColor = eColor;
            this.currentDistance = dist;
            
            if (this.displayDuration < FADE_IN_DURATION) {
                this.displayDuration = FADE_IN_DURATION;
            }

            this.startTime = System.currentTimeMillis();
            this.textHideTime = this.startTime + this.displayDuration;
            this.isVisible = true;
        }
    }

    private void addItemToStack(String format, int pColor, int eColor, String wName, String vName, long duration, float distance, int victimId) {
        SubtitleItem newItem = new SubtitleItem(format, pColor, eColor, wName, vName, 0, duration, distance, victimId);         
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

        int centerX = screenWidth / 2 + configXOffset;
        int textY = screenHeight - configYOffset;

        if (this.enableStacking) {
            renderStacked(guiGraphics, font, centerX, textY);
        } else {
            RenderState state = resolveRenderState();
            if (state == null) return;
            renderInternal(guiGraphics, font, centerX, textY, state, this.format, this.placeholderColor, this.emphasisColor, this.currentWeaponName, this.victimName, this.currentDistance, this.currentVictimId, this.startTime);
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
            renderInternal(guiGraphics, font, resolvedCenterX, resolvedTextY, state, this.format, this.placeholderColor, this.emphasisColor, this.currentWeaponName, this.victimName, this.currentDistance, this.currentVictimId, this.startTime);
        }
    }

    private void renderStacked(GuiGraphics guiGraphics, Font font, int centerX, int startY) {
        long now = System.currentTimeMillis();
        
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

    private void renderStackItems(GuiGraphics guiGraphics, Font font, int centerX, int startY) {
        long now = System.currentTimeMillis();
        
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

            int drawY = startY + Math.round(item.currentRelY);
            
            RenderState state = new RenderState(now - item.spawnTime, itemAlpha, this.scale);
            
            renderInternal(guiGraphics, font, centerX, drawY, state, item.format, item.pColor, item.eColor, item.wName, item.vName, item.distance, item.victimId, item.spawnTime);
        }
    }

    private RenderState resolveRenderState() {
        if (!isVisible || startTime == -1) return null;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - startTime;

        float alpha = calculateAlpha(currentTime);
        if (alpha <= 0.05f) {
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

    private void renderInternal(GuiGraphics guiGraphics, Font font, int centerX, int textY, RenderState state, 
                              String fmt, int pColor, int eColor, String wName, String vName, float distance, int victimId, long referenceTime) {
        float colorProgress = getColorProgress(state.elapsed);
        String scoreStr = resolveScoreString(victimId, referenceTime);
        Component fullText = buildFullText(fmt, pColor, eColor, wName, vName, scoreStr, colorProgress, distance);

        int textWidth = font.width(fullText);
        int textX = centerX - textWidth / 2;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        float pivotX = textX + textWidth / 2.0f;
        float pivotY = textY + font.lineHeight / 2.0f;

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

        int alphaInt = (int) (state.alpha * 255.0f) << 24;
        int colorWithAlpha = (normalTextColor & 0x00FFFFFF) | alphaInt;
        guiGraphics.drawString(font, fullText, textX, textY, colorWithAlpha, true);

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
        
        public SubtitleItem(String format, int pColor, int eColor, String wName, String vName, long spawnTime, long duration, float distance, int victimId) {
            this.format = format;
            this.pColor = pColor;
            this.eColor = eColor;
            this.wName = wName;
            this.vName = vName;
            this.spawnTime = spawnTime;
            this.duration = duration;
            this.currentRelY = 0;             this.distance = distance;
            this.victimId = victimId;
        }
    }

    public static void recordBonusScore(int bonusType, float score, int victimId) {
        if (victimId == -1) return;
        RECENT_SCORES.put(victimId, new ScoreEntry(score, System.currentTimeMillis()));
    }

    private static String resolveScoreString(int victimId, long referenceTime) {
        if (victimId == PREVIEW_SCORE_VICTIM_ID) return "20";
        if (victimId == -1) return "0";
        long now = System.currentTimeMillis();
        ScoreEntry entry = RECENT_SCORES.get(victimId);
        if (entry != null) {
            if (now - entry.timestamp <= SCORE_CACHE_WINDOW_MS) {
                return formatScore(entry.score);
            }
            RECENT_SCORES.remove(victimId);
        }
        if (referenceTime > 0 && now - referenceTime < SCORE_WAIT_WINDOW_MS) {
            return "";
        }
        return "0";
    }

    private static String formatScore(float score) {
        if (score < 1.0f && score > 0.0f) {
            return String.format("%.1f", score);
        }
        return String.valueOf(Math.round(score));
    }

    private record ScoreEntry(float score, long timestamp) {}


    /**
     * Loads configuration from the JSON object.
     * @param config The configuration JSON object.
     */
    private void loadConfig(JsonObject config) {
        try {
            this.configXOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
            this.configYOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 100;
            this.displayDuration = config.has("display_duration")
                ? (long)(config.get("display_duration").getAsFloat() * 1000)
                : 3000L;
            this.scale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
            this.enableScaleAnimation = !config.has("enable_scale_animation") || config.get("enable_scale_animation").getAsBoolean();

            this.enableNormalKill = !config.has("enable_normal_kill") || config.get("enable_normal_kill").getAsBoolean();
            this.enableHeadshotKill = !config.has("enable_headshot_kill") || config.get("enable_headshot_kill").getAsBoolean();
            this.enableExplosionKill = !config.has("enable_explosion_kill") || config.get("enable_explosion_kill").getAsBoolean();
            this.enableCritKill = !config.has("enable_crit_kill") || config.get("enable_crit_kill").getAsBoolean();
            this.enableAssistKill = !config.has("enable_assist_kill") || config.get("enable_assist_kill").getAsBoolean();
            this.enableDestroyVehicleKill = !config.has("enable_destroy_vehicle_kill") || config.get("enable_destroy_vehicle_kill").getAsBoolean();

            this.enableStacking = config.has("enable_stacking") && config.get("enable_stacking").getAsBoolean();
            this.maxLines = config.has("max_lines") ? config.get("max_lines").getAsInt() : 5;
            this.lineSpacing = config.has("line_spacing") ? config.get("line_spacing").getAsInt() : 12;

            String normalFormat = config.has("format_normal")
                    ? config.get("format_normal").getAsString()
                    : "gd656killicon.client.format.normal";
            String normalColorHex = config.has("color_normal_placeholder")
                    ? config.get("color_normal_placeholder").getAsString()
                    : "#008B8B";
            
            this.format = normalFormat;
            if (net.minecraft.client.resources.language.I18n.exists(this.format)) {
                this.format = net.minecraft.client.resources.language.I18n.get(this.format);
            }

            this.placeholderColor = parseColorHexOrDefault(normalColorHex, DEFAULT_PLACEHOLDER_COLOR);
            this.enablePlaceholderBold = config.has("enable_placeholder_bold") && config.get("enable_placeholder_bold").getAsBoolean();
            this.normalTextColor = parseColorHexOrDefault(config.has("color_normal_text") ? config.get("color_normal_text").getAsString() : "#FFFFFF", 0xFFFFFFFF);
            
        } catch (Exception e) {
            ClientMessageLogger.chatWarn("gd656killicon.client.subtitle.config_error");
            this.configXOffset = 0;
            this.configYOffset = 100;
            this.displayDuration = 3000L;
            this.scale = 1.0f;
            this.format = "gd656killicon.client.format.normal";
            if (net.minecraft.client.resources.language.I18n.exists(this.format)) {
                this.format = net.minecraft.client.resources.language.I18n.get(this.format);
            }
            this.placeholderColor = DEFAULT_PLACEHOLDER_COLOR;
            this.enablePlaceholderBold = false;
            this.enableScaleAnimation = true;
            this.enableNormalKill = true;
            this.enableStacking = false;
            this.normalTextColor = 0xFFFFFFFF;
        }
    }

    private boolean isKillTypeEnabled(int type) {
        return switch (type) {
            case KillType.NORMAL -> enableNormalKill;
            case KillType.HEADSHOT -> enableHeadshotKill;
            case KillType.EXPLOSION -> enableExplosionKill;
            case KillType.CRIT -> enableCritKill;
            case KillType.ASSIST -> enableAssistKill;
            case KillType.DESTROY_VEHICLE -> enableDestroyVehicleKill;
            default -> true;
        };
    }

    /**
     * Calculates the alpha transparency based on fade-out duration.
     * @param currentTime Current system time.
     * @return Alpha value between 0.0 and 1.0.
     */
    private float calculateAlpha(long currentTime) {
        if (this.textHideTime > 0) {
            if (currentTime < this.textHideTime) {
                return 1.0f;
            } else {
                long fadeElapsed = currentTime - this.textHideTime;
                return Math.max(0.0f, 1.0f - (float) fadeElapsed / FADE_OUT_DURATION);
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
                String translatedVName = net.minecraft.client.resources.language.I18n.get(vName);
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
        return switch (killType) {
            case KillType.HEADSHOT -> "format_headshot";
            case KillType.EXPLOSION -> "format_explosion";
            case KillType.CRIT -> "format_crit";
            case KillType.ASSIST -> "format_assist";
            case KillType.DESTROY_VEHICLE -> "format_destroy_vehicle";
            default -> "format_normal";
        };
    }

    /**
     * Returns the config key for the placeholder color based on kill type.
     */
    private static String placeholderColorKeyForType(int killType) {
        return switch (killType) {
            case KillType.HEADSHOT -> "color_headshot_placeholder";
            case KillType.EXPLOSION -> "color_explosion_placeholder";
            case KillType.CRIT -> "color_crit_placeholder";
            case KillType.ASSIST -> "color_assist_placeholder";
            case KillType.DESTROY_VEHICLE -> "color_destroy_vehicle_placeholder";
            default -> "color_normal_placeholder";
        };
    }

    private static String emphasisColorKeyForType(int killType) {
        return switch (killType) {
            case KillType.HEADSHOT -> "color_headshot_emphasis";
            case KillType.EXPLOSION -> "color_explosion_emphasis";
            case KillType.CRIT -> "color_crit_emphasis";
            case KillType.ASSIST -> "color_assist_emphasis";
            case KillType.DESTROY_VEHICLE -> "color_destroy_vehicle_emphasis";
            default -> "color_normal_emphasis";
        };
    }

    private boolean isNormalKillType(int type) {
        return type == KillType.NORMAL || type == KillType.HEADSHOT || type == KillType.EXPLOSION || type == KillType.CRIT;
    }

    /**
     * Parses a hex color string or returns a default value.
     */
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
