package tk.darrow.tribalpower.quest;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import tk.darrow.tribalpower.tribe.TribeDefinition;

/**
 * A tribe's relic: the reward at the end of its story. Carried anywhere in the pack, it keeps that tribe's boon on
 * you whatever your standing, and it is the mark of one who finished the tribe's trial.
 */
public class RelicItem extends Item {
    public final TribeDefinition tribe;

    public RelicItem(TribeDefinition tribe, Properties properties) {
        super(properties.stacksTo(1).fireResistant());
        this.tribe = tribe;
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.relic." + tribe.id() + ".desc").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.tribalpower.relic.boon", Component.translatable("effect.tribalpower." + tribe.id() + "_boon")).withStyle(ChatFormatting.DARK_AQUA));
    }
}
