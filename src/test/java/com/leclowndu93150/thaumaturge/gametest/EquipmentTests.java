package com.leclowndu93150.thaumaturge.gametest;

import com.leclowndu93150.thaumaturge.TTIds;
import com.leclowndu93150.thaumaturge.api.items.GogglesAccess;
import com.leclowndu93150.thaumaturge.registry.TTBulkContent;
import com.leclowndu93150.thaumaturge.registry.TTCreativeTabs;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

@GameTestHolder(TTIds.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentTests {
    private EquipmentTests() {}

    @GameTest(template = "essentia_lab")
    public static void gogglesWorkInTheCuriosHeadSlot(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.fromString("fbff1b19-9142-4dd7-b324-02f4f80c5675"), "tt_curio_goggles"));
        var goggles = new ItemStack(TTItems.GOGGLES_REVEALING.get());
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        var head = inventory.getStacksHandler("head").orElseThrow().getStacks();
        helper.assertTrue(head.getSlots() > 0, "Curios head slot is missing");
        helper.assertTrue(CuriosApi.isStackValid(new SlotContext("head", player, 0, false, true), goggles), "Curios rejected goggles in its head slot");
        helper.assertTrue(!GogglesAccess.wearsRevealingGear(player), "Revealing without goggles");
        helper.assertTrue(head.insertItem(0, goggles, false).isEmpty(), "Could not equip the goggles");
        try {
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "Curio goggles occupied the armor slot");
            helper.assertTrue(GogglesAccess.wearsRevealingGear(player), "Curio goggles do not reveal");
        } finally {
            head.extractItem(0, 1, false);
        }
        helper.assertTrue(!GogglesAccess.wearsRevealingGear(player), "Goggles effect remained after removing them");
        helper.succeed();
    }

    @GameTest(template = "essentia_lab")
    public static void hideDuplicateStonesButKeepTheirIds(GameTestHelper helper) {
        var hiddenFromViewers = TagKey.create(Registries.ITEM, ResourceLocation.tryParse("c:hidden_from_recipe_viewers"));
        for (String id : new String[] {"stone_arcane", "stone_arcane_brick", "placeholder_obsidian"}) {
            var stack = new ItemStack(TTBulkContent.BLOCK_ITEMS.get(id).get());
            helper.assertTrue(stack.is(TTCreativeTabs.INTERNAL_ITEMS) && stack.is(hiddenFromViewers), "Duplicate is still exposed: " + id);
        }
        var stone = new ItemStack(TTItems.ARCANE_STONE.get());
        helper.assertTrue(!stone.is(TTCreativeTabs.INTERNAL_ITEMS) && !stone.is(hiddenFromViewers), "Craftable arcane stone is hidden");
        helper.succeed();
    }
}
