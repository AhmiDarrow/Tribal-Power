package tk.darrow.tribalpower.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import tk.darrow.tribalpower.block.PulseLightBlock;

import java.util.List;

public class PulseLightBlockItem extends BlockItem {
    public PulseLightBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        int cost = getBlock() instanceof PulseLightBlock light ? light.kind().cost : 1;
        lines.add(Component.translatable("block.tribalpower.pulse_light.desc", cost));
    }
}
