package org.mods.gd656killicon.client.render.impl;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementTextureDefinition;
import org.mods.gd656killicon.client.render.IHudRenderer;
import org.mods.gd656killicon.client.sounds.ExternalSoundManager;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;
import org.mods.gd656killicon.client.util.ClientMessageLogger;
import org.mods.gd656killicon.common.KillType;

import java.util.ArrayDeque;
import java.util.Queue;

public class Battlefield1Renderer implements IHudRenderer {

    private static final String SOUND_NAME = "killsound_bf1";
    private static final String HEADSHOT_SOUND_NAME = "headshotkillsound_bf1";

    private boolean visible = true;
    private int iconSize = 40;     private int borderSize = 3;
    private int xOffset = 0;
    private int yOffset = 0;
    private int backgroundColor = 0x000000;
    private float iconBoxAlpha = 0.2f;     private float textBoxAlpha = 0.1f;     private float scaleWeapon = 1.0f;
    private float scaleVictim = 1.2f;
    private float scaleHealth = 1.5f;
    private int colorVictim = 0xFF0000;
    private long animationDuration = 200L;
    private long displayDuration = 300L;
    private JsonObject currentConfig;

    private long startTime = -1;
    private boolean isVisible = false;
    
    private static final int MAX_QUEUE_SIZE = 5;
    private final Queue<TriggerContext> displayQueue = new ArrayDeque<>();
    private long lastSwitchTime = 0;
    
    private String weaponName = "";
    private String victimName = "";
    private String healthText = "";
    private int killType = KillType.NORMAL;
    private String currentIconPath = "killicon_battlefield1_default.png";

    @Override
    public void trigger(TriggerContext context) {
        if (context.type() == KillType.ASSIST) {
            return;
        }

        JsonObject config = ConfigManager.getElementConfig("kill_icon", "battlefield1");
        if (config == null) return;
        
        
        if (displayQueue.size() < MAX_QUEUE_SIZE) {
            displayQueue.offer(context);
        }
    }

    private void processContext(TriggerContext context) {
        JsonObject config = ConfigManager.getElementConfig("kill_icon", "battlefield1");
        if (config == null) return;

        loadConfig(config);

        if (!this.visible) {
            this.isVisible = false;
            return;
        }

        this.killType = context.type();
        Minecraft mc = Minecraft.getInstance();

        String soundToPlay = SOUND_NAME;
        if (this.killType == KillType.HEADSHOT) {
            soundToPlay = HEADSHOT_SOUND_NAME;
        }
        String textureKey = getTextureKey(this.killType);
        this.currentIconPath = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/battlefield1",
            textureKey,
            currentConfig
        );

        ExternalSoundManager.playSound(soundToPlay);

        if (this.killType == KillType.DESTROY_VEHICLE) {
            if (context.extraData() != null && context.extraData().contains("|")) {
                String[] parts = context.extraData().split("\\|", 2);
                if (parts.length > 0) {
                    this.victimName = net.minecraft.client.resources.language.I18n.get(parts[0]);
                } else {
                    this.victimName = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.vehicle");
                }
                if (parts.length > 1) {
                    this.healthText = parts[1];
                } else {
                    this.healthText = "?";
                }
            } else {
                 this.victimName = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.vehicle");
                 this.healthText = "?";
            }
            
            if (mc.player != null) {
                if (mc.player.getVehicle() != null) {
                    this.weaponName = mc.player.getVehicle().getDisplayName().getString();
                } else {
                    ItemStack held = mc.player.getMainHandItem();
                    this.weaponName = held.isEmpty() ? 
                        net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.bare_hand") : 
                        held.getHoverName().getString();
                }
            } else {
                this.weaponName = "Unknown";
            }
        } else {
            if (mc.player != null) {
                if (mc.player.getVehicle() != null) {
                    this.weaponName = mc.player.getVehicle().getDisplayName().getString();
                } else {
                    ItemStack held = mc.player.getMainHandItem();
                    this.weaponName = held.isEmpty() ? 
                        net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.bare_hand") : 
                        held.getHoverName().getString();
                }
            } else {
                this.weaponName = "Unknown";
            }

            String nameOverride = null;
            if (context.extraData() != null && !context.extraData().isEmpty()) {
                 String extra = context.extraData();
                 if (extra.contains("|")) {
                     nameOverride = extra.substring(0, extra.indexOf("|"));
                 } else {
                     nameOverride = extra;
                 }
            }

            if (nameOverride != null) {
                this.victimName = net.minecraft.client.resources.language.I18n.get(nameOverride);
                if (mc.level != null && context.entityId() != -1) {
                     Entity entity = mc.level.getEntity(context.entityId());
                     if (entity instanceof LivingEntity living) {
                         this.healthText = String.valueOf((int) living.getMaxHealth());
                     } else {
                         this.healthText = "20";
                     }
                } else {
                     this.healthText = "?";
                }
            } else if (mc.level != null && context.entityId() != -1) {
                Entity entity = mc.level.getEntity(context.entityId());
                if (entity != null) {
                    this.victimName = entity.getDisplayName().getString();
                    if (entity instanceof LivingEntity living) {
                        float maxHealth = living.getMaxHealth();
                        this.healthText = String.valueOf((int) maxHealth);
                    } else {
                        this.healthText = "20";                     }
                } else {
                    this.victimName = "Unknown";
                    this.healthText = "?";
                }
            } else {
                this.victimName = "Unknown";
                this.healthText = "?";
            }
        }

        this.startTime = System.currentTimeMillis();
        this.lastSwitchTime = this.startTime;
        this.isVisible = true;
    }

    private String getTextureKey(int killType) {
        return switch (killType) {
            case KillType.HEADSHOT -> "headshot";
            case KillType.EXPLOSION -> "explosion";
            case KillType.CRIT -> "crit";
            case KillType.DESTROY_VEHICLE -> "destroy_vehicle";
            default -> "default";
        };
    }

    private long resolveQueueSwitchDelay(int nextKillType) {
        long baseDelay = animationDuration;
        if (this.killType == KillType.DESTROY_VEHICLE || nextKillType == KillType.DESTROY_VEHICLE) {
            baseDelay = Math.max(baseDelay, displayDuration);
        }
        return baseDelay;
    }

    public void triggerPreview(int killType, String weaponName, String victimName, String healthText) {
        JsonObject config = ConfigManager.getElementConfig("kill_icon", "battlefield1");
        if (config == null) return;

        loadConfig(config);
        if (!this.visible) {
            this.isVisible = false;
            return;
        }

        this.killType = killType;
        this.weaponName = weaponName;
        this.victimName = victimName;
        this.healthText = healthText;

        String textureKey = getTextureKey(this.killType);
        this.currentIconPath = ElementTextureDefinition.getSelectedTextureFileName(
            ConfigManager.getCurrentPresetId(),
            "kill_icon/battlefield1",
            textureKey,
            currentConfig
        );

        this.startTime = System.currentTimeMillis();
        this.lastSwitchTime = this.startTime;
        this.isVisible = true;
        this.displayQueue.clear();
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTick) {
        long currentTime = System.currentTimeMillis();

        if (!displayQueue.isEmpty()) {
            boolean shouldSwitch = !isVisible;
            if (isVisible) {
                long elapsedSinceSwitch = currentTime - lastSwitchTime;
                TriggerContext nextContext = displayQueue.peek();
                int nextKillType = nextContext != null ? nextContext.type() : KillType.NORMAL;
                long switchDelay = resolveQueueSwitchDelay(nextKillType);
                if (elapsedSinceSwitch >= switchDelay) {
                    shouldSwitch = true;
                }
            }
            if (shouldSwitch) {
                processContext(displayQueue.poll());
            }
        }

        if (!isVisible || startTime == -1) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float centerX = screenWidth / 2.0f + xOffset;
        float centerY = screenHeight - yOffset;
        renderInternal(guiGraphics, partialTick, currentTime, centerX, centerY);
    }

    public void renderAt(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY) {
        if (!isVisible || startTime == -1) return;
        long currentTime = System.currentTimeMillis();
        renderInternal(guiGraphics, partialTick, currentTime, centerX, centerY);
    }

    private void renderInternal(GuiGraphics guiGraphics, float partialTick, long currentTime, float centerX, float centerY) {
        long elapsed = currentTime - startTime;

        float alpha = getAlpha(elapsed);
        float globalScale = getGlobalScale(elapsed);

        if (displayQueue.isEmpty() && alpha <= 0.001f && elapsed > displayDuration) {
            isVisible = false;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int weaponW = font.width(weaponName);
        int victimW = font.width(victimName);
        int healthW = font.width(healthText);
        int fontH = font.lineHeight;

        float effWeaponW = weaponW * scaleWeapon;
        float effWeaponH = fontH * scaleWeapon;
        
        float effVictimW = victimW * scaleVictim;
        float effVictimH = fontH * scaleVictim;
        
        float effHealthW = healthW * scaleHealth;
        float effHealthH = fontH * scaleHealth;

        float weaponX = -effWeaponW / 2.0f;
        float weaponY = -effWeaponH / 2.0f;
        float weaponRight = weaponX + effWeaponW;
        float weaponTop = weaponY;
        float weaponBottom = weaponY + effWeaponH;

        float victimRight = weaponRight;
        float victimX = victimRight - effVictimW;
        float victimBottom = weaponTop - borderSize;
        float victimY = victimBottom - effVictimH;
        float victimTop = victimY;
        float victimLeft = victimX;

        float healthX = weaponRight + borderSize;
        float spanTop = victimTop;
        float spanBottom = weaponBottom;
        float midY = (spanTop + spanBottom) / 2.0f;
        float healthY = midY - effHealthH / 2.0f;
        float healthRight = healthX + effHealthW;
        float healthTop = healthY;
        float healthBottom = healthY + effHealthH;
        float healthLeft = healthX;

        float destroyW = 0;
        float destroyH = 0;
        float destroyX = 0;
        float destroyY = 0;
        
        if (this.killType == KillType.DESTROY_VEHICLE) {
            String destroyText = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.vehicle_destroyed");
            destroyW = font.width(destroyText) * scaleVictim;
            destroyH = font.lineHeight * scaleVictim;
            destroyY = midY - destroyH / 2.0f;
            destroyX = healthX - borderSize - destroyW;
        }

        float subBoxTop = Math.min(weaponTop, Math.min(healthTop, victimTop)) - borderSize;
        
        float subBoxRight;
        if (this.killType == KillType.DESTROY_VEHICLE) {
            subBoxRight = Math.max(weaponRight, Math.max(healthRight, victimRight)) + borderSize;
        } else {
            float distY = healthTop - subBoxTop;
            distY = Math.max(distY, borderSize); 
            subBoxRight = healthRight + distY;
        }

        float subBoxLeft = Math.min(weaponX, Math.min(healthLeft, victimLeft)) - borderSize;
        float subBoxBottom = Math.max(weaponBottom, Math.max(healthBottom, victimBottom)) + borderSize;

        if (this.killType == KillType.DESTROY_VEHICLE) {
            subBoxLeft = Math.min(subBoxLeft, destroyX - borderSize);
            subBoxTop = Math.min(subBoxTop, destroyY - borderSize);
            subBoxBottom = Math.max(subBoxBottom, destroyY + destroyH + borderSize);
        }

        float subBoxW = subBoxRight - subBoxLeft;
        float subBoxH = subBoxBottom - subBoxTop;

        float iconBoxSize = subBoxH;
        float iconBoxRight = subBoxLeft;
        float iconBoxTop = subBoxTop;
        float iconBoxLeft = iconBoxRight - iconBoxSize;
        float iconBoxBottom = iconBoxTop + iconBoxSize;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        
        pose.translate(centerX, centerY, 0);
        pose.scale(globalScale, globalScale, 1.0f);
        
        float subBoxFinalAlpha = alpha * textBoxAlpha;
        int subBoxColor = (int)(subBoxFinalAlpha * 255) << 24 | (backgroundColor & 0xFFFFFF);
        guiGraphics.fill((int)subBoxLeft, (int)subBoxTop, (int)subBoxRight, (int)subBoxBottom, subBoxColor);
        
        float iconBoxFinalAlpha = alpha * iconBoxAlpha;
        int iconBoxColor = (int)(iconBoxFinalAlpha * 255) << 24 | (backgroundColor & 0xFFFFFF);
        guiGraphics.fill((int)iconBoxLeft, (int)iconBoxTop, (int)iconBoxRight, (int)iconBoxBottom, iconBoxColor);
        
        float iconDrawSize = (float) this.iconSize;
        float iconCenterX = iconBoxLeft + iconBoxSize / 2.0f;
        float iconCenterY = iconBoxTop + iconBoxSize / 2.0f;
        float iconDrawX = iconCenterX - iconDrawSize / 2.0f;
        float iconDrawY = iconCenterY - iconDrawSize / 2.0f;
        
        renderIcon(guiGraphics, iconDrawX, iconDrawY, iconDrawSize, alpha);

        int textAlpha = (int)(alpha * 255);
        if (textAlpha < 5) textAlpha = 5; 
        
        if (this.killType == KillType.DESTROY_VEHICLE) {
            String destroyText = net.minecraft.client.resources.language.I18n.get("gd656killicon.client.text.vehicle_destroyed");
            drawScaledText(guiGraphics, font, destroyText, destroyX, destroyY, scaleVictim, 0xFFFFFF, textAlpha);
        } else {
            drawScaledText(guiGraphics, font, weaponName, weaponX, weaponY, scaleWeapon, 0xFFFFFF, textAlpha);
            drawScaledText(guiGraphics, font, victimName, victimX, victimY, scaleVictim, colorVictim, textAlpha);
        }
        
        drawScaledText(guiGraphics, font, healthText, healthX, healthY, scaleHealth, 0xFFFFFF, textAlpha);

        pose.popPose();
    }

    private void renderIcon(GuiGraphics guiGraphics, float x, float y, float size, float alpha) {
        ResourceLocation texture = ExternalTextureManager.getTexture(this.currentIconPath);
        String textureKey = getBattlefieldTextureKey();
        float frameWidthRatio = resolveFrameRatio(textureKey, "texture_frame_width_ratio");
        float frameHeightRatio = resolveFrameRatio(textureKey, "texture_frame_height_ratio");
        float drawWidth = size * frameWidthRatio;
        float drawHeight = size * frameHeightRatio;
        float drawX = x + (size - drawWidth) / 2.0f;
        float drawY = y + (size - drawHeight) / 2.0f;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        RenderSystem.enableBlend();
        
        guiGraphics.blit(texture, (int)drawX, (int)drawY, 0, 0, (int)drawWidth, (int)drawHeight, (int)drawWidth, (int)drawHeight);
        
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawScaledText(GuiGraphics guiGraphics, Font font, String text, float x, float y, float scale, int colorRGB, int alpha) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0f);
        
        int color = (alpha << 24) | (colorRGB & 0x00FFFFFF);
        guiGraphics.drawString(font, text, 0, 0, color, true); 
        
        pose.popPose();
    }

    private void loadConfig(JsonObject config) {
        try {
            this.currentConfig = config;
            this.visible = !config.has("visible") || config.get("visible").getAsBoolean();
            this.iconSize = config.has("icon_size") ? config.get("icon_size").getAsInt() : 40;
            this.borderSize = config.has("border_size") ? config.get("border_size").getAsInt() : 3;
            this.xOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
            this.yOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 0;
            
            String bgHex = config.has("background_color") ? config.get("background_color").getAsString() : "#000000";
            this.backgroundColor = parseHex(bgHex, 0x000000);
            
            int iconTrans = config.has("icon_box_opacity") ? config.get("icon_box_opacity").getAsInt() : 80;
            this.iconBoxAlpha = (100 - iconTrans) / 100.0f;
            
            int textTrans = config.has("text_box_opacity") ? config.get("text_box_opacity").getAsInt() : 90;
            this.textBoxAlpha = (100 - textTrans) / 100.0f;
            
            this.scaleWeapon = config.has("scale_weapon") ? config.get("scale_weapon").getAsFloat() : 1.0f;
            this.scaleVictim = config.has("scale_victim") ? config.get("scale_victim").getAsFloat() : 1.2f;
            this.scaleHealth = config.has("scale_health") ? config.get("scale_health").getAsFloat() : 1.5f;
            
            String vicHex = config.has("color_victim") ? config.get("color_victim").getAsString() : "#FF0000";
            this.colorVictim = parseHex(vicHex, 0xFF0000);
            
            this.animationDuration = config.has("animation_duration") ? (long)(config.get("animation_duration").getAsFloat() * 1000) : 200L;
            this.displayDuration = config.has("display_duration") ? (long)(config.get("display_duration").getAsFloat() * 1000) : 300L;

        } catch (Exception e) {
            ClientMessageLogger.chatWarn("gd656killicon.client.config_error", "battlefield1");
        }
    }

    private String getBattlefieldTextureKey() {
        return switch (this.killType) {
            case KillType.HEADSHOT -> "headshot";
            case KillType.EXPLOSION -> "explosion";
            case KillType.CRIT -> "crit";
            case KillType.DESTROY_VEHICLE -> "destroy_vehicle";
            default -> "default";
        };
    }

    private float resolveFrameRatio(String textureKey, String suffixKey) {
        if (currentConfig == null || textureKey == null) {
            return 1.0f;
        }
        String key = "anim_" + textureKey + "_" + suffixKey;
        if (!currentConfig.has(key)) {
            return 1.0f;
        }
        int value = currentConfig.get(key).getAsInt();
        return value > 0 ? value : 1.0f;
    }

    private int parseHex(String hex, int def) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (Exception e) {
            return def;
        }
    }

    private float getAnimationProgress(long elapsed) {
        if (elapsed < animationDuration) {
            return (float) elapsed / animationDuration;
        }
        return 1.0f;
    }

    private float getAlpha(long elapsed) {
        if (elapsed < animationDuration) {
            return (float) elapsed / animationDuration;
        }
        
        long fadeOutStart = displayDuration;
        if (elapsed > fadeOutStart) {
            long fadeElapsed = elapsed - fadeOutStart;
            if (fadeElapsed >= animationDuration) return 0.0f;
            return 1.0f - (float) fadeElapsed / animationDuration;
        }
        return 1.0f;
    }

    private float getGlobalScale(long elapsed) {
        if (elapsed < animationDuration) {
            float t = (float) elapsed / animationDuration;
            float eased = 1.0f - (float) Math.pow(1.0f - t, 3);
            return Mth.lerp(eased, 0.6f, 1.0f);
        }
        return 1.0f;
    }
}
