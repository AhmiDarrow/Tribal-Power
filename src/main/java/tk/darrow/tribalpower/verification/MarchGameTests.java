package tk.darrow.tribalpower.verification;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.boss.TheUnsungEntity;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.world.structure.LoreTabletBlock;
import tk.darrow.tribalpower.world.structure.MarchRegistry;
import tk.darrow.tribalpower.world.structure.SilentDrumBlockEntity;

/** The March: Silent Drum rhythm, The Unsung's phases and drops, structure templates (design §3). */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public class MarchGameTests {
    private static final List<String> TEMPLATES = List.of(
            "tribe_camp_soil", "tribe_camp_stone", "tribe_camp_sprout", "tribe_camp_claw", "tribe_camp_spark",
            "tribe_camp_clock", "tribe_camp_swarm", "tribe_camp_sigil", "tribe_camp_spindle",
            "ancestor_hall", "drum_circle", "crystal_spire");

    private static SilentDrumBlockEntity drum(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, MarchRegistry.SILENT_DRUM.get());
        return (SilentDrumBlockEntity) h.getBlockEntity(pos);
    }

    private static List<TheUnsungEntity> bosses(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(TheUnsungEntity.class, h.getBounds().inflate(8));
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void silentDrumWakesOnRhythmAndIgnoresWrongOne(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        drum(h, pos);
        // four beats 20 ticks apart: the rhythm lands and The Unsung rises bound to this drum
        for (int i = 0; i < 4; i++) h.runAtTickTime(1 + i * 20L, () -> ((SilentDrumBlockEntity) h.getBlockEntity(pos)).strike(null));
        h.runAtTickTime(70, () -> {
            var alive = bosses(h);
            h.assertTrue(alive.size() == 1, "A correct four-beat must wake exactly one Unsung, found " + alive.size());
            var boss = alive.get(0);
            h.assertTrue(h.absolutePos(pos).equals(boss.drumPos()), "The Unsung must be bound to the drum that woke it");
            h.assertTrue(boss.phase() == TheUnsungEntity.PHASE_BEAT, "A fresh Unsung starts in the Beat phase");
            h.assertTrue(((SilentDrumBlockEntity) h.getBlockEntity(pos)).resting(), "The drum must rest after a wake");
        });
        // a second correct rhythm while it lives is ignored (no second boss)
        for (int i = 0; i < 4; i++) h.runAtTickTime(72 + i * 20L, () -> ((SilentDrumBlockEntity) h.getBlockEntity(pos)).strike(null));
        h.runAtTickTime(160, () -> {
            h.assertTrue(bosses(h).size() == 1, "A second wake while The Unsung lives must be ignored");
            bosses(h).forEach(TheUnsungEntity::discard);
        });
        // a second drum struck with a broken rhythm (gap of 8, then 40 ticks) never completes
        var wrong = new BlockPos(6, 2, 6);
        drum(h, wrong);
        h.runAtTickTime(1, () -> ((SilentDrumBlockEntity) h.getBlockEntity(wrong)).strike(null));
        h.runAtTickTime(21, () -> h.assertTrue(((SilentDrumBlockEntity) h.getBlockEntity(wrong)).strike(null) == 2, "Second beat in time counts"));
        h.runAtTickTime(29, () -> h.assertTrue(((SilentDrumBlockEntity) h.getBlockEntity(wrong)).strike(null) == 0, "A beat 8 ticks after the last resets the rhythm"));
        h.runAtTickTime(69, () -> h.assertTrue(((SilentDrumBlockEntity) h.getBlockEntity(wrong)).strike(null) == 1, "A beat 40 ticks later starts over"));
        h.runAtTickTime(170, () -> {
            h.assertTrue(bosses(h).isEmpty(), "A wrong rhythm must never wake The Unsung");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void bossPhaseTransitionsAtHealthThresholds(GameTestHelper h) {
        h.setBlock(3, 1, 3, Blocks.STONE);
        var boss = h.spawn(MarchRegistry.THE_UNSUNG.get(), new BlockPos(3, 2, 3));
        boss.bindDrum(h.absolutePos(new BlockPos(3, 2, 3)));
        h.assertTrue(boss.getMaxHealth() == 400F, "The Unsung has 400 health");
        h.assertTrue(boss.fireImmune(), "The Unsung is immune to fire");
        h.assertTrue(TheUnsungEntity.phaseFor(400, 400) == TheUnsungEntity.PHASE_BEAT
                && TheUnsungEntity.phaseFor(260, 400) == TheUnsungEntity.PHASE_CHORUS
                && TheUnsungEntity.phaseFor(130, 400) == TheUnsungEntity.PHASE_SILENCE, "Phase thresholds sit at 66% and 33%");
        h.runAtTickTime(5, () -> {
            h.assertTrue(boss.phase() == TheUnsungEntity.PHASE_BEAT, "Full health is the Beat phase");
            boss.setHealth(200F);
        });
        h.runAtTickTime(10, () -> {
            h.assertTrue(boss.phase() == TheUnsungEntity.PHASE_CHORUS, "Below 66% The Unsung enters Chorus, got " + boss.phase());
            h.assertTrue(boss.hurt(h.getLevel().damageSources().magic(), 10F), "In Chorus it can still be hurt");
            boss.setHealth(100F);
        });
        h.runAtTickTime(15, () -> {
            h.assertTrue(boss.phase() == TheUnsungEntity.PHASE_SILENCE, "Below 33% The Unsung falls Silent, got " + boss.phase());
            h.assertTrue(!boss.vulnerable(), "During Silence it is invulnerable");
            h.assertTrue(!boss.hurt(h.getLevel().damageSources().magic(), 10F) && boss.getHealth() == 100F, "Silence must swallow ordinary damage");
            h.assertTrue(boss.resync(), "The four-beat resyncs a silent Unsung");
            h.assertTrue(boss.isStunned() && boss.vulnerable(), "A resynced Unsung is stunned and open");
            h.assertTrue(!boss.resync(), "A second resync during the window is ignored");
            boss.invulnerableTime = 0;
            h.assertTrue(boss.hurt(h.getLevel().damageSources().magic(), 10F), "A stunned Unsung can be hurt");
            h.assertTrue(Math.abs(boss.getHealth() - 80F) < 0.01F, "Stunned it takes double damage, health " + boss.getHealth());
        });
        h.runAtTickTime(20, () -> { boss.discard(); h.succeed(); });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void bossDropsUnsungHeart(GameTestHelper h) {
        h.setBlock(3, 1, 3, Blocks.STONE);
        var boss = h.spawn(MarchRegistry.THE_UNSUNG.get(), new BlockPos(3, 2, 3));
        h.runAtTickTime(5, () -> boss.kill());
        h.runAtTickTime(40, () -> {
            var items = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, h.getBounds().inflate(6));
            int heart = 0, thread = 0, core = 0;
            for (var item : items) {
                var stack = item.getItem();
                if (stack.is(ModItems.UNSUNG_HEART.get())) heart += stack.getCount();
                if (stack.is(ModItems.LOOM_THREAD.get())) thread += stack.getCount();
                if (stack.is(ModItems.RESONANT_CORE.get())) core += stack.getCount();
            }
            h.assertTrue(heart == 1, "The Unsung drops exactly one Unsung Heart, found " + heart);
            h.assertTrue(thread >= 16 && thread <= 24, "The Unsung drops 16–24 Loom Thread, found " + thread);
            h.assertTrue(core == 4, "The Unsung drops four Resonant Cores, found " + core);
            h.assertTrue(bosses(h).isEmpty(), "The Unsung must be gone after death");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void structureTemplatesReferencedByPoolsExist(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var pools = h.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        var structures = h.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        var sets = h.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        for (String name : TEMPLATES) {
            var id = ResourceLocation.fromNamespaceAndPath("tribalpower", name);
            var template = manager.get(id);
            h.assertTrue(template.isPresent(), "Structure template must load: " + id);
            var size = template.get().getSize();
            h.assertTrue(size.getX() > 8 && size.getY() > 4 && size.getZ() > 8, "Template " + name + " must have a real footprint, got " + size);
            h.assertTrue(pools.containsKey(id), "Template pool must exist for " + id);
            h.assertTrue(structures.containsKey(id), "Structure must exist for " + id);
            h.assertTrue(sets.containsKey(id), "Structure set must exist for " + id);
        }
        var drum = manager.get(ResourceLocation.fromNamespaceAndPath("tribalpower", "drum_circle")).orElseThrow();
        boolean hasDrum = drum.filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), MarchRegistry.SILENT_DRUM.get())
                .stream().anyMatch(info -> info.state().is(MarchRegistry.SILENT_DRUM.get()));
        h.assertTrue(hasDrum, "The Drum Circle must carry a Silent Drum at its centre");
        var hall = manager.get(ResourceLocation.fromNamespaceAndPath("tribalpower", "ancestor_hall")).orElseThrow();
        long tablets = hall.filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), MarchRegistry.LORE_TABLET.get()).size();
        h.assertTrue(tablets == 4, "The Ancestor Hall must hang four Lore Tablets, found " + tablets);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void loreTabletRecordsReadsInPersistentData(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(LoreTabletBlock.readCount(player) == 0, "A new player has read no tablets");
        h.assertTrue(LoreTabletBlock.markRead(player, 4), "First read of a tablet is new");
        h.assertTrue(!LoreTabletBlock.markRead(player, 4), "Re-reading the same tablet is not new");
        LoreTabletBlock.markRead(player, 11);
        h.assertTrue(LoreTabletBlock.readCount(player) == 2 && LoreTabletBlock.hasRead(player, 11) && !LoreTabletBlock.hasRead(player, 0),
                "The TribalTabletsRead bitmask must track exactly the tablets read");
        h.assertTrue(player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).getInt(LoreTabletBlock.READ_KEY) == ((1 << 4) | (1 << 11)),
                "Reads are stored under PlayerPersisted/TribalTabletsRead");
        h.succeed();
    }
}
