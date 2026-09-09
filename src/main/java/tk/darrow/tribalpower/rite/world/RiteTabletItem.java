package tk.darrow.tribalpower.rite.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.blockentity.RitualBrazierBlockEntity;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

import java.util.List;

/**
 * Rite Tablet: sneak-use on a Ritual Brazier whose seated seal matches the rite. The brazier draws the
 * rite's Pulse from the lattice within 8 blocks; if there is not enough, or the rite cannot take effect,
 * nothing is consumed.
 */
public class RiteTabletItem extends Item {
    public static final int RADIUS = LatticeNetwork.DEFAULT_RADIUS;
    public static final ResourceLocation FIRST_RITE = ResourceLocation.fromNamespaceAndPath("tribalpower", "first_rite");

    private final WorldRite rite;

    public RiteTabletItem(WorldRite rite, Properties properties) {
        super(properties);
        this.rite = rite;
    }

    public WorldRite rite() { return rite; }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof RitualBrazierBlockEntity)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        Component failure = perform(server, context.getClickedPos(), player, rite);
        if (failure != null) {
            player.displayClientMessage(failure.copy().withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (!player.isCreative()) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    /**
     * Perform a rite at a brazier. Checks the seated seal, probes the lattice, applies the effect, then draws Pulse.
     * @return null on success, otherwise the reason nothing happened (nothing is consumed on failure)
     */
    @Nullable
    public static Component perform(ServerLevel level, BlockPos brazierPos, @Nullable Player player, WorldRite rite) {
        if (!(level.getBlockEntity(brazierPos) instanceof RitualBrazierBlockEntity brazier))
            return Component.translatable("message.tribalpower.rite.no_brazier");
        Attunement seated = RitualBrazierBlockEntity.element(brazier.seal());
        if (seated != rite.element())
            return Component.translatable("message.tribalpower.rite.wrong_seal",
                    Component.translatable("attunement.tribalpower." + rite.element().getSerializedName()));
        if (level.hasNeighborSignal(brazierPos)) return Component.translatable("message.tribalpower.rite.paused");
        int available = LatticeNetwork.extractPulseNearby(level, brazierPos, RADIUS, rite.cost(), true);
        if (available < rite.cost()) return Component.translatable("message.tribalpower.rite.no_pulse", available, rite.cost());
        Component failure = apply(level, brazierPos, rite);
        if (failure != null) return failure;
        LatticeNetwork.extractPulseNearby(level, brazierPos, RADIUS, rite.cost(), false);
        celebrate(level, brazierPos, rite);
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.tribalpower.rite." + rite.key() + ".done").withStyle(ChatFormatting.AQUA), true);
            if (player instanceof ServerPlayer serverPlayer) award(serverPlayer);
        }
        return null;
    }

    /** The rite's world effect. Returns a failure reason when it cannot take effect. */
    @Nullable
    private static Component apply(ServerLevel level, BlockPos pos, WorldRite rite) {
        ServerLevel overworld = level.getServer().overworld();
        switch (rite) {
            case RAIN_CALLING -> overworld.setWeatherParameters(0, rite.durationTicks(), true, false);
            case SKY_CLEARING -> overworld.setWeatherParameters(rite.durationTicks(), 0, false, false);
            case DAWN_CALLING -> {
                if (!level.getGameRules().getBoolean(WorldRiteRegistry.ALLOW_DAWN_RITE))
                    return Component.translatable("message.tribalpower.rite.dawn_disabled");
                long day = overworld.getDayTime();
                overworld.setDayTime((day / 24000L + 1L) * 24000L);
            }
            case GREEN_BLESSING -> GreenBlessing.bless(level, pos, rite.durationTicks());
            case STILL_NIGHT -> TemporaryWards.add(level, pos, 64, rite.durationTicks());
            case LEY_BINDING -> {
                ResonanceTotemBlockEntity first = LeyLines.nearestTotem(level, pos, RADIUS, null);
                if (first == null) return Component.translatable("message.tribalpower.rite.no_totem");
                ResonanceTotemBlockEntity second = LeyLines.nearestTotem(level, first.getBlockPos(), LeyLines.RANGE, first.getBlockPos());
                if (second == null) return Component.translatable("message.tribalpower.rite.no_second_totem", LeyLines.RANGE);
                LeyLines.bind(level, first.getBlockPos(), second.getBlockPos(), rite.durationTicks());
                SpiritEffects.beam(level, first.getBlockPos().getCenter(), second.getBlockPos().getCenter(), Attunement.LOOM);
            }
        }
        return null;
    }

    private static void celebrate(ServerLevel level, BlockPos pos, WorldRite rite) {
        SpiritEffects.ring(level, pos.getCenter().add(0, 0.6, 0), rite.element(), 1.6, 24);
        SpiritEffects.ring(level, pos.getCenter().add(0, 1.4, 0), rite.element(), 0.9, 16);
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.5, 0.6, 0.5, 0.05);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.9F, 1.3F);
    }

    private static void award(ServerPlayer player) {
        var advancement = player.getServer().getAdvancements().get(FIRST_RITE);
        if (advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone())
            player.getAdvancements().award(advancement, "done");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower." + rite.tabletId() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.tribalpower.rite_tablet.use",
                Component.translatable("attunement.tribalpower." + rite.element().getSerializedName()), rite.cost()).withStyle(ChatFormatting.DARK_AQUA));
    }
}
