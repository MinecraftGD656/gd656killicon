package org.mods.gd656killicon.client.gui.elements;

import com.google.gson.JsonObject;
import com.mojang.math.Axis;
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
    private float hoverProgress = 0.0f;     private float clickProgress = 0.0f;     private boolean isPressed = false;
    private float rotateHandleProgress = 0.0f;
    private long lastTime;
    private boolean visible = true;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private boolean externalHover = false;
    
    private float scale = 1.0f;
    private int xOffset = 0;
    private int yOffset = 0;
    private float rotationAngle = 0.0f;
    
    private float scaleWeapon = 1.0f;
    private float scaleVictim = 1.2f;
    private float scaleHealth = 1.5f;
    private int borderSize = 3;
    
    private int bf1MinX = 0;
    private int bf1MinY = 0;
    
    private boolean alignLeft = false;
    private boolean alignRight = false;
    
    private int zIndex = 0;
    
    private enum ResizeHandle { NONE, TL, TR, BL, BR, ROTATE }
    private ResizeHandle currentHandle = ResizeHandle.NONE;
    private float dragStartScale;
    private double dragStartDist;
    private int dragCenterX, dragCenterY;
    
    private long lastClickTime = 0;

    public enum PreviewInteractionResult {
        PASS,
        HANDLED,
        DOUBLE_CLICK,
        RIGHT_CLICK
    }
    
    private static final int HANDLE_SIZE = 3;
    private static final int HANDLE_HIT_RADIUS = 5;
    private static final int HANDLE_VISIBILITY_RADIUS = 60; 
    private static final int ROTATE_HANDLE_OFFSET = 10;
    public int getZIndex() {
        return zIndex;
    }

    private void updateZIndex() {
        if ("kill_icon/card_bar".equals(elementId)) {
            this.zIndex = 40;
        } else if (elementId.startsWith("subtitle/")) {
            this.zIndex = 20;
        } else {
            this.zIndex = 30;
        }
    }
    
    private GDTextRenderer elementIdRenderer;
    private GDTextRenderer elementNameRenderer;
    private GDTextRenderer xCoordRenderer;
    private GDTextRenderer yCoordRenderer;
    private GDTextRenderer scaleRenderer;
    private GDTextRenderer rotationRenderer;

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
        this.rotationAngle = config.has("rotation_angle") ? config.get("rotation_angle").getAsFloat() : 0.0f;
        
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
            calculateBattlefield1Size();
        } else if ("subtitle/kill_feed".equals(elementId)) {
            this.width = (int)(this.scale * 120);
            this.height = (int)(this.scale * 10); 
        } else if ("subtitle/score".equals(elementId)) {
            this.width = (int)(this.scale * 15);
            this.height = (int)(this.scale * 6) + 4;
        } else if ("subtitle/bonus_list".equals(elementId)) {
            this.width = (int)(this.scale * 40);
            this.height = (int)(this.scale * 25);
        } else if ("kill_icon/scrolling".equals(elementId)) {
            int size = (int)(this.scale * 70);
            this.width = size;
            this.height = size;
        } else if ("subtitle/combo".equals(elementId)) {
            this.width = (int)(this.scale * 30);
            this.height = (int)(this.scale * 10);
        } else if ("kill_icon/combo".equals(elementId)) {
            int size = (int)(this.scale * 55);
            this.width = size;
            this.height = size;
        } else if ("kill_icon/valorant".equals(elementId)) {
            int size = (int)(this.scale * 90);
            this.width = size;
            this.height = size;
        } else if ("kill_icon/card_bar".equals(elementId)) {
            this.width = (int)(this.scale * 300);
            this.height = (int)(this.scale * 40);
        } else if ("kill_icon/card".equals(elementId)) {
            this.width = (int)(this.scale * 240);
            this.height = (int)(this.scale * 333);
        } else {
            int size = (int)(this.scale * 60);
            this.width = size;
            this.height = size;
        }
    }

    private void calculateBattlefield1Size() {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        if (font == null) return;
        
        String weaponName = "HK416A5 突击步枪";
        String victimName = "-六五六-";
        String healthText = "20";
        
        int weaponW = font.width(weaponName);
        int victimW = font.width(victimName);
        int healthW = font.width(healthText);
        int fontH = font.lineHeight;
        
        float effWeaponW = weaponW * scaleWeapon * scale;
        float effWeaponH = fontH * scaleWeapon * scale;
        
        float effVictimW = victimW * scaleVictim * scale;
        float effVictimH = fontH * scaleVictim * scale;
        
        float effHealthW = healthW * scaleHealth * scale;
        float effHealthH = fontH * scaleHealth * scale;
        
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
        
        float subBoxRight = Math.max(weaponRight, Math.max(healthRight, victimRight)) + borderSize;
        float subBoxTop = Math.min(weaponTop, Math.min(healthTop, victimTop)) - borderSize;
        float subBoxLeft = Math.min(weaponX, Math.min(healthLeft, victimLeft)) - borderSize;
        float subBoxBottom = Math.max(weaponBottom, Math.max(healthBottom, victimBottom)) + borderSize;
        
        float subBoxH = subBoxBottom - subBoxTop;
        
        float iconBoxSize = subBoxH;
        float iconBoxRight = subBoxLeft;
        float iconBoxTop = subBoxTop;
        float iconBoxLeft = iconBoxRight - iconBoxSize;
        float iconBoxBottom = iconBoxTop + iconBoxSize;
        
        
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
        int centerX = (screenWidth / 2) + xOffset;
        int centerY = screenHeight - yOffset;
        
        if ("subtitle/bonus_list".equals(elementId) || "subtitle/score".equals(elementId)) {
            int baseY = "subtitle/score".equals(elementId) ? centerY - 4 : centerY;
            
            if (alignLeft && !alignRight) {
                this.x = centerX;
                this.y = baseY;
            } else if (alignRight && !alignLeft) {
                this.x = centerX - width;
                this.y = baseY;
            } else {
                this.x = centerX - width / 2;
                this.y = baseY;
            }
        } else if ("kill_icon/card".equals(elementId)) {
            this.x = centerX - width / 2;
            this.y = centerY - height;
        } else if ("subtitle/kill_feed".equals(elementId)) {
            this.x = centerX - width / 2;
            this.y = centerY;
        } else if ("kill_icon/battlefield1".equals(elementId)) {
            this.x = centerX + bf1MinX;
            this.y = centerY + bf1MinY;
        } else {
            this.x = centerX - width / 2;
            this.y = centerY - height / 2;
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        double[] local = toLocalPoint(mouseX, mouseY);
        double halfW = width / 2.0;
        double halfH = height / 2.0;
        return local[0] >= -halfW && local[0] <= halfW && local[1] >= -halfH && local[1] <= halfH;
    }

    public void setExternalHover(boolean hover) {
        this.externalHover = hover;
    }

    public void render(GuiGraphics guiGraphics, float partialTick, int screenWidth, boolean isHovered, double mouseX, double mouseY) {
        boolean hovered = isHovered || externalHover;
        
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000.0f;
        lastTime = now;

        if (hovered) {
            hoverProgress = Math.min(1.0f, hoverProgress + dt * 2.0f);
        } else {
            hoverProgress = Math.max(0.0f, hoverProgress - dt * 2.0f);
        }
        
        if (isPressed) {
            clickProgress = Math.min(1.0f, clickProgress + dt * 5.0f);         } else {
            clickProgress = Math.max(0.0f, clickProgress - dt * 5.0f);         }
        boolean nearRotateHandle = isMouseNearRotateHandle(mouseX, mouseY, 5.0);
        boolean rotateHandleShouldShow = hovered || ((rotateHandleProgress > 0.001f) && nearRotateHandle) || currentHandle == ResizeHandle.ROTATE;
        if (rotateHandleShouldShow) {
            rotateHandleProgress = Math.min(1.0f, rotateHandleProgress + dt * 6.0f);
        } else {
            rotateHandleProgress = Math.max(0.0f, rotateHandleProgress - dt * 6.0f);
        }

        int baseAlpha = (GuiConstants.COLOR_BG >> 24) & 0xFF;
        int borderAlpha = 0x40;
        
        if (visible) {
            baseAlpha = Math.min(255, (int)(baseAlpha * 2.0)); 
            borderAlpha = Math.min(255, (int)(borderAlpha * 2.0));
        }
        
        int baseColor = (GuiConstants.COLOR_BG & 0x00FFFFFF) | (baseAlpha << 24);
        int borderColor = (GuiConstants.COLOR_BLACK & 0x00FFFFFF) | (borderAlpha << 24);
        int trailColor = visible ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY;

        int targetClickColor = visible ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY;
        int targetColor = (targetClickColor & 0x00FFFFFF) | (0x80 << 24);         
        int fillColor = interpolateColor(baseColor, targetColor, clickProgress);
        guiGraphics.pose().pushPose();
        float centerX = x + width / 2.0f;
        float centerY = y + height / 2.0f;
        guiGraphics.pose().translate(centerX, centerY, 0.0f);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(rotationAngle));
        int localX = -width / 2;
        int localY = -height / 2;
        guiGraphics.fill(localX, localY, localX + width, localY + height, fillColor);

        if (hoverProgress < 1.0f) {
            guiGraphics.fill(localX, localY, localX + width, localY + 1, borderColor);
            guiGraphics.fill(localX, localY + height - 1, localX + width, localY + height, borderColor);
            guiGraphics.fill(localX, localY + 1, localX + 1, localY + height - 1, borderColor);
            guiGraphics.fill(localX + width - 1, localY + 1, localX + width, localY + height - 1, borderColor);
        }

        if (hoverProgress > 0.001f) {
            renderHoverTrail(guiGraphics, localX, localY, width, height, trailColor);
        }
        
        renderHandles(guiGraphics, mouseX, mouseY);
        guiGraphics.pose().popPose();

        if (hovered) {
            renderSubtitle(guiGraphics, partialTick, screenWidth);
        }
    }
    
    private void renderSubtitle(GuiGraphics guiGraphics, float partialTick, int screenWidth) {
            Minecraft mc = Minecraft.getInstance();
            int spaceWidth = mc.font.width(" ");
            int[] bounds = getRotatedBounds();
            int textX = bounds[1] + spaceWidth;
            int textY = bounds[2];
            int textX2 = screenWidth;
            
            if (textX < textX2) {
                int idColor = visible ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY;
                int nameColor = visible ? GuiConstants.COLOR_WHITE : GuiConstants.COLOR_GRAY;
                String idText = visible ? elementId : elementId + " [" + I18n.get("gd656killicon.client.gui.preview.hidden_state") + "]";
                
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

                int nameY = textY + 10;                 String nameKey = "gd656killicon.element.name." + elementId.replace("/", ".");
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

                String xText = "X: " + this.xOffset;
                int xLineY = nameY + 10;                 if (xCoordRenderer == null) {
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
                String rText = "R: " + String.format("%.1f°", this.rotationAngle);
                int rLineY = ("kill_icon/battlefield1".equals(elementId) ? yLineY : yLineY + 10);
                if (rotationRenderer == null) {
                    rotationRenderer = new GDTextRenderer(rText, textX, rLineY, textX2, rLineY + 5, 0.5f, GuiConstants.COLOR_GRAY, false);
                } else {
                    rotationRenderer.setX1(textX);
                    rotationRenderer.setY1(rLineY);
                    rotationRenderer.setX2(textX2);
                    rotationRenderer.setY2(rLineY + 5);
                    rotationRenderer.setText(rText);
                    rotationRenderer.setColor(GuiConstants.COLOR_GRAY);
                }
                rotationRenderer.render(guiGraphics, partialTick);
            }
    }
    
    private void renderHandles(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        if ("kill_icon/battlefield1".equals(elementId)) return;
        
        int color = visible ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY;
        
        drawHandle(guiGraphics, -width / 2, -height / 2, mouseX, mouseY, color, ResizeHandle.TL);
        drawHandle(guiGraphics, width / 2 - 1, -height / 2, mouseX, mouseY, color, ResizeHandle.TR);
        drawHandle(guiGraphics, -width / 2, height / 2 - 1, mouseX, mouseY, color, ResizeHandle.BL);
        drawHandle(guiGraphics, width / 2 - 1, height / 2 - 1, mouseX, mouseY, color, ResizeHandle.BR);
        if (rotateHandleProgress > 0.001f) {
            float eased = easeOut(rotateHandleProgress);
            float armLen = ROTATE_HANDLE_OFFSET * eased;
            int rotateY = (int)Math.round(-height / 2.0f - armLen);
            int lineEnd = (int)Math.round(-height / 2.0f - Math.max(1.0f, armLen));
            guiGraphics.fill(0, -height / 2, 1, lineEnd + 1, color);
            drawHandle(guiGraphics, 0, rotateY, mouseX, mouseY, color, ResizeHandle.ROTATE);
        }
    }
    
    private void drawHandle(GuiGraphics guiGraphics, int hx, int hy, double mx, double my, int color, ResizeHandle handleType) {
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        double dx = mx - centerX;
        double dy = my - centerY;
        double rad = Math.toRadians(-rotationAngle);
        double localMouseX = dx * Math.cos(rad) - dy * Math.sin(rad);
        double localMouseY = dx * Math.sin(rad) + dy * Math.cos(rad);
        double dist = Math.sqrt(Math.pow(localMouseX - hx, 2) + Math.pow(localMouseY - hy, 2));
        
        int alpha = 0;
        int finalColor = color;
        
        if (currentHandle == handleType) {
            alpha = 255;
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
        int[] tl = rotatedHandlePoint(-width / 2, -height / 2);
        int[] tr = rotatedHandlePoint(width / 2 - 1, -height / 2);
        int[] bl = rotatedHandlePoint(-width / 2, height / 2 - 1);
        int[] br = rotatedHandlePoint(width / 2 - 1, height / 2 - 1);
        int[] rotate = rotatedHandlePoint(0, getCurrentRotateHandleLocalY());
        if (isNear(mouseX, mouseY, tl[0], tl[1])) return ResizeHandle.TL;
        if (isNear(mouseX, mouseY, tr[0], tr[1])) return ResizeHandle.TR;
        if (isNear(mouseX, mouseY, bl[0], bl[1])) return ResizeHandle.BL;
        if (isNear(mouseX, mouseY, br[0], br[1])) return ResizeHandle.BR;
        if (rotateHandleProgress > 0.05f && isNear(mouseX, mouseY, rotate[0], rotate[1])) return ResizeHandle.ROTATE;
        return ResizeHandle.NONE;
    }

    private int[] rotatedHandlePoint(int localX, int localY) {
        double rad = Math.toRadians(rotationAngle);
        double rx = localX * Math.cos(rad) - localY * Math.sin(rad);
        double ry = localX * Math.sin(rad) + localY * Math.cos(rad);
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        return new int[] { centerX + (int)Math.round(rx), centerY + (int)Math.round(ry) };
    }

    private boolean isNear(double mx, double my, int hx, int hy) {
        return Math.sqrt(Math.pow(mx - hx, 2) + Math.pow(my - hy, 2)) <= HANDLE_HIT_RADIUS;
    }
    
    private int getCurrentRotateHandleLocalY() {
        float eased = easeOut(rotateHandleProgress);
        float armLen = ROTATE_HANDLE_OFFSET * eased;
        return (int)Math.round(-height / 2.0f - armLen);
    }
    
    private boolean isMouseNearRotateHandle(double mouseX, double mouseY, double radius) {
        int[] lineStart = rotatedHandlePoint(0, -height / 2);
        int[] lineEnd = rotatedHandlePoint(0, -height / 2 - ROTATE_HANDLE_OFFSET);
        double distanceToLine = distancePointToSegment(mouseX, mouseY, lineStart[0], lineStart[1], lineEnd[0], lineEnd[1]);
        if (distanceToLine <= radius) {
            return true;
        }
        return Math.sqrt(Math.pow(mouseX - lineEnd[0], 2) + Math.pow(mouseY - lineEnd[1], 2)) <= radius;
    }

    private double distancePointToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double vx = x2 - x1;
        double vy = y2 - y1;
        double wx = px - x1;
        double wy = py - y1;
        double len2 = vx * vx + vy * vy;
        if (len2 <= 0.0001) {
            return Math.sqrt(Math.pow(px - x1, 2) + Math.pow(py - y1, 2));
        }
        double t = (wx * vx + wy * vy) / len2;
        t = Math.max(0.0, Math.min(1.0, t));
        double projX = x1 + t * vx;
        double projY = y1 + t * vy;
        return Math.sqrt(Math.pow(px - projX, 2) + Math.pow(py - projY, 2));
    }
    
    
    public void resetVisualState() {
        this.isPressed = false;
        this.clickProgress = 0.0f;
        this.dragging = false;
        this.currentHandle = ResizeHandle.NONE;
    }

    public PreviewInteractionResult mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {              if (isMouseOver(mouseX, mouseY)) {
                 return PreviewInteractionResult.RIGHT_CLICK;
             }
             return PreviewInteractionResult.PASS;
        }

        if (button != 0) return PreviewInteractionResult.PASS;
        
        ResizeHandle handle = getHandleAt(mouseX, mouseY);
        if (handle != ResizeHandle.NONE) {
            currentHandle = handle;
            
            dragStartScale = this.scale;
            dragCenterX = x + width / 2;
            dragCenterY = y + height / 2;
            
            dragStartDist = Math.sqrt(Math.pow(mouseX - dragCenterX, 2) + Math.pow(mouseY - dragCenterY, 2));
            
            if (dragStartDist < 1.0) dragStartDist = 1.0;
            
            isPressed = true;             return PreviewInteractionResult.HANDLED;
        }
        
        if (isMouseOver(mouseX, mouseY)) {
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
            if (currentHandle == ResizeHandle.ROTATE) {
                double angleWorld = Math.toDegrees(Math.atan2(mouseY - dragCenterY, mouseX - dragCenterX));
                float newRotation = normalizeDegrees((float)(angleWorld + 90.0));
                this.rotationAngle = newRotation;
                String presetId = ClientConfigManager.getCurrentPresetId();
                ElementConfigManager.updateConfigValue(presetId, elementId, "rotation_angle", String.valueOf(newRotation));
                return true;
            }
            
            float oldScale = this.scale;
            int oldWidth = this.width;
            int oldHeight = this.height;
            int oldX = this.x;
            int oldY = this.y;
            
            double currentDist = Math.sqrt(Math.pow(mouseX - dragCenterX, 2) + Math.pow(mouseY - dragCenterY, 2));
            
            float scaleFactor = (float)(currentDist / dragStartDist);
            float newScale = dragStartScale * scaleFactor;
            
            if (newScale < 0.1f) newScale = 0.1f;
            if (newScale > 5.0f) newScale = 5.0f;
            
            this.scale = newScale;
            recalculateSize();
            updatePosition(screenWidth, screenHeight);
            
            int padding = GuiConstants.DEFAULT_PADDING;
            int minX = padding;
            int maxX = screenWidth - padding;
            int minY = GuiConstants.HEADER_HEIGHT + GuiConstants.GOLD_BAR_HEIGHT + padding;
            int maxY = screenHeight - padding;
            
            boolean outOfBounds = x < minX || x + width > maxX || y < minY || y + height > maxY;
            
            if (outOfBounds && newScale > oldScale) {
                this.scale = oldScale;
                this.width = oldWidth;
                this.height = oldHeight;
                this.x = oldX;
                this.y = oldY;
            } else {
                String presetId = ClientConfigManager.getCurrentPresetId();
                ElementConfigManager.updateConfigValue(presetId, elementId, "scale", String.valueOf(this.scale));
            }
            
            dragCenterX = x + width / 2;
            dragCenterY = y + height / 2;
            
            return true;
        }
        
        if (!dragging) return false;
        
        int newTopLeftX = (int)mouseX - dragOffsetX;
        int newTopLeftY = (int)mouseY - dragOffsetY;
        
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
        float totalLength = w + h + w + h;
        float currentLength = totalLength * easeOut(hoverProgress);
        
        float drawn = 0;
        
        if (currentLength > 0) {
            float segLen = Math.min(w, currentLength);
            guiGraphics.fill(x, y + h - 1, x + (int)segLen, y + h, color);
            drawn += w;
        }
        
        if (currentLength > drawn) {
            float rem = currentLength - drawn;
            float segLen = Math.min(h, rem);
            guiGraphics.fill(x + w - 1, y + h - (int)segLen, x + w, y + h, color);
            drawn += h;
        }
        
        if (currentLength > drawn) {
            float rem = currentLength - drawn;
            float segLen = Math.min(w, rem);
            guiGraphics.fill(x + w - (int)segLen, y, x + w, y + 1, color);
            drawn += w;
        }
        
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

    private double[] toLocalPoint(double worldX, double worldY) {
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        double dx = worldX - centerX;
        double dy = worldY - centerY;
        double rad = Math.toRadians(-rotationAngle);
        double localX = dx * Math.cos(rad) - dy * Math.sin(rad);
        double localY = dx * Math.sin(rad) + dy * Math.cos(rad);
        return new double[] { localX, localY };
    }

    private int[] getRotatedBounds() {
        int[] tl = rotatedHandlePoint(-width / 2, -height / 2);
        int[] tr = rotatedHandlePoint(width / 2, -height / 2);
        int[] bl = rotatedHandlePoint(-width / 2, height / 2);
        int[] br = rotatedHandlePoint(width / 2, height / 2);
        int minX = Math.min(Math.min(tl[0], tr[0]), Math.min(bl[0], br[0]));
        int maxX = Math.max(Math.max(tl[0], tr[0]), Math.max(bl[0], br[0]));
        int minY = Math.min(Math.min(tl[1], tr[1]), Math.min(bl[1], br[1]));
        int maxY = Math.max(Math.max(tl[1], tr[1]), Math.max(bl[1], br[1]));
        return new int[] { minX, maxX, minY, maxY };
    }

    private float normalizeDegrees(float angle) {
        float result = angle % 360.0f;
        if (result > 180.0f) {
            result -= 360.0f;
        } else if (result < -180.0f) {
            result += 360.0f;
        }
        return result;
    }
}
