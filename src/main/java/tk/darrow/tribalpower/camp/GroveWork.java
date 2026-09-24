package tk.darrow.tribalpower.camp;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;

/** Grove Tender: crops, saplings, cane, cactus, wart, cocoa, berries. */
final class GroveWork {
    private GroveWork() {}

    static boolean isSeed(ItemStack stack) {
        if (tk.darrow.tribalpower.compat.AgriCraftCompat.isSeedOrSticks(stack)) return true;
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) return false;
        Block block = blockItem.getBlock();
        return block instanceof CropBlock || block instanceof SaplingBlock || block instanceof StemBlock
                || block instanceof SugarCaneBlock || block instanceof CactusBlock
                || block instanceof NetherWartBlock || block instanceof CocoaBlock
                || block instanceof SweetBerryBushBlock || block instanceof BambooSaplingBlock
                || block instanceof KelpBlock || block instanceof ChorusFlowerBlock;
    }

    static void tend(CampBlockEntity be, ServerLevel server) {
        be.setReason("tending");
        if (be.owner == null) { be.setReason("unclaimed"); return; }
        if (be.pulse < 4) return;
        var farmer = FakePlayerFactory.get(server, new GameProfile(be.owner, be.ownerName));
        if (water(be, server)) return;
        for (int scan = 0; scan < 9; scan++) {
            int n = be.cursor;
            be.cursor = (be.cursor + 1) % 81;
            BlockPos pos = be.getBlockPos().offset(n % 9 - 4, 0, n / 9 - 4);
            if (pos.equals(be.getBlockPos()) || !server.hasChunkAt(pos) || !server.getWorldBorder().isWithinBounds(pos)) continue;
            if (!server.mayInteract(farmer, pos)) continue;
            BlockState state = server.getBlockState(pos);
            if (tk.darrow.tribalpower.compat.AgriCraftCompat.LOADED && agricraft(be, server, farmer, pos, state)) return;
            if (harvest(be, server, farmer, pos, state)) return;
            if (grow(be, server, pos, state)) return;
            if (state.isAir() && plant(be, server, farmer, pos)) return;
        }
        harvestLogs(be, server, farmer);
    }

    /**
     * AgriCraft's crops, when the mod is in the pack: harvest a ripe plant (it stays, cut back), rake weeds, urge
     * an unripe plant, set a seed into empty sticks, and set sticks from the store onto bare soil. True when the
     * tender acted (or was stopped) here.
     */
    private static boolean agricraft(CampBlockEntity be, ServerLevel server, net.minecraft.world.entity.player.Player farmer,
                                     BlockPos pos, BlockState state) {
        if (tk.darrow.tribalpower.compat.AgriCraftCompat.isCrop(server, pos)) {
            List<ItemStack> ripe = tk.darrow.tribalpower.compat.AgriCraftCompat.harvestable(server, pos);
            if (ripe != null) {
                if (be.pulse < 12) return false;
                if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(server, pos, state, farmer)).isCanceled()) { be.setReason("crop_protected"); return false; }
                var preview = be.copyItemsPublic();
                if (!CampBlockEntity.storeDrops(preview, ripe)) { be.setReason("output_full"); return true; }
                tk.darrow.tribalpower.compat.AgriCraftCompat.harvest(server, pos);
                be.replaceItems(preview); be.spendPublic(12); be.active = true; be.setReason("harvested"); return true;
            }
            if (be.pulse >= 4 && tk.darrow.tribalpower.compat.AgriCraftCompat.rake(server, pos)) {
                be.spendPublic(4); be.active = true; be.setReason("raked"); return true;
            }
            if (be.pulse >= 16 && tk.darrow.tribalpower.compat.AgriCraftCompat.urge(server, pos)) {
                be.spendPublic(16); be.active = true; be.setReason("urged"); return true;
            }
            if (be.pulse >= 4) for (int slot = 0; slot < 9; slot++) {
                ItemStack seed = be.getItem(slot);
                if (tk.darrow.tribalpower.compat.AgriCraftCompat.plant(server, pos, seed)) {
                    seed.shrink(1); if (seed.isEmpty()) be.setItem(slot, ItemStack.EMPTY);
                    be.spendPublic(4); be.active = true; be.setReason("planted"); return true;
                }
            }
            return false;
        }
        if (!state.isAir() || be.pulse < 4) return false;
        // a seed goes into empty sticks before it goes into bare soil, so sticks set for it are not wasted
        boolean sticksWaiting = tk.darrow.tribalpower.compat.AgriCraftCompat.emptySticksNear(server, be.getBlockPos(), 4);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack held = be.getItem(slot);
            if (held.isEmpty()) continue;
            var snapshot = BlockSnapshot.create(server.dimension(), server, pos);
            boolean set = tk.darrow.tribalpower.compat.AgriCraftCompat.setSticks(server, pos, held)
                    || (!sticksWaiting && tk.darrow.tribalpower.compat.AgriCraftCompat.plant(server, pos, held));
            if (!set) continue;
            if (EventHooks.onBlockPlace(farmer, snapshot, Direction.UP)) { snapshot.restore(3); be.setReason("plant_protected"); return false; }
            held.shrink(1); if (held.isEmpty()) be.setItem(slot, ItemStack.EMPTY);
            be.spendPublic(4); be.active = true; be.setReason("planted"); return true;
        }
        return false;
    }

    /**
     * A Water totem kept within the tender's reach lets it keep the bed's farmland wet: every dry furrow in the
     * 9-by-9 under the bed is soaked in one beat, for the config's price, whenever any has dried.
     */
    private static boolean water(CampBlockEntity be, ServerLevel server) {
        int cost = tk.darrow.tribalpower.config.TribalConfig.groveWaterCost();
        if (be.pulse < cost) return false;
        boolean water = false;
        for (var totem : tk.darrow.tribalpower.lattice.LatticeNetwork.findNearbyTotems(server, be.getBlockPos(), 8))
            if (totem.getAttunement() == tk.darrow.tribalpower.api.pulse.Attunement.WATER
                    && totem.keeping() != tk.darrow.tribalpower.lattice.Keeping.State.QUIET) { water = true; break; }
        if (!water) return false;
        int wetted = 0;
        BlockPos origin = be.getBlockPos();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -1, -4), origin.offset(4, -1, 4))) {
            if (!server.hasChunkAt(pos)) continue;
            BlockState state = server.getBlockState(pos);
            if (!(state.getBlock() instanceof FarmBlock) || !state.hasProperty(FarmBlock.MOISTURE) || state.getValue(FarmBlock.MOISTURE) >= 7) continue;
            if (server.setBlock(pos, state.setValue(FarmBlock.MOISTURE, 7), 2)) wetted++;
        }
        if (wetted == 0) return false;
        be.spendPublic(cost); be.active = true; be.setReason("watered"); return true;
    }

    private static boolean harvest(CampBlockEntity be, ServerLevel server, net.minecraft.world.entity.player.Player farmer,
                                   BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
            return breakAndReplant(be, server, farmer, pos, state, crop.getStateForAge(0), crop.asItem(), 12);
        }
        if (block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3) {
            return breakAndReplant(be, server, farmer, pos, state, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.asItem(), 12);
        }
        if (block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) >= 2) {
            return breakAndReplant(be, server, farmer, pos, state, state.setValue(CocoaBlock.AGE, 0), ItemsCocoa(), 12);
        }
        if (block instanceof SweetBerryBushBlock && state.getValue(SweetBerryBushBlock.AGE) >= 3) {
            if (be.pulse < 12) return false;
            List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, server, pos, null, farmer, ItemStack.EMPTY));
            var preview = be.copyItemsPublic();
            if (!CampBlockEntity.storeDrops(preview, drops)) { be.setReason("output_full"); return true; }
            if (!server.setBlock(pos, state.setValue(SweetBerryBushBlock.AGE, 1), 3)) return false;
            be.replaceItems(preview); be.spendPublic(12); be.active = true; be.setReason("berries"); return true;
        }
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock || block instanceof BambooStalkBlock) {
            BlockPos top = pos;
            int extra = 0;
            while (server.getBlockState(top.above()).is(block)) { top = top.above(); extra++; }
            int below = 0;
            BlockPos down = pos.below();
            while (server.getBlockState(down).is(block)) { below++; down = down.below(); }
            if (extra + below + 1 >= 3 && extra > 0) return breakLoose(be, server, farmer, top, 12);
        }
        return false;
    }

    private static net.minecraft.world.item.Item ItemsCocoa() {
        return net.minecraft.world.item.Items.COCOA_BEANS;
    }

    private static boolean grow(CampBlockEntity be, ServerLevel server, BlockPos pos, BlockState state) {
        if (be.pulse < 16) return false;
        if (state.getBlock() instanceof BonemealableBlock growable && growable.isValidBonemealTarget(server, pos, state)
                && (state.getBlock() instanceof SaplingBlock || state.getBlock() instanceof StemBlock
                || state.getBlock() instanceof CocoaBlock || state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof SweetBerryBushBlock || state.getBlock() instanceof BambooSaplingBlock)) {
            if (!growable.isBonemealSuccess(server, server.random, pos, state)) return false;
            growable.performBonemeal(server, server.random, pos, state);
            be.spendPublic(16); be.active = true; be.setReason("urged"); return true;
        }
        return false;
    }

    private static boolean plant(CampBlockEntity be, ServerLevel server, net.minecraft.world.entity.player.Player farmer, BlockPos pos) {
        // with AgriCraft: a plain seed it knows (a March crop, a potato) is kept for empty sticks while any wait
        boolean sticksWaiting = tk.darrow.tribalpower.compat.AgriCraftCompat.LOADED
                && tk.darrow.tribalpower.compat.AgriCraftCompat.emptySticksNear(server, be.getBlockPos(), 4);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack seed = be.getItem(slot);
            if (!isSeed(seed)) continue;
            // AgriCraft's seeds and sticks are the compat's to set; planted as blocks they would be bare sticks
            if (tk.darrow.tribalpower.compat.AgriCraftCompat.isSeedOrSticks(seed)) continue;
            if (sticksWaiting && tk.darrow.tribalpower.compat.AgriCraftCompat.knownSeed(seed)) continue;
            Block crop = ((BlockItem) seed.getItem()).getBlock();
            BlockState planted = crop.defaultBlockState();
            if (crop instanceof CropBlock c) planted = c.getStateForAge(0);
            if (crop instanceof CocoaBlock) {
                if (plantCocoa(be, server, farmer, pos, seed, slot)) return true;
                continue;
            }
            if (!planted.canSurvive(server, pos)) continue;
            var snapshot = BlockSnapshot.create(server.dimension(), server, pos);
            if (!server.setBlock(pos, planted, 3)) continue;
            if (EventHooks.onBlockPlace(farmer, snapshot, Direction.UP)) { snapshot.restore(3); be.setReason("plant_protected"); return false; }
            seed.shrink(1); if (seed.isEmpty()) be.setItem(slot, ItemStack.EMPTY); be.spendPublic(4); be.active = true; be.setReason("planted"); return true;
        }
        return false;
    }

    private static boolean plantCocoa(CampBlockEntity be, ServerLevel server,
            net.minecraft.world.entity.player.Player farmer, BlockPos pos, ItemStack seed, int slot) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState planted = Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, dir);
            if (!planted.canSurvive(server, pos)) continue;
            var snapshot = BlockSnapshot.create(server.dimension(), server, pos);
            if (!server.setBlock(pos, planted, 3)) continue;
            if (EventHooks.onBlockPlace(farmer, snapshot, dir)) { snapshot.restore(3); be.setReason("plant_protected"); return false; }
            seed.shrink(1); if (seed.isEmpty()) be.setItem(slot, ItemStack.EMPTY); be.spendPublic(4); be.active = true; be.setReason("planted_cocoa"); return true;
        }
        return false;
    }

    private static void harvestLogs(CampBlockEntity be, ServerLevel server, net.minecraft.world.entity.player.Player farmer) {
        if (be.pulse < 12) return;
        BlockPos origin = be.getBlockPos();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, 0, -4), origin.offset(4, 10, 4))) {
            if (!server.mayInteract(farmer, pos)) continue;
            BlockState state = server.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) continue;
            if (breakLoose(be, server, farmer, pos.immutable(), 12)) return;
        }
    }

    private static boolean breakAndReplant(CampBlockEntity be, ServerLevel server, net.minecraft.world.entity.player.Player farmer,
                                          BlockPos pos, BlockState state, BlockState replant, net.minecraft.world.item.Item seed, int cost) {
        if (be.pulse < cost) return false;
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(server, pos, state, farmer)).isCanceled()) {
            be.setReason("crop_protected"); return false;
        }
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, server, pos, null, farmer, ItemStack.EMPTY));
        boolean reserved = false;
        for (var drop : drops) if (drop.is(seed) && !drop.isEmpty()) { drop.shrink(1); reserved = true; break; }
        var preview = be.copyItemsPublic();
        if (!reserved) for (int i = 0; i < 9; i++) if (!preview.get(i).isEmpty() && preview.get(i).is(seed)) {
            preview.get(i).shrink(1);
            if (preview.get(i).isEmpty()) preview.set(i, ItemStack.EMPTY);
            reserved = true;
            break;
        }
        if (!reserved) { be.setReason("need_seed"); return false; }
        if (!CampBlockEntity.storeDrops(preview, drops)) { be.setReason("output_full"); return true; }
        if (!server.setBlock(pos, replant, 3)) return false;
        be.replaceItems(preview); be.spendPublic(cost); be.active = true; be.setReason("replanted"); return true;
    }

    private static boolean breakLoose(CampBlockEntity be, ServerLevel server, net.minecraft.world.entity.player.Player farmer,
                                      BlockPos pos, int cost) {
        if (!server.mayInteract(farmer, pos)) return false;
        BlockState state = server.getBlockState(pos);
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(server, pos, state, farmer)).isCanceled()) return false;
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, server, pos, null, farmer, ItemStack.EMPTY));
        var preview = be.copyItemsPublic();
        if (!CampBlockEntity.storeDrops(preview, drops)) { be.setReason("output_full"); return true; }
        if (!server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) return false;
        be.replaceItems(preview); be.spendPublic(cost); be.active = true; be.setReason("harvested"); return true;
    }
}
