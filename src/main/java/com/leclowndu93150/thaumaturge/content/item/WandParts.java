package com.leclowndu93150.thaumaturge.content.item;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.registry.TTItemData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class WandParts {
    private WandParts() {}

    public static Cap cap(ItemStack wand) {
        var tag = wand.getTagElement(TTItemData.ROOT);
        return tag == null ? Cap.IRON : Cap.read(tag.getString("Cap"));
    }

    public static Rod rod(ItemStack wand) {
        var tag = wand.getTagElement(TTItemData.ROOT);
        return tag == null ? Rod.WOOD : Rod.read(tag.getString("Rod"));
    }

    public static boolean sceptre(ItemStack wand) {
        var tag = wand.getTagElement(TTItemData.ROOT);
        return tag != null && tag.getBoolean("Sceptre") && !rod(wand).staff();
    }

    public static int costPercent(ItemStack wand, ResourceLocation aspect) {
        return cap(wand).costPercent(aspect) - (sceptre(wand) ? 10 : 0);
    }

    public static float averageCost(ItemStack wand) {
        int sum = 0;
        for (var primal : WandCharge.PRIMALS)
            sum += costPercent(wand, primal);
        return sum / (100F * WandCharge.PRIMALS.size());
    }

    public enum Cap {
        IRON(110), COPPER(110), GOLD(100), SILVER(100), THAUMIUM(90), VOID(80);

        private final int cost;

        Cap(int cost) {
            this.cost = cost;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public int costPercent(ResourceLocation aspect) {
            if (this == COPPER && (TTAspects.ORDO.equals(aspect) || TTAspects.PERDITIO.equals(aspect)))
                return 100;
            if (this == SILVER && (TTAspects.AER.equals(aspect) || TTAspects.TERRA.equals(aspect) || TTAspects.IGNIS.equals(aspect) || TTAspects.AQUA.equals(aspect)))
                return 95;
            return cost;
        }

        static Cap read(String id) {
            for (Cap cap : values())
                if (cap.id().equals(id))
                    return cap;
            return IRON;
        }
    }

    public enum Rod {
        WOOD(25, (ResourceLocation) null), GREATWOOD(50, (ResourceLocation) null), SILVERWOOD(100, (ResourceLocation) null), OBSIDIAN(75, TTAspects.TERRA), BLAZE(75, TTAspects.IGNIS), ICE(75,
                TTAspects.AQUA), QUARTZ(75, TTAspects.ORDO), BONE(75, TTAspects.PERDITIO), REED(75, TTAspects.AER), GREATWOOD_STAFF(125, GREATWOOD), SILVERWOOD_STAFF(250, SILVERWOOD), OBSIDIAN_STAFF(
                        175, OBSIDIAN), BLAZE_STAFF(175, BLAZE), ICE_STAFF(175, ICE), QUARTZ_STAFF(175, QUARTZ), BONE_STAFF(175, BONE), REED_STAFF(175, REED), PRIMAL_STAFF(250, "primal", null);

        private final int capacity;
        private final String material;
        private final ResourceLocation recharge;
        private final boolean staff;

        Rod(int capacity, ResourceLocation recharge) {
            this.capacity = capacity;
            this.material = id();
            this.recharge = recharge;
            this.staff = false;
        }

        Rod(int capacity, Rod base) {
            this(capacity, base.material, base.recharge);
        }

        Rod(int capacity, String material, ResourceLocation recharge) {
            this.capacity = capacity;
            this.material = material;
            this.recharge = recharge;
            this.staff = true;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
        public int capacity() {
            return capacity;
        }
        public String material() {
            return material;
        }
        public ResourceLocation recharge() {
            return recharge;
        }
        public boolean staff() {
            return staff;
        }
        public boolean glows() {
            return TTAspects.IGNIS.equals(recharge);
        }

        static Rod read(String id) {
            for (Rod rod : values())
                if (rod.id().equals(id))
                    return rod;
            return WOOD;
        }
    }
}
