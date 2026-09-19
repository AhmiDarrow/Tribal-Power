package tk.darrow.tribalpower.familiar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.entity.CreatureProfile;

/**
 * Five-thread bloodline plus named Marks on a lattice creature. Written at spawn; a Bonding Charm does not reroll.
 * Phenotype 1 matches today's {@link CreatureProfile} baselines. Wild stock never rolls a 4; only breeding reaches
 * 4 and the Exalted 5. Two parents both strong in a thread can pass on a step above either of them.
 */
public final class FamiliarData {
    public enum Thread { FRAME, STRIDE, FANG, HUM, KEEP }
    public enum Mark {
        NONE("", false),
        QUIET_THREAD("quiet_thread", true),
        STONEHIDE("stonehide", true),
        DRIFT("drift", true),
        KEEN("keen", false),
        SOFT_MAW("soft_maw", true),
        NIGHT_HUM("night_hum", false),
        KIN_MARK("kin_mark", false),
        FRAYED("frayed", true),
        LEAP("leap", false),
        GLOW_VEIN("glow_vein", true),
        DEEP_POCKET("deep_pocket", false),
        BRACE("brace", true),
        TRACK("track", false),
        TEMPO("tempo", false),
        CLICK_TRUE("click_true", false),
        STILL("still", false),
        GATHER("gather", false);

        public final String id;
        public final boolean dominant;
        Mark(String id, boolean dominant) { this.id = id; this.dominant = dominant; }
        public static Mark fromId(String id) {
            if (id == null || id.isEmpty()) return NONE;
            for (Mark mark : values()) if (mark.id.equals(id)) return mark;
            return NONE;
        }
    }

    public static final ResourceLocation HEALTH = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "lattice_health");
    public static final ResourceLocation SPEED = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "lattice_speed");
    public static final ResourceLocation ARMOR = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "lattice_armor");
    public static final ResourceLocation DAMAGE = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "lattice_damage");
    public static final ResourceLocation KNOCKBACK = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "lattice_knockback");
    public static final int BASE_SADDLEBAG = 9, EXTRA_SADDLEBAG = 3;
    public static final int DRIFT_SIT_TICKS = 1200;
    /** Highest allele: reached only by breeding. */
    public static final int MAX = 5;
    /** Chance, per thread, that parents both at 3 or better lift their child a step. */
    public static final float LINE_BREEDING = 0.25F;

    private static final Mark[] WILD_COMMON = { Mark.QUIET_THREAD, Mark.STONEHIDE, Mark.DRIFT, Mark.KEEN, Mark.SOFT_MAW, Mark.NIGHT_HUM };
    private static final float WILD_SPARK = 0.03F, WILD_SPARK_MARK = 0.15F, LEAN_BUMP = 0.40F;

    private final byte[] a = new byte[Thread.values().length];
    private final byte[] b = new byte[Thread.values().length];
    private Mark markA = Mark.NONE, markB = Mark.NONE;
    private int generation, fray;
    private boolean sparked, rolled;

    public boolean rolled() { return rolled; }
    public boolean sparked() { return sparked; }
    public int generation() { return generation; }
    public int fray() { return fray; }
    public Mark markA() { return markA; }
    public Mark markB() { return markB; }
    public int alleleA(Thread thread) { return a[thread.ordinal()] & 0xFF; }
    public int alleleB(Thread thread) { return b[thread.ordinal()] & 0xFF; }

    public int phenotype(Thread thread) {
        int value = Math.round((alleleA(thread) + alleleB(thread)) / 2F);
        if (thread == Thread.HUM && expressed(Mark.FRAYED)) value -= 1;
        return Mth.clamp(value, 0, MAX);
    }

    /** Phenotype 1 is the species baseline; each step is ±12%. */
    public double multiplier(Thread thread) {
        return 1.0 + (phenotype(thread) - 1) * 0.12;
    }

    public boolean expressed(Mark mark) {
        return expressedMarks().contains(mark);
    }

    public List<Mark> expressedMarks() {
        List<Mark> out = new ArrayList<>(2);
        consider(markA, out);
        consider(markB, out);
        if (out.contains(Mark.STONEHIDE)) out.remove(Mark.DRIFT);
        if (out.size() > 2) return List.copyOf(out.subList(0, 2));
        return List.copyOf(out);
    }

    private void consider(Mark mark, List<Mark> out) {
        if (mark == Mark.NONE || out.contains(mark) || out.size() >= 2) return;
        boolean pair = markA == mark && markB == mark;
        if (mark.dominant || pair || mark == Mark.FRAYED) out.add(mark);
    }

    public double abilityScale(boolean night) {
        double scale = multiplier(Thread.HUM);
        if (expressed(Mark.KEEN)) scale /= 0.8;
        if (expressed(Mark.NIGHT_HUM) && night) scale *= 1.2;
        return scale;
    }

    public int abilityPeriod(int base, boolean night) {
        return Math.max(base / 2, Math.round(base / (float) Math.max(0.5, abilityScale(night))));
    }

    public float followStart() { return expressed(Mark.QUIET_THREAD) ? 8 : 10; }
    public float followStop() { return expressed(Mark.QUIET_THREAD) ? 2 : 3; }
    public int saddlebagSlots() { return expressed(Mark.DEEP_POCKET) ? BASE_SADDLEBAG + EXTRA_SADDLEBAG : BASE_SADDLEBAG; }

    public static Thread lean(CreatureProfile profile) {
        return switch (profile) {
            case DAWN_STAG, STORM_MOTH -> Thread.STRIDE;
            case LANTERN_FOX, CINDER_IMP, MOURNING_BELL -> Thread.HUM;
            case MOSSBACK, ECHO_WEAVER -> Thread.KEEP;
            case SHARDBACK, ROOTBOUND, HOLLOW_SENTINEL -> Thread.FRAME;
            case RIFT_HOUND, REED_STALKER, ASHBOUND -> Thread.FANG;
            default -> Thread.HUM;
        };
    }

    public static Mark speciesMark(CreatureProfile profile) {
        return switch (profile) {
            case DAWN_STAG -> Mark.LEAP;
            case LANTERN_FOX -> Mark.GLOW_VEIN;
            case MOSSBACK -> Mark.DEEP_POCKET;
            case SHARDBACK -> Mark.BRACE;
            case RIFT_HOUND -> Mark.TRACK;
            case CINDER_IMP -> Mark.TEMPO;
            case STORM_MOTH -> Mark.CLICK_TRUE;
            case MOURNING_BELL -> Mark.STILL;
            case ECHO_WEAVER -> Mark.GATHER;
            default -> Mark.NONE;
        };
    }

    public void rollWild(CreatureProfile profile, RandomSource random, boolean march) {
        for (Thread thread : Thread.values()) {
            a[thread.ordinal()] = (byte) wildAllele(random);
            b[thread.ordinal()] = (byte) wildAllele(random);
        }
        Thread lean = lean(profile);
        if (random.nextFloat() < LEAN_BUMP) a[lean.ordinal()] = (byte) Math.min(3, alleleA(lean) + 1);
        if (random.nextFloat() < LEAN_BUMP) b[lean.ordinal()] = (byte) Math.min(3, alleleB(lean) + 1);
        rollWildMarks(profile, random);
        sparked = march && random.nextFloat() < WILD_SPARK;
        if (sparked) {
            int slot = random.nextBoolean() ? 0 : 1;
            byte[] alleles = slot == 0 ? a : b;
            alleles[lean.ordinal()] = 3;
            if (random.nextFloat() < WILD_SPARK_MARK) {
                Mark species = speciesMark(profile);
                if (species != Mark.NONE && markA == Mark.NONE) markA = species;
                else if (species != Mark.NONE && markB == Mark.NONE) markB = species;
            }
        }
        generation = 0;
        fray = 0;
        rolled = true;
    }

    private void rollWildMarks(CreatureProfile profile, RandomSource random) {
        markA = Mark.NONE;
        markB = Mark.NONE;
        int roll = random.nextInt(100);
        if (roll < 55) return;
        if (roll < 85) {
            markA = WILD_COMMON[random.nextInt(WILD_COMMON.length)];
            return;
        }
        if (roll < 97) {
            Mark pick = WILD_COMMON[random.nextInt(WILD_COMMON.length)];
            if (!pick.dominant) pick = Mark.QUIET_THREAD;
            markA = pick;
            return;
        }
        Mark species = speciesMark(profile);
        markA = species == Mark.NONE ? WILD_COMMON[random.nextInt(WILD_COMMON.length)] : species;
    }

    private static int wildAllele(RandomSource random) {
        int n = random.nextInt(100);
        if (n < 30) return 0;
        if (n < 80) return 1;
        if (n < 98) return 2;
        return 3;
    }

    /** Test helper: both alleles of every thread set to {@code value}, no marks. */
    public void copyFrom(FamiliarData other) {
        System.arraycopy(other.a, 0, a, 0, a.length);
        System.arraycopy(other.b, 0, b, 0, b.length);
        markA = other.markA;
        markB = other.markB;
        generation = other.generation;
        fray = other.fray;
        sparked = other.sparked;
        rolled = other.rolled;
    }

    public void fill(int value) {
        byte allele = (byte) Mth.clamp(value, 0, MAX);
        for (int i = 0; i < a.length; i++) { a[i] = allele; b[i] = allele; }
        markA = Mark.NONE;
        markB = Mark.NONE;
        generation = 0;
        fray = 0;
        sparked = false;
        rolled = true;
    }

    public void setAlleles(Thread thread, int first, int second) {
        a[thread.ordinal()] = (byte) Mth.clamp(first, 0, MAX);
        b[thread.ordinal()] = (byte) Mth.clamp(second, 0, MAX);
        rolled = true;
    }

    public void setMarks(Mark first, Mark second) {
        markA = first == null ? Mark.NONE : first;
        markB = second == null ? Mark.NONE : second;
        rolled = true;
    }

    public static FamiliarData inherit(FamiliarData mother, FamiliarData father, CreatureProfile profile, RandomSource random, UUID ownerA, UUID ownerB) {
        return inherit(mother, father, profile, random, ownerA, ownerB, 0.08F, 0.02F, 0.05F);
    }

    public static FamiliarData inherit(FamiliarData mother, FamiliarData father, CreatureProfile profile, RandomSource random, UUID ownerA, UUID ownerB, float mutation, float ownerBonus, float markMutation) {
        FamiliarData child = new FamiliarData();
        boolean sameOwner = ownerA != null && ownerA.equals(ownerB);
        for (Thread thread : Thread.values()) {
            child.a[thread.ordinal()] = (byte) pick(mother, thread, random);
            child.b[thread.ordinal()] = (byte) pick(father, thread, random);
            mutateThread(child, thread, random, mutation, sameOwner ? ownerBonus : 0);
            // Line breeding: two strong parents can give a child better than either.
            if (mother.rolled && father.rolled && mother.phenotype(thread) >= 3 && father.phenotype(thread) >= 3
                    && random.nextFloat() < LINE_BREEDING) {
                int i = thread.ordinal();
                if (child.alleleA(thread) <= child.alleleB(thread)) child.a[i] = (byte) Math.min(MAX, child.alleleA(thread) + 1);
                else child.b[i] = (byte) Math.min(MAX, child.alleleB(thread) + 1);
            }
        }
        child.markA = random.nextBoolean() ? mother.markA : mother.markB;
        child.markB = random.nextBoolean() ? father.markA : father.markB;
        if (random.nextFloat() < markMutation) mutateMark(child, profile, random, sameOwner);
        boolean sameFF = mother.phenotype(Thread.FRAME) == father.phenotype(Thread.FRAME)
                && mother.phenotype(Thread.FANG) == father.phenotype(Thread.FANG)
                && child.phenotype(Thread.FRAME) == mother.phenotype(Thread.FRAME)
                && child.phenotype(Thread.FANG) == mother.phenotype(Thread.FANG);
        child.fray = sameOwner && sameFF ? Math.max(mother.fray, father.fray) + 1 : 0;
        child.generation = Math.max(mother.generation, father.generation) + 1;
        if (child.fray >= 3) {
            int hum = Thread.HUM.ordinal();
            if (child.a[hum] > 0) child.a[hum]--;
            else if (child.b[hum] > 0) child.b[hum]--;
            if (child.markA == Mark.NONE) child.markA = Mark.FRAYED;
            else if (child.markB == Mark.NONE) child.markB = Mark.FRAYED;
            else child.markB = Mark.FRAYED;
        }
        child.sparked = false;
        child.rolled = true;
        return child;
    }

    private static int pick(FamiliarData parent, Thread thread, RandomSource random) {
        if (!parent.rolled) return 1;
        return random.nextBoolean() ? parent.alleleA(thread) : parent.alleleB(thread);
    }

    private static void mutateThread(FamiliarData child, Thread thread, RandomSource random, float mutation, float bonus) {
        int i = thread.ordinal();
        if (random.nextFloat() < mutation) {
            int delta = random.nextBoolean() ? 1 : -1;
            if (random.nextBoolean()) child.a[i] = (byte) Mth.clamp(child.alleleA(thread) + delta, 0, MAX);
            else child.b[i] = (byte) Mth.clamp(child.alleleB(thread) + delta, 0, MAX);
        } else if (bonus > 0 && random.nextFloat() < bonus) {
            if (random.nextBoolean()) child.a[i] = (byte) Mth.clamp(child.alleleA(thread) + 1, 0, MAX);
            else child.b[i] = (byte) Mth.clamp(child.alleleB(thread) + 1, 0, MAX);
        }
    }

    private static void mutateMark(FamiliarData child, CreatureProfile profile, RandomSource random, boolean sameOwner) {
        Mark incoming = random.nextFloat() < 0.35F ? speciesMark(profile) : WILD_COMMON[random.nextInt(WILD_COMMON.length)];
        if (incoming == Mark.NONE) incoming = Mark.QUIET_THREAD;
        if (incoming == speciesMark(profile) && (child.markA == incoming || child.markB == incoming)) incoming = WILD_COMMON[random.nextInt(WILD_COMMON.length)];
        if (sameOwner && random.nextFloat() < 0.2F) incoming = Mark.KIN_MARK;
        if (child.markA == Mark.NONE) child.markA = incoming;
        else child.markB = incoming;
    }

    public void apply(LivingEntity entity, CreatureProfile profile) {
        if (!rolled) return;
        boolean full = entity.getHealth() >= entity.getMaxHealth() - 0.05F;
        double health = profile.health * multiplier(Thread.FRAME);
        if (expressed(Mark.DRIFT)) health -= 2;
        double speed = profile.speed * multiplier(Thread.STRIDE);
        if (expressed(Mark.STONEHIDE)) speed *= 0.95;
        if (expressed(Mark.DRIFT)) speed *= 1.08;
        double armor = profile.armor * multiplier(Thread.KEEP);
        if (expressed(Mark.STONEHIDE)) armor += 1;
        if (expressed(Mark.KIN_MARK)) armor += 0.5;
        double damage = profile.damage * multiplier(Thread.FANG);
        setDelta(entity, Attributes.MAX_HEALTH, HEALTH, health - profile.health);
        setDelta(entity, Attributes.MOVEMENT_SPEED, SPEED, speed - profile.speed);
        setDelta(entity, Attributes.ARMOR, ARMOR, armor - profile.armor);
        setDelta(entity, Attributes.ATTACK_DAMAGE, DAMAGE, damage - profile.damage);
        if (expressed(Mark.BRACE) && entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null)
            setDelta(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, 0.4);
        else if (entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null)
            entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(KNOCKBACK);
        if (full || entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
    }

    private static void setDelta(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double delta) {
        var inst = entity.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(id);
        if (Math.abs(delta) > 1.0E-4)
            inst.addOrUpdateTransientModifier(new AttributeModifier(id, delta, AttributeModifier.Operation.ADD_VALUE));
    }

    public List<Component> lensLines(Component name) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("message.tribalpower.lattice.header", name).withStyle(ChatFormatting.AQUA));
        for (Thread thread : Thread.values()) {
            lines.add(Component.literal("  · ").withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.translatable("gui.tribalpower.lattice." + thread.name().toLowerCase()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" " + dots(phenotype(thread))).withStyle(ChatFormatting.AQUA)));
        }
        List<Mark> shown = expressedMarks();
        List<Mark> latent = new ArrayList<>();
        if (markA != Mark.NONE && !shown.contains(markA)) latent.add(markA);
        if (markB != Mark.NONE && markB != markA && !shown.contains(markB)) latent.add(markB);
        if (shown.isEmpty() && latent.isEmpty())
            lines.add(Component.translatable("gui.tribalpower.lattice.marks_none").withStyle(ChatFormatting.DARK_GRAY));
        else {
            if (!shown.isEmpty()) {
                var marks = Component.translatable("gui.tribalpower.lattice.marks").withStyle(ChatFormatting.GRAY);
                for (int i = 0; i < shown.size(); i++) {
                    if (i > 0) marks.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
                    marks.append(Component.translatable("mark.tribalpower." + shown.get(i).id).withStyle(ChatFormatting.GOLD));
                }
                lines.add(Component.literal("  · ").withStyle(ChatFormatting.DARK_AQUA).append(marks));
            }
            for (Mark mark : latent)
                lines.add(Component.literal("  · ").withStyle(ChatFormatting.DARK_AQUA)
                        .append(Component.translatable("gui.tribalpower.lattice.latent", Component.translatable("mark.tribalpower." + mark.id)).withStyle(ChatFormatting.DARK_GRAY)));
        }
        if (sparked) lines.add(Component.translatable("gui.tribalpower.lattice.sparked").withStyle(ChatFormatting.YELLOW));
        return lines;
    }

    public static String dots(int phenotype) {
        int filled = Mth.clamp(phenotype, 0, MAX);
        return "●".repeat(filled) + "○".repeat(MAX - filled);
    }

    /** The sum of all five threads: 5 is a plain wild creature, 25 a perfect Exalted line. */
    public int bloodline() {
        int sum = 0;
        for (Thread thread : Thread.values()) sum += phenotype(thread);
        return sum;
    }

    /**
     * Everything the stat panel shows, packed into one synced int: five 3-bit phenotypes, the two expressed Marks
     * (5 bits each), the generation (capped at 63) and the sparked flag.
     */
    public int pack() {
        int packed = 0;
        for (Thread thread : Thread.values()) packed |= phenotype(thread) << (thread.ordinal() * 3);
        List<Mark> marks = expressedMarks();
        if (marks.size() > 0) packed |= marks.get(0).ordinal() << 15;
        if (marks.size() > 1) packed |= marks.get(1).ordinal() << 20;
        packed |= Math.min(63, generation) << 25;
        if (sparked) packed |= 1 << 31;
        return packed;
    }

    public static int unpackThread(int packed, Thread thread) { return (packed >>> (thread.ordinal() * 3)) & 7; }
    public static Mark unpackMark(int packed, int slot) { return Mark.values()[Math.min(Mark.values().length - 1, (packed >>> (slot == 0 ? 15 : 20)) & 31)]; }
    public static int unpackGeneration(int packed) { return (packed >>> 25) & 63; }
    public static boolean unpackSparked(int packed) { return (packed >>> 31) != 0; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Thread thread : Thread.values())
            tag.putByteArray(cap(thread.name()), new byte[]{a[thread.ordinal()], b[thread.ordinal()]});
        ListTag marks = new ListTag();
        marks.add(StringTag.valueOf(markA.id));
        marks.add(StringTag.valueOf(markB.id));
        tag.put("Marks", marks);
        tag.putInt("Generation", generation);
        tag.putInt("Fray", fray);
        tag.putBoolean("Sparked", sparked);
        return tag;
    }

    public void load(CompoundTag tag) {
        for (Thread thread : Thread.values()) {
            byte[] pair = tag.getByteArray(cap(thread.name()));
            a[thread.ordinal()] = (byte) (pair.length > 0 ? Mth.clamp(pair[0], 0, MAX) : 1);
            b[thread.ordinal()] = (byte) (pair.length > 1 ? Mth.clamp(pair[1], 0, MAX) : 1);
        }
        markA = Mark.NONE;
        markB = Mark.NONE;
        if (tag.contains("Marks", Tag.TAG_LIST)) {
            ListTag marks = tag.getList("Marks", Tag.TAG_STRING);
            if (marks.size() > 0) markA = Mark.fromId(marks.getString(0));
            if (marks.size() > 1) markB = Mark.fromId(marks.getString(1));
        }
        generation = Math.max(0, tag.getInt("Generation"));
        fray = Math.max(0, tag.getInt("Fray"));
        sparked = tag.getBoolean("Sparked");
        rolled = true;
    }

    private static String cap(String name) {
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
