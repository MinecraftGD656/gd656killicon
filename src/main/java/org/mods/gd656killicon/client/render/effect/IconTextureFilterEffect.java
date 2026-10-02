package org.mods.gd656killicon.client.render.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.mods.gd656killicon.client.config.ClientConfigManager;

public final class IconTextureFilterEffect {
    private IconTextureFilterEffect() {
    }

    public static void apply(Identifier textureLocation) {
        if (textureLocation == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getTextureManager() == null) {
            return;
        }
        AbstractTexture texture = minecraft.getTextureManager().getTexture(textureLocation);
        if (texture != null) {
            // TODO(filter): 26.1 AbstractTexture.setFilter 已移除，需改用 GpuSampler
        }
    }
}
