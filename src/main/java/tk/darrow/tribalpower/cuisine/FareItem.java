package tk.darrow.tribalpower.cuisine;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.config.TribalConfig;

/** A camp fare: food that says what it is for, drinks when it is a drink, and settles what it cures. */
public class FareItem extends Item {
    public final Fare fare;

    public FareItem(Fare fare, Properties properties) {
        super(properties);
        this.fare = fare;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        if (!level.isClientSide) fare.cures().forEach(eater::removeEffect);
        return super.finishUsingItem(stack, level, eater);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return fare.has(Fare.Trait.DRINK) ? UseAnim.DRINK : super.getUseAnimation(stack);
    }

    @Override
    public SoundEvent getEatingSound() {
        return fare.has(Fare.Trait.DRINK) ? SoundEvents.GENERIC_DRINK : super.getEatingSound();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower." + fare.id() + ".desc").withStyle(ChatFormatting.GRAY));
        if (fare.effect != null && TribalConfig.fareEffectSeconds(fare) > 0)
            lines.add(Component.translatable("item.tribalpower.fare.effect", Component.translatable(fare.effect.value().getDescriptionId()),
                    TribalConfig.fareEffectSeconds(fare)).withStyle(ChatFormatting.BLUE));
        if (!fare.cures().isEmpty())
            lines.add(Component.translatable("item.tribalpower.fare.cures").withStyle(ChatFormatting.BLUE));
        if (fare.has(Fare.Trait.FAST))
            lines.add(Component.translatable("item.tribalpower.fare.fast").withStyle(ChatFormatting.BLUE));
    }
}
