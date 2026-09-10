package tk.darrow.tribalpower.gate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Tying two keystones together, and letting them go (design 3.1 section 8).
 *
 * <p>A Way Gate is a local thing and is linked locally: a Waystone Compass already bound to the other
 * keystone, or chalk drawn from one keystone to the other within {@link GateKeystoneBlockEntity#LINK_RANGE}.
 * A Far Gate crosses dimensions and needs a Gate Sigil, which is the Loom's own handwriting.
 */
public final class GateLinking {
    private GateLinking() {}

    /** Sneak-use a bound Waystone Compass on a keystone to answer the keystone it was bound to. */
    public static boolean linkWithCompass(ItemStack compass, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return true;
        ServerLevel server = (ServerLevel) level;
        CompoundTag data = compass.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ResourceLocation dimension = ResourceLocation.tryParse(data.getString("Dimension"));
        if (!data.contains("Waypoint") || dimension == null) {
            say(player, Component.translatable("message.tribalpower.gate.compass_unbound"), true);
            return false;
        }
        if (!dimension.equals(level.dimension().location())) {
            say(player, Component.translatable("message.tribalpower.gate.local_only"), true);
            return false;
        }
        BlockPos waypoint = BlockPos.of(data.getLong("Waypoint"));
        // The compass binds to the block above the face it was used on, so accept either stone.
        GateKeystoneBlockEntity other = keystoneAt(server, waypoint);
        if (other == null) other = keystoneAt(server, waypoint.below());
        return join(server, pos, other, player, false);
    }

    /** Chalk from keystone to keystone, the same two-click idiom the totems use. */
    public static boolean linkWithChalk(ItemStack chalk, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return true;
        ServerLevel server = (ServerLevel) level;
        CompoundTag data = chalk.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!data.contains("GatePending")) {
            CustomData.update(DataComponents.CUSTOM_DATA, chalk, tag -> tag.putLong("GatePending", pos.asLong()));
            say(player, Component.translatable("message.tribalpower.gate.chalk_mark"), false);
            return true;
        }
        BlockPos pending = BlockPos.of(data.getLong("GatePending"));
        CustomData.update(DataComponents.CUSTOM_DATA, chalk, tag -> tag.remove("GatePending"));
        if (pending.equals(pos)) {
            say(player, Component.translatable("message.tribalpower.gate.chalk_clear"), false);
            return true;
        }
        if (!pending.closerThan(pos, GateKeystoneBlockEntity.LINK_RANGE)) {
            say(player, Component.translatable("message.tribalpower.gate.too_far", GateKeystoneBlockEntity.LINK_RANGE), true);
            return false;
        }
        boolean linked = join(server, pos, keystoneAt(server, pending), player, false);
        if (linked && !player.getAbilities().instabuild) chalk.shrink(1);
        return linked;
    }

    /** A Gate Sigil carries a keystone across dimensions in writing. */
    public static boolean linkWithSigil(ItemStack sigil, ServerLevel level, BlockPos pos, Player player) {
        CompoundTag data = sigil.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!data.contains("GateDimension")) {
            say(player, Component.translatable("message.tribalpower.gate.sigil_blank"), true);
            return false;
        }
        ResourceLocation dimension = ResourceLocation.tryParse(data.getString("GateDimension"));
        if (dimension == null) return false;
        ServerLevel other = level.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension));
        if (other == null) {
            say(player, Component.translatable("message.tribalpower.gate.sigil_lost"), true);
            return false;
        }
        BlockPos written = BlockPos.of(data.getLong("GatePos"));
        // Reading the written end may mean loading it; a player asking for a link may load a chunk.
        other.getChunk(written.getX() >> 4, written.getZ() >> 4);
        return join(level, pos, keystoneAt(other, written), player, true);
    }

    @Nullable
    private static GateKeystoneBlockEntity keystoneAt(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof GateKeystoneBlockEntity keystone
                ? keystone : null;
    }

    /** The shared half of every link: both ends must be real gates, and a Far link must be a Far Gate. */
    private static boolean join(ServerLevel level, BlockPos pos, @Nullable GateKeystoneBlockEntity other,
                                Player player, boolean crossDimension) {
        if (!(level.getBlockEntity(pos) instanceof GateKeystoneBlockEntity self)) return false;
        if (other == null) {
            say(player, Component.translatable("message.tribalpower.gate.no_other"), true);
            return false;
        }
        if (other.getBlockPos().equals(pos) && other.getLevel() == level) {
            say(player, Component.translatable("message.tribalpower.gate.self_link"), true);
            return false;
        }
        ServerLevel otherLevel = (ServerLevel) other.getLevel();
        if (otherLevel == null) return false;
        boolean acrossDimensions = crossDimension && otherLevel != level;
        if (acrossDimensions) {
            if (!tk.darrow.tribalpower.config.TribalConfig.farGatesEnabled()) {
                say(player, Component.translatable("message.tribalpower.gate.far_disabled"), true);
                return false;
            }
            if (self.kind(level) != GateKeystoneBlockEntity.Kind.FAR
                    || other.kind(otherLevel) != GateKeystoneBlockEntity.Kind.FAR) {
                say(player, Component.translatable("message.tribalpower.gate.needs_far"), true);
                return false;
            }
        }
        GateSavedData data = GateSavedData.get(level.getServer());
        GateSavedData.Gate a = self.record(level);
        GateSavedData.Gate b = other.record(otherLevel);
        data.link(a.id(), b.id());
        self.refreshTint(level);
        other.refreshTint(otherLevel);
        say(player, Component.translatable("message.tribalpower.gate.linked", a.name(), b.name()), false);
        return true;
    }

    /**
     * A broken keystone lets its partner go and tells anyone standing near the far end, so a gate that
     * stops answering is never a silent failure.
     */
    public static void onKeystoneBroken(ServerLevel level, GateKeystoneBlockEntity keystone) {
        if (keystone.gateId() == null) return;
        GateSavedData data = GateSavedData.get(level.getServer());
        GateSavedData.Gate self = data.gate(keystone.gateId());
        if (self == null) return;
        GateSavedData.Gate partner = data.remove(self.id());
        if (partner == null) return;
        ServerLevel partnerLevel = level.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                        ResourceLocation.parse(partner.dimension())));
        if (partnerLevel == null) return;
        if (partnerLevel.getBlockEntity(partner.pos()) instanceof GateKeystoneBlockEntity far) {
            far.extinguish(partnerLevel);
            far.refreshTint(partnerLevel);
        }
        Component message = Component.translatable("message.tribalpower.gate.partner_lost", self.name())
                .withStyle(ChatFormatting.RED);
        for (Player nearby : partnerLevel.getEntitiesOfClass(Player.class,
                new AABB(partner.pos()).inflate(16), p -> !p.isSpectator()))
            nearby.displayClientMessage(message, false);
    }

    private static void say(Player player, Component message, boolean bad) {
        if (player == null) return;
        player.displayClientMessage(bad ? message.copy().withStyle(ChatFormatting.RED) : message, true);
    }
}
