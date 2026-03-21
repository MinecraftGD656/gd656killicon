package org.mods.gd656killicon.client.config;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ValorantStyleCatalog {
    public static final String STYLE_PRIME = ElementTextureDefinition.VALORANT_SKIN_PRIME;
    public static final String STYLE_GLITCHPOP = ElementTextureDefinition.VALORANT_SKIN_GLITCHPOP;
    public static final String STYLE_SINGULARITY_V1 = ElementTextureDefinition.VALORANT_SKIN_SINGULARITY_V1;
    public static final String STYLE_SINGULARITY_V2 = ElementTextureDefinition.VALORANT_SKIN_SINGULARITY_V2;
    public static final String STYLE_SINGULARITY_V3 = ElementTextureDefinition.VALORANT_SKIN_SINGULARITY_V3;
    public static final String STYLE_GAIA = ElementTextureDefinition.VALORANT_SKIN_GAIA;
    public static final String STYLE_GAIA_V1 = ElementTextureDefinition.VALORANT_SKIN_GAIA_V1;
    public static final String STYLE_GAIA_V2 = ElementTextureDefinition.VALORANT_SKIN_GAIA_V2;
    public static final String STYLE_GAIA_V3 = ElementTextureDefinition.VALORANT_SKIN_GAIA_V3;
    public static final String STYLE_BUBBLEGUM_DEATHWISH = "bubblegum_deathwish";
    public static final String STYLE_BUBBLEGUM_DEATHWISH_V1 = "bubblegum_deathwish_v1";
    public static final String STYLE_BUBBLEGUM_DEATHWISH_V2 = "bubblegum_deathwish_v2";
    public static final String STYLE_BUBBLEGUM_DEATHWISH_V3 = "bubblegum_deathwish_v3";
    public static final String STYLE_CHAMPIONS_2021 = "champions_2021";
    public static final String STYLE_PRELUDE_TO_CHAOS_V1 = "prelude_to_chaos_v1";
    public static final String STYLE_PRELUDE_TO_CHAOS_V2 = "prelude_to_chaos_v2";
    public static final String STYLE_PRELUDE_TO_CHAOS_V3 = "prelude_to_chaos_v3";
    public static final String STYLE_PRIMORDIUM = "primordium";
    public static final String STYLE_PRIMORDIUM_V1 = "primordium_v1";
    public static final String STYLE_PRIMORDIUM_V2 = "primordium_v2";
    public static final String STYLE_PRIMORDIUM_V3 = "primordium_v3";
    public static final String STYLE_RADIANT_CRISIS_001 = "radiant_crisis_001";
    public static final String STYLE_RGX_11Z_PRO = "rgx_11z_pro";
    public static final String STYLE_RGX_11Z_PRO_V1 = "rgx_11z_pro_v1";
    public static final String STYLE_RGX_11Z_PRO_V2 = "rgx_11z_pro_v2";
    public static final String STYLE_RGX_11Z_PRO_V3 = "rgx_11z_pro_v3";

    public static final String TEXTURE_ICON = "icon";
    public static final String TEXTURE_FRAME = "frame";
    public static final String TEXTURE_RING = "ring";
    public static final String TEXTURE_BLADE = "blade";
    public static final String TEXTURE_BAR = "bar";
    public static final String TEXTURE_HEADSHOT = "headshot";
    public static final String TEXTURE_BASE_PARTICLE = "base_particle";
    public static final String TEXTURE_HERO_FLAME = "hero_flame";
    public static final String TEXTURE_LARGE_SPARKS = "large_sparks";
    public static final String TEXTURE_X_SPARKS = "x_sparks";

    public record StyleSpec(
        String presetId,
        String styleId,
        String displayName,
        int accentColor,
        String comboSoundPrefix,
        int headshotOffsetX,
        int headshotOffsetY,
        boolean emblemOnlyEntryMotion,
        String frameBladeTexture,
        Map<String, String> textures
    ) {
        public String texture(String textureKey) {
            return textures.get(textureKey);
        }
    }

    private static final Map<String, StyleSpec> STYLE_SPECS = new LinkedHashMap<>();
    private static final Map<String, StyleSpec> PRESET_SPECS = new LinkedHashMap<>();

    static {
        register(style(
            "00009",
            STYLE_PRIME,
            "Prime",
            0x908CCD,
            "valorant_primekill_",
            0,
            -16,
            false,
            null,
            "killicon_valorant_prime_emblem.png",
            "killicon_valorant_prime_frame.png",
            "killicon_valorant_bar.png"
        ));
        register(style(
            "00010",
            STYLE_GLITCHPOP,
            "Glitchpop",
            0x2697F5,
            "valorant_glitchpopkill_",
            0,
            -16,
            false,
            null,
            "killicon_valorant_glitchpop_emblem.png",
            "killicon_valorant_glitchpop_frame.png",
            "killicon_valorant_glitchpop_bar.png"
        ));
        register(style(
            "00011",
            STYLE_SINGULARITY_V1,
            "Singularity V1",
            0xDF7E49,
            "valorant_singularitykill_",
            0,
            -10,
            false,
            null,
            "killicon_valorant_singularity_v1_emblem.png",
            "killicon_valorant_base_frame.png",
            "killicon_valorant_singularity_v1_bar.png"
        ));
        register(style(
            "00012",
            STYLE_SINGULARITY_V2,
            "Singularity V2",
            0xDCC971,
            "valorant_singularitykill_",
            0,
            -10,
            false,
            null,
            "killicon_valorant_singularity_v2_emblem.png",
            "killicon_valorant_base_frame.png",
            "killicon_valorant_singularity_v2_bar.png"
        ));
        register(style(
            "00013",
            STYLE_SINGULARITY_V3,
            "Singularity V3",
            0x7E9EDC,
            "valorant_singularitykill_",
            0,
            -10,
            false,
            null,
            "killicon_valorant_singularity_v3_emblem.png",
            "killicon_valorant_base_frame.png",
            "killicon_valorant_singularity_v3_bar.png"
        ));
        register(style(
            "00014",
            STYLE_GAIA,
            "Gaia's Vengeance",
            0xF9545E,
            "valorant_gaiavengeancekill_",
            -2,
            -20,
            false,
            null,
            "killicon_valorant_gaia_emblem.png",
            "killicon_valorant_gaia_frame.png",
            "killicon_valorant_gaia_bar.png"
        ));
        register(style(
            "00015",
            STYLE_GAIA_V1,
            "Gaia's Vengeance V1",
            0x287EF3,
            "valorant_gaiavengeancekill_",
            -2,
            -20,
            false,
            null,
            "killicon_valorant_gaia_v1_emblem.png",
            "killicon_valorant_gaia_v1_frame.png",
            "killicon_valorant_gaia_v1_bar.png"
        ));
        register(style(
            "00016",
            STYLE_GAIA_V2,
            "Gaia's Vengeance V2",
            0x27B748,
            "valorant_gaiavengeancekill_",
            -2,
            -20,
            false,
            null,
            "killicon_valorant_gaia_v2_emblem.png",
            "killicon_valorant_gaia_v2_frame.png",
            "killicon_valorant_gaia_v2_bar.png"
        ));
        register(style(
            "00017",
            STYLE_GAIA_V3,
            "Gaia's Vengeance V3",
            0xF77124,
            "valorant_gaiavengeancekill_",
            -2,
            -20,
            false,
            null,
            "killicon_valorant_gaia_v3_emblem.png",
            "killicon_valorant_gaia_v3_frame.png",
            "killicon_valorant_gaia_v3_bar.png"
        ));
        register(style(
            "00018",
            STYLE_BUBBLEGUM_DEATHWISH,
            "Bubblegum Deathwish",
            0xC94FB9,
            "valorant_bubblegumdeathwishkill_",
            0,
            -12,
            true,
            "killicon_valorant_bubblegum_deathwish_blade.png",
            "killicon_valorant_bubblegum_deathwish_emblem.png",
            "killicon_valorant_bubblegum_deathwish_frame.png",
            "killicon_valorant_bubblegum_deathwish_bar.png"
        ));
        register(style(
            "00019",
            STYLE_BUBBLEGUM_DEATHWISH_V1,
            "Bubblegum Deathwish V1",
            0xC98E4C,
            "valorant_bubblegumdeathwishkill_",
            0,
            -12,
            true,
            "killicon_valorant_bubblegum_deathwish_blade.png",
            "killicon_valorant_bubblegum_deathwish_v3_emblem.png",
            "killicon_valorant_bubblegum_deathwish_frame.png",
            "killicon_valorant_bubblegum_deathwish_v1_bar.png"
        ));
        register(style(
            "00020",
            STYLE_BUBBLEGUM_DEATHWISH_V2,
            "Bubblegum Deathwish V2",
            0x9D332F,
            "valorant_bubblegumdeathwishkill_",
            0,
            -12,
            true,
            "killicon_valorant_bubblegum_deathwish_blade.png",
            "killicon_valorant_bubblegum_deathwish_v2_emblem.png",
            "killicon_valorant_bubblegum_deathwish_frame.png",
            "killicon_valorant_bubblegum_deathwish_v2_bar.png"
        ));
        register(style(
            "00021",
            STYLE_BUBBLEGUM_DEATHWISH_V3,
            "Bubblegum Deathwish V3",
            0x6EB037,
            "valorant_bubblegumdeathwishkill_",
            0,
            -12,
            true,
            "killicon_valorant_bubblegum_deathwish_blade.png",
            "killicon_valorant_bubblegum_deathwish_v1_emblem.png",
            "killicon_valorant_bubblegum_deathwish_frame.png",
            "killicon_valorant_bubblegum_deathwish_v3_bar.png"
        ));
        register(style(
            "00022",
            STYLE_CHAMPIONS_2021,
            "Champions 2021",
            0x947046,
            "valorant_champions2021kill_",
            0,
            -12,
            false,
            null,
            "killicon_valorant_champions_2021_emblem.png",
            "killicon_valorant_base_frame.png",
            "killicon_valorant_champions_2021_bar.png"
        ));
        register(style(
            "00023",
            STYLE_PRELUDE_TO_CHAOS_V1,
            "Prelude to Chaos V1",
            0xF46E57,
            "valorant_preludetochaoskill_",
            0,
            -12,
            false,
            null,
            "killicon_valorant_prelude_to_chaos_v1_emblem.png",
            "killicon_valorant_base_frame.png",
            "killicon_valorant_prelude_to_chaos_v1_bar.png"
        ));
        register(style(
            "00024",
            STYLE_PRELUDE_TO_CHAOS_V2,
            "Prelude to Chaos V2",
            0x10C110,
            "valorant_preludetochaoskill_",
            0,
            -12,
            false,
            null,
            "killicon_valorant_prelude_to_chaos_v2_emblem.png",
            "killicon_valorant_base_frame.png",
            "killicon_valorant_prelude_to_chaos_v2_bar.png"
        ));
        register(style(
            "00025",
            STYLE_PRELUDE_TO_CHAOS_V3,
            "Prelude to Chaos V3",
            0x1168C1,
            "valorant_preludetochaoskill_",
            0,
            -12,
            false,
            null,
            "killicon_valorant_prelude_to_chaos_v3_emblem.png",
            "killicon_valorant_base_frame.png",
            "killicon_valorant_prelude_to_chaos_v3_bar.png"
        ));
        register(style(
            "00026",
            STYLE_PRIMORDIUM,
            "Primordium",
            0x8F3E31,
            "valorant_primordiumkill_",
            0,
            -14,
            false,
            null,
            "killicon_valorant_primordium_emblem.png",
            "killicon_valorant_primordium_frame.png",
            "killicon_valorant_primordium_bar.png"
        ));
        register(style(
            "00027",
            STYLE_PRIMORDIUM_V1,
            "Primordium V1",
            0x387A51,
            "valorant_primordiumkill_",
            0,
            -14,
            false,
            null,
            "killicon_valorant_primordium_v1_emblem.png",
            "killicon_valorant_primordium_frame.png",
            "killicon_valorant_primordium_v1_bar.png"
        ));
        register(style(
            "00028",
            STYLE_PRIMORDIUM_V2,
            "Primordium V2",
            0x316884,
            "valorant_primordiumkill_",
            0,
            -14,
            false,
            null,
            "killicon_valorant_primordium_v2_emblem.png",
            "killicon_valorant_primordium_frame.png",
            "killicon_valorant_primordium_v2_bar.png"
        ));
        register(style(
            "00029",
            STYLE_PRIMORDIUM_V3,
            "Primordium V3",
            0x8D6F43,
            "valorant_primordiumkill_",
            0,
            -14,
            false,
            null,
            "killicon_valorant_primordium_v3_emblem.png",
            "killicon_valorant_primordium_frame.png",
            "killicon_valorant_primordium_v3_bar.png"
        ));
        register(style(
            "00030",
            STYLE_RADIANT_CRISIS_001,
            "Radiant Crisis 001",
            0x73C0C4,
            "valorant_radiantcrisis001kill_",
            0,
            -12,
            false,
            null,
            "killicon_valorant_radiant_crisis_001_emblem.png",
            "killicon_valorant_base_frame.png",
            "killicon_valorant_radiant_crisis_001_bar.png"
        ));
        register(style(
            "00031",
            STYLE_RGX_11Z_PRO,
            "RGX 11z Pro",
            0xC1F341,
            "valorant_rgx11zprokill_",
            0,
            -12,
            false,
            null,
            "killicon_valorant_rgx_11z_pro_emblem.png",
            "killicon_valorant_rgx_11z_pro_frame.png",
            "killicon_valorant_rgx_11z_pro_bar.png"
        ));
        register(style(
            "00032",
            STYLE_RGX_11Z_PRO_V1,
            "RGX 11z Pro V1",
            0xF3414A,
            "valorant_rgx11zprokill_",
            0,
            -12,
            false,
            null,
            "killicon_valorant_rgx_11z_pro_v1_emblem.png",
            "killicon_valorant_rgx_11z_pro_frame.png",
            "killicon_valorant_rgx_11z_pro_v1_bar.png"
        ));
        register(style(
            "00033",
            STYLE_RGX_11Z_PRO_V2,
            "RGX 11z Pro V2",
            0x41BAF3,
            "valorant_rgx11zprokill_",
            0,
            -12,
            false,
            null,
            "killicon_valorant_rgx_11z_pro_v2_emblem.png",
            "killicon_valorant_rgx_11z_pro_frame.png",
            "killicon_valorant_rgx_11z_pro_v2_bar.png"
        ));
        register(style(
            "00034",
            STYLE_RGX_11Z_PRO_V3,
            "RGX 11z Pro V3",
            0xF3A741,
            "valorant_rgx11zprokill_",
            0,
            -12,
            false,
            null,
            "killicon_valorant_rgx_11z_pro_v3_emblem.png",
            "killicon_valorant_rgx_11z_pro_frame.png",
            "killicon_valorant_rgx_11z_pro_v3_bar.png"
        ));
    }

    private ValorantStyleCatalog() {
    }

    private static StyleSpec style(
        String presetId,
        String styleId,
        String displayName,
        int accentColor,
        String comboSoundPrefix,
        int headshotOffsetX,
        int headshotOffsetY,
        boolean emblemOnlyEntryMotion,
        String frameBladeTexture,
        String emblemTexture,
        String frameTexture,
        String barTexture
    ) {
        Map<String, String> textures = new LinkedHashMap<>();
        textures.put(TEXTURE_ICON, emblemTexture);
        textures.put(TEXTURE_FRAME, frameTexture);
        textures.put(TEXTURE_RING, getDefaultRingTexture(styleId));
        if (frameBladeTexture != null && !frameBladeTexture.isBlank()) {
            textures.put(TEXTURE_BLADE, frameBladeTexture);
        }
        textures.put(TEXTURE_BAR, barTexture);
        textures.put(TEXTURE_HEADSHOT, "killicon_valorant_headshot.png");
        textures.put(TEXTURE_BASE_PARTICLE, "killicon_valorant_particle_base_t1.png");
        textures.put(TEXTURE_HERO_FLAME, "killicon_valorant_particle_hero_flame.png");
        textures.put(TEXTURE_LARGE_SPARKS, "killicon_valorant_particle_large_sparks.png");
        textures.put(TEXTURE_X_SPARKS, "killicon_valorant_particle_x_sparks.png");
        return new StyleSpec(
            presetId,
            styleId,
            displayName,
            accentColor,
            comboSoundPrefix,
            headshotOffsetX,
            headshotOffsetY,
            emblemOnlyEntryMotion,
            frameBladeTexture,
            Map.copyOf(textures)
        );
    }

    private static void register(StyleSpec spec) {
        STYLE_SPECS.put(spec.styleId(), spec);
        PRESET_SPECS.put(spec.presetId(), spec);
    }

    public static StyleSpec getStyleSpec(String styleId) {
        return STYLE_SPECS.get(normalizeStyleId(styleId));
    }

    public static StyleSpec getPresetSpec(String presetId) {
        return PRESET_SPECS.get(presetId);
    }

    public static List<StyleSpec> getOfficialPresetSpecs() {
        return new ArrayList<>(PRESET_SPECS.values());
    }

    public static List<StyleSpec> getDefinitions() {
        return getOfficialPresetSpecs();
    }

    public static StyleSpec getByPresetId(String presetId) {
        return getPresetSpec(presetId);
    }

    public static StyleSpec getByStyleId(String styleId) {
        return getStyleSpec(styleId);
    }

    public static String getDefaultStyleForPreset(String presetId) {
        StyleSpec spec = getPresetSpec(presetId);
        return spec != null ? spec.styleId() : STYLE_PRIME;
    }

    public static String getTextureFileName(String styleId, String textureKey) {
        StyleSpec spec = getStyleSpec(styleId);
        if (spec == null || textureKey == null) {
            return null;
        }
        return spec.texture(normalizeTextureKey(textureKey));
    }

    public static String getDisplayName(String styleId) {
        StyleSpec spec = getStyleSpec(styleId);
        return spec != null ? spec.displayName() : "Prime";
    }

    public static boolean isBuiltInStyle(String styleId) {
        return STYLE_SPECS.containsKey(normalizeStyleId(styleId));
    }

    public static List<String> getBuiltInStyleIds() {
        return new ArrayList<>(STYLE_SPECS.keySet());
    }

    public static int getAccentColor(String styleId) {
        StyleSpec spec = getStyleSpec(styleId);
        return spec != null ? spec.accentColor() : 0x908CCD;
    }

    public static int getHeadshotOffsetX(String styleId) {
        return 0;
    }

    public static int getHeadshotOffsetY(String styleId) {
        return 0;
    }

    public static String getFrameBladeTextureFileName(String styleId) {
        if (!usesBlade(styleId)) {
            return null;
        }
        StyleSpec spec = getStyleSpec(styleId);
        if (spec == null) {
            return null;
        }
        String texture = spec.texture(TEXTURE_BLADE);
        return texture != null ? texture : spec.frameBladeTexture();
    }

    public static boolean isBubblegumDeathwishStyle(String styleId) {
        return STYLE_BUBBLEGUM_DEATHWISH.equals(styleId)
            || STYLE_BUBBLEGUM_DEATHWISH_V1.equals(styleId)
            || STYLE_BUBBLEGUM_DEATHWISH_V2.equals(styleId)
            || STYLE_BUBBLEGUM_DEATHWISH_V3.equals(styleId);
    }

    public static boolean usesBlade(String styleId) {
        return isBubblegumDeathwishStyle(styleId);
    }

    public static boolean useEmblemOnlyEntryMotion(String styleId) {
        StyleSpec spec = getStyleSpec(styleId);
        return spec != null && spec.emblemOnlyEntryMotion();
    }

    public static String normalizeStyleId(String styleId) {
        if (styleId == null || styleId.isBlank()) {
            return STYLE_PRIME;
        }
        String normalized = styleId.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "prime" -> STYLE_PRIME;
            case "glitchpop" -> STYLE_GLITCHPOP;
            case "singularity", "singularity_v1" -> STYLE_SINGULARITY_V1;
            case "singularity_v2" -> STYLE_SINGULARITY_V2;
            case "singularity_v3" -> STYLE_SINGULARITY_V3;
            case "gaia", "gaia_vengeance" -> STYLE_GAIA;
            case "gaia_v1", "gaia_vengeance_v1" -> STYLE_GAIA_V1;
            case "gaia_v2", "gaia_vengeance_v2" -> STYLE_GAIA_V2;
            case "gaia_v3", "gaia_vengeance_v3" -> STYLE_GAIA_V3;
            default -> STYLE_SPECS.containsKey(normalized) ? normalized : STYLE_PRIME;
        };
    }

    public static String resolveStyleId(String presetId, JsonObject config) {
        return getDefaultStyleForPreset(presetId);
    }

    public static String getOfficialTextureFileName(String presetId, String textureKey) {
        StyleSpec spec = getPresetSpec(presetId);
        if (spec == null) {
            spec = getPresetSpec("00009");
        }
        return spec != null ? spec.texture(normalizeTextureKey(textureKey)) : null;
    }

    public static String getOfficialTextureFileNameForStyle(String styleId, String textureKey) {
        StyleSpec spec = getStyleSpec(styleId);
        if (spec == null) {
            spec = getStyleSpec(STYLE_PRIME);
        }
        return spec != null ? spec.texture(normalizeTextureKey(textureKey)) : null;
    }

    public static String getComboSoundBaseName(String presetId, JsonObject config, int comboTier) {
        StyleSpec spec = getStyleSpec(resolveStyleId(presetId, config));
        int tier = Math.max(1, Math.min(comboTier, 5));
        String prefix = spec != null ? spec.comboSoundPrefix() : "valorant_primekill_";
        return prefix + tier;
    }

    public static String getDefaultParticleColorHex(String presetId, JsonObject config) {
        StyleSpec spec = getStyleSpec(resolveStyleId(presetId, config));
        int color = spec != null ? spec.accentColor() : 0x908CCD;
        return String.format("#%06X", color & 0xFFFFFF);
    }

    private static String getDefaultRingTexture(String styleId) {
        String normalized = normalizeStyleId(styleId);
        return switch (normalized) {
            case STYLE_BUBBLEGUM_DEATHWISH,
                 STYLE_BUBBLEGUM_DEATHWISH_V1,
                 STYLE_BUBBLEGUM_DEATHWISH_V2,
                 STYLE_BUBBLEGUM_DEATHWISH_V3 -> "killicon_valorant_bubblegum_deathwish_ring.png";
            case STYLE_GLITCHPOP -> "killicon_valorant_glitchpop_ring.png";
            case STYLE_RGX_11Z_PRO,
                 STYLE_RGX_11Z_PRO_V1,
                 STYLE_RGX_11Z_PRO_V2,
                 STYLE_RGX_11Z_PRO_V3 -> "killicon_valorant_rgx_11z_pro_ring.png";
            default -> "killicon_valorant_base_ring.png";
        };
    }

    private static String normalizeTextureKey(String textureKey) {
        if ("emblem".equals(textureKey)) {
            return TEXTURE_ICON;
        }
        return textureKey;
    }
}
