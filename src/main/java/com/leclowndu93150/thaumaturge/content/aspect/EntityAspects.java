package com.leclowndu93150.thaumaturge.content.aspect;

import static com.leclowndu93150.thaumaturge.api.aspect.TTAspects.*;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IEntityAspectSource;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Entity aspect lookup (CE EntityAspects). Forge 1.20.1 has no data maps, so the table CE ships in
 * {@code EntityAspectsProvider} is baked in here; entities implementing {@link IEntityAspectSource} win over it.
 */
public final class EntityAspects {
    private static final Map<EntityType<?>, AspectList> BUILTIN = new HashMap<>();

    static {
        put(EntityType.ZOMBIE, EXANIMIS, 20, HUMANUS, 10, TERRA, 5);
        put(EntityType.HUSK, EXANIMIS, 20, HUMANUS, 10, IGNIS, 5);
        put(EntityType.GIANT, EXANIMIS, 25, HUMANUS, 15, TERRA, 10);
        put(EntityType.SKELETON, EXANIMIS, 20, HUMANUS, 5, TERRA, 5);
        put(EntityType.WITHER_SKELETON, EXANIMIS, 25, HUMANUS, 5, PERDITIO, 10);
        put(EntityType.CREEPER, HERBA, 15, IGNIS, 15);
        put(EntityType.HORSE, BESTIA, 15, TERRA, 5, AER, 5);
        put(EntityType.DONKEY, BESTIA, 15, TERRA, 5, AER, 5);
        put(EntityType.MULE, BESTIA, 15, TERRA, 5, AER, 5);
        put(EntityType.SKELETON_HORSE, BESTIA, 5, EXANIMIS, 10, TERRA, 5, AER, 5);
        put(EntityType.ZOMBIE_HORSE, BESTIA, 10, EXANIMIS, 5, TERRA, 5, AER, 5);
        put(EntityType.PIG, BESTIA, 10, TERRA, 10, DESIDERIUM, 5);
        put(EntityType.EXPERIENCE_ORB, COGNITIO, 10);
        put(EntityType.SHEEP, BESTIA, 10, TERRA, 10);
        put(EntityType.COW, BESTIA, 15, TERRA, 15);
        put(EntityType.MOOSHROOM, BESTIA, 15, HERBA, 15, TERRA, 15);
        put(EntityType.SNOW_GOLEM, GELUM, 10, HUMANUS, 5, MACHINA, 5, PRAECANTATIO, 5);
        put(EntityType.OCELOT, BESTIA, 10, PERDITIO, 10);
        put(EntityType.CHICKEN, BESTIA, 5, VOLATUS, 5, AER, 5);
        put(EntityType.SQUID, BESTIA, 5, AQUA, 10);
        put(EntityType.WOLF, BESTIA, 15, TERRA, 10, AVERSIO, 5);
        put(EntityType.BAT, BESTIA, 5, VOLATUS, 5, TENEBRAE, 5);
        put(EntityType.SPIDER, BESTIA, 10, PERDITIO, 10, VINCULUM, 10);
        put(EntityType.SLIME, VICTUS, 10, AQUA, 10, ALKIMIA, 5);
        put(EntityType.GHAST, EXANIMIS, 15, IGNIS, 15);
        put(EntityType.ZOMBIFIED_PIGLIN, EXANIMIS, 15, IGNIS, 15, BESTIA, 10);
        put(EntityType.ENDERMAN, ALIENIS, 10, MOTUS, 15, DESIDERIUM, 5);
        put(EntityType.CAVE_SPIDER, BESTIA, 5, MORTUUS, 10, VINCULUM, 10);
        put(EntityType.SILVERFISH, BESTIA, 5, TERRA, 10);
        put(EntityType.BLAZE, ALIENIS, 5, IGNIS, 15, VOLATUS, 5);
        put(EntityType.MAGMA_CUBE, AQUA, 5, IGNIS, 10, ALKIMIA, 5);
        put(EntityType.ENDER_DRAGON, ALIENIS, 50, BESTIA, 30, PERDITIO, 50, VOLATUS, 10);
        put(EntityType.WITHER, EXANIMIS, 50, PERDITIO, 25, IGNIS, 25);
        put(EntityType.WITCH, HUMANUS, 15, PRAECANTATIO, 5, ALKIMIA, 10);
        put(EntityType.VILLAGER, HUMANUS, 15);
        put(EntityType.IRON_GOLEM, METALLUM, 15, HUMANUS, 5, MACHINA, 5, PRAECANTATIO, 5);
        put(EntityType.END_CRYSTAL, ALIENIS, 15, AURAM, 15, VICTUS, 15);
        put(EntityType.ITEM_FRAME, SENSUS, 5, FABRICO, 5);
        put(EntityType.GLOW_ITEM_FRAME, SENSUS, 5, FABRICO, 5);
        put(EntityType.PAINTING, SENSUS, 10, FABRICO, 5);
        put(EntityType.GUARDIAN, BESTIA, 10, ALIENIS, 10, AQUA, 10);
        put(EntityType.ELDER_GUARDIAN, BESTIA, 10, ALIENIS, 15, AQUA, 15);
        put(EntityType.RABBIT, BESTIA, 5, TERRA, 5, MOTUS, 5);
        put(EntityType.ENDERMITE, BESTIA, 5, ALIENIS, 5, MOTUS, 5);
        put(EntityType.POLAR_BEAR, BESTIA, 15, GELUM, 10);
        put(EntityType.SHULKER, ALIENIS, 10, VINCULUM, 5, VOLATUS, 5, PRAEMUNIO, 5);
        put(EntityType.EVOKER, ALIENIS, 5, PRAECANTATIO, 5, HUMANUS, 10);
        put(EntityType.VINDICATOR, AVERSIO, 5, PRAECANTATIO, 5, HUMANUS, 10);
        put(EntityType.ILLUSIONER, SENSUS, 5, PRAECANTATIO, 5, HUMANUS, 10);
        put(EntityType.LLAMA, BESTIA, 15, AQUA, 5);
        put(EntityType.PARROT, BESTIA, 5, VOLATUS, 5, SENSUS, 5);
        put(EntityType.STRAY, EXANIMIS, 20, HUMANUS, 5, VINCULUM, 5);
        put(EntityType.VEX, ALIENIS, 5, VOLATUS, 5, PRAECANTATIO, 5, HUMANUS, 5);
        put(EntityType.DOLPHIN, BESTIA, 10, AQUA, 10, MOTUS, 5, COGNITIO, 5);
        put(EntityType.DROWNED, EXANIMIS, 20, HUMANUS, 10, AQUA, 5);
        put(EntityType.TURTLE, BESTIA, 10, AQUA, 5, PRAEMUNIO, 5);
        put(EntityType.COD, BESTIA, 5, AQUA, 5);
        put(EntityType.SALMON, BESTIA, 5, AQUA, 5);
        put(EntityType.TROPICAL_FISH, BESTIA, 5, AQUA, 5, SENSUS, 3);
        put(EntityType.PUFFERFISH, BESTIA, 5, AQUA, 5, AVERSIO, 3);
        put(EntityType.PHANTOM, EXANIMIS, 15, VOLATUS, 10, TENEBRAE, 5, SPIRITUS, 5);
        put(EntityType.CAT, BESTIA, 10, SENSUS, 5);
        put(EntityType.FOX, BESTIA, 10, SENSUS, 5, MOTUS, 3);
        put(EntityType.PANDA, BESTIA, 15, HERBA, 5);
        put(EntityType.PILLAGER, HUMANUS, 10, AVERSIO, 10);
        put(EntityType.RAVAGER, BESTIA, 20, AVERSIO, 15, TERRA, 5);
        put(EntityType.TRADER_LLAMA, BESTIA, 15, AQUA, 5);
        put(EntityType.WANDERING_TRADER, HUMANUS, 10, PERMUTATIO, 10, DESIDERIUM, 5);
        put(EntityType.BEE, BESTIA, 5, VOLATUS, 5, HERBA, 5, FABRICO, 2);
        put(EntityType.HOGLIN, BESTIA, 15, AVERSIO, 10, IGNIS, 5);
        put(EntityType.ZOGLIN, EXANIMIS, 15, BESTIA, 10, AVERSIO, 10);
        put(EntityType.PIGLIN, HUMANUS, 10, BESTIA, 5, DESIDERIUM, 10);
        put(EntityType.PIGLIN_BRUTE, HUMANUS, 10, BESTIA, 5, AVERSIO, 15);
        put(EntityType.STRIDER, BESTIA, 10, IGNIS, 10, MOTUS, 5);
        put(EntityType.AXOLOTL, BESTIA, 10, AQUA, 5, VICTUS, 5);
        put(EntityType.GLOW_SQUID, BESTIA, 5, AQUA, 5, LUX, 5);
        put(EntityType.GOAT, BESTIA, 10, TERRA, 5, MOTUS, 5);
        put(EntityType.ALLAY, SPIRITUS, 10, VOLATUS, 5, SENSUS, 5, DESIDERIUM, 3);
        put(EntityType.FROG, BESTIA, 8, AQUA, 4, MOTUS, 3);
        put(EntityType.TADPOLE, BESTIA, 4, AQUA, 4, VICTUS, 2);
        put(EntityType.WARDEN, TENEBRAE, 20, SENSUS, 15, AVERSIO, 15, TERRA, 10, VITIUM, 5);
        put(EntityType.CAMEL, BESTIA, 12, TERRA, 5, MOTUS, 3);
        put(EntityType.SNIFFER, BESTIA, 15, HERBA, 10, SENSUS, 10, TERRA, 5);
        put(EntityType.ZOMBIE_VILLAGER, EXANIMIS, 20, HUMANUS, 15, TERRA, 5);
        put(TTEntities.FLUX_RIFT.get(), VITIUM, 30, VACUOS, 20, ALIENIS, 15);
        put(TTEntities.TAINT_SEED.get(), VITIUM, 20, AURAM, 10, HERBA, 5);
        put(TTEntities.GOLEM.get(), MOTUS, 10, MACHINA, 10, PRAECANTATIO, 5);
    }

    private EntityAspects() {}

    public static AspectList of(Entity entity) {
        return entity instanceof IEntityAspectSource source ? source.entityAspects() : of(entity.getType());
    }

    public static AspectList of(EntityType<?> type) {
        AspectList aspects = BUILTIN.get(type);
        return aspects == null ? AspectList.EMPTY : aspects;
    }

    private static void put(EntityType<?> type, Object... pairs) {
        AspectList list = AspectList.EMPTY;
        for (int i = 0; i < pairs.length; i += 2) {
            list = list.add(new AspectInstance((ResourceLocation) pairs[i], (Integer) pairs[i + 1]));
        }
        BUILTIN.put(type, list);
    }
}
