package com.leclowndu93150.thaumaturge.client.model.mesh;

import com.mojang.math.Transformation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class TTMeshGeometry implements IUnbakedGeometry<TTMeshGeometry> {
    private static final float CENTER_OFFSET = 0.5F;

    private final ResourceLocation model;
    private final boolean flipV;
    private final boolean cornerSpace;
    private final boolean worldLight;
    private final String nodeDevice;

    public TTMeshGeometry(ResourceLocation model, boolean flipV, boolean cornerSpace, boolean worldLight, String nodeDevice) {
        this.model = model;
        this.flipV = flipV;
        this.cornerSpace = cornerSpace;
        this.worldLight = worldLight;
        this.nodeDevice = nodeDevice;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides,
            ResourceLocation modelLocation) {
        TTMesh mesh = loadMesh();
        Transformation root = context.getRootTransform();
        Matrix4f transform = new Matrix4f().translate(CENTER_OFFSET, CENTER_OFFSET, CENTER_OFFSET);
        transform.mul(modelState.getRotation().getMatrix());
        if (!root.isIdentity()) {
            transform.mul(root.getMatrix());
        }
        if (cornerSpace) {
            transform.translate(-CENTER_OFFSET, -CENTER_OFFSET, -CENTER_OFFSET);
        }
        if (!nodeDevice.isEmpty()) {
            boolean transducer = nodeDevice.equals("transducer");
            transform.translate(0, transducer ? 0.5F : -0.5F, 0)
                    .rotateX((float) (transducer ? Math.PI / 2 : -Math.PI / 2));
        }

        List<BakedQuad> unculled = new ArrayList<>();
        TextureAtlasSprite particle = null;
        for (TTMeshPart part : mesh.parts()) {
            String slot = part.materialSlot() == null || part.materialSlot().isEmpty() ? "texture" : part.materialSlot();
            if (!context.hasMaterial(slot) && context.hasMaterial("particle")) {
                slot = "particle";
            }
            if (!context.hasMaterial(slot)) {
                continue;
            }
            Material material = context.getMaterial(slot);
            TextureAtlasSprite sprite = spriteGetter.apply(material);
            if (particle == null) {
                particle = sprite;
            }
            if (!nodeDevice.isEmpty() && part.name().equals("piston")) {
                for (int arm = 0; arm < 4; arm++) {
                    Matrix4f armTransform = new Matrix4f(transform)
                            .rotateZ((float) (arm * Math.PI / 2)).rotateX((float) (Math.PI / 4));
                    TTMeshQuadBaker.bakePart(part, sprite, armTransform, flipV, worldLight, unculled);
                }
            } else {
                TTMeshQuadBaker.bakePart(part, sprite, transform, flipV, worldLight, unculled);
            }
        }
        if (particle == null && context.hasMaterial("particle")) {
            particle = spriteGetter.apply(context.getMaterial("particle"));
        }
        if (particle == null) {
            particle = spriteGetter.apply(new Material(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("minecraft", "block/stone")));
        }

        ItemTransforms transforms = context.getTransforms();
        boolean ao = context.useAmbientOcclusion();
        boolean gui3d = context.isGui3d();
        boolean blockLight = context.useBlockLight();
        var hint = context.getRenderTypeHint();
        var group = hint == null ? net.minecraftforge.client.RenderTypeGroup.EMPTY : context.getRenderType(hint);
        var renderTypes = group.isEmpty() ? ChunkRenderTypeSet.of(RenderType.cutout(), RenderType.translucent())
                : ChunkRenderTypeSet.of(group.block());
        return new Baked(unculled, particle, transforms, overrides, ao, gui3d, blockLight, renderTypes);
    }

    private TTMesh loadMesh() {
        try {
            return TTMeshLoader.load(Minecraft.getInstance().getResourceManager(), model);
        } catch (IOException e) {
            throw new RuntimeException("Could not load mesh model at " + model, e);
        }
    }

    private static final class Baked implements BakedModel {
        private final List<BakedQuad> quads;
        private final TextureAtlasSprite particle;
        private final ItemTransforms transforms;
        private final ItemOverrides overrides;
        private final boolean ao;
        private final boolean gui3d;
        private final boolean blockLight;
        private final ChunkRenderTypeSet renderTypes;
        private final Map<Direction, List<BakedQuad>> bySide = new EnumMap<>(Direction.class);

        private Baked(List<BakedQuad> quads, TextureAtlasSprite particle, ItemTransforms transforms, ItemOverrides overrides, boolean ao, boolean gui3d, boolean blockLight, ChunkRenderTypeSet renderTypes) {
            this.quads = List.copyOf(quads);
            this.particle = particle;
            this.transforms = transforms;
            this.overrides = overrides;
            this.ao = ao;
            this.gui3d = gui3d;
            this.blockLight = blockLight;
            this.renderTypes = renderTypes;
            for (Direction dir : Direction.values()) {
                bySide.put(dir, Collections.emptyList());
            }
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return side == null ? quads : bySide.getOrDefault(side, Collections.emptyList());
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
            return getQuads(state, side, rand);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
            return renderTypes;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return ao;
        }

        @Override
        public boolean isGui3d() {
            return gui3d;
        }

        @Override
        public boolean usesBlockLight() {
            return blockLight;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return particle;
        }

        @Override
        public ItemTransforms getTransforms() {
            return transforms;
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }
}
