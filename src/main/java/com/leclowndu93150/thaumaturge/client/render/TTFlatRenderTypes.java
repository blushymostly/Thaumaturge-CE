package com.leclowndu93150.thaumaturge.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Forge stand-in for CE {@code TTFlatRenderTypes} / FX depth policy:
 * <ul>
 *   <li>GUI flat: translucent, no special depth (overlay quads).</li>
 *   <li>World nodes: depth-<em>tested</em>, depth-<em>write off</em> (CE {@code TEST_NO_WRITE}) —
 *       solids occlude nodes; water stays correct because nodes do not pollute the depth buffer.</li>
 * </ul>
 */
public final class TTFlatRenderTypes extends RenderStateShard {
    /** GUI / overlay quads that must not fight glyph depth. */
    private static final Function<ResourceLocation, RenderType> TRANSLUCENT_FLAT = Util.memoize(texture -> {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(true);
        return RenderType.create("thaumaturge_translucent_flat", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, state);
    });

    /**
     * World node billboards: LEQUAL depth test + color-only write (CE FX translucent).
     * Never use NO_DEPTH_TEST (wall X-ray) or COLOR_DEPTH_WRITE (water punch-through).
     */
    private static final Function<ResourceLocation, RenderType> TRANSLUCENT_FLAT_WORLD = Util.memoize(texture -> {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(true);
        return RenderType.create("thaumaturge_translucent_flat_world", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, state);
    });

    private static final Function<ResourceLocation, RenderType> ADDITIVE_WORLD = Util.memoize(texture -> {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create("thaumaturge_additive_world", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, false, state);
    });

    public static RenderType entityAdditiveWorld(ResourceLocation texture) {
        return ADDITIVE_WORLD.apply(texture);
    }

    private TTFlatRenderTypes() {
        super("thaumaturge_flat_access", () -> {}, () -> {});
        throw new AssertionError();
    }

    public static RenderType entityTranslucentFlat(ResourceLocation texture) {
        return TRANSLUCENT_FLAT.apply(texture);
    }

    public static RenderType entityTranslucentFlatWorld(ResourceLocation texture) {
        return TRANSLUCENT_FLAT_WORLD.apply(texture);
    }

    /** @deprecated X-ray path removed — always depth-tested color-only write. */
    @Deprecated
    public static RenderType entityTranslucentFlatNoDepth(ResourceLocation texture) {
        return entityTranslucentFlatWorld(texture);
    }
}
