package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.registry.TTItemData;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.content.world.crystal.BlockCrystal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.function.Consumer;
import com.leclowndu93150.thaumaturge.client.render.item.WandRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;


public class WandItem extends Item {
    public static final float MAX_VIS = 25.0F;
    private static final int USE_DURATION = 72000;
    private static final int CHARGE_INTERVAL = 5;
    private static final int REFINE_CENTIVIS = 10;
    private static final int RAW_VIS_RATIO = 3;

    public WandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private WandRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new WandRenderer();
                return renderer;
            }
        });
    }

    public static ItemStack focus(ItemStack wand) {
        return StarterFocus.socket(wand);
    }

    public static int[] focusCosts(ItemStack wand) {
        return StarterFocus.primalCosts(wand);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !(newStack.getItem() instanceof WandItem) || isGreatwood(oldStack) != isGreatwood(newStack)
                || !ItemStack.matches(focus(oldStack), focus(newStack));
    }

    public static int focusColor(ItemStack wand) {
        return switch (StarterFocus.spell(StarterFocus.socket(wand))) {
            case "fire" -> 0xFF5A01;
            case "frost" -> 0xE1FFFF;
            default -> 0;
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        return player == null ? InteractionResult.PASS : use(context.getLevel(), player, context.getHand()).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && StarterFocus.swap(player, stack)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (focus(stack).isEmpty() || targetNode(level, target(player)) != null) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return new InteractionResultHolder<>(player.isShiftKeyDown() ? InteractionResult.PASS
                : StarterFocus.cast(level, player, hand, stack), stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack wand, int remaining) {
        if (!(level instanceof ServerLevel server) || !(user instanceof Player player)
                || remaining % CHARGE_INTERVAL != 0) return;
        BlockHitResult hit = target(player);
        BlockEntityNode node = targetNode(level, hit);
        if (node != null) {
            ResourceLocation aspect = drainPrimal(wand, node);
            if (aspect != null) {
                WandFx.stream(server, Vec3.atCenterOf(node.getBlockPos()), WandFx.tip(player, player.getUsedItemHand()),
                        TTAspects.get(aspect).color());
            }
            return;
        }
        refineFromAura(server, player, wand, hit);
    }

    private static void refineFromAura(ServerLevel level, Player player, ItemStack wand, BlockHitResult hit) {
        var block = hit.getType() == HitResult.Type.BLOCK ? level.getBlockState(hit.getBlockPos()).getBlock() : null;
        ResourceLocation crystal = block instanceof BlockCrystal cluster && WandCharge.PRIMALS.contains(cluster.aspect())
                ? cluster.aspect() : null;
        ResourceLocation aspect = crystal != null ? crystal : WandCharge.PRIMALS.stream()
                .min(Comparator.comparingInt(id -> WandCharge.stored(wand, id))).orElseThrow();
        int wanted = Math.min(REFINE_CENTIVIS, WandCharge.room(wand, aspect));
        if (wanted <= 0) return;
        float rawCost = wanted * RAW_VIS_RATIO / (float) WandCharge.CENTIVIS_PER_VIS;
        double drawn = AuraHelper.drainVis(level, player.blockPosition(), rawCost, false);
        int gained = WandCharge.add(wand, aspect, (int) (drawn * WandCharge.CENTIVIS_PER_VIS / RAW_VIS_RATIO));
        if (gained > 0) {
            var random = level.getRandom();
            Vec3 origin = player.getEyePosition().add((random.nextDouble() - 0.5) * 0.6,
                    random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.6);
            WandFx.stream(level, origin, WandFx.tip(player, player.getUsedItemHand()), TTAspects.get(aspect).color());
        }
    }

    private static BlockHitResult target(Player player) {
        Vec3 eye = player.getEyePosition();
        return player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(4.5)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    private static @Nullable BlockEntityNode targetNode(Level level, BlockHitResult hit) {
        return hit.getType() == HitResult.Type.BLOCK && level.getBlockEntity(hit.getBlockPos()) instanceof BlockEntityNode node
                ? node : null;
    }

    public static boolean drawFromNode(ItemStack wand, BlockEntityNode node) {
        return drainPrimal(wand, node) != null;
    }

    private static @Nullable ResourceLocation drainPrimal(ItemStack wand, BlockEntityNode node) {
        for (var entry : node.getAspects().entries()) {
            var aspect = entry.aspectId();
            if (node.isEnergized()) {
                int taken = node.drainCentivis(aspect, Math.min(WandCharge.CENTIVIS_PER_VIS, WandCharge.room(wand, aspect)));
                if (taken > 0) {
                    WandCharge.add(wand, aspect, taken);
                    return aspect;
                }
                continue;
            }
            if (entry.amount() > 0 && WandCharge.PRIMALS.contains(aspect) && WandCharge.room(wand, aspect) >= WandCharge.CENTIVIS_PER_VIS) {
                int taken = node.drain(aspect, 1);
                if (taken > 0) {
                    WandCharge.add(wand, aspect, taken * WandCharge.CENTIVIS_PER_VIS);
                    return aspect;
                }
            }
        }
        return null;
    }

    public static float capacity(ItemStack wand) {
        return isGreatwood(wand) ? 50 : MAX_VIS;
    }

    public static boolean isGreatwood(ItemStack wand) {
        var tag = wand.getTagElement(TTItemData.ROOT);
        return tag != null && tag.getString("Rod").equals("greatwood");
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.thaumaturge.wand.named", Component.translatable("wand.thaumaturge.cap.iron"),
                Component.translatable("wand.thaumaturge.rod." + (isGreatwood(stack) ? "greatwood" : "wood")));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        for (var id : WandCharge.PRIMALS) {
            var aspect = TTAspects.get(id);
            if (aspect == null) continue;
            String amount = String.format(Locale.ROOT, "%.2f", WandCharge.stored(stack, id) / 100F);
            tooltip.add(Component.translatable("tooltip.thaumaturge.wand.primal_vis",
                    Component.translatable("aspect.thaumaturge." + id.getPath()), amount, (int) capacity(stack))
                    .withStyle(style -> style.withColor(aspect.color())));
        }
        String spell = StarterFocus.spell(StarterFocus.socket(stack));
        if (!spell.isEmpty()) tooltip.add(Component.translatable("tooltip.thaumaturge.focus." + spell));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int sum = WandCharge.PRIMALS.stream().mapToInt(id -> WandCharge.stored(stack, id)).sum();
        return Math.round(13F * sum / (WandCharge.capacity(stack) * WandCharge.PRIMALS.size()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x55FFFF;
    }
}
