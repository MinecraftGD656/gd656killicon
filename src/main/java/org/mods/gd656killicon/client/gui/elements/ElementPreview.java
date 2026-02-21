package org.mods.gd656killicon.client.gui.elements;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.config.ElementConfigManager;

public class ElementPreview {
    private final String elementId;
    private int x, y, width, height;
    private float hoverProgress = 0.0f; // 0.0 to 1.0
    private float clickProgress = 0.0f; // 0.0 to 1.0
    private boolean isPressed = false;
    private long lastTime;
    private boolean visible = true;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private boolean externalHover = false;
    
    // Config values
    private float scale = 1.0f;
    private int xOffset = 0;
    private int yOffset = 0;
    
    // BF1 specific config
    private float scaleWeapon = 1.0f;
    private float scaleVictim = 1.2f;
    private float scaleHealth = 1.5f;
    private int borderSize = 3;
    
    // BF1 layout offset
    private int bf1MinX = 0;
    private int bf1MinY = 0;
    
    // Bonus List specific config
    private boolean alignLeft = false;
    private boolean alignRight = false;
    
    // Z-Index for rendering order
    private int zIndex = 0;
    
    // Resize Handles
    private enum ResizeHandle { NONE, TL, TR, BL, BR }
    private ResizeHandle currentHandle = ResizeHandle.NONE;
    private float dragStartScale;
    private double dragStartDist;
    private int dragCenterX, dragCenterY;
    
    // Interaction State
    private long lastClickTime = 0;

    public enum PreviewInteractionResult {
        PASS,
        HANDLED,
        DOUBLE_CLICK,
        RIGHT_CLICK
    }
    
    // Handle Constants
    private static final int HANDLE_SIZE = 3;
    private static final int HANDLE_HIT_RADIUS = 5;
    private static final int HANDLE_VISIBILITY_RADIUS = 60; // Distance where handle becomes fully transparent

    public int getZIndex() {
        return zIndex;
    }

    private void updateZIndex() {
        if ("kill_icon/card_bar".equals(elementId)) {
            this.zIndex = 40;
        } else if (elementId.startsWith("subtitle/")) {
            this.zIndex = 20;
        } else {
            // Icons (classic, bf1, scrolling, combo, card, etc.)
            this.zIndex = 30;
        }
    }
    
    // ID Renderer
    private GDTextRenderer elementIdRenderer;
    private GDTextRenderer elementNameRenderer;
    private GDTextRenderer xCoordRenderer;
    private GDTextRenderer yCoordRenderer;
    private GDTextRenderer scaleRenderer;

    public ElementPreview(String elementId) {
        this.elementId = elementId;
        this.lastTime = System.currentTimeMillis();
    }

    public void updateConfig(JsonObject config) {
        if (config == null) {
            this.visible = false;
            return;
        }
        this.visible = config.has("visible") ? config.get("visible").getAsBoolean() : true;
        this.scale = config.has("scale") ? config.get("scale").getAsFloat() : 1.0f;
        this.xOffset = config.has("x_offset") ? config.get("x_offset").getAsInt() : 0;
        this.yOffset = config.has("y_offset") ? config.get("y_offset").getAsInt() : 0;
        
        if ("kill_icon/battlefield1".equals(elementId)) {
            this.scaleWeapon = config.has("scale_weapon") ? config.get("scale_weapon").getAsFloat() : 1.0f;
            this.scaleVictim = config.has("scale_victim") ? config.get("scale_victim").getAsFloat() : 1.2f;
            this.scaleHealth = config.has("scale_health") ? config.get("scale_health").getAsFloat() : 1.5f;
            this.borderSize = config.has("border_size") ? config.get("border_size").getAsInt() : 3;
        } else if ("subtitle/score".equals(elementId)) {
            this.alignLeft = config.has("align_left") ? config.get("align_left").getAsBoolean() : false;
            this.alignRight = config.has("align_right") ? config.get("align_right").getAsBoolean() : false;
        } else if ("subtitle/bonus_list".equals(elementId)) {
            this.alignLeft = config.has("align_left") ? config.get("align_left").getAsBoolean() : false;
            this.alignRight = config.has("align_right") ? config.get("align_right").getAsBoolean() : false;
        }
        
        recalculateSize();
        updateZIndex();
    }
    
    private void recalculateSize() {
        if ("kill_icon/battlefield1".equals(elementId)) {
            // Calculate size based on dummy data
            calculateBattlefield1Size();
        } else if ("subtitle/kill_feed".equals(elementId)) {
            // Width: S * 120
            // Height: S * 10
            this.width = (int)(this.scale * 120);
            this.height = (int)(this.scale * 10); 
        } else if ("subtitle/score".equals(elementId)) {
            // Width: S * 15
            // Height: S * 6
            this.width = (int)(this.scale * 15);
            this.height = (int)(this.scale * 6) + 4;
        } else if ("subtitle/bonus_list".equals(elementId)) {
            this.width = (int)(this.scale * 40);
            this.height = (int)(this.scale * 25);
        } else if ("kill_icon/scrolling".equals(elementId)) {
            int size = (int)(this.scale * 70);
            this.width = size;
            this.height = size;
        } else if ("kill_icon/combo".equals(elementId)) {
            // Size calculation: S * 55
            int size = (int)(this.scale * 55);
            this.width = size;
            this.height = size;
        } else if ("kill_icon/card_bar".equals(elementId)) {
            // Width: S * 300
            // Height: S * 40
            this.width = (int)(this.scale * 300);
            this.height = (int)(this.scale * 40);
        } else if ("kill_icon/card".equals(elementId)) {
            // Width: S * 240
            // Height: S * 333
            this.width = (int)(this.scale * 240);
            this.height = (int)(this.scale * 333);
        } else {
            // Default Size calculation for unknown elements (fallback to S * 60)
            int size = (int)(this.scale * 60);
            this.width = size;
            this.height = size;
        }
    }

    // Placeholder method for BF1 size calculation
    private void calculateBattlefield1Size() {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        if (font == null) return;
        
        String weaponName = "HK416A5 突击步枪";
        String victimName = "-六五六-";
        String healthText = "20";
        
        // Calculate Text Sizes
        int weaponW = font.width(weaponName);
        int victimW = font.width(victimName);
        int healthW = font.width(healthText);
        int fontH = font.lineHeight;
        
        // Effective Size (Apply global scale)
        float effWeaponW = weaponW * scaleWeapon * scale;
        float effWeaponH = fontH * scaleWeapon * scale;
        
        float effVictimW = victimW * scaleVictim * scale;
        float effVictimH = fontH * scaleVictim * scale;
        
        float effHealthW = healthW * scaleHealth * scale;
        float effHealthH = fontH * scaleHealth * scale;
        
        // Layout
        // Weapon: Centered at (0, 0)
        float weaponX = -effWeaponW / 2.0f;
        float weaponY = -effWeaponH / 2.0f;
        float weaponRight = weaponX + effWeaponW;
        float weaponTop = weaponY;
        float weaponBottom = weaponY + effWeaponH;
        
        // Victim: Right aligned with Weapon Right, Bottom aligned with Weapon Top - Border
        float victimRight = weaponRight;
        float victimX = victimRight - effVictimW;
        float victimBottom = weaponTop - borderSize;
        float victimY = victimBottom - effVictimH;
        float victimTop = victimY;
        float victimLeft = victimX;
        
        // Health: Left aligned with Weapon Right + Border
        float healthX = weaponRight + borderSize;
        float spanTop = victimTop;
        float spanBottom = weaponBottom;
        float midY = (spanTop + spanBottom) / 2.0f;
        float healthY = midY - effHealthH / 2.0f;
        float healthRight = healthX + effHealthW;
        float healthTop = healthY;
        float healthBottom = healthY + effHealthH;
        float healthLeft = healthX;
        
        // Subtitle Box
        float subBoxRight = Math.max(weaponRight, Math.max(healthRight, victimRight)) + borderSize;
        float subBoxTop = Math.min(weaponTop, Math.min(healthTop, victimTop)) - borderSize;
        float subBoxLeft = Math.min(weaponX, Math.min(healthLeft, victimLeft)) - borderSize;
        float subBoxBottom = Math.max(weaponBottom, Math.max(healthBottom, victimBottom)) + borderSize;
        
        float subBoxH = subBoxBottom - subBoxTop;
        
        // Icon Box
        float iconBoxSize = subBoxH;
        float iconBoxRight = subBoxLeft;
        float iconBoxTop = subBoxTop;
        float iconBoxLeft = iconBoxRight - iconBoxSize;
        float iconBoxBottom = iconBoxTop + iconBoxSize;
        
        // Total Bounding Box
        // Min X is iconBoxLeft
        // Max X is subBoxRight
        // Min Y is subBoxTop
        // Max Y is subBoxBottom
        
        float minX = iconBoxLeft;
        float maxX = subBoxRight;
        float minY = subBoxTop;
        float maxY = subBoxBottom;
        
        this.width = (int)(maxX - minX);
        this.height = (int)(maxY - minY);
        
        this.bf1MinX = (int)minX;
        this.bf1MinY = (int)minY;
    }

    public void updatePosition(int screenWidth, int screenHeight) {
        // Calculate Position
        // Origin (0,0) is bottom-center of screen.
        // Y axis: Positive is UP (implied by typical HUD config where 0 is bottom)
        int centerX = (screenWidth / 2) + xOffset;
        int centerY = screenHeight - yOffset;
        
        if ("subtitle/bonus_list".equals(elementId) || "subtitle/score".equals(elementId)) {
            // For bonus_list and score, position depends on alignment
            int baseY = "subtitle/score".equals(elementId) ? centerY - 4 : centerY;
            
            if (alignLeft && !alignRight) {
                // Top-Left alignment: (centerX, centerY) is Top-Left of element
                this.x = centerX;
                this.y = baseY;
            } else if (alignRight && !alignLeft) {
                // Top-Right alignment: (centerX, centerY) is Top-Right of element
                this.x = centerX - width;
                this.y = baseY;
            } else {
                // Center alignment: (centerX, centerY) is Top-Center of element
                this.x = centerX - width / 2;
                this.y = baseY;
            }
        } else if ("kill_icon/card".equals(elementId)) {
            // For card, position is Bottom-Center
            this.x = centerX - width / 2;
            this.y = centerY - height;
        } else if ("subtitle/kill_feed".equals(elementId)) {
            // For kill_feed, position is Top-Center
            this.x = centerX - width / 2;
            this.y = centerY;
        } else if ("kill_icon/battlefield1".equals(elementId)) {
            // For BF1, position is relative to anchor + calculated min offsets
            this.x = centerX + bf1MinX;
            this.y = centerY + bf1MinY;
        } else {
            // Assume position defines the CENTER of the element
            this.x = centerX - width / 2;
            this.y = centerY - height / 2;
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void setExternalHover(boolean hover) {
        this.externalHover = hover;
    }

    public void render(GuiGraphics guiGraphics, float partialTick, int screenWidth, boolean isHovered, double mouseX, double mouseY) {
        // Position (x, y) must be updated via updatePosition() before calling render
        boolean hovered = isHovered || externalHover;
        
        // Update Animation
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000.0f;
        lastTime = now;

        // Hover Animation
        if (hovered) {
            hoverProgress = Math.min(1.0f, hoverProgress + dt * 2.0f);
        } else {
            hoverProgress = Math.max(0.0f, hoverProgress - dt * 2.0f);
        }
        
        // Click Animation
        if (isPressed) {
            clickProgress = Math.min(1.0f, clickProgress + dt * 5.0f); // Fast fade in
        } else {
            clickProgress = Math.max(0.0f, clickProgress - dt * 5.0f); // Fast fade out
        }

        // Determine Colors based on Visibility
        int baseAlpha = (GuiConstants.COLOR_BG >> 24) & 0xFF;
        int borderAlpha = 0x40;
        
        if (visible) {
            // Non-hidden: increase opacity
            baseAlpha = Math.min(255, (int)(baseAlpha * 2.0)); 
            borderAlpha = Math.min(255, (int)(borderAlpha * 2.0));
        }
        
        int baseColor = (GuiConstants.COLOR_BG & 0x00FFFFFF) | (baseAlpha << 24);
        int borderColor = (GuiConstants.COLOR_BLACK & 0x00FFFFFF) | (borderAlpha << 24);
        int trailColor = visible ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY;

        // Render Background
        int targetClickColor = visible ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY;
        int targetColor = (targetClickColor & 0x00FFFFFF) | (0x80 << 24); // ~50% opacity
        
        int fillColor = interpolateColor(baseColor, targetColor, clickProgress);
        guiGraphics.fill(x, y, x + width, y + height, fillColor);

        // Render Border
        if (hoverProgress < 1.0f) {
            // Draw static border
            guiGraphics.fill(x, y, x + width, y + 1, borderColor); // Top
            guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor); // Bottom
            guiGraphics.fill(x, y + 1, x + 1, y + height - 1, borderColor); // Left
            guiGraphics.fill(x + width - 1, y + 1, x + width, y + height - 1, borderColor); // Right
        }

        // Render Hover Trail (if hovering)
        if (hoverProgress > 0.001f) {
            renderHoverTrail(guiGraphics, x, y, width, height, trailColor);
        }
        
        // Render Resize Handles
        renderHandles(guiGraphics, mouseX, mouseY);

        // Render Preset ID Subtitle
        if (isHovered) {
            renderSubtitle(guiGraphics, partialTick, screenWidth);
        }
    }
    
    private void renderSubtitle(GuiGraphics guiGraphics, float partialTick, int screenWidth) {
            Minecraft mc = Minecraft.getInstance();
            int spaceWidth = mc.font.width(" ");
            int textX = this.x + this.width + spaceWidth;
            int textY = this.y;
            int textX2 = screenWidth;
            
            // Only render if there's enough space
            if (textX < textX2) {
                // Text Colors
                int idColor = visible ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY;
                int nameColor = visible ? GuiConstants.COLOR_WHITE : GuiConstants.COLOR_GRAY;
                String idText = visible ? elementId : elementId + " [隐藏状态]";
                
                if (elementIdRenderer == null) {
                    elementIdRenderer = new GDTextRenderer(idText, textX, textY, textX2, textY + 9, 1.0f, idColor, false);
                } else {
                    elementIdRenderer.setX1(textX);
                    elementIdRenderer.setY1(textY);
                    elementIdRenderer.setX2(textX2);
                    elementIdRenderer.setY2(textY + 9);
                    elementIdRenderer.setText(idText);
                    elementIdRenderer.setColor(idColor);
                }
                elementIdRenderer.render(guiGraphics, partialTick);

                // Render Element Name
                int nameY = textY + 10; // 1px spacing
                String nameKey = "gd656killicon.element.name." + elementId.replace("/", ".");
                String nameText = I18n.get(nameKey);
                
                if (elementNameRenderer == null) {
                    elementNameRenderer = new GDTextRenderer(nameText, textX, nameY, textX2, nameY + 9, 1.0f, nameColor, false);
                } else {
                    elementNameRenderer.setX1(textX);
                    elementNameRenderer.setY1(nameY);
                    elementNameRenderer.setX2(textX2);
                    elementNameRenderer.setY2(nameY + 9);
                    elementNameRenderer.setText(nameText);
                    elementNameRenderer.setColor(nameColor);
                }
                elementNameRenderer.render(guiGraphics, partialTick);

                // Render X Coord
                String xText = "X: " + this.xOffset;
                int xLineY = nameY + 10; // 1px spacing after name
                if (xCoordRenderer == null) {
                    xCoordRenderer = new GDTextRenderer(xText, textX, xLineY, textX2, xLineY + 5, 0.5f, GuiConstants.COLOR_GRAY, false);
                } else {
                    xCoordRenderer.setX1(textX);
                    xCoordRenderer.setY1(xLineY);
                    xCoordRenderer.setX2(textX2);
                    xCoordRenderer.setY2(xLineY + 5);
                    xCoordRenderer.setText(xText);
                    xCoordRenderer.setColor(GuiConstants.COLOR_GRAY);
                }
                xCoordRenderer.render(guiGraphics, partialTick);

                // Render Y Coord
                String yText = "Y: " + this.yOffset;
                int yLineY = xLineY + 5;
                if (yCoordRenderer == null) {
                    yCoordRenderer = new GDTextRenderer(yText, textX, yLineY, textX2, yLineY + 5, 0.5f, GuiConstants.COLOR_GRAY, false);
                } else {
                    yCoordRenderer.setX1(textX);
                    yCoordRenderer.setY1(yLineY);
                    yCoordRenderer.setX2(textX2);
                    yCoordRenderer.setY2(yLineY + 5);
                    yCoordRenderer.setText(yText);
                    yCoordRenderer.setColor(GuiConstants.COLOR_GRAY);
                }
                yCoordRenderer.render(guiGraphics, partialTick);

                // Render Scale
                if (!"kill_icon/battlefield1".equals(elementId)) {
                    String sText = "S: " + String.format("%.2f", this.scale);
                    int sLineY = yLineY + 5;
                    if (scaleRenderer == null) {
                        scaleRenderer = new GDTextRenderer(sText, textX, sLineY, textX2, sLineY + 5, 0.5f, GuiConstants.COLOR_GRAY, false);
                    } else {
                        scaleRenderer.setX1(textX);
                        scaleRenderer.setY1(sLineY);
                        scaleRenderer.setX2(textX2);
                        scaleRenderer.setY2(sLineY + 5);
                        scaleRenderer.setText(sText);
                        scaleRenderer.setColor(GuiConstants.COLOR_GRAY);
                    }
                    scaleRenderer.render(guiGraphics, partialTick);
                }
            }
    }
    
    private void renderHandles(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        if ("kill_icon/battlefield1".equals(elementId)) return;
        
        int color = visible ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY;
        
        drawHandle(guiGraphics, x, y, mouseX, mouseY, color, ResizeHandle.TL);
        drawHandle(guiGraphics, x + width - 1, y, mouseX, mouseY, color, ResizeHandle.TR);
        drawHandle(guiGraphics, x, y + height - 1, mouseX, mouseY, color, ResizeHandle.BL);
        drawHandle(guiGraphics, x + width - 1, y + height - 1, mouseX, mouseY, color, ResizeHandle.BR);
    }
    
    private void drawHandle(GuiGraphics guiGraphics, int hx, int hy, double mx, double my, int color, ResizeHandle handleType) {
        double dist = Math.sqrt(Math.pow(mx - hx, 2) + Math.pow(my - hy, 2));
        
        int alpha = 0;
        int finalColor = color;
        
        if (currentHandle == handleType) {
            alpha = 255;
            // Smooth transition to white using clickProgress
            finalColor = interpolateColor(color, 0xFFFFFFFF, clickProgress);
        } else if (dist < HANDLE_VISIBILITY_RADIUS) {
            float factor = 1.0f - (float)(dist / HANDLE_VISIBILITY_RADIUS);
            alpha = (int)(factor * 255);
            
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = (color) & 0xFF;
            finalColor = (alpha << 24) | (r << 16) | (g << 8) | b;
        } else {
            return;
        }
        
        if (alpha <= 5) return;
        
        int half = HANDLE_SIZE / 2;
        int x1 = hx - half;
        int y1 = hy - half;
        int x2 = hx + half + 1;
        int y2 = hy + half + 1;
        
        guiGraphics.fill(x1, y1, x2, y2, finalColor);
    }
    
    private ResizeHandle getHandleAt(double mouseX, double mouseY) {
        if ("kill_icon/battlefield1".equals(elementId)) return ResizeHandle.NONE;

        if (isNear(mouseX, mouseY, x, y)) return ResizeHandle.TL;
        if (isNear(mouseX, mouseY, x + width - 1, y)) return ResizeHandle.TR;
        if (isNear(mouseX, mouseY, x, y + height - 1)) return ResizeHandle.BL;
        if (isNear(mouseX, mouseY, x + width - 1, y + height - 1)) return ResizeHandle.BR;
        return ResizeHandle.NONE;
    }

    private boolean isNear(double mx, double my, int hx, int hy) {
        return Math.sqrt(Math.pow(mx - hx, 2) + Math.pow(my - hy, 2)) <= HANDLE_HIT_RADIUS;
    }
    
    public void resetVisualState() {
        this.isPressed = false;
        this.clickProgress = 0.0f;
        this.dragging = false;
        this.currentHandle = ResizeHandle.NONE;
    }

    public PreviewInteractionResult mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) { // Right Click
             if (isMouseOver(mouseX, mouseY)) {
                 return PreviewInteractionResult.RIGHT_CLICK;
             }
             return PreviewInteractionResult.PASS;
        }

        if (button != 0) return PreviewInteractionResult.PASS;
        
        // Check Handles First
        ResizeHandle handle = getHandleAt(mouseX, mouseY);
        if (handle != ResizeHandle.NONE) {
            currentHandle = handle;
            
            // Initialize Drag State for Resizing
            dragStartScale = this.scale;
            
            // Calculate center of the element (visual center)
            dragCenterX = x + width / 2;
            dragCenterY = y + height / 2;
            
            // Calculate initial distance from center to mouse (handle)
            dragStartDist = Math.sqrt(Math.pow(mouseX - dragCenterX, 2) + Math.pow(mouseY - dragCenterY, 2));
            
            // Prevent division by zero
            if (dragStartDist < 1.0) dragStartDist = 1.0;
            
            isPressed = true; // Visual feedback
            return PreviewInteractionResult.HANDLED;
        }
        
        // Check Body for Moving
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            // Check Double Click
            long now = System.currentTimeMillis();
            if (now - lastClickTime < 250) {
                lastClickTime = now;
                return PreviewInteractionResult.DOUBLE_CLICK;
            }
            lastClickTime = now;

            isPressed = true;
            dragging = true;
            dragOffsetX = (int)mouseX - x;
            dragOffsetY = (int)mouseY - y;
            return PreviewInteractionResult.HANDLED;
        }
        return PreviewInteractionResult.PASS;
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        
        boolean wasInteracting = isPressed || dragging || currentHandle != ResizeHandle.NONE;
        
        isPressed = false;
        dragging = false;
        currentHandle = ResizeHandle.NONE;
        
        return wasInteracting;
    }
    
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, int screenWidth, int screenHeight) {
        if (button != 0) return false;
        
        if (currentHandle != ResizeHandle.NONE) {
            // Resizing Logic
            
            // Backup State
            float oldScale = this.scale;
            int oldWidth = this.width;
            int oldHeight = this.height;
            int oldX = this.x;
            int oldY = this.y;
            
            // Calculate new distance from center
            double currentDist = Math.sqrt(Math.pow(mouseX - dragCenterX, 2) + Math.pow(mouseY - dragCenterY, 2));
            
            // Calculate new scale
            float scaleFactor = (float)(currentDist / dragStartDist);
            float newScale = dragStartScale * scaleFactor;
            
            // Clamp Scale (0.1 to 5.0 seems reasonable)
            if (newScale < 0.1f) newScale = 0.1f;
            if (newScale > 5.0f) newScale = 5.0f;
            
            // Update Scale (Tentative)
            this.scale = newScale;
            recalculateSize();
            updatePosition(screenWidth, screenHeight);
            
            // Check Bounds
            int padding = GuiConstants.DEFAULT_PADDING;
            int minX = padding;
            int maxX = screenWidth - padding;
            int minY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + padding;
            int maxY = screenHeight - padding;
            
            boolean outOfBounds = x < minX || x + width > maxX || y < minY || y + height > maxY;
            
            if (outOfBounds && newScale > oldScale) {
                // Revert if expanding out of bounds
                this.scale = oldScale;
                this.width = oldWidth;
                this.height = oldHeight;
                this.x = oldX;
                this.y = oldY;
            } else {
                // Commit Config
                String presetId = ClientConfigManager.getCurrentPresetId();
                ElementConfigManager.updateConfigValue(presetId, elementId, "scale", String.valueOf(this.scale));
            }
            
            // Update Drag State for next frame to avoid jitter if center moves
            dragCenterX = x + width / 2;
            dragCenterY = y + height / 2;
            
            return true;
        }
        
        if (!dragging) return false;
        
        int newTopLeftX = (int)mouseX - dragOffsetX;
        int newTopLeftY = (int)mouseY - dragOffsetY;
        
        // Limit drag to main display area (below header) with padding
        int padding = GuiConstants.DEFAULT_PADDING;
        int minX = padding;
        int maxX = screenWidth - width - padding;
        int minY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + padding;
        int maxY = screenHeight - height - padding;

        if (newTopLeftX < minX) newTopLeftX = minX;
        if (newTopLeftX > maxX) newTopLeftX = maxX;
        if (newTopLeftY < minY) newTopLeftY = minY;
        if (newTopLeftY > maxY) newTopLeftY = maxY;
        
        int anchorX;
        int anchorY;
        
        if ("subtitle/bonus_list".equals(elementId) || "subtitle/score".equals(elementId)) {
            if (alignLeft && !alignRight) {
                anchorX = newTopLeftX;
                anchorY = newTopLeftY;
            } else if (alignRight && !alignLeft) {
                anchorX = newTopLeftX + width;
                anchorY = newTopLeftY;
            } else {
                anchorX = newTopLeftX + width / 2;
                anchorY = newTopLeftY;
            }
            
            if ("subtitle/score".equals(elementId)) {
                anchorY += 4;
            }
        } else if ("kill_icon/card".equals(elementId)) {
            anchorX = newTopLeftX + width / 2;
            anchorY = newTopLeftY + height;
        } else if ("subtitle/kill_feed".equals(elementId)) {
            anchorX = newTopLeftX + width / 2;
            anchorY = newTopLeftY;
        } else if ("kill_icon/battlefield1".equals(elementId)) {
            anchorX = newTopLeftX - bf1MinX;
            anchorY = newTopLeftY - bf1MinY;
        } else {
            anchorX = newTopLeftX + width / 2;
            anchorY = newTopLeftY + height / 2;
        }
        
        int newXOffset = anchorX - (screenWidth / 2);
        int newYOffset = screenHeight - anchorY;
        
        String presetId = ClientConfigManager.getCurrentPresetId();
        ElementConfigManager.updateConfigValue(presetId, elementId, "x_offset", String.valueOf(newXOffset));
        ElementConfigManager.updateConfigValue(presetId, elementId, "y_offset", String.valueOf(newYOffset));
        
        this.xOffset = newXOffset;
        this.yOffset = newYOffset;
        return true;
    }
    
    private void renderHoverTrail(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        if (hoverProgress <= 0.001f) return;
        // Total perimeter
        float totalLength = w + h + w + h;
        float currentLength = totalLength * easeOut(hoverProgress);
        
        // 1. Bottom Edge (Left to Right)
        float drawn = 0;
        
        // Bottom: (x, y+h) -> (x+w, y+h)
        if (currentLength > 0) {
            float segLen = Math.min(w, currentLength);
            guiGraphics.fill(x, y + h - 1, x + (int)segLen, y + h, color);
            drawn += w;
        }
        
        // Right: (x+w, y+h) -> (x+w, y)
        if (currentLength > drawn) {
            float rem = currentLength - drawn;
            float segLen = Math.min(h, rem);
            guiGraphics.fill(x + w - 1, y + h - (int)segLen, x + w, y + h, color);
            drawn += h;
        }
        
        // Top: (x+w, y) -> (x, y)
        if (currentLength > drawn) {
            float rem = currentLength - drawn;
            float segLen = Math.min(w, rem);
            guiGraphics.fill(x + w - (int)segLen, y, x + w, y + 1, color);
            drawn += w;
        }
        
        // Left: (x, y) -> (x, y+h)
        if (currentLength > drawn) {
            float rem = currentLength - drawn;
            float segLen = Math.min(h, rem);
            guiGraphics.fill(x, y, x + 1, y + (int)segLen, color);
        }
    }
    
    private float easeOut(float t) {
        return 1.0f - (float)Math.pow(1.0f - t, 3);
    }
    
    private int interpolateColor(int color1, int color2, float t) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = (color1) & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = (color2) & 0xFF;
        
        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public String getElementId() {
        return elementId;
    }
}
