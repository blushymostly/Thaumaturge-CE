package com.leclowndu93150.thaumaturge.api.aspect;

import com.leclowndu93150.thaumaturge.TTIds;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Built-in primal + compound aspects. Component pairs must match CE {@code AspectBootstrap.DEFINITIONS}
 * so research-table combine / JEI composition stay unique and correct.
 */
public final class TTAspects {
    private static final Map<ResourceLocation, IAspect> ASPECTS = new LinkedHashMap<>();

    public static final ResourceLocation AER = primal("aer", 0xFFFF7E);
    public static final ResourceLocation TERRA = primal("terra", 0x56C000);
    public static final ResourceLocation IGNIS = primal("ignis", 0xFF5A01);
    public static final ResourceLocation AQUA = primal("aqua", 0x3CD4FC);
    public static final ResourceLocation ORDO = primal("ordo", 0xD5D4EC);
    public static final ResourceLocation PERDITIO = primal("perditio", 0x404040);

    public static final ResourceLocation VACUOS = compound("vacuos", 0x888888, AER, PERDITIO);
    public static final ResourceLocation LUX = compound("lux", 0xFFFFC0, AER, IGNIS);
    public static final ResourceLocation MOTUS = compound("motus", 0xCDCCF4, AER, ORDO);
    public static final ResourceLocation GELUM = compound("gelum", 0xE1FFFF, IGNIS, PERDITIO);
    public static final ResourceLocation VITREUS = compound("vitreus", 0x80FFFF, TERRA, AER);
    public static final ResourceLocation METALLUM = compound("metallum", 0xB5B5CD, TERRA, ORDO);
    public static final ResourceLocation VICTUS = compound("victus", 0xDE0005, TERRA, AQUA);
    public static final ResourceLocation MORTUUS = compound("mortuus", 0x6A0005, VICTUS, PERDITIO);
    public static final ResourceLocation POTENTIA = compound("potentia", 0xC0FFFF, ORDO, IGNIS);
    public static final ResourceLocation PERMUTATIO = compound("permutatio", 0x578357, PERDITIO, ORDO);
    public static final ResourceLocation PRAECANTATIO = compound("praecantatio", 0xCF00FF, POTENTIA, AER);
    public static final ResourceLocation AURAM = compound("auram", 0xFFC0FF, PRAECANTATIO, AER);
    public static final ResourceLocation ALKIMIA = compound("alkimia", 0x23AC9D, PRAECANTATIO, AQUA);
    public static final ResourceLocation VITIUM = compound("vitium", 0x800080, PERDITIO, PRAECANTATIO);
    public static final ResourceLocation TENEBRAE = compound("tenebrae", 0x222222, VACUOS, LUX);
    public static final ResourceLocation ALIENIS = compound("alienis", 0x805080, VACUOS, TENEBRAE);
    public static final ResourceLocation VOLATUS = compound("volatus", 0xE7E7D7, AER, MOTUS);
    public static final ResourceLocation HERBA = compound("herba", 0x01AC00, VICTUS, TERRA);
    public static final ResourceLocation INSTRUMENTUM = compound("instrumentum", 0x4040EE, METALLUM, POTENTIA);
    public static final ResourceLocation FABRICO = compound("fabrico", 0x809D80, PERMUTATIO, INSTRUMENTUM);
    public static final ResourceLocation MACHINA = compound("machina", 0x8080A0, MOTUS, INSTRUMENTUM);
    public static final ResourceLocation VINCULUM = compound("vinculum", 0x9A8080, MOTUS, PERDITIO);
    public static final ResourceLocation SPIRITUS = compound("spiritus", 0xEBEBFB, VICTUS, MORTUUS);
    public static final ResourceLocation COGNITIO = compound("cognitio", 0xF9967F, IGNIS, SPIRITUS);
    public static final ResourceLocation SENSUS = compound("sensus", 0xC0FFC0, AER, SPIRITUS);
    public static final ResourceLocation AVERSIO = compound("aversio", 0xC05050, SPIRITUS, PERDITIO);
    public static final ResourceLocation PRAEMUNIO = compound("praemunio", 0x00C0C0, SPIRITUS, TERRA);
    public static final ResourceLocation DESIDERIUM = compound("desiderium", 0xE6BE44, SPIRITUS, VACUOS);
    public static final ResourceLocation EXANIMIS = compound("exanimis", 0x3A4000, MOTUS, MORTUUS);
    public static final ResourceLocation BESTIA = compound("bestia", 0x9F6409, MOTUS, VICTUS);
    public static final ResourceLocation HUMANUS = compound("humanus", 0xFFD7C0, SPIRITUS, VICTUS);

    private TTAspects() {}

    private static ResourceLocation primal(String tag, int color) {
        ResourceLocation id = TTIds.rl(tag);
        ASPECTS.put(id, new Aspect(tag, color));
        return id;
    }

    private static ResourceLocation compound(String tag, int color, ResourceLocation a, ResourceLocation b) {
        ResourceLocation id = TTIds.rl(tag);
        ASPECTS.put(id, new Aspect(tag, color, List.of(a, b)));
        return id;
    }

    public static Map<ResourceLocation, IAspect> all() {
        return Collections.unmodifiableMap(ASPECTS);
    }

    public static IAspect get(ResourceLocation id) {
        return ASPECTS.get(id);
    }

    public static void bootstrap() {
        ASPECTS.size();
    }
}
