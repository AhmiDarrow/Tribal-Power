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

    /**
     * Runs before the clicked block's own use. Caches, chests and relay plates open their screen on a right-click,
     * and as {@code useOn} the tuner never got that click: it could not mark a cache or chest, and could not bind
     * a plate at all (a crouch skipped the screen but the tuner then refused). Now a plain right-click marks or binds.
     */
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        var level = context.getLevel(); var pos = context.getClickedPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.FAIL;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (level.getBlockEntity(pos) instanceof WirelessRelayBlockEntity relay) {
            if (!tk.darrow.tribalpower.camp.Ownership.check(level, relay.owner(), player)) return InteractionResult.FAIL;
            if (!data.contains("Endpoint")) {
                player.displayClientMessage(Component.translatable("message.tribalpower.tuner.need_mark"), true);
                return InteractionResult.CONSUME;
            }
            BlockPos endpoint = BlockPos.of(data.getLong("Endpoint"));
            String dimension = data.getString("Dimension");
            if (endpoint.equals(relay.host()) && dimension.equals(level.dimension().location().toString())) {
                player.displayClientMessage(Component.translatable("message.tribalpower.tuner.own_host"), true);
                return InteractionResult.CONSUME;
            }
            boolean ok = relay.bind(endpoint, Direction.from3DDataValue(data.getInt("Face")), dimension);
            Component said = !ok ? Component.translatable("message.tribalpower.tuner.range")
                    : relay.channelCount() > 1 ? Component.translatable("message.tribalpower.tuner.linked_channel", relay.selected() + 1)
                    : Component.translatable("message.tribalpower.tuner.linked");
            player.displayClientMessage(said, true);
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
