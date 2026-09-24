package tk.darrow.tribalpower.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.util.List;
import java.util.function.Consumer;

/**
 * AgriCraft, when it is in the pack: the Grove Tender treats its crops as its own. It sets crop sticks from its
 * store onto bare soil, plants AgriCraft seeds into empty sticks (or straight onto soil when AgriCraft allows
 * that), urges growth, rakes weeds, and harvests a mature plant the way a player's click does: the products come
 * off and the plant stays, cut back to its after-harvest stage, sticks and all.
 *
 * <p>Every AgriCraft type lives in {@link Impl}, which is only touched once {@link #LOADED} is true, so a pack
 * without AgriCraft never loads a class that needs it.
 */
public final class AgriCraftCompat {
    public static final String MOD_ID = "agricraft";
    public static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    private AgriCraftCompat() {}

    /** An AgriCraft seed, or a bundle of crop sticks: things the tender's top row may hold. */
    public static boolean isSeedOrSticks(ItemStack stack) {
        return LOADED && !stack.isEmpty() && (Impl.isSeed(stack) || Impl.isSticks(stack));
    }

    /** Whether a block is an AgriCraft crop (sticks, plant or both). */
    public static boolean isCrop(ServerLevel level, BlockPos pos) {
        return LOADED && Impl.crop(level, pos) != null;
    }

    /** Harvest products of a mature plant at {@code pos}, or null when there is nothing ripe there. */
    public static List<ItemStack> harvestable(ServerLevel level, BlockPos pos) {
        return LOADED ? Impl.harvestable(level, pos) : null;
    }

    /** Takes the mature plant's products and cuts it back; call only after {@link #harvestable} said yes. */
    public static void harvest(ServerLevel level, BlockPos pos) {
        if (LOADED) Impl.harvest(level, pos);
    }

    /** Weeds on the sticks, raked off. */
    public static boolean rake(ServerLevel level, BlockPos pos) {
        return LOADED && Impl.rake(level, pos);
    }

    /** A growth tick on a planted, unripe crop: the tender's urging. */
    public static boolean urge(ServerLevel level, BlockPos pos) {
        return LOADED && Impl.urge(level, pos);
    }

    /**
     * Sets a seed from {@code stack} into the crop at {@code pos} (empty sticks), or onto the soil at {@code pos}
     * if AgriCraft allows planting without sticks. Returns true when one seed went in.
     */
    public static boolean plant(ServerLevel level, BlockPos pos, ItemStack stack) {
        return LOADED && Impl.plant(level, pos, stack);
    }

    /** Whether AgriCraft would take this plain item as a seed of a plant it knows (a March crop, a potato): such a seed waits for empty sticks. */
    public static boolean knownSeed(ItemStack stack) {
        return LOADED && !stack.isEmpty() && Impl.knownSeed(stack);
    }

    /** Whether empty crop sticks stand anywhere within {@code radius} of {@code centre} on its level: seeds go there first. */
    public static boolean emptySticksNear(ServerLevel level, BlockPos centre, int radius) {
        return LOADED && Impl.emptySticksNear(level, centre, radius);
    }

    /** Sets a pair of crop sticks from {@code stack} on the soil below {@code pos}. */
    public static boolean setSticks(ServerLevel level, BlockPos pos, ItemStack stack) {
        return LOADED && Impl.setSticks(level, pos, stack);
    }

    /** A seed of the plant with that id (say {@code agricraft:wheat}), for tests and packs; empty when unknown. */
    public static ItemStack seedFor(net.minecraft.resources.ResourceLocation plant) {
        return LOADED ? Impl.seedFor(plant) : ItemStack.EMPTY;
    }

    /** For tests: the seed's species and whether this world's plant registry knows it. */
    public static String describeSeed(ItemStack seed) {
        return LOADED ? Impl.describeSeed(seed) : "no agricraft";
    }

    /** Sets the plant at {@code pos} to its final growth stage. */
    public static boolean ripen(ServerLevel level, BlockPos pos) {
        return LOADED && Impl.ripen(level, pos);
    }

    /** Whether the crop at {@code pos} holds a plant. */
    public static boolean hasPlant(ServerLevel level, BlockPos pos) {
        if (!LOADED) return false;
        var crop = Impl.crop(level, pos);
        return crop != null && crop.hasPlant();
    }

    /** Everything that names an AgriCraft class. */
    private static final class Impl {
        private Impl() {}

        static com.agricraft.agricraft.api.crop.AgriCrop crop(ServerLevel level, BlockPos pos) {
            return com.agricraft.agricraft.api.AgriApi.get().getCrop(level, pos).orElse(null);
        }

        static boolean isSeed(ItemStack stack) {
            return stack.getItem() instanceof com.agricraft.agricraft.common.item.AgriSeedItem
                    && stack.has(com.agricraft.agricraft.common.registry.AgriDataComponents.GENOME.get());
        }

        static boolean knownSeed(ItemStack stack) {
            if (isSeed(stack)) return true;
            return com.agricraft.agricraft.api.AgriApi.get().getGenomeAdapter(stack).flatMap(adapter -> adapter.valueOf(stack)).isPresent();
        }

        static boolean isSticks(ItemStack stack) {
            return stack.getItem() instanceof com.agricraft.agricraft.common.item.CropSticksItem;
        }

        static List<ItemStack> harvestable(ServerLevel level, BlockPos pos) {
            var crop = crop(level, pos);
            if (crop == null || !crop.hasPlant() || !crop.canBeHarvested()) return null;
            List<ItemStack> out = new java.util.ArrayList<>();
            Consumer<ItemStack> add = out::add;
            crop.getHarvestProducts(add);
            return out;
        }

        static void harvest(ServerLevel level, BlockPos pos) {
            var crop = crop(level, pos);
            if (crop == null || !crop.hasPlant()) return;
            crop.setGrowthStage(crop.getPlant().getGrowthStageAfterHarvest());
            crop.getPlant().onHarvest(crop, null);
        }

        static ItemStack seedFor(net.minecraft.resources.ResourceLocation plant) {
            return com.agricraft.agricraft.api.AgriApi.get().getPlant(plant)
                    .map(com.agricraft.agricraft.common.item.AgriSeedItem::toStack).orElse(ItemStack.EMPTY);
        }

        static String describeSeed(ItemStack seed) {
            var genome = seed.get(com.agricraft.agricraft.common.registry.AgriDataComponents.GENOME.get());
            if (genome == null) return "no genome";
            String species = genome.species().trait();
            var id = net.minecraft.resources.ResourceLocation.tryParse(species);
            boolean known = id != null && com.agricraft.agricraft.api.AgriApi.get().getPlant(id).isPresent();
            return species + (known ? " known" : " unknown");
        }

        static boolean ripen(ServerLevel level, BlockPos pos) {
            var crop = crop(level, pos);
            if (crop == null || !crop.hasPlant()) return false;
            int total = crop.getGrowthStage().total();
            crop.setGrowthStage(new com.agricraft.agricraft.api.crop.AgriGrowthStage(total - 1, total));
            return true;
        }

        static boolean rake(ServerLevel level, BlockPos pos) {
            var crop = crop(level, pos);
            if (crop == null || !crop.hasWeeds()) return false;
            crop.removeWeeds();
            return true;
        }

        /** One growth stage, paid for: never a weed tick, never a roll that changes nothing. */
        static boolean urge(ServerLevel level, BlockPos pos) {
            var crop = crop(level, pos);
            if (crop == null || !crop.hasPlant() || crop.isFullyGrown() || !crop.isFertile() || crop.hasWeeds()) return false;
            var stage = crop.getGrowthStage();
            var next = stage.getNext(crop, level.random);
            if (next.index() == stage.index()) return false;
            crop.setGrowthStage(next);
            return true;
        }

        static boolean plant(ServerLevel level, BlockPos pos, ItemStack stack) {
            var genome = isSeed(stack) ? stack.get(com.agricraft.agricraft.common.registry.AgriDataComponents.GENOME.get()) : null;
            // a plain crop item AgriCraft knows as a seed (a March crop, a potato) goes into sticks already set for
            // it; on bare ground it stays a vanilla planting
            if (genome == null && !isSeed(stack) && crop(level, pos) != null)
                genome = com.agricraft.agricraft.api.AgriApi.get().getGenomeAdapter(stack).flatMap(adapter -> adapter.valueOf(stack)).orElse(null);
            if (genome == null) return false;
            // a seed of a plant this world does not know would crash AgriCraft's own planting: leave it in the store
            var species = net.minecraft.resources.ResourceLocation.tryParse(genome.species().trait());
            if (species == null || com.agricraft.agricraft.api.AgriApi.get().getPlant(species, level.registryAccess()).isEmpty()) return false;
            var crop = crop(level, pos);
            if (crop != null) {
                if (crop.hasPlant() || crop.isCrossCropSticks()) return false;
                crop.plantGenome(genome);
                return true;
            }
            // bare soil: AgriCraft may allow a seed straight into the ground
            if (!level.getBlockState(pos).isAir()) return false;
            if (!com.agricraft.agricraft.api.config.AgriCraftConfig.PLANT_OFF_CROP_STICKS.get()) return false;
            if (com.agricraft.agricraft.api.AgriApi.get().getSoil(level, pos.below()).isEmpty()) return false;
            var block = com.agricraft.agricraft.common.registry.AgriBlocks.CROP.get();
            BlockState planted = block.blockStatePlant(block.defaultBlockState());
            if (!planted.canSurvive(level, pos) || !level.setBlock(pos, planted, 3)) return false;
            var set = crop(level, pos);
            if (set == null) return false;
            set.plantGenome(genome);
            return true;
        }

        static boolean emptySticksNear(ServerLevel level, BlockPos centre, int radius) {
            for (BlockPos at : BlockPos.betweenClosed(centre.offset(-radius, 0, -radius), centre.offset(radius, 0, radius))) {
                var crop = crop(level, at);
                if (crop != null && crop.hasCropSticks() && !crop.hasPlant() && !crop.isCrossCropSticks()) return true;
            }
            return false;
        }

        static boolean setSticks(ServerLevel level, BlockPos pos, ItemStack stack) {
            if (!isSticks(stack) || !level.getBlockState(pos).isAir()) return false;
            if (com.agricraft.agricraft.api.AgriApi.get().getSoil(level, pos.below()).isEmpty()) return false;
            var variant = com.agricraft.agricraft.common.block.CropStickVariant.fromItem(stack);
            if (variant == null) return false;
            var block = com.agricraft.agricraft.common.registry.AgriBlocks.CROP.get();
            BlockState sticks = block.blockStateCropStick(block.defaultBlockState(), variant);
            return sticks.canSurvive(level, pos) && level.setBlock(pos, sticks, 3);
        }
    }
}
