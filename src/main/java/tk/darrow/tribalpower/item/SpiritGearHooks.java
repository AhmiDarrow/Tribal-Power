package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/** Voice perks that have to listen to world events instead of the tool's own methods. */
public final class SpiritGearHooks {
    private SpiritGearHooks() {}

    /**
     * Sneak-click with an empty hand while the hood is worn, the same off switch as using the hood itself. The
     * event only fires on the client, and vanilla sends the server nothing for an empty hand in the air, so the
     * client asks for the toggle with a payload.
     */
    public static void toggleWornGoggles(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickEmpty event) {
        if (!event.getLevel().isClientSide()) return;
        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        ItemStack hood = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!SpiritGear.goggles(hood)) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new GogglesTogglePayload());
    }

    /** Pulse and voice must be recorded here: vanilla drops run before {@code Item#mineBlock}. */
    public static void beforeBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack tool = player.getMainHandItem();
        if (!SpiritGear.isTool(tool)) return;
        // Only the blocks an area swing breaks ride on its payment; every other break pays for itself.
        SpiritGear.Swing current = SpiritGear.swingFor(player);
        if (current != null && current.aoe()) return;
        boolean paid = player.getAbilities().instabuild
                || (tool.getItem() instanceof SpiritgearShearsItem && SpiritgearShearsItem.freeTrim(tool))
                || SpiritGear.consumeForMine(player, tool);
        SpiritGear.beginSwing(player, tool, paid, false);
    }

    public static void drops(BlockDropsEvent event) {
        SpiritGear.Swing swing = SpiritGear.SWING.get();
        if (swing == null || !swing.pulsePaid() || event.getBreaker() != swing.player()) return;
        ItemStack tool = swing.tool();
        Attunement voice = SpiritGear.voice(tool).orElse(null);
        if (voice == null) return;
        ServerLevel level = event.getLevel();
        BlockState state = event.getState();

        if (voice == Attunement.FIRE) {
            smelt(event, level);
        } else if (voice == Attunement.WATER && tool.getItem() instanceof SpiritgearPickaxeItem) {
            silk(event, level, tool);
        } else if (voice == Attunement.LOOM && tool.getItem() instanceof SpiritgearShovelItem
                && (state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.SUSPICIOUS_GRAVEL))) {
            silk(event, level, tool);
        } else if (voice == Attunement.WATER && tool.getItem() instanceof SpiritgearShovelItem
                && state.is(BlockTags.DIRT)
                && SpiritGear.chance(level.random, tool, 0.10F)) {
            event.getDrops().add(new ItemEntity(level, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5,
                    event.getPos().getZ() + 0.5, new ItemStack(net.minecraft.world.item.Items.CLAY_BALL)));
        }

        if (voice == Attunement.LOOM && swing.player() != null
                && (tool.getItem() instanceof SpiritgearAxeItem || tool.getItem() instanceof SpiritgearShearsItem)) {
            for (ItemEntity drop : event.getDrops()) {
                drop.setPos(swing.player().getX(), swing.player().getY() + 0.2, swing.player().getZ());
                drop.setPickUpDelay(0);
            }
        }
    }

    private static void smelt(BlockDropsEvent event, ServerLevel level) {
        List<ItemEntity> drops = event.getDrops();
        for (ItemEntity entity : new ArrayList<>(drops)) {
            ItemStack stack = entity.getItem();
            var input = new SingleRecipeInput(stack);
            var cooked = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level)
                    .map(holder -> holder.value().assemble(input, level.registryAccess()))
                    .orElse(ItemStack.EMPTY);
            if (cooked.isEmpty()) continue;
            cooked.setCount(stack.getCount());
            entity.setItem(cooked);
        }
    }

    private static void silk(BlockDropsEvent event, ServerLevel level, ItemStack tool) {
        ItemStack silk = tool.copy();
        level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH)
                .ifPresent(holder -> silk.enchant(holder, 1));
        List<ItemStack> silkDrops = Block.getDrops(event.getState(), level, event.getPos(),
                event.getBlockEntity(), event.getBreaker(), silk);
        event.getDrops().clear();
        for (ItemStack drop : silkDrops) {
            MachineRank.copyToItem(event.getBlockEntity(), drop);
            event.getDrops().add(new ItemEntity(level, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5,
                    event.getPos().getZ() + 0.5, drop));
        }
    }

    /** A Manifested blade heals a tenth of every blow that actually lands (after shields and invulnerability). */
    public static void dealtDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0 || !(event.getSource().getEntity() instanceof Player striker)
                || event.getSource().getDirectEntity() != striker || !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) return;
        ItemStack blade = striker.getMainHandItem();
        if (blade.getItem() instanceof SpiritgearBladeItem && SpiritGear.rank(blade) >= 3) striker.heal(event.getNewDamage() * SpiritGear.LIFESTEAL);
    }

    public static void incomingDamage(LivingIncomingDamageEvent event) {
        // A Manifested blade: bosses take a quarter more, and a tenth of every blow comes back as health.
        if (event.getSource().getEntity() instanceof Player striker && event.getSource().getDirectEntity() == striker
                && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            ItemStack blade = striker.getMainHandItem();
            if (blade.getItem() instanceof SpiritgearBladeItem && SpiritGear.rank(blade) >= 3) {
                if (event.getEntity().getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES))
                    event.setAmount(event.getAmount() * SpiritGear.BOSS_BONUS);
            }
        }
        if (!(event.getEntity() instanceof Player player)) return;
        int set = SpiritGear.setRank(player);
        if (set >= 3) event.setAmount(event.getAmount() * SpiritGear.MANIFESTED_SET);
        else if (set >= 2) event.setAmount(event.getAmount() * SpiritGear.BOUND_SET);
        ItemStack hood = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack robe = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (SpiritGear.voice(hood).orElse(null) == Attunement.EARTH
                && event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                && player.getRandom().nextFloat() < (SpiritGear.rank(hood) >= 3 ? 0.35F : 0.20F)) {
            event.setCanceled(true);
            return;
        }
        if (SpiritGear.voice(boots).orElse(null) == Attunement.FIRE
                && event.getSource().is(DamageTypes.HOT_FLOOR)) {
            event.setCanceled(true);
            return;
        }
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            if (SpiritGear.voice(robe).orElse(null) == Attunement.FIRE) attacker.igniteForSeconds(3);
            if (SpiritGear.voice(robe).orElse(null) == Attunement.SPIRIT)
                attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, 80, 0));
            if (SpiritGear.voice(robe).orElse(null) == Attunement.WATER)
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.REGENERATION,
                        SpiritGear.rank(robe) >= 3 ? 160 : 100, 0, true, false, true));
        }
    }

    public static void knockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack robe = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (SpiritGear.voice(robe).orElse(null) == Attunement.AIR) event.setCanceled(true);
        if (SpiritGear.voice(boots).orElse(null) == Attunement.EARTH) event.setStrength(event.getStrength() * 0.4F);
    }

    public static void fall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (SpiritGear.voice(boots).orElse(null) != Attunement.SPIRIT) return;
        if (SpiritGear.rank(boots) >= 3 && event.getDistance() >= 4) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.55, player.getDeltaMovement().z);
            player.hurtMarked = true;
        }
        event.setCanceled(true);
    }

    public static void trample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof Player player
                && SpiritGear.voice(player.getItemBySlot(EquipmentSlot.FEET)).orElse(null) == Attunement.EARTH) {
            event.setCanceled(true);
            return;
        }
        if (event.getLevel() instanceof net.minecraft.world.level.Level level
                && level.dimension().equals(tk.darrow.tribalpower.world.ModDimensions.THE_MARCH)
                && event.getState().is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
            event.setCanceled(true);
            level.setBlockAndUpdate(event.getPos(), tk.darrow.tribalpower.block.ModBlocks.MARCH_SOIL.get().defaultBlockState());
        }
    }

    private static final String KEPT_HEALTH = "TribalKeptHealth";

    /**
     * Health above 20 from ranked Spiritweave is lost on a relog: the save loads before the armour's bonus comes
     * back, and health is clamped to the plain maximum. Keep it at logout and give it back once the armour is on.
     */
    public static void keepHealth(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.getHealth() > 20) player.getPersistentData().putFloat(KEPT_HEALTH, player.getHealth());
    }

    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount > 5 && player.getPersistentData().contains(KEPT_HEALTH)) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getPersistentData().getFloat(KEPT_HEALTH)));
            player.getPersistentData().remove(KEPT_HEALTH);
        }
        SpiritGear.endSwing();
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        Attunement legVoice = SpiritGear.voice(legs).orElse(null);
        Attunement bootVoice = SpiritGear.voice(boots).orElse(null);
        // The leggings grant their step bonus from their own tick; take it back here once they are no longer worn.
        var step = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
        if (step != null && step.hasModifier(SpiritweaveArmor.STEP)
                && !(legs.getItem() instanceof SpiritweaveArmor && legVoice == Attunement.SPIRIT))
            step.removeModifier(SpiritweaveArmor.STEP);

        if (legVoice == Attunement.EARTH || legVoice == Attunement.LOOM) {
            BlockState feet = player.level().getBlockState(player.blockPosition());
            if (feet.is(Blocks.COBWEB) || feet.is(Blocks.SWEET_BERRY_BUSH)
                    || (legVoice == Attunement.EARTH && (feet.is(Blocks.SOUL_SAND) || feet.is(Blocks.SOUL_SOIL)))) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1.7, 1.0, 1.7));
            }
        }

        if (bootVoice == Attunement.LOOM && player.isShiftKeyDown() && player.zza > 0.1F
                && !player.getCooldowns().isOnCooldown(boots.getItem())
                && player.level() instanceof ServerLevel server) {
            int reach = SpiritGear.rank(boots) >= 3 ? 6 : 4;
            if (stitch(server, player, reach)) player.getCooldowns().addCooldown(boots.getItem(), 160);
        }

        // A whole Manifested set mends its wearer: a heart every four seconds, paid in Pulse.
        if (player.tickCount % 80 == 0 && player.getHealth() < player.getMaxHealth() && SpiritGear.setRank(player) >= 3
                && (player.getAbilities().instabuild || GearCell.spend(player, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST), 4)))
            player.heal(2.0F);

        if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel server
                && player.getMainHandItem().getItem() instanceof SpiritgearPickaxeItem
                && SpiritGear.voice(player.getMainHandItem()).orElse(null) == Attunement.SPIRIT
                && server.getGameTime() % 80 == 0) {
            glintOres(server, serverPlayer, SpiritGear.rank(player.getMainHandItem()) >= 3 ? 10 : 6);
        }
        if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel server
                && player.getMainHandItem().getItem() instanceof SpiritgearShovelItem
                && SpiritGear.voice(player.getMainHandItem()).orElse(null) == Attunement.SPIRIT
                && server.getGameTime() % 80 == 0) {
            glintBuried(server, serverPlayer, 8);
        }
    }

    static boolean stoneLike(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.STONE_ORE_REPLACEABLES) || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(Tags.Blocks.COBBLESTONES) || state.is(ModBlocks.MARCH_STONE.get())
                || state.is(ModBlocks.MARCH_COBBLE.get());
    }

    static boolean dirtLike(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY);
    }

    static void aoe(ServerPlayer player, ItemStack tool, BlockPos center, net.minecraft.core.Direction.Axis axis,
                    java.util.function.Predicate<BlockState> filter) {
        if (SpiritGear.SWING.get() != null && SpiritGear.SWING.get().aoe()) return;
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
            if (a == 0 && b == 0) continue;
            BlockPos pos = switch (axis) {
                case X -> center.offset(0, a, b);
                case Y -> center.offset(a, 0, b);
                case Z -> center.offset(a, b, 0);
            };
            var state = player.level().getBlockState(pos);
            if (!filter.test(state) || !tool.isCorrectToolForDrops(state)
                    || state.getDestroySpeed(player.level(), pos) < 0
                    || !player.level().mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, net.minecraft.core.Direction.UP, tool)) continue;
            SpiritGear.beginSwing(player, tool, true, true);
            try {
                player.gameMode.destroyBlock(pos);
            } finally {
                SpiritGear.beginSwing(player, tool, true, false);
            }
        }
    }

    private static void glintOres(ServerLevel level, ServerPlayer owner, int range) {
        sweep(level, owner, range, 96, 3, 0.35, state -> state.is(Tags.Blocks.ORES));
    }

    private static void glintBuried(ServerLevel level, ServerPlayer owner, int range) {
        sweep(level, owner, range, 32, 4, 0.25, state -> state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.BARREL) || state.is(Blocks.SPAWNER) || state.is(Blocks.TRIAL_SPAWNER));
    }

    /**
     * Mark what the filter finds around the player. The cube is walked a chunk section at a time and any
     * section whose palette cannot hold a match is skipped whole — most of them, underground or not —
     * instead of reading all 9,261 block states of a radius-10 cube every sweep.
     */
    private static void sweep(ServerLevel level, ServerPlayer owner, int range, int cap, int count, double spread,
                              java.util.function.Predicate<BlockState> filter) {
        BlockPos center = owner.blockPosition();
        int minX = center.getX() - range, maxX = center.getX() + range;
        int minZ = center.getZ() - range, maxZ = center.getZ() + range;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - range);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + range);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int marked = 0;
        for (int cx = minX >> 4; cx <= maxX >> 4 && marked < cap; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4 && marked < cap; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                var sections = chunk.getSections();
                for (int sy = minY >> 4; sy <= maxY >> 4 && marked < cap; sy++) {
                    int index = chunk.getSectionIndexFromSectionY(sy);
                    if (index < 0 || index >= sections.length) continue;
                    var section = sections[index];
                    if (section == null || section.hasOnlyAir() || !section.maybeHas(filter)) continue;
                    int baseY = sy << 4;
                    for (int y = Math.max(minY, baseY); y <= Math.min(maxY, baseY + 15) && marked < cap; y++) {
                        for (int x = Math.max(minX, cx << 4); x <= Math.min(maxX, (cx << 4) + 15) && marked < cap; x++) {
                            for (int z = Math.max(minZ, cz << 4); z <= Math.min(maxZ, (cz << 4) + 15) && marked < cap; z++) {
                                if (!filter.test(section.getBlockState(x & 15, y & 15, z & 15))) continue;
                                cursor.set(x, y, z);
                                level.sendParticles(owner, ParticleTypes.END_ROD, true,
                                        cursor.getX() + 0.5, cursor.getY() + 0.5, cursor.getZ() + 0.5,
                                        count, spread, spread, spread, 0);
                                marked++;
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean stitch(ServerLevel server, Player player, int range) {
        net.minecraft.world.phys.Vec3 direction = player.getLookAngle();
        net.minecraft.world.phys.Vec3 flat = new net.minecraft.world.phys.Vec3(direction.x, 0, direction.z);
        if (flat.lengthSqr() < 1.0E-4) flat = net.minecraft.world.phys.Vec3.directionFromRotation(0, player.getYRot());
        flat = flat.normalize();
        for (int reach = range; reach >= 2; reach--) {
            var candidate = player.position().add(flat.scale(reach));
            BlockPos feet = BlockPos.containing(candidate);
            if (!tk.darrow.tribalpower.world.TravelSafety.withinBounds(server, feet)
                    || tk.darrow.tribalpower.world.TravelSafety.hasHazard(server, feet)) continue;
            var box = player.getBoundingBox().move(candidate.subtract(player.position()));
            if (!server.noCollision(player, box)) continue;
            var clip = server.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),
                    candidate.add(0, player.getEyeHeight(), 0), net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            if (clip.getType() != net.minecraft.world.phys.HitResult.Type.MISS) continue;
            var from = player.position();
            player.teleportTo(candidate.x, candidate.y, candidate.z);
            player.fallDistance = 0;
            tk.darrow.tribalpower.effect.SpiritEffects.beam(server, from.add(0, 1, 0), candidate.add(0, 1, 0), Attunement.LOOM);
            return true;
        }
        return false;
    }
}
