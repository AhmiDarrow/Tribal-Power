package tk.darrow.tribalpower.healing;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A brewed remedy in one of its three forms. A tincture is drunk; a salve is laid on yourself (use) or on someone
 * else (use on them); incense is seated in a Ritual Brazier and burned for everyone around it.
 */
public class RemedyItem extends Item {
    private final Remedies.Form form;

    public RemedyItem(Remedies.Form form, Properties properties) {
        super(properties);
        this.form = form;
    }

    public Remedies.Form form() {
        return form;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.tribalpower.spirit_" + form.id() + ".named",
                Component.translatable("remedy.tribalpower." + Remedies.remedy(stack).id()));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return form == Remedies.Form.TINCTURE ? UseAnim.DRINK : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return form == Remedies.Form.TINCTURE ? 32 : 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return switch (form) {
            case TINCTURE -> ItemUtils.startUsingInstantly(level, player, hand);
            case SALVE -> {
                if (!level.isClientSide) {
                    Remedies.apply(player, stack);
                    level.playSound(null, player.blockPosition(), SoundEvents.HONEY_BLOCK_PLACE, SoundSource.PLAYERS, 0.8F, 1.2F);
                    stack.consume(1, player);
                }
                yield InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            case INCENSE -> InteractionResultHolder.pass(stack);
        };
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (form != Remedies.Form.TINCTURE) return stack;
        if (!level.isClientSide) Remedies.drink(entity, stack);
        if (entity instanceof Player player && player.getAbilities().instabuild) return stack;
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        stack.shrink(1);
        if (stack.isEmpty()) return bottle;
        if (entity instanceof Player player && !player.getInventory().add(bottle)) player.drop(bottle, false);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Remedy remedy = Remedies.remedy(stack);
        lines.add(Component.translatable("remedy.tribalpower." + remedy.id() + ".desc").withStyle(ChatFormatting.GRAY));
        Remedies.reagent(stack).ifPresent(profile -> lines.add(Component.translatable("item.tribalpower.remedy.reagent",
                Component.translatable("item.tribalpower." + profile.reagent)).withStyle(ChatFormatting.DARK_GRAY)));
        var voice = Remedies.voice(stack);
        if (voice != null) lines.add(Component.translatable("item.tribalpower.remedy.voice." + voice.getSerializedName()).withStyle(ChatFormatting.DARK_AQUA));
        lines.add(Component.translatable("item.tribalpower.spirit_" + form.id() + ".use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
