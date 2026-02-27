package org.mods.gd656killicon.client.render.impl;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.stats.ClientStatsManager;
import org.mods.gd656killicon.client.util.ClientMessageLogger;
import org.mods.gd656killicon.common.KillType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ComboSubtitleRenderer implements IHudRenderer {

    private static final long FADE_IN_DURATION = 200L;
    private static final long FADE_OUT_DURATION = 200L; 
    
    private static final long EXIT_ANIMATION_DURATION = 500L;

    
    private static final long LIGHT_SCAN_DURATION = 400L; 
    private static final float LIGHT_SCAN_DISTANCE = 20.0f;
    private static final long LIGHT_STRIP_FADE_DELAY = 200L; 
    private static final long LIGHT_STRIP_FADE_OUT_DURATION = 200L;

    private static ComboSubtitleRenderer instance;

    
    private boolean visible = true;
    private float scale = 1.5f;
    private int xOffset = 0;
    private int yOffset = 150;
    private int colorKillCombo = 0xFF0000;
    private int colorAssistCombo = 0xFFD700;
    private String formatKillSingle = "gd656killicon.client.format.combo_kill_single";
    private String formatKillMulti = "gd656killicon.client.format.combo_kill_multi";
    private String formatAssistSingle = "gd656killicon.client.format.combo_assist_single";
    private String formatAssistMulti = "gd656killicon.client.format.combo_assist_multi";
    private boolean enableAnimation = true;
    private boolean enableLightEffect = true;
    private boolean enableBold = true;
    private double lightHeight = 12.0;
    private double lightHoldDuration = 0.2;
    private boolean enableScaleAnimation = false;
    private long displayDuration = 5000L;
    private String resetKillCombo = "death";
    private String resetAssistCombo = "death";
    private float comboResetTimeout = 10.0f;

    
    private long startTime = -1;
    private boolean isVisible = false;
    private int currentCombo = 0;
    private boolean isAssist = false;
    
    
    private final java.util.Deque<ComboItem> pendingQueue = new java.util.ArrayDeque<>();
    private long lastDequeueTime = 0;
    
    
    private int localKillComboCount = 0;
    private int localAssistComboCount = 0;
    private long lastKillTime = 0;
    private long lastAssistTime = 0;

    
    private float lastScanX = 0;

    private static class ComboItem {
        int combo;
        boolean isAssist;
        
        ComboItem(int combo, boolean isAssist) {
            this.combo = combo;
            this.isAssist = isAssist;
        }
    }

    private ComboSubtitleRenderer() {}

    public static synchronized ComboSubtitleRenderer getInstance() {
        if (instance == null) {
            instance = new ComboSubtitleRenderer();
        }
        return instance;
    }

    @Override
    public void trigger(TriggerContext context) {
        
        JsonObject config = ConfigManager.getElementConfig("subtitle", "combo");
        if (config == null) return;

        loadConfig(config);

        if (!this.visible) {
            this.isVisible = false;
            return;
        }

        long now = System.currentTimeMillis();
        
        if (context.type() == KillType.DESTROY_VEHICLE) return;

        this.isAssist = context.type() == KillType.ASSIST;
        
        
        checkResetTimeout(now);

        
        int newCombo;
        boolean isAssistEvent = this.isAssist;

        if (this.isAssist) {
            if ("server".equals(resetAssistCombo)) {
                newCombo = context.comboCount();
                this.localAssistComboCount = newCombo;
            } else if ("never".equals(resetAssistCombo)) {
                newCombo = (int)ClientStatsManager.getTotalAssists() + 1;
                this.localAssistComboCount = newCombo;
            } else {
                this.localAssistComboCount++;
                newCombo = this.localAssistComboCount;
                this.lastAssistTime = now;
            }
        } else {
            if ("server".equals(resetKillCombo)) {
                newCombo = context.comboCount();
                this.localKillComboCount = newCombo;
            } else if ("never".equals(resetKillCombo)) {
                newCombo = (int)ClientStatsManager.getTotalKills();
                this.localKillComboCount = newCombo;
            } else {
                this.localKillComboCount++;
                newCombo = this.localKillComboCount;
                this.lastKillTime = now;
            }
        }

        
        if (this.pendingQueue.size() < 10) {
            this.pendingQueue.add(new ComboItem(newCombo, isAssistEvent));
        }

        
        this.isVisible = true;
        
    }
    
    public void resetKillCombo() {
        this.localKillComboCount = 0;
    }
    
    public void resetAssistCombo() {
        this.localAssistComboCount = 0;
    }

    public void onPlayerDeath() {
        JsonObject config = ConfigManager.getElementConfig("subtitle", "combo");
        if (config != null) {
            loadConfig(config);
        }

        boolean resetKill = "death".equals(resetKillCombo);
        boolean resetAssist = "death".equals(resetAssistCombo);
        if (resetKill) {
            localKillComboCount = 0;
            lastKillTime = 0;
        }
        if (resetAssist) {
            localAssistComboCount = 0;
            lastAssistTime = 0;
        }
        if (resetKill || resetAssist) {
            pendingQueue.clear();
            currentCombo = 0;
            isVisible = false;
            startTime = -1;
            lastDequeueTime = 0;
            lastScanX = 0;
        }
    }

    public void onPlayerLogout() {
        JsonObject config = ConfigManager.getElementConfig("subtitle", "combo");
        if (config != null) {
            loadConfig(config);
        }
        if (!"never".equals(resetKillCombo)) {
            localKillComboCount = 0;
            lastKillTime = 0;
        }
        if (!"never".equals(resetAssistCombo)) {
            localAssistComboCount = 0;
            lastAssistTime = 0;
        }
        pendingQueue.clear();
        currentCombo = 0;
        isVisible = false;
        startTime = -1;
        lastDequeueTime = 0;
        lastScanX = 0;
    }

    private void checkResetTimeout(long now) {
        long timeoutMs = (long)(this.comboResetTimeout * 1000);
        if ("time".equals(resetKillCombo)) {
            if (now - lastKillTime > timeoutMs) {
                localKillComboCount = 0;
            }
        }
        if ("time".equals(resetAssistCombo)) {
            if (now - lastAssistTime > timeoutMs) {
                localAssistComboCount = 0;
            }
        }
    }
    
    
    public void triggerPreview(boolean assist, int combo) {
        JsonObject config = ConfigManager.getElementConfig("subtitle", "combo");
        if (config == null) return;
        loadConfig(config);
        if (!this.visible) {
             this.isVisible = false;
             return;
        }
        
        this.isAssist = assist;
        this.currentCombo = combo;
        this.startTime = System.currentTimeMillis();
        this.isVisible = true;
        this.pendingQueue.clear();
        
        if (this.enableLightEffect) {
            this.lastScanX = 0;
        }
    }

    public void resetPreview() {
        this.pendingQueue.clear();
        this.isVisible = false;
        this.startTime = -1;
        this.currentCombo = 0;
        this.isAssist = false;
        this.lastDequeueTime = 0;
        this.lastScanX = 0;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        processQueue();
        RenderState state = resolveRenderState();
        if (state == null) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        int centerX = screenWidth / 2 + this.xOffset;
        int centerY = screenHeight - this.yOffset;
        
        renderInternal(guiGraphics, partialTick, centerX, centerY, state);
    }

    public void renderAt(GuiGraphics guiGraphics, float partialTick, float x, float y) {
        processQueue();
        RenderState state = resolveRenderState();
        if (state == null) {
            return;
        }
        renderInternal(guiGraphics, partialTick, (int)x, (int)y, state);
    }
    
    private void processQueue() {
        long now = System.currentTimeMillis();
        
        if (this.pendingQueue.isEmpty()) {
            
            return;
        }

        
        if (now - this.lastDequeueTime >= 200) {
            ComboItem item = this.pendingQueue.poll();
            if (item != null) {
                this.currentCombo = item.combo;
                this.isAssist = item.isAssist;
                this.startTime = now;
                this.lastDequeueTime = now;
                this.isVisible = true; 
                
                
                if (this.enableLightEffect) {
                    this.lastScanX = 0;
                }
            }
        }
    }

    private RenderState resolveRenderState() {
        if (!isVisible || startTime == -1) return null;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - startTime;

        
        if (elapsed > this.displayDuration + EXIT_ANIMATION_DURATION) {
            isVisible = false;
            startTime = -1;
            return null;
        }

        
        float alpha = 1.0f;
        if (this.enableAnimation) {
            if (elapsed < FADE_IN_DURATION) {
                alpha = (float)elapsed / FADE_IN_DURATION;
            } else if (elapsed > this.displayDuration) {
                alpha = 1.0f - (float)(elapsed - this.displayDuration) / EXIT_ANIMATION_DURATION;
            }
        }
        alpha = Mth.clamp(alpha, 0.0f, 1.0f);
        if (alpha <= 0.05f && elapsed > this.displayDuration) {
             isVisible = false;
             startTime = -1;
             return null;
        }

        
        float currentScale = this.scale;
        if (this.enableAnimation && this.enableScaleAnimation && elapsed < FADE_IN_DURATION) {
            float progress = (float) elapsed / FADE_IN_DURATION;
            float easedProgress = 1.0f - (float) Math.pow(1.0f - progress, 3);
            
            currentScale = Mth.lerp(easedProgress, this.scale * 1.5f, this.scale);
        }

        return new RenderState(elapsed, alpha, currentScale);
    }

    private void renderInternal(GuiGraphics guiGraphics, float partialTick, int centerX, int centerY, RenderState state) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        
        String formatKey;
        if (this.isAssist) {
            formatKey = this.currentCombo > 1 ? this.formatAssistMulti : this.formatAssistSingle;
        } else {
            formatKey = this.currentCombo > 1 ? this.formatKillMulti : this.formatKillSingle;
        }
        
        String format = net.minecraft.client.resources.language.I18n.exists(formatKey) 
            ? net.minecraft.client.resources.language.I18n.get(formatKey) 
            : formatKey;
            
        String text = format.replace("<combo>", String.valueOf(this.currentCombo));
        int color = this.isAssist ? this.colorAssistCombo : this.colorKillCombo;
        
        int textWidth = font.width(text);
        int textHeight = font.lineHeight;
        
        
        
        float textHalfWidth = textWidth / 2.0f;
        float textHalfHeight = textHeight / 2.0f;
        
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        
        
        poseStack.translate(centerX, centerY, 0);
        
        poseStack.scale(state.currentScale, state.currentScale, 1.0f);
        
        
        if (this.enableLightEffect) {
            renderLightEffect(guiGraphics, poseStack, state.elapsed, textWidth, textHeight);
        }
        
        
        
        int alphaInt = (int)(state.alpha * 255);
        int finalColor = (color & 0xFFFFFF) | (alphaInt << 24);
        
        if (this.enableBold) {
            
            
            
            
            
            
            Component comp = Component.literal(text).withStyle(style -> style.withBold(true));
            
            textWidth = font.width(comp);
            textHalfWidth = textWidth / 2.0f;
            
            guiGraphics.drawString(font, comp, (int)(-textHalfWidth), (int)(-textHalfHeight), finalColor);
        } else {
            guiGraphics.drawString(font, text, (int)(-textHalfWidth), (int)(-textHalfHeight), finalColor, true);
        }
        
        poseStack.popPose();
    }
    
    private void renderLightEffect(GuiGraphics guiGraphics, PoseStack poseStack, long elapsed, int width, int height) {
        long holdDurationMs = (long)(this.lightHoldDuration * 1000);
        if (elapsed > LIGHT_SCAN_DURATION + holdDurationMs + LIGHT_STRIP_FADE_OUT_DURATION) return;
        
        float currentScanX = LIGHT_SCAN_DISTANCE;
        
        
        if (elapsed < LIGHT_SCAN_DURATION) {
            float progress = (float)elapsed / LIGHT_SCAN_DURATION;
            
            float t = progress;
            float ease = 1 - (1-t)*(1-t)*(1-t);
            currentScanX = ease * LIGHT_SCAN_DISTANCE;
        }
        
        
        float baseAlpha = 1.0f;
        if (elapsed > LIGHT_SCAN_DURATION + holdDurationMs) {
            long fadeOutElapsed = elapsed - (LIGHT_SCAN_DURATION + holdDurationMs);
            if (fadeOutElapsed < LIGHT_STRIP_FADE_OUT_DURATION) {
                float fadeOutProgress = (float)fadeOutElapsed / LIGHT_STRIP_FADE_OUT_DURATION;
                baseAlpha = Mth.lerp(fadeOutProgress, 1.0f, 0.0f);
            } else {
                baseAlpha = 0.0f;
            }
        }
        
        if (baseAlpha <= 0.01f) return;
        
        
        com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        com.mojang.blaze3d.vertex.BufferBuilder buffer = tesselator.getBuilder();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        
        float halfHeight = (float)this.lightHeight / 2.0f;
        float yOffset = 1.0f;
        
        
        int r = 255;
        int g = 255;
        int b = 255;
        int aCenter = (int)(200 * baseAlpha); 
        int aEdge = 0; 
        
        
        
        
        buffer.vertex(poseStack.last().pose(), 0, -halfHeight + yOffset, 0).color(r, g, b, aCenter).endVertex();
        
        buffer.vertex(poseStack.last().pose(), -currentScanX, -halfHeight + yOffset, 0).color(r, g, b, aEdge).endVertex();
        
        buffer.vertex(poseStack.last().pose(), -currentScanX, halfHeight + yOffset, 0).color(r, g, b, aEdge).endVertex();
        
        buffer.vertex(poseStack.last().pose(), 0, halfHeight + yOffset, 0).color(r, g, b, aCenter).endVertex();
        
        
        
        
        buffer.vertex(poseStack.last().pose(), currentScanX, -halfHeight + yOffset, 0).color(r, g, b, aEdge).endVertex();
        
        buffer.vertex(poseStack.last().pose(), 0, -halfHeight + yOffset, 0).color(r, g, b, aCenter).endVertex();
        
        buffer.vertex(poseStack.last().pose(), 0, halfHeight + yOffset, 0).color(r, g, b, aCenter).endVertex();
        
        buffer.vertex(poseStack.last().pose(), currentScanX, halfHeight + yOffset, 0).color(r, g, b, aEdge).endVertex();
        
        tesselator.end();
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

    private void loadConfig(JsonObject config) {
        try {
            this.visible = config.has("visible") ? config.get("visible").getAsBoolean() : true;
            this.scale = config.has("scale") ? config.get("scale").getAsFloat() : 1.5f;
            this.xOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
            this.yOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 150;
            this.colorKillCombo = parseColor(config, "color_kill_combo", 0xFF0000);
            this.colorAssistCombo = parseColor(config, "color_assist_combo", 0xFFD700);
            
            this.formatKillSingle = getFormat(config, "format_kill_single", "gd656killicon.client.format.combo_kill_single");
            this.formatKillMulti = getFormat(config, "format_kill_multi", "gd656killicon.client.format.combo_kill_multi");
            this.formatAssistSingle = getFormat(config, "format_assist_single", "gd656killicon.client.format.combo_assist_single");
            this.formatAssistMulti = getFormat(config, "format_assist_multi", "gd656killicon.client.format.combo_assist_multi");
            
            this.enableAnimation = config.has("enable_animation") ? config.get("enable_animation").getAsBoolean() : true;
            this.enableLightEffect = config.has("enable_light_effect") ? config.get("enable_light_effect").getAsBoolean() : true;
            this.enableBold = config.has("enable_bold") ? config.get("enable_bold").getAsBoolean() : true;
            this.lightHeight = config.has("light_height") ? config.get("light_height").getAsDouble() : 12.0;
            this.lightHoldDuration = config.has("light_hold_duration") ? config.get("light_hold_duration").getAsDouble() : 0.2;
            this.enableScaleAnimation = config.has("enable_scale_animation") ? config.get("enable_scale_animation").getAsBoolean() : false;
            this.displayDuration = config.has("display_duration") ? (long)(config.get("display_duration").getAsFloat() * 1000) : 5000L;
            
            this.resetKillCombo = config.has("reset_kill_combo") ? config.get("reset_kill_combo").getAsString() : "death";
            this.resetAssistCombo = config.has("reset_assist_combo") ? config.get("reset_assist_combo").getAsString() : "death";
            this.comboResetTimeout = config.has("combo_reset_timeout") ? config.get("combo_reset_timeout").getAsFloat() : 10.0f;
            
        } catch (Exception e) {
            ClientMessageLogger.chatWarn("gd656killicon.client.config_error");
        }
    }
    
    private int parseColor(JsonObject config, String key, int def) {
        if (!config.has(key)) return def;
        String hex = config.get(key).getAsString();
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return def;
        }
    }
    
    private String getFormat(JsonObject config, String key, String def) {
        if (!config.has(key)) return def;
        return config.get(key).getAsString();
    }
}
