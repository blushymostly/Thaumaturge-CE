package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
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
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
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
    private static final ResourceLocation ADVANCED_TAPPING = TTIds.rl("node_tapper_1");
    private static final ResourceLocation MASTER_TAPPING = TTIds.rl("node_tapper_2");
    private static final ResourceLocation NODE_PRESERVATION = TTIds.rl("node_preserve");

    public WandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private static final HumanoidModel.ArmPose AIMING =
                    HumanoidModel.ArmPose.create("THAUMATURGE_WAND", false, (model, entity, arm) -> {
                        var castingArm = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                        castingArm.xRot = model.head.xRot - Mth.PI / 4;
                        castingArm.yRot = model.head.yRot;
                        castingArm.zRot = 0;
                    });
            private WandRenderer renderer;

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return entity.isUsingItem() && entity.getUsedItemHand() == hand ? AIMING : null;
            }

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
        return slotChanged || !(newStack.getItem() instanceof WandItem) || !rod(oldStack).equals(rod(newStack))
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
        if (player != null && com.leclowndu93150.thaumaturge.content.aura.node.NodeJarRitual.use(context)) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
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
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack wand, int remaining) {
        if (!(level instanceof ServerLevel server) || !(user instanceof Player player)
                || remaining % CHARGE_INTERVAL != 0) return;
        BlockHitResult hit = target(player);
        BlockEntityNode node = targetNode(level, hit);
        if (node != null) {
            ResourceLocation aspect = tapNode(server, player, wand, node);
            if (aspect != null) {
                node.recordDrain(player, TTAspects.get(aspect).color());
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
            Vec3 origin = hit.getType() == HitResult.Type.BLOCK
                    ? hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.03))
                    : player.getEyePosition().add(player.getLookAngle().scale(2.5));
            WandFx.drawAura(level, player, origin, TTAspects.get(aspect).color());
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

    private static @Nullable ResourceLocation tapNode(ServerLevel level, Player player, ItemStack wand, BlockEntityNode node) {
        if (node.isEnergized()) return drainPrimal(wand, node);

        var knowledge = TTAttachments.knowledge(player);
        int strength = 1;
        if (knowledge.isComplete(ADVANCED_TAPPING)) strength++;
        if (knowledge.isComplete(MASTER_TAPPING)) strength++;
        int reserve = !player.isShiftKeyDown() && !rod(wand).equals("wood") && knowledge.isComplete(NODE_PRESERVATION) ? 1 : 0;
        var eligible = WandCharge.PRIMALS.stream()
                .filter(aspect -> node.getAspects().amountOf(aspect) > reserve
                        && WandCharge.room(wand, aspect) >= WandCharge.CENTIVIS_PER_VIS)
                .toList();
        if (eligible.isEmpty()) return null;

        ResourceLocation aspect = eligible.get(level.random.nextInt(eligible.size()));
        int room = WandCharge.room(wand, aspect) / WandCharge.CENTIVIS_PER_VIS;
        int offered = Math.min(strength, node.getAspects().amountOf(aspect) - reserve);
        int taken = node.drain(aspect, Math.min(offered, room));
        WandCharge.add(wand, aspect, taken * WandCharge.CENTIVIS_PER_VIS);
        return aspect;
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
        return switch (rod(wand)) {
            case "silverwood" -> 100;
            case "greatwood" -> 50;
            default -> MAX_VIS;
        };
    }

    public static String rod(ItemStack wand) {
        var tag = wand.getTagElement(TTItemData.ROOT);
        return switch (tag == null ? "" : tag.getString("Rod")) {
            case "greatwood" -> "greatwood";
            case "silverwood" -> "silverwood";
            default -> "wood";
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.thaumaturge.wand.named", Component.translatable("wand.thaumaturge.cap.iron"),
                Component.translatable("wand.thaumaturge.rod." + rod(stack)));
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
