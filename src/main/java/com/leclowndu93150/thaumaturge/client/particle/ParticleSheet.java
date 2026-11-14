package com.leclowndu93150.thaumaturge.client.particle;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.Thaumaturge;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

/**
 * A horizontal strip of square frames at {@code textures/particle/<name>.png}, drawn additively (CE particle sheet).
 * Frame count is {@code width / height}, read from the PNG header.
 */
public final class ParticleSheet {
    private static final Map<String, ParticleSheet> SHEETS = new ConcurrentHashMap<>();
    private static final int PNG_DIMENSION_OFFSET = 16;

    private final ResourceLocation texture;
    private final ParticleRenderType renderType;
    private volatile int frames;

    private ParticleSheet(String name) {
        this.texture = TTIds.rl("textures/particle/" + name + ".png");
        this.renderType = new AdditiveType(texture);
    }

    public static ParticleSheet sheet(String name) {
        return SHEETS.computeIfAbsent(name, ParticleSheet::new);
    }

    public ParticleRenderType renderType() {
        return renderType;
    }

    public int frames() {
        int resolved = frames;
        if (resolved == 0) {
            resolved = readFrameCount();
            if (resolved > 0) {
                frames = resolved;
            } else {
                return 1;
            }
        }
        return resolved;
    }

    public float u0(int frame) {
        return (float) frame / frames();
    }

    public float u1(int frame) {
        return (float) (frame + 1) / frames();
    }

    private int readFrameCount() {
        try (InputStream stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(texture).open(); DataInputStream data = new DataInputStream(stream)) {
            data.skipBytes(PNG_DIMENSION_OFFSET);
            int width = data.readInt();
            int height = data.readInt();
            return Math.max(1, width / Math.max(1, height));
        } catch (IOException e) {
            Thaumaturge.LOGGER.error("Cannot read particle sheet {}", texture, e);
            return -1;
        }
    }

    private record AdditiveType(ResourceLocation texture) implements ParticleRenderType {
        @Override
        public void begin(BufferBuilder builder, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, texture);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "thaumaturge:additive_sheet[" + texture + "]";
        }
    }
}
