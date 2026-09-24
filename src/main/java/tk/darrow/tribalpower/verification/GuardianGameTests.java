package tk.darrow.tribalpower.verification;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.finale.AgreementSavedData;
import tk.darrow.tribalpower.finale.NinthAgreement;
import tk.darrow.tribalpower.guardian.Guardian;
import tk.darrow.tribalpower.guardian.GuardianAltarBlockEntity;
import tk.darrow.tribalpower.guardian.GuardianEntity;
import tk.darrow.tribalpower.guardian.GuardianRegistry;
import tk.darrow.tribalpower.quest.QuestRegistry;
import tk.darrow.tribalpower.quest.QuestSavedData;
import tk.darrow.tribalpower.quest.Questline;
import tk.darrow.tribalpower.tribe.TribeDefinition;
import tk.darrow.tribalpower.tribe.TribeRank;

/** The guardians: altar-or-nothing calling, headroom, phases, silent adds, loot, the trial they end, and the finale. */
@GameTestHolder("tribalpower")
@PrefixGameTestTemplate(false)
public final class GuardianGameTests {
    private GuardianGameTests() {}

    private static GuardianAltarBlockEntity altar(GameTestHelper h, BlockPos rel, Guardian guardian) {
        h.setBlock(rel, GuardianRegistry.ALTAR.get());
        var altar = (GuardianAltarBlockEntity) h.getBlockEntity(rel);
        altar.setGuardian(guardian);
        return altar;
    }

    private static void cap(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel.below(), Blocks.POLISHED_DEEPSLATE);
        for (int dx : new int[] {-1, 1}) for (int dz : new int[] {-1, 1}) h.setBlock(rel.offset(dx, 0, dz), Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
    }

    private static List<GuardianEntity> guardians(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(GuardianEntity.class, h.getBounds().inflate(12));
    }

    @GameTest(template = "empty")
    public static void altarCallsOnlyFromItsCircleWithRoomAndTheRightReagent(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            Guardian guardian = Guardian.STAMPEDE_SPIRIT;
            BlockPos rel = new BlockPos(4, 2, 4);
            var altar = altar(h, rel, guardian);
            ServerLevel level = h.getLevel();
            ItemStack call = new ItemStack(guardian.callItem(), TribalConfig.guardianCallCost());
            h.assertTrue(altar.call(level, player, new ItemStack(net.minecraft.world.item.Items.STICK)) != null, "The wrong item is refused");
            h.assertTrue(altar.call(level, player, call) != null && guardians(h).isEmpty(), "No cap, no candles: nothing rises");
            cap(h, rel);
            h.assertTrue(altar.call(level, player, new ItemStack(guardian.callItem(), 1)) != null && guardians(h).isEmpty(), "One reagent is not enough");
            h.setBlock(rel.above(3), Blocks.STONE);
            h.assertTrue(altar.call(level, player, call) != null && guardians(h).isEmpty(), "A ceiling keeps the guardian down");
            h.assertFalse(GuardianAltarBlockEntity.headroom(level, h.absolutePos(rel), guardian), "Headroom sees the ceiling");
            h.setBlock(rel.above(3), Blocks.AIR);
            h.assertTrue(altar.call(level, player, call) == null, "The circle, the room and the reagent: it rises");
            h.assertTrue(guardians(h).size() == 1, "One guardian rose");
            h.assertTrue(altar.living(level) != null && altar.living(level).altar().equals(h.absolutePos(rel)), "The altar knows its guardian and the guardian its altar");
            h.assertTrue(altar.call(level, player, new ItemStack(guardian.callItem(), TribalConfig.guardianCallCost())) != null && guardians(h).size() == 1, "A second call while it stands is refused");
            GuardianEntity entity = guardians(h).get(0);
            h.assertTrue(entity.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES), "Guardians are bosses to everything that asks");
            h.assertTrue(tk.darrow.tribalpower.kit.SoulUrnItem.refusal(entity) != null, "No urn bottles a guardian");
            h.assertTrue(entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) >= 1, "Guardians do not move for a hit");
            entity.discard();
            altar.onGuardianGone();
            h.assertTrue(altar.call(level, player, new ItemStack(guardian.callItem(), TribalConfig.guardianCallCost())) != null && guardians(h).isEmpty(), "The altar rests between calls");
        } finally {
            guardians(h).forEach(g -> g.discard());
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void guardianRousesAtHalfHealthAndItsAddsDropNothing(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        Guardian guardian = Guardian.BOG_MATRIARCH;
        GuardianEntity entity = h.spawn(GuardianRegistry.ENTITIES.get(guardian).get(), new BlockPos(6, 2, 6));
        entity.setTarget(player);
        h.assertTrue(entity.phase() == 1, "Starts in its first phase");
        entity.setHealth(entity.getMaxHealth() * 0.4F);
        h.runAtTickTime(5, () -> {
            h.assertTrue(entity.phase() == 2, "Half health rouses it");
            h.assertTrue(entity.addsAlive() > 0, "Rousing calls its creatures");
            var adds = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, h.getBounds().inflate(12), m -> m.getTags().contains(GuardianEntity.SUMMONED_TAG));
            h.assertTrue(!adds.isEmpty(), "The adds carry the summoned tag");
            adds.forEach(net.minecraft.world.entity.Mob::kill);
        });
        h.runAtTickTime(40, () -> {
            var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(12));
            h.assertTrue(drops.isEmpty(), "Summoned creatures drop nothing, got " + drops.size());
            entity.discard();
            player.remove(Entity.RemovalReason.DISCARDED);
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void everyGuardianDropsItsCoreAndEveryAbilityFires(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            ServerLevel level = h.getLevel();
            for (Guardian guardian : Guardian.values()) {
                GuardianEntity entity = h.spawn(GuardianRegistry.ENTITIES.get(guardian).get(), new BlockPos(4, 2, 4));
                var key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("tribalpower", "entities/" + guardian.id));
                var params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, entity)
                        .withParameter(LootContextParams.ORIGIN, entity.position())
                        .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player))
                        .create(LootContextParamSets.ENTITY);
                var drops = level.getServer().reloadableRegistries().getLootTable(key).getRandomItems(params);
                h.assertTrue(drops.stream().anyMatch(s -> s.is(GuardianRegistry.CORES.get(guardian).get())), guardian.id + " drops its core");
                h.assertTrue(drops.stream().anyMatch(s -> s.is(tk.darrow.tribalpower.item.ModItems.RESONANT_CORE.get())), guardian.id + " drops resonant cores");
                entity.setTarget(player);
                entity.useAbility(level, player);
                h.assertTrue(entity.attackTime() != 0 || guardian.ability == Guardian.Ability.SNARE || guardian.ability == Guardian.Ability.BOLT
                        || guardian.ability == Guardian.Ability.SWOOP, guardian.id + " ability leaves its mark");
                entity.discard();
            }
            // The core is worth Resonant Cores, and all eight make the last tablet.
            var recipes = level.getRecipeManager();
            h.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath("tribalpower", "rite_ninth_agreement")).isPresent(), "The Ninth Agreement tablet has a recipe");
            h.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath("tribalpower", "resonant_cores_from_slag_core")).isPresent(), "A core turns to Resonant Cores");
        } finally {
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aGuardiansFallEndsItsTribesStory(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        Guardian guardian = Guardian.SLAG_TITAN;
        QuestSavedData data = QuestSavedData.get(player.server);
        data.setStep(player.getUUID(), guardian.tribe, Questline.STEPS - 1, 0);
        GuardianEntity entity = h.spawn(GuardianRegistry.ENTITIES.get(guardian).get(), new BlockPos(4, 2, 4));
        player.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(2, 2, 2)));
        h.runAtTickTime(5, () -> entity.hurt(h.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE));
        h.runAtTickTime(60, () -> {
            h.assertTrue(data.step(player.getUUID(), guardian.tribe) == Questline.STEPS, "The trial is the last step");
            h.assertTrue(data.hasRelic(player.getUUID(), guardian.tribe), "The relic is granted");
            h.assertTrue(player.getAdvancements().getOrStartProgress(player.server.getAdvancements().get(
                    ResourceLocation.fromNamespaceAndPath("tribalpower", "march/guardian_" + guardian.id))).isDone(), "The guardian's advancement is awarded");
            player.remove(Entity.RemovalReason.DISCARDED);
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void theNinthAgreementNeedsNineRelicsAndIsKept(GameTestHelper h) {
        var player = VerificationPlayers.inLevel(h);
        try {
            ServerLevel level = h.getLevel();
            h.assertTrue(NinthAgreement.check(level, player) != null, "Without the relics the Loom will not agree");
            h.assertTrue(tk.darrow.tribalpower.camp.identity.CampStanding.effectiveStanding(player, TribeDefinition.CLOCK) < TribeRank.KIN.threshold(), "No floor before the agreement");
            for (TribeDefinition tribe : TribeDefinition.values()) player.getInventory().add(new ItemStack(QuestRegistry.RELICS.get(tribe).get()));
            h.assertTrue(NinthAgreement.check(level, player) == null, "Nine relics carried: the rite may take");
            NinthAgreement.perform(level, h.absolutePos(new BlockPos(4, 2, 4)), player);
            h.assertTrue(AgreementSavedData.get(player.server).agreed(player.getUUID()), "The world remembers the agreement");
            for (TribeDefinition tribe : TribeDefinition.values()) {
                h.assertTrue(tk.darrow.tribalpower.effect.ModEffects.hasBoon(player, tribe), "All nine boons settle: " + tribe.id());
                h.assertTrue(tk.darrow.tribalpower.camp.identity.CampStanding.effectiveStanding(player, tribe) >= TribeRank.KIN.threshold(), "Kin at the least: " + tribe.id());
            }
            h.assertTrue(player.getAdvancements().getOrStartProgress(player.server.getAdvancements().get(
                    ResourceLocation.fromNamespaceAndPath("tribalpower", "march/ninth_agreement"))).isDone(), "The finale's advancement is awarded");
            h.assertTrue(tk.darrow.tribalpower.quest.QuestStatePayload.of(player).agreed(), "The Codex state carries the agreement");
            var saved = AgreementSavedData.get(player.server).save(new net.minecraft.nbt.CompoundTag(), player.server.registryAccess());
            h.assertTrue(AgreementSavedData.load(saved, player.server.registryAccess()).agreed(player.getUUID()), "The agreement survives a save");
        } finally {
            player.remove(Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyGuardianGroundShipsWithItsAltar(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        for (String name : List.of("slag_throne", "drowned_root", "cairn_ring", "singing_fracture", "sealed_gate", "tide_stone", "trampled_ring", "roost")) {
            var template = manager.get(ResourceLocation.fromNamespaceAndPath("tribalpower", name));
            h.assertTrue(template.isPresent(), "Guardian ground template " + name + " exists");
            boolean altar = template.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), GuardianRegistry.ALTAR.get())
                    .stream().anyMatch(info -> info.nbt() != null && Guardian.byId(info.nbt().getString("Guardian")) != null);
            h.assertTrue(altar, name + " holds a Guardian Altar that names its guardian");
        }
        h.succeed();
    }
}
