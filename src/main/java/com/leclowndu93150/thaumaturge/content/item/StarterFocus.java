package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTBulkItems;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class StarterFocus extends Block {
    private static final String SOCKET = "StarterFocus";
    private static final String FIRE = "fire";
    private static final String FROST = "frost";
    private static final int IMPRINT_VIS = 10;

    public StarterFocus() {
        super(Properties.of().strength(3).noOcclusion());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack focus = player.getItemInHand(hand);
        if (!isFocus(focus)) return InteractionResult.PASS;
        if (focus.getCount() != 1) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        var knowledge = TTAttachments.knowledge(player);
        String spell = FIRE;
        ItemStack reagent = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        var crystal = TTItemData.getCrystalAspect(reagent);
        if (crystal != null && crystal.aspectId().equals(TTAspects.AQUA)) spell = FROST;
        FocusSpell graph = FocusSpell.starter(spell);
        if (player.isShiftKeyDown()) {
            graph = focus.is(TTBulkItems.ITEMS.get("focus_1").get()) ? FocusSpell.bolt(spell) : FocusSpell.split();
        }
        if (!fits(focus, graph) || !permitted(player, graph) || !knowledge.isComplete(TTIds.rl("base_auromancy")) || spell.equals(FROST) && !knowledge.isComplete(TTIds.rl("focus_elemental"))
                || !player.getAbilities().instabuild && (crystal == null || !crystal.aspectId().equals(spell.equals(FIRE) ? TTAspects.IGNIS : TTAspects.AQUA)
                || player.experienceLevel < 1 || AuraHelper.drainVis(server, pos, IMPRINT_VIS, true) < IMPRINT_VIS)) {
            player.displayClientMessage(Component.translatable("message.thaumaturge.focus.imprint_" + spell), true);
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            AuraHelper.drainVis(server, pos, IMPRINT_VIS, false);
            reagent.shrink(1);
            player.giveExperienceLevels(-1);
        }
        graph.write(focus);
        server.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1, 1);
        return InteractionResult.CONSUME;
    }

    static boolean isFocus(ItemStack stack) {
        return stack.is(TTBulkItems.ITEMS.get("focus_1").get()) || stack.is(TTBulkItems.ITEMS.get("focus_2").get())
                || stack.is(TTBulkItems.ITEMS.get("focus_3").get());
    }

    static String spell(ItemStack stack) {
        var spell = FocusSpell.read(stack);
        return spell == null ? "" : spell.effect();
    }

    static ItemStack socket(ItemStack wand) {
        var tag = wand.getTagElement(TTItemData.ROOT);
        return tag == null ? ItemStack.EMPTY : ItemStack.of(tag.getCompound(SOCKET));
    }

    static boolean swap(Player player, ItemStack wand) {
        if (!player.isShiftKeyDown()) return false;
        ItemStack offhand = player.getOffhandItem();
        ItemStack socket = socket(wand);
        if (offhand.isEmpty() && socket.isEmpty() || !offhand.isEmpty() && (!isFocus(offhand) || spell(offhand).isEmpty())) return false;
        if (!player.level().isClientSide) {
            if (offhand.isEmpty()) TTItemData.root(wand).remove(SOCKET);
            else {
                ItemStack inserted = offhand.copyWithCount(1);
                FocusSpell.read(inserted).write(inserted);
                TTItemData.root(wand).put(SOCKET, inserted.save(new net.minecraft.nbt.CompoundTag()));
            }
            if (offhand.getCount() <= 1) player.setItemInHand(InteractionHand.OFF_HAND, socket);
            else {
                offhand.shrink(1);
                if (!socket.isEmpty() && !player.addItem(socket)) player.drop(socket, false);
            }
        }
        return true;
    }

    static int[] primalCosts(ItemStack wand) {
        var graph = FocusSpell.read(socket(wand));
        return graph == null ? new int[WandCharge.PRIMALS.size()] : costs(graph, 0);
    }

    private static int[] costs(FocusSpell graph, int exhaust) {
        int base = graph.visCost();
        return WandCharge.focusCosts(graph.aspects(), base + base * exhaust / 10);
    }

    private static boolean fits(ItemStack focus, FocusSpell graph) {
        if (focus.is(TTBulkItems.ITEMS.get("focus_1").get())) return !graph.branched() && graph.complexity() <= 20;
        if (focus.is(TTBulkItems.ITEMS.get("focus_2").get())) return graph.complexity() <= 40;
        return focus.is(TTBulkItems.ITEMS.get("focus_3").get()) && graph.complexity() <= 75;
    }

    private static boolean permitted(Player player, FocusSpell graph) {
        var knowledge = TTAttachments.knowledge(player);
        if (graph.branched() && !knowledge.isComplete(TTIds.rl("focus_split"))) return false;
        return graph.casts().stream().allMatch(cast ->
                (!cast.bolt() || knowledge.isComplete(TTIds.rl("focus_bolt")))
                        && (!cast.effect().equals(FROST) || knowledge.isComplete(TTIds.rl("focus_elemental"))));
    }

    static InteractionResult cast(Level level, Player player, InteractionHand hand, ItemStack wand) {
        ItemStack focus = socket(wand);
        var graph = FocusSpell.read(focus);
        if (graph == null || !fits(focus, graph)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!TTAttachments.knowledge(player).isComplete(TTIds.rl("base_auromancy")) || !permitted(player, graph)) return InteractionResult.FAIL;
        var exhaust = player.getEffect(TTMobEffects.VIS_EXHAUST.get());
        int[] cost = costs(graph, exhaust == null ? 0 : exhaust.getAmplifier() + 1);
        if (!player.getAbilities().instabuild && !WandCharge.payFocus(wand, cost, true)) {
            player.displayClientMessage(Component.translatable("message.thaumaturge.focus.no_vis"), true);
            return InteractionResult.FAIL;
        }
        float power = graph.branched() ? 0.75F : 1;
        for (var cast : graph.casts()) apply(server, player, hand, wand, cast, power);
        player.getCooldowns().addCooldown(wand.getItem(), graph.cooldown());
        return InteractionResult.CONSUME;
    }

    private static void apply(ServerLevel server, Player player, InteractionHand hand, ItemStack wand, FocusSpell.Cast cast, float powerScale) {
        float power = cast.power() * powerScale;
        Vec3 eye = player.getEyePosition();
        Vec3 aim = player.getLookAngle().scale(cast.bolt() ? 16 : player.getBlockReach());
        var hit = server.clip(new ClipContext(eye, eye.add(aim), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double distance = hit.getLocation().distanceToSqr(eye);
        var target = ProjectileUtil.getEntityHitResult(player, eye, hit.getLocation(),
                player.getBoundingBox().expandTowards(aim).inflate(1), e -> e instanceof LivingEntity && e.isPickable(), distance);
        Vec3 at = hit.getLocation();
        if (target != null && target.getEntity() instanceof LivingEntity living) {
            if (living instanceof Player other && !player.canHarmPlayer(other)) return;
            at = living.position().add(0, living.getBbHeight() / 2, 0);
            if (cast.fire()) {
                living.hurt(server.damageSources().playerAttack(player), 3 + power);
                if (!living.fireImmune()) living.setSecondsOnFire(Mth.floor(1 + 2 * cast.duration()));
            } else {
                living.hurt(server.damageSources().freeze(), 3 + power);
                if (living.canFreeze()) {
                    int freeze = living.getTicksRequiredToFreeze() + 40 * cast.duration();
                    living.setTicksFrozen(Math.max(living.getTicksFrozen(), freeze));
                }
                int slowTicks = 10 * cast.duration();
                int amplifier = Math.min(2, Mth.floor(0.3F * power));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, amplifier));
            }
        } else if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            BlockPos adjacent = hit.getBlockPos().relative(hit.getDirection());
            if (cast.fire()) {
                if (power >= 0.5F && player.mayUseItemAt(adjacent, hit.getDirection(), wand) && server.mayInteract(player, adjacent)
                        && BaseFireBlock.canBePlacedAt(server, adjacent, player.getDirection())) {
                    server.setBlockAndUpdate(adjacent, BaseFireBlock.getState(server, adjacent));
                }
            } else {
                freezeWater(server, player, wand, hit.getDirection(), adjacent, Math.min(8, 1.5F * power));
            }
        }
        WandFx.stream(server, WandFx.tip(player, hand), at, WandItem.focusColor(wand));
        server.sendParticles(cast.fire() ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 12, 0.18, 0.18, 0.18, 0.04);
        server.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.6F, cast.fire() ? 1 : 1.4F);
    }

    private static void freezeWater(ServerLevel level, Player player, ItemStack wand, Direction face, BlockPos center, float radius) {
        int reach = Mth.ceil(radius);
        var ice = Blocks.FROSTED_ICE.defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach))) {
            if (pos.distSqr(center) > radius * radius || !level.isLoaded(pos)) continue;
            var state = level.getBlockState(pos);
            if (state.is(Blocks.WATER) && state.getFluidState().isSource() && level.mayInteract(player, pos) && player.mayUseItemAt(pos, face, wand)
                    && level.isUnobstructed(ice, pos, net.minecraft.world.phys.shapes.CollisionContext.empty())) {
                level.setBlockAndUpdate(pos, ice);
                level.scheduleTick(pos, Blocks.FROSTED_ICE, Mth.nextInt(level.getRandom(), 60, 120));
            }
        }
    }
}
