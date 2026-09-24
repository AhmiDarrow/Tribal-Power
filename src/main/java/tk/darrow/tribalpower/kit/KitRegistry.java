package tk.darrow.tribalpower.kit;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.gate.DrumPractice;
import tk.darrow.tribalpower.song.PulseCrossbowItem;

/** The camp kit: Vine Lifts, the Songkeeper Drum, Soul Urns, the Pulse Crossbow and reagent dyes. */
public final class KitRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TribalPower.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TribalPower.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TribalPower.MOD_ID);

    public static final DeferredBlock<VineLiftBlock> VINE_LIFT = BLOCKS.register("vine_lift", () -> new VineLiftBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(1.5F).sound(SoundType.MOSS)));
    public static final DeferredBlock<Block> SONGKEEPER_DRUM = BLOCKS.register("songkeeper_drum", () -> new SongkeeperDrumBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredItem<BlockItem> VINE_LIFT_ITEM = ITEMS.registerSimpleBlockItem("vine_lift", VINE_LIFT);
    public static final DeferredItem<BlockItem> SONGKEEPER_DRUM_ITEM = ITEMS.registerSimpleBlockItem("songkeeper_drum", SONGKEEPER_DRUM);
    public static final DeferredItem<PulseCrossbowItem> PULSE_CROSSBOW = ITEMS.register("pulse_crossbow", () -> new PulseCrossbowItem(new Item.Properties()));
    public static final Map<SoulUrnItem.Tier, DeferredItem<SoulUrnItem>> SOUL_URNS = urns();

    public static final DeferredHolder<RecipeSerializer<?>, GrindingRecipe.Serializer> GRINDING =
            SERIALIZERS.register("grinding", GrindingRecipe.Serializer::new);

    private KitRegistry() {}

    private static Map<SoulUrnItem.Tier, DeferredItem<SoulUrnItem>> urns() {
        Map<SoulUrnItem.Tier, DeferredItem<SoulUrnItem>> out = new EnumMap<>(SoulUrnItem.Tier.class);
        for (SoulUrnItem.Tier tier : SoulUrnItem.Tier.values()) {
            Item.Properties properties = new Item.Properties();
            if (tier == SoulUrnItem.Tier.RESONANT) properties = properties.rarity(Rarity.EPIC).fireResistant();
            else if (tier == SoulUrnItem.Tier.MANIFESTED) properties = properties.rarity(Rarity.RARE);
            Item.Properties finalProperties = properties;
            out.put(tier, ITEMS.register(tier.id(), () -> new SoulUrnItem(tier, finalProperties)));
        }
        return java.util.Collections.unmodifiableMap(out);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SERIALIZERS.register(modBus);
        modBus.addListener(DrumPractice::register);
        NeoForge.EVENT_BUS.addListener(VineLifts::jump);
        NeoForge.EVENT_BUS.addListener(VineLifts::tick);
        NeoForge.EVENT_BUS.addListener(VineLifts::loggedOut);
        NeoForge.EVENT_BUS.addListener(DrumPractice::loggedOut);
        NeoForge.EVENT_BUS.addListener(KitRegistry::captureSoul);
    }

    /** A soul urn used on a creature takes it before the creature's own right-click can. */
    public static void captureSoul(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getItemStack().getItem() instanceof SoulUrnItem urn)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!event.getLevel().isClientSide) urn.capture(event.getEntity(), event.getItemStack(), event.getTarget());
    }

    public static void displayItems(CreativeModeTab.Output out) {
        out.accept(VINE_LIFT_ITEM.get());
        out.accept(SONGKEEPER_DRUM_ITEM.get());
        out.accept(PULSE_CROSSBOW.get());
        SOUL_URNS.values().forEach(urn -> out.accept(urn.get()));
    }

    /** The Songkeeper Drum: open it to pick a track and play. */
    public static class SongkeeperDrumBlock extends Block {
        public SongkeeperDrumBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer server) DrumPractice.browse(server, pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                                    net.minecraft.world.phys.shapes.CollisionContext context) {
            return Block.box(2, 0, 2, 14, 13, 14);
        }
    }
}
