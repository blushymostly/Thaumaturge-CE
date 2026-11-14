package com.leclowndu93150.thaumaturge.registry;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaHandler;
import com.leclowndu93150.thaumaturge.content.essentia.EssentiaTank;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TTCapabilities {
    public static final ResourceLocation ESSENTIA_CAP = TTIds.rl("essentia");

    public static final Capability<IEssentiaHandler> ESSENTIA = CapabilityManager.get(new CapabilityToken<>() {});

    private TTCapabilities() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(TTCapabilities::onRegister);
    }

    private static void onRegister(RegisterCapabilitiesEvent event) {
        event.register(IEssentiaHandler.class);
    }

    @SubscribeEvent
    public static void attachBlockEntity(AttachCapabilitiesEvent<BlockEntity> event) {
        // Block entities that need essentia expose it themselves; this hook is reserved for wrappers.
    }

    public static ICapabilityProvider essentiaProvider(EssentiaTank tank) {
        return new EssentiaCapabilityProvider(tank);
    }

    private static final class EssentiaCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final EssentiaTank tank;
        private final LazyOptional<IEssentiaHandler> optional;

        private EssentiaCapabilityProvider(EssentiaTank tank) {
            this.tank = tank;
            this.optional = LazyOptional.of(() -> tank);
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == ESSENTIA ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return tank.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            tank.load(nbt);
        }
    }
}
