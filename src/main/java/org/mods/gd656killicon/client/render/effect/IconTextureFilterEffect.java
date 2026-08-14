package org.mods.gd656killicon.client.render.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.mods.gd656killicon.client.config.ClientConfigManager;

public final class IconTextureFilterEffect {
    private IconTextureFilterEffect() {
    }

    public static void apply(ResourceLocation textureLocation) {
        if (textureLocation == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getTextureManager() == null) {
            return;
        }
        AbstractTexture texture = minecraft.getTextureManager().getTexture(textureLocation);
        if (texture != null) {
            texture.setFilter(ClientConfigManager.isEnableIconAntialiasing(), false);
        }
    }
}
