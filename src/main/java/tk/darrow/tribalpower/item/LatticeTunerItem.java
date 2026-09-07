package tk.darrow.tribalpower.item;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import tk.darrow.tribalpower.blockentity.WirelessRelayBlockEntity;
import java.util.List;

public class LatticeTunerItem extends Item {
    public LatticeTunerItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null || context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        var level = context.getLevel(); var player = context.getPlayer(); var pos = context.getClickedPos(); var stack = context.getItemInHand();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.FAIL;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof WirelessRelayBlockEntity relay && data.contains("Endpoint")) {
            boolean ok = relay.bind(BlockPos.of(data.getLong("Endpoint")), Direction.from3DDataValue(data.getInt("Face")), data.getString("Dimension"));
            player.displayClientMessage(Component.translatable(ok ? "message.tribalpower.tuner.linked" : "message.tribalpower.tuner.range"), true);
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putLong("Endpoint", pos.asLong()); tag.putString("Dimension", level.dimension().location().toString()); tag.putInt("Face", context.getClickedFace().ordinal());
            });
            player.displayClientMessage(Component.translatable("message.tribalpower.tuner.marked"), true);
        }
        return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) { lines.add(Component.translatable("item.tribalpower.lattice_tuner.desc")); }
}
