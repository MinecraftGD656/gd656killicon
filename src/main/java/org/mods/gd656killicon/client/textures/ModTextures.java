package org.mods.gd656killicon.client.textures;

import net.minecraft.resources.ResourceLocation;

public class ModTextures {
    public static ResourceLocation get(String path) {
        return ExternalTextureManager.getTexture(path);
    }
}
