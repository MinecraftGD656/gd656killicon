package org.mods.gd656killicon.client.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义文本渲染器，支持滚动和自动换行。
 */
public class GDTextRenderer {
    private final Minecraft minecraft;
    private String text;
    private int x1, y1, x2, y2;
    private int width, height;
    private float fontSize;
    private int color;
    private List<ColoredText> coloredTexts;
    private boolean autoWrap;
    private long pauseStartTime = -1;

    /**
     * 支持混合颜色的文本段
     */
    public static class ColoredText {
        public String text;
        public int color;

        public ColoredText(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }

    
    private float scrollOffset = 0;
    private boolean scrollingForward = true;
    private long lastTime;
    private static final float SCROLL_SPEED = 30.0f; 
    private static final long PAUSE_TIME_MS = 1000;  
    private boolean centered = false; 
    private Integer overrideColor = null;

    
    private float scrollY = 0;

    public void setOverrideColor(Integer color) {
        this.overrideColor = color;
    }

    public GDTextRenderer(String text, int x1, int y1, int x2, int y2, float fontSize, int color, boolean autoWrap) {
        this.minecraft = Minecraft.getInstance();
        this.text = text;
        this.coloredTexts = null;
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.width = this.x2 - this.x1;
        this.height = this.y2 - this.y1;
        this.fontSize = fontSize;
        this.color = color;
        this.autoWrap = autoWrap;
        this.lastTime = System.currentTimeMillis();
    }

    public GDTextRenderer(List<ColoredText> coloredTexts, int x1, int y1, int x2, int y2, float fontSize, boolean autoWrap) {
        this.minecraft = Minecraft.getInstance();
        this.coloredTexts = coloredTexts;
        this.text = "";
        for (ColoredText ct : coloredTexts) this.text += ct.text;
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.width = this.x2 - this.x1;
        this.height = this.y2 - this.y1;
        this.fontSize = fontSize;
        this.color = 0xFFFFFFFF;
        this.autoWrap = autoWrap;
        this.lastTime = System.currentTimeMillis();
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    public void render(GuiGraphics guiGraphics, float partialTick) {
        render(guiGraphics, partialTick, true);
    }

    public void render(GuiGraphics guiGraphics, float partialTick, boolean useScissor) {
        
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000.0f;
        lastTime = now;
        
        renderInternal(guiGraphics, partialTick, useScissor, dt);
    }

    /**
     * 内部渲染核心。
     * 无论是否使用外部 Scissor，只要不换行且宽度超限，就执行滚动逻辑。
     */
    public void renderInternal(GuiGraphics guiGraphics, float partialTick, boolean useOwnScissorAndScroll, float dt) {
        guiGraphics.pose().pushPose();
        
        if (useOwnScissorAndScroll) {
            guiGraphics.enableScissor(x1, y1, x2, y2);
        }

        float scale = fontSize; 
        guiGraphics.pose().translate(x1, y1, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);

        float scaledWidth = width / scale;

        if (autoWrap) {
            renderWrappedText(guiGraphics, scaledWidth);
        } else {
            renderScrollingText(guiGraphics, dt, scaledWidth);
        }

        guiGraphics.pose().popPose();
        
        if (useOwnScissorAndScroll) {
            guiGraphics.disableScissor();
        }
    }

    private void renderWrappedText(GuiGraphics guiGraphics, float maxWidth) {
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, -scrollY, 0);
        drawWrappedInternal(guiGraphics, maxWidth);
        guiGraphics.pose().popPose();
    }

    public void setScrollY(float scrollY) {
        this.scrollY = scrollY;
    }

    public float getScrollY() {
        return scrollY;
    }

    public float getMaxScrollY() {
        if (!autoWrap) return 0;
        int contentHeight = getFinalHeight();
        
        float containerHeight = this.height / fontSize;
        return Math.max(0, contentHeight - containerHeight);
    }

    private void drawWrappedInternal(GuiGraphics guiGraphics, float maxWidth) {
        if (coloredTexts != null) {
            renderMixedColorWrapped(guiGraphics, maxWidth);
            return;
        }
        List<String> lines = wrapText(text, maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float lineX = 0;
            if (centered) {
                lineX = (maxWidth - minecraft.font.width(line)) / 2.0f;
            }
            guiGraphics.drawString(minecraft.font, line, (int)lineX, i * 9, overrideColor != null ? overrideColor : color, true);
        }
    }

    private void renderMixedColorWrapped(GuiGraphics guiGraphics, float maxWidth) {
        int yOffset = 0;
        float currentX = 0;
        
        for (ColoredText ct : coloredTexts) {
            String remaining = ct.text;
            while (!remaining.isEmpty()) {
                int count = minecraft.font.width(remaining) > (maxWidth - currentX) ? 
                            minecraft.font.plainSubstrByWidth(remaining, (int)(maxWidth - currentX)).length() : 
                            remaining.length();
                
                if (count == 0 && currentX > 0) { 
                    currentX = 0;
                    yOffset += 9;
                    continue;
                }
                
                String part = remaining.substring(0, Math.max(1, count));
                guiGraphics.drawString(minecraft.font, part, (int)currentX, yOffset, overrideColor != null ? overrideColor : ct.color, true);
                
                currentX += minecraft.font.width(part);
                remaining = remaining.substring(part.length());
                
                if (currentX >= maxWidth - 1) {
                    currentX = 0;
                    yOffset += 9;
                }
            }
        }
    }

    private List<String> wrapText(String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        
        
        
        String processedText = text.replace("\\n", "\n").replace("\r\n", "\n").replace("\r", "\n");
        String[] paragraphs = processedText.split("\n", -1);

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            StringBuilder currentLine = new StringBuilder();
            
            for (int i = 0; i < paragraph.length(); i++) {
                char c = paragraph.charAt(i);
                String nextText = currentLine.toString() + c;
                if (minecraft.font.width(nextText) > maxWidth && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                currentLine.append(c);
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        return lines;
    }

    private void renderScrollingText(GuiGraphics guiGraphics, float dt, float maxWidth) {
        int textWidth = 0;
        if (coloredTexts == null) {
            textWidth = minecraft.font.width(text);
        } else {
            for (ColoredText ct : coloredTexts) textWidth += minecraft.font.width(ct.text);
        }
        
        
        if (textWidth <= maxWidth + 1) { 
            drawInternal(guiGraphics, 0, 0, maxWidth);
            scrollOffset = 0;
            pauseStartTime = -1;
            scrollingForward = true;
            return;
        }

        
        float maxScroll = textWidth - maxWidth;
        
        if (pauseStartTime != -1) {
            long elapsed = System.currentTimeMillis() - pauseStartTime;
            if (elapsed > PAUSE_TIME_MS) {
                pauseStartTime = -1;
            }
        } else {
            if (scrollingForward) {
                scrollOffset += SCROLL_SPEED * dt;
                if (scrollOffset >= maxScroll) {
                    scrollOffset = maxScroll;
                    scrollingForward = false;
                    pauseStartTime = System.currentTimeMillis();
                }
            } else {
                scrollOffset -= SCROLL_SPEED * dt;
                if (scrollOffset <= 0) {
                    scrollOffset = 0;
                    scrollingForward = true;
                    pauseStartTime = System.currentTimeMillis();
                }
            }
        }

        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(-scrollOffset, 0, 0);
        
        
        
        drawInternal(guiGraphics, 0, 0, textWidth); 
        
        guiGraphics.pose().popPose();
    }

    private void drawInternal(GuiGraphics guiGraphics, int x, int y, float containerWidth) {
        if (coloredTexts == null) {
            float drawX = x;
            if (centered && !isCurrentlyScrolling()) {
                drawX += (containerWidth - minecraft.font.width(text)) / 2.0f;
            }
            guiGraphics.drawString(minecraft.font, text, (int)drawX, y, overrideColor != null ? overrideColor : color, true);
        } else {
            float currentX = x;
            if (centered && !isCurrentlyScrolling()) {
                float totalWidth = 0;
                for (ColoredText ct : coloredTexts) totalWidth += minecraft.font.width(ct.text);
                currentX += (containerWidth - totalWidth) / 2.0f;
            }
            for (ColoredText ct : coloredTexts) {
                guiGraphics.drawString(minecraft.font, ct.text, (int)currentX, y, overrideColor != null ? overrideColor : ct.color, true);
                currentX += minecraft.font.width(ct.text);
            }
        }
    }

    private boolean isCurrentlyScrolling() {
        int textWidth = 0;
        if (coloredTexts == null) {
            textWidth = minecraft.font.width(text);
        } else {
            for (ColoredText ct : coloredTexts) textWidth += minecraft.font.width(ct.text);
        }
        float scale = fontSize;
        float scaledWidth = width / scale;
        return textWidth > scaledWidth + 1;
    }

    public int getFinalHeight() {
        if (!autoWrap) return (int)(9 * fontSize);
        
        
        float scale = fontSize;
        float scaledWidth = width / scale;
        
        if (coloredTexts != null) {
            return (int)(getMixedColorWrappedHeight(scaledWidth) * scale);
        }
        
        List<String> lines = wrapText(text, scaledWidth);
        return (int)(lines.size() * 9 * scale);
    }

    private int getMixedColorWrappedHeight(float maxWidth) {
        int lines = 1;
        float currentX = 0;
        for (ColoredText ct : coloredTexts) {
            String remaining = ct.text;
            while (!remaining.isEmpty()) {
                int count = minecraft.font.width(remaining) > (maxWidth - currentX) ? 
                            minecraft.font.plainSubstrByWidth(remaining, (int)(maxWidth - currentX)).length() : 
                            remaining.length();
                if (count == 0 && currentX > 0) {
                    currentX = 0;
                    lines++;
                    continue;
                }
                String part = remaining.substring(0, Math.max(1, count));
                currentX += minecraft.font.width(part);
                remaining = remaining.substring(part.length());
                if (currentX >= maxWidth - 1 && !remaining.isEmpty()) {
                    currentX = 0;
                    lines++;
                }
            }
        }
        return lines * 9;
    }

    public void setText(String text) {
        if (text != null && !text.equals(this.text)) {
            this.text = text;
            this.coloredTexts = null;
        }
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setColoredTexts(List<ColoredText> coloredTexts) {
        if (coloredTexts != null) {
            this.coloredTexts = coloredTexts;
            StringBuilder sb = new StringBuilder();
            for (ColoredText ct : coloredTexts) sb.append(ct.text);
            this.text = sb.toString();
        }
    }

    public void setX1(int x1) {
        this.x1 = x1;
        updateWidthHeight();
    }

    public void setY1(int y1) {
        this.y1 = y1;
        updateWidthHeight();
    }

    public void setX2(int x2) {
        this.x2 = x2;
        updateWidthHeight();
    }

    public void setY2(int y2) {
        this.y2 = y2;
        updateWidthHeight();
    }

    private void updateWidthHeight() {
        this.width = Math.abs(x2 - x1);
        this.height = Math.abs(y2 - y1);
    }
}
