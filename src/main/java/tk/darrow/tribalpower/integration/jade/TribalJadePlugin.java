package tk.darrow.tribalpower.integration.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import tk.darrow.tribalpower.api.pulse.PulseGenerator;
import tk.darrow.tribalpower.api.pulse.PulseHandler;
import tk.darrow.tribalpower.api.pulse.PulseRate;
import tk.darrow.tribalpower.camp.identity.CampStanding;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.cuisine.MarchCropBlock;
import tk.darrow.tribalpower.guardian.GuardianAltarBlock;
import tk.darrow.tribalpower.guardian.GuardianAltarBlockEntity;
import tk.darrow.tribalpower.tribe.TribalKinEntity;
import tk.darrow.tribalpower.tribe.TribeRank;

/** Jade: Pulse in a machine, how far a March crop has grown, who a Kin is and where you stand, and whether an altar will answer. */
@WailaPlugin
public class TribalJadePlugin implements IWailaPlugin {
    private static final ResourceLocation PULSE = ResourceLocation.fromNamespaceAndPath("tribalpower", "pulse");
    private static final ResourceLocation CROP = ResourceLocation.fromNamespaceAndPath("tribalpower", "crop");
    private static final ResourceLocation ALTAR = ResourceLocation.fromNamespaceAndPath("tribalpower", "altar");
    private static final ResourceLocation KIN = ResourceLocation.fromNamespaceAndPath("tribalpower", "kin");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PulseData.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(AltarData.INSTANCE, GuardianAltarBlockEntity.class);
        registration.registerEntityDataProvider(KinData.INSTANCE, TribalKinEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Pulse.INSTANCE, Block.class);
        registration.registerBlockComponent(Crop.INSTANCE, MarchCropBlock.class);
        registration.registerBlockComponent(Altar.INSTANCE, GuardianAltarBlock.class);
        registration.registerEntityComponent(Kin.INSTANCE, TribalKinEntity.class);
    }

    /**
     * Pulse machines never sync their store to clients, so the client-side block entity always holds 0.
     * The server reads the store and the rates with the same {@link PulseRate} the Ley Lens uses.
     */
    private enum PulseData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid() { return PULSE; }
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getLevel() instanceof ServerLevel level)) return;
            BlockEntity be = accessor.getBlockEntity();
            var pos = accessor.getPosition();
            if (be instanceof PulseHandler handler && handler.getPulseCapacity() > 0) {
                data.putInt("PulseStored", handler.getPulseStored());
                data.putInt("PulseCapacity", handler.getPulseCapacity());
            }
            int made = PulseRate.perSecond(level, pos, be), draw = PulseRate.drawPerSecond(level, pos, be);
            // A generator reports its rate even when stilled: "0 a second" is the answer the player is after.
            if (made > 0 || be instanceof PulseGenerator) data.putInt("PulseRate", made);
            if (draw > 0) data.putInt("PulseDraw", draw);
            if (be instanceof PulseGenerator generator) data.putString("PulseVoice", generator.voice().getSerializedName());
        }
    }

    private enum Pulse implements IBlockComponentProvider {
        INSTANCE;
        @Override public ResourceLocation getUid() { return PULSE; }
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains("PulseCapacity"))
                tooltip.add(Component.translatable("jade.tribalpower.pulse", data.getInt("PulseStored"), data.getInt("PulseCapacity")).withStyle(ChatFormatting.AQUA));
            if (data.contains("PulseRate"))
                tooltip.add((data.contains("PulseVoice")
                        ? Component.translatable("jade.tribalpower.rate", data.getInt("PulseRate"),
                                Component.translatable("attunement.tribalpower." + data.getString("PulseVoice")))
                        : Component.translatable("jade.tribalpower.rate.plain", data.getInt("PulseRate"))).withStyle(ChatFormatting.GRAY));
            if (data.contains("PulseDraw"))
                tooltip.add(Component.translatable("jade.tribalpower.draw", data.getInt("PulseDraw")).withStyle(ChatFormatting.GRAY));
        }
    }

    private enum Crop implements IBlockComponentProvider {
        INSTANCE;
        @Override public ResourceLocation getUid() { return CROP; }
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!accessor.getBlockState().hasProperty(MarchCropBlock.AGE)) return;
            int age = accessor.getBlockState().getValue(MarchCropBlock.AGE), max = MarchCropBlock.AGE.getPossibleValues().size() - 1;
            tooltip.add(age >= max ? Component.translatable("jade.tribalpower.crop.grown").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("jade.tribalpower.crop", age * 100 / Math.max(1, max)).withStyle(ChatFormatting.GRAY));
        }
    }

    private enum AltarData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid() { return ALTAR; }
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof GuardianAltarBlockEntity altar) || !(accessor.getLevel() instanceof ServerLevel level)) return;
            data.putString("Guardian", altar.guardian().id);
            data.putString("State", !TribalConfig.guardiansEnabled() ? "disabled" : altar.living(level) != null ? "awake"
                    : !altar.onAltar(level) ? "no_altar" : !GuardianAltarBlockEntity.headroom(level, accessor.getPosition(), altar.guardian()) ? "no_room" : "ready");
        }
    }

    private enum Altar implements IBlockComponentProvider {
        INSTANCE;
        @Override public ResourceLocation getUid() { return ALTAR; }
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Guardian")) return;
            var guardian = tk.darrow.tribalpower.guardian.Guardian.byId(data.getString("Guardian"));
            if (guardian == null) return;
            tooltip.add(Component.translatable(guardian.nameKey()).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("jade.tribalpower.altar.wants", TribalConfig.guardianCallCost(), guardian.callItem().getDescription()).withStyle(ChatFormatting.GRAY));
            String state = data.getString("State");
            tooltip.add(Component.translatable("jade.tribalpower.altar." + state).withStyle(state.equals("ready") ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
    }

    private enum KinData implements IServerDataProvider<EntityAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid() { return KIN; }
        @Override
        public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            if (!(accessor.getEntity() instanceof TribalKinEntity kin) || !(accessor.getPlayer() instanceof ServerPlayer player)) return;
            data.putInt("Standing", CampStanding.effectiveStanding(player, kin.tribe()));
        }
    }

    private enum Kin implements IEntityComponentProvider {
        INSTANCE;
        @Override public ResourceLocation getUid() { return KIN; }
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof TribalKinEntity kin)) return;
            tooltip.add(Component.translatable("jade.tribalpower.kin", kin.tribe().displayNameComponent(),
                    Component.translatable("entity.tribalpower.tribal_kin." + kin.role().name().toLowerCase(java.util.Locale.ROOT), "").getString().trim()));
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Standing")) return;
            int standing = data.getInt("Standing");
            tooltip.add(Component.translatable("jade.tribalpower.standing", standing, Component.translatable(TribeRank.of(standing).translationKey())).withStyle(ChatFormatting.AQUA));
        }
    }
}
