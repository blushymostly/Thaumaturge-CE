package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.recipe.DustTrigger;
import com.leclowndu93150.thaumaturge.api.recipe.DustTriggerInput;
import com.leclowndu93150.thaumaturge.api.recipe.DustTriggerPlacement;
import com.leclowndu93150.thaumaturge.content.recipe.dust.DustTriggerFx;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTRecipeTypes;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Salis Mundus: right-click a valid block to trigger in-world dust transformations. */
@Mod.EventBusSubscriber(modid = TTIds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SalisMundusItem extends Item {
    private static final float SOUND_VOLUME = 0.33F;
    private static final float SOUND_PITCH_BASE = 1.0F;
    private static final float SOUND_PITCH_DEVIATION = 0.05F;

    public SalisMundusItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * Crafting tables (and other interactive blocks) eat the click before {@link #useOn}.
     * Run the trigger first via {@link #onItemUseFirst}; deny the block when we matched.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        InteractionResult result = tryTrigger(context);
        return result == InteractionResult.PASS ? InteractionResult.PASS : result;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return tryTrigger(context);
    }

    /**
     * Crafting tables otherwise open their GUI and swallow the click before {@link #useOn}.
     * Only deny the block when a dust trigger matches — otherwise crucibles (and similar) keep RMB.
     */
    @SubscribeEvent
    public static void preferDustOverBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(TTItems.SALIS_MUNDUS.get())) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        DustTriggerInput input = new DustTriggerInput(event.getItemStack(), level, pos, level.getBlockState(pos));
        boolean matches = level.getRecipeManager().getAllRecipesFor(TTRecipeTypes.DUST_TRIGGER.get()).stream()
                .anyMatch(recipe -> recipe.matches(input, level));
        if (matches) {
            event.setUseBlock(Event.Result.DENY);
        }
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    private InteractionResult tryTrigger(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        ItemStack held = context.getItemInHand();
        if (!player.mayUseItemAt(pos, face, held)) {
            return InteractionResult.FAIL;
        }

        BlockState clicked = level.getBlockState(pos);
        DustTriggerInput input = new DustTriggerInput(held, level, pos, clicked);
        Optional<DustTrigger> match = findTrigger(level.getRecipeManager(), input, level);
        if (match.isEmpty()) {
            if (clicked.is(com.leclowndu93150.thaumaturge.registry.TTBlocks.INFUSION_MATRIX.get())) {
                if (level instanceof ServerLevel) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.thaumaturge.infusion.invalid_structure"), true);
                }
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }

        InteractionHand hand = context.getHand();
        player.swing(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        return applyOnServer(serverLevel, serverPlayer, held, match.get(), input, pos, face);
    }

    private static InteractionResult applyOnServer(ServerLevel level, ServerPlayer player, ItemStack held, DustTrigger trigger,
            DustTriggerInput input, BlockPos pos, Direction face) {
        if (!trigger.doesPassGate(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.thaumaturge.salis_mundus.no_research"), true);
            return InteractionResult.FAIL;
        }
        DustTriggerPlacement placement = trigger.findPlacement(input);
        trigger.execute(input, player, placement, face);
        Vec3 handPos = player.getEyePosition().add(player.getLookAngle().scale(0.3));
        DustTriggerFx.emitTriggerSparkles(level, player, pos, trigger, handPos, placement);
        level.playSound(null, pos, TTSounds.DUST.get(), SoundSource.PLAYERS, SOUND_VOLUME,
                SOUND_PITCH_BASE + level.random.nextFloat() * SOUND_PITCH_DEVIATION);
        CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, pos, held);
        player.awardStat(Stats.ITEM_USED.get(held.getItem()));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private static Optional<DustTrigger> findTrigger(RecipeManager recipes, DustTriggerInput input, Level level) {
        return recipes.getAllRecipesFor(TTRecipeTypes.DUST_TRIGGER.get()).stream()
                .filter(recipe -> recipe.matches(input, level))
                .map(recipe -> (DustTrigger) recipe)
                .findFirst();
    }
}
