package tk.darrow.tribalpower.integration.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
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

    private enum Pulse implements IBlockComponentProvider {
        INSTANCE;
        @Override public ResourceLocation getUid() { return PULSE; }
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof PulseHandler handler) || handler.getPulseCapacity() <= 0) return;
            tooltip.add(Component.translatable("jade.tribalpower.pulse", handler.getPulseStored(), handler.getPulseCapacity()).withStyle(ChatFormatting.AQUA));
            if (accessor.getBlockEntity() instanceof PulseGenerator generator)
                tooltip.add(Component.translatable("jade.tribalpower.rate", generator.currentOutput(),
                        Component.translatable("attunement.tribalpower." + generator.voice().getSerializedName())).withStyle(ChatFormatting.GRAY));
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
