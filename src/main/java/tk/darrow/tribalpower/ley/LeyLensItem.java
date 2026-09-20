package tk.darrow.tribalpower.ley;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.blockentity.LeyCollectorBlockEntity;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.blockentity.WirelessRelayBlockEntity;
import tk.darrow.tribalpower.familiar.Familiar;
import tk.darrow.tribalpower.lattice.Keeping;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.List;

/**
 * Ley Lens: held in either hand it shows a HUD and paints the ground. Right-click cycles the sight
 * mode (ley, pulse zone, voices, machines). Sneak-use on a Ley Collector prints that collector's
 * exact factor breakdown. Sneak-use on a familiar prints its threads and Marks.
 */
public class LeyLensItem extends Item {
    public static final int GRID = 8;
    public static final int INTERVAL = 10;
    public static final int MODES = 4;
    public static final int LEY = 0, PULSE = 1, VOICE = 2, MACHINE = 3;
    private static final Vector3f WEAK = new Vector3f(0.25F, 0.45F, 1.0F);
    private static final Vector3f STRONG = new Vector3f(1.0F, 0.82F, 0.25F);
    private static final Vector3f PULSE_COL = new Vector3f(0.45F, 0.85F, 0.78F);
    private static final Vector3f[] VOICE_COL = {
            new Vector3f(0.52F, 0.68F, 0.37F), // earth
            new Vector3f(0.92F, 0.52F, 0.29F), // fire
            new Vector3f(0.37F, 0.72F, 0.85F), // water
            new Vector3f(0.75F, 0.85F, 0.76F), // air
            new Vector3f(0.65F, 0.53F, 0.87F), // spirit
            new Vector3f(0.34F, 0.82F, 0.79F)  // loom
    };

    public LeyLensItem(Properties properties) {
        super(properties);
    }

    public static boolean holding(Entity entity) {
        return entity instanceof net.minecraft.world.entity.LivingEntity living
                && (living.getMainHandItem().getItem() instanceof LeyLensItem || living.getOffhandItem().getItem() instanceof LeyLensItem);
    }

    public static ItemStack held(net.minecraft.world.entity.LivingEntity living) {
        if (living.getMainHandItem().getItem() instanceof LeyLensItem) return living.getMainHandItem();
        if (living.getOffhandItem().getItem() instanceof LeyLensItem) return living.getOffhandItem();
        return ItemStack.EMPTY;
    }

    public static int mode(ItemStack stack) {
        if (stack.isEmpty()) return LEY;
        return Math.floorMod(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("LensMode"), MODES);
    }

    public static void cycle(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("LensMode", (mode(stack) + 1) % MODES));
    }

    public static Vector3f colour(double strength) {
        float t = (float) Math.max(0, Math.min(1, strength));
        return new Vector3f(WEAK).lerp(STRONG, t);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            cycle(stack);
            player.displayClientMessage(Component.translatable("gui.tribalpower.lens.mode." + mode(stack)), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player)) return;
        if (player.getMainHandItem() != stack && player.getOffhandItem() != stack) return;
        if (server.getGameTime() % INTERVAL != 0) return;
        BlockPos origin = player.blockPosition();
        switch (mode(stack)) {
            case PULSE -> paintPulse(server, player, origin);
            case VOICE -> paintVoices(server, player, origin);
            case MACHINE -> paintMachines(server, player, origin);
            default -> paintLey(server, player, origin);
        }
    }

    private static void paintLey(ServerLevel server, ServerPlayer player, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -GRID; dx <= GRID; dx++) {
            for (int dz = -GRID; dz <= GRID; dz++) {
                cursor.set(origin.getX() + dx, origin.getY() + 1, origin.getZ() + dz);
                int ground = groundY(server, cursor, 6);
                if (ground == Integer.MIN_VALUE) continue;
                cursor.setY(ground + 1);
                double strength = LeyMath.glimpse(server, cursor).strength();
                DustParticleOptions dust = new DustParticleOptions(colour(strength), 0.6F + (float) strength * 0.6F);
                server.sendParticles(player, dust, false, cursor.getX() + 0.5, ground + 1.08, cursor.getZ() + 0.5, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Pulse-handling blocks, and an 8-block ring showing the draw/listen zone around you and each core. */
    private static void paintPulse(ServerLevel server, ServerPlayer player, BlockPos origin) {
        int radius = LatticeNetwork.DEFAULT_RADIUS;
        ring(server, player, origin, radius, PULSE_COL);
        int shown = 0;
        for (BlockEntity be : LatticeNetwork.blockEntitiesAround(server, origin, radius)) {
            if (!(be instanceof PulseHandler pulse) || pulse.getPulseCapacity() <= 0) continue;
            BlockPos at = be.getBlockPos();
            float fill = (float) pulse.getPulseStored() / pulse.getPulseCapacity();
            DustParticleOptions core = new DustParticleOptions(new Vector3f(PULSE_COL).lerp(STRONG, fill), 1.1F);
            server.sendParticles(player, core, false, at.getX() + 0.5, at.getY() + 1.15, at.getZ() + 0.5, 2, 0.12, 0.08, 0.12, 0);
            if (shown++ < 4) ring(server, player, at, radius, PULSE_COL);
        }
    }

    private static void paintVoices(ServerLevel server, ServerPlayer player, BlockPos origin) {
        for (ResonanceTotemBlockEntity totem : LatticeNetwork.findNearbyTotems(server, origin, LatticeNetwork.DEFAULT_RADIUS)) {
            int idx = Math.max(0, Math.min(VOICE_COL.length - 1, totem.getAttunement().ordinal()));
            BlockPos pos = totem.getBlockPos();
            Vector3f col = switch (totem.keeping()) {
                case ANSWERED -> VOICE_COL[idx];
                case DIM -> new Vector3f(VOICE_COL[idx]).mul(0.45F);
                case QUIET -> new Vector3f(0.45F, 0.48F, 0.5F);
            };
            float size = totem.keeping() == Keeping.State.ANSWERED ? 1.0F : 0.7F;
            server.sendParticles(player, new DustParticleOptions(col, size), false,
                    pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 3, 0.15, 0.2, 0.15, 0);
        }
    }

    private static void paintMachines(ServerLevel server, ServerPlayer player, BlockPos origin) {
        int r = LatticeNetwork.DEFAULT_RADIUS;
        for (BlockEntity be : LatticeNetwork.blockEntitiesAround(server, origin, r)) {
            Vector3f col = null;
            if (be instanceof tk.darrow.tribalpower.blockentity.EchoStationBlockEntity) col = STRONG;
            else if (be instanceof WirelessRelayBlockEntity) col = PULSE_COL;
            else if (be instanceof tk.darrow.tribalpower.blockentity.AncestralCacheBlockEntity) col = WEAK;
            else if (be instanceof PulseHandler) col = new Vector3f(0.85F, 0.7F, 0.35F);
            if (col == null) continue;
            BlockPos at = be.getBlockPos();
            server.sendParticles(player, new DustParticleOptions(col, 0.95F), false,
                    at.getX() + 0.5, at.getY() + 1.1, at.getZ() + 0.5, 1, 0.1, 0.05, 0.1, 0);
        }
    }

    private static void ring(ServerLevel server, ServerPlayer player, BlockPos center, int radius, Vector3f colour) {
        DustParticleOptions dust = new DustParticleOptions(colour, 0.55F);
        for (int i = 0; i < 16; i++) {
            double a = i * (Math.PI * 2 / 16);
            int x = center.getX() + (int) Math.round(Math.cos(a) * radius);
            int z = center.getZ() + (int) Math.round(Math.sin(a) * radius);
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, center.getY() + 1, z);
            int ground = groundY(server, at, 8);
            if (ground == Integer.MIN_VALUE) continue;
            server.sendParticles(player, dust, false, x + 0.5, ground + 1.06, z + 0.5, 1, 0, 0, 0, 0);
        }
    }

    /** Y of the first block with a solid top below {@code from}, scanning at most {@code depth} blocks; MIN_VALUE if none. */
    private static int groundY(Level level, BlockPos from, int depth) {
        BlockPos.MutableBlockPos cursor = from.mutable();
        for (int i = 0; i <= depth; i++) {
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && !state.getCollisionShape(level, cursor).isEmpty()) return cursor.getY();
            cursor.move(0, -1, 0);
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Familiar familiar) || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (player.level() instanceof ServerLevel) {
            if (target instanceof tk.darrow.tribalpower.entity.LatticeAnimal animal)
                animal.ensureLattice(animal.getRandom(), player.level().dimension().equals(tk.darrow.tribalpower.world.ModDimensions.THE_MARCH));
            else if (target instanceof tk.darrow.tribalpower.entity.LatticeMonster monster)
                monster.ensureLattice(monster.getRandom(), player.level().dimension().equals(tk.darrow.tribalpower.world.ModDimensions.THE_MARCH));
            for (Component line : familiar.lattice().lensLines(familiar.asMob().getDisplayName())) player.sendSystemMessage(line);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof LeyCollectorBlockEntity collector)) return InteractionResult.PASS;
        if (context.getLevel() instanceof ServerLevel server) {
            BlockPos pos = context.getClickedPos();
            player.sendSystemMessage(Component.translatable("ley.tribalpower.header", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA));
            for (Component line : LeyMath.breakdown(server, pos))
                player.sendSystemMessage(Component.literal("  · ").withStyle(ChatFormatting.DARK_AQUA).append(line.copy().withStyle(ChatFormatting.GRAY)));
            player.sendSystemMessage(Component.literal("  · ").withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.translatable("ley.tribalpower.live", collector.currentBeat(server, pos), LeyMath.MAX_GAIN).withStyle(ChatFormatting.GRAY)));
            player.sendSystemMessage(Component.literal("  · ").withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.translatable("diag.tribalpower.stored", collector.getPulseStored(), collector.getPulseCapacity()).withStyle(ChatFormatting.GRAY)));
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.ley_lens.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gui.tribalpower.lens.mode." + mode(stack)).withStyle(ChatFormatting.AQUA));
    }
}
