package com.leclowndu93150.thaumaturge.client.render.crystal;

import com.leclowndu93150.thaumaturge.client.model.mesh.TTMesh;
import com.leclowndu93150.thaumaturge.content.world.crystal.BlockCrystal;
import com.leclowndu93150.thaumaturge.content.world.crystal.CrystalFaceTransforms;
import com.leclowndu93150.thaumaturge.content.world.crystal.CrystalModelData;
import com.leclowndu93150.thaumaturge.content.world.crystal.CrystalShards;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CrystalBakedModel implements IDynamicBakedModel {
    private static final Direction[] FACES = Direction.values();

    private final List<BakedQuad>[][] shards;
    private final TextureAtlasSprite particle;
    private final ItemTransforms transforms;
    private final ItemOverrides overrides;
    private final boolean ao;
    private final boolean gui3d;
    private final boolean blockLight;

    public CrystalBakedModel(TTMesh mesh, TextureAtlasSprite particle, ItemTransforms transforms, ItemOverrides overrides, boolean ao, boolean gui3d, boolean blockLight) {
        this.particle = particle;
        this.transforms = transforms;
        this.overrides = overrides;
        this.ao = ao;
        this.gui3d = gui3d;
        this.blockLight = blockLight;
        @SuppressWarnings("unchecked")
        List<BakedQuad>[][] built = new List[FACES.length][CrystalShards.COUNT];
        this.shards = built;
        for (Direction face : FACES) {
            for (int shard = 0; shard < CrystalShards.COUNT; shard++) {
                List<BakedQuad> quads = new ArrayList<>();
                CrystalQuadBaker.bakePart(mesh.parts().get(shard), particle, CrystalFaceTransforms.forFace(face), quads);
                shards[face.ordinal()][shard] = List.copyOf(quads);
            }
        }
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        if (side != null) {
            return Collections.emptyList();
        }
        int growth = state != null && state.hasProperty(BlockCrystal.SIZE) ? state.getValue(BlockCrystal.SIZE) : 0;
        Integer liveMask = data.get(CrystalModelData.SUPPORT_MASK);
        int supportMask = liveMask != null
                ? liveMask
                : (state != null && state.hasProperty(BlockCrystal.SUPPORT) ? state.getValue(BlockCrystal.SUPPORT) : 0);
        Long seedValue = data.get(CrystalModelData.SEED);
        long seed = seedValue != null ? seedValue : (state != null ? CrystalShards.seed(state, BlockPos.ZERO) : 0L);

        List<BakedQuad> out = new ArrayList<>();
        boolean any = false;
        for (Direction face : FACES) {
            if ((supportMask & 1 << face.ordinal()) == 0) {
                continue;
            }
            any = true;
            int[] order = CrystalShards.order(face, seed);
            for (int i = 0; i <= growth; i++) {
                out.addAll(shards[face.ordinal()][order[i]]);
            }
        }
        if (!any) {
            out.addAll(shards[Direction.DOWN.ordinal()][CrystalShards.unsupported(seed)]);
        }
        return out;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        return ModelData.builder()
                .with(CrystalModelData.SUPPORT_MASK, CrystalShards.supportMask(level, pos))
                .with(CrystalModelData.SEED, CrystalShards.seed(state, pos))
                .build();
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutout());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
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
