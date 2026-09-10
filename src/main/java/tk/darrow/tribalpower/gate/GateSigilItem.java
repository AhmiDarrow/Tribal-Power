package tk.darrow.tribalpower.gate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Loom's handwriting (design 3.1 section 8): sneak-use on a keystone to write down where it stands,
 * then use it on a second keystone to tie the two together, both ways.
 */
public class GateSigilItem extends Item {
    public GateSigilItem(Properties properties) {
        super(properties);
    }

    /**
     * Handles a sigil used on a keystone. Sneaking writes this keystone down; otherwise it links the
     * keystone under the hand to whatever the sigil already carries.
     */
    public static boolean useOnKeystone(ItemStack sigil, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return true;
        ServerLevel server = (ServerLevel) level;
        if (!(level.getBlockEntity(pos) instanceof GateKeystoneBlockEntity keystone)) return false;

        if (player.isShiftKeyDown()) {
            GateSavedData.Gate gate = keystone.record(server);
            CustomData.update(DataComponents.CUSTOM_DATA, sigil, tag -> {
                tag.putString("GateDimension", level.dimension().location().toString());
                tag.putLong("GatePos", pos.asLong());
                tag.putString("GateName", gate.name());
            });
            player.displayClientMessage(Component.translatable("message.tribalpower.gate.sigil_written", gate.name()), true);
            return true;
        }
        return GateLinking.linkWithSigil(sigil, server, pos, player);
    }

    /** The gate a sigil is carrying, for the tooltip and for tests. */
    public static String written(ItemStack sigil) {
        CompoundTag data = sigil.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.contains("GateName") ? data.getString("GateName") : "";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.gate_sigil.desc").withStyle(ChatFormatting.GRAY));
        String written = written(stack);
        tooltip.add(written.isEmpty()
                ? Component.translatable("item.tribalpower.gate_sigil.blank").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("item.tribalpower.gate_sigil.carrying", written).withStyle(ChatFormatting.DARK_AQUA));
    }
}
