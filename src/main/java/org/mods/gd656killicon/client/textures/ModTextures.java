package org.mods.gd656killicon.client.textures;

import net.minecraft.resources.Identifier;

public class ModTextures {
    public static Identifier get(String path) {
        return ExternalTextureManager.getTexture(path);
    }
}
