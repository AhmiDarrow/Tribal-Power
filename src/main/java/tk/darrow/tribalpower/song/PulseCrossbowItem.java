package tk.darrow.tribalpower.song;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.item.GearCell;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.SpiritgearHelper;

/**
 * The Pulse Bow's heavier sister. Loading takes longer than a full draw and spends its Pulse up front -- and takes
 * a verse arrow then, if you carry one -- but a loaded crossbow holds its shot for as long as you like, and the bolt
 * flies faster, straighter and hits harder than any bow bolt. The bow is quicker and can loose a half draw.
 */
public class PulseCrossbowItem extends Item {
    public static final String LOADED = "Loaded", VERSE = "LoadedVerse";

    public PulseCrossbowItem(Properties properties) {
        super(properties.stacksTo(1).durability(465));
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
    }

    public static boolean loaded(ItemStack stack) {
        return tag(stack).getBoolean(LOADED);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack crossbow = player.getItemInHand(hand);
        if (loaded(crossbow)) {
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer server) fire(server, crossbow, hand);
            return InteractionResultHolder.sidedSuccess(crossbow, level.isClientSide);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(crossbow);
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.CROSSBOW; }

    @Override
    public void releaseUsing(ItemStack crossbow, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || level.isClientSide) return;
        if (getUseDuration(crossbow, entity) - timeLeft < TribalConfig.crossbowLoadTicks()) return;
        ItemStack arrow = PulseBowItem.findVerse(player);
        int price = TribalConfig.crossbowPulse() + (arrow != null ? PulseBowItem.VERSE_PULSE : 0);
        if (!player.getAbilities().instabuild && price > 0 && !GearCell.spend(player, crossbow, price)) {
            SpiritgearHelper.notifyStarved(player);
            return;
        }
        CompoundTag verse = arrow == null ? null : arrow.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (arrow != null) PulseBowItem.takeVerse(player, arrow);
        CustomData.update(DataComponents.CUSTOM_DATA, crossbow, tag -> {
            tag.putBoolean(LOADED, true);
            if (verse != null) tag.put(VERSE, verse);
            else tag.remove(VERSE);
        });
        crossbow.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    private static void fire(net.minecraft.server.level.ServerPlayer player, ItemStack crossbow, InteractionHand hand) {
        SongVerse verse = null;
        if (tag(crossbow).contains(VERSE)) {
            ItemStack arrow = new ItemStack(ModItems.VERSE_ARROW.get());
            arrow.set(DataComponents.CUSTOM_DATA, CustomData.of(tag(crossbow).getCompound(VERSE)));
            verse = VerseArrowItem.verse(arrow);
        }
        SonicBolt.shoot(player, crossbow, verse, 1.0F, (float) TribalConfig.crossbowVelocity(), TribalConfig.crossbowDamageMultiplier(), 0.05F);
        CustomData.update(DataComponents.CUSTOM_DATA, crossbow, tag -> {
            tag.remove(LOADED);
            tag.remove(VERSE);
        });
        crossbow.remove(DataComponents.CUSTOM_MODEL_DATA);
        crossbow.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.1F);
    }

    /** For the loading animation: how far along a load is. */
    public static float loadProgress(ItemStack stack, @Nullable LivingEntity user) {
        if (user == null || user.getUseItem() != stack) return 0;
        return Math.min(1F, (float) (stack.getUseDuration(user) - user.getUseItemRemainingTicks()) / TribalConfig.crossbowLoadTicks());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.pulse_crossbow.desc", TribalConfig.crossbowPulse()));
        if (loaded(stack)) lines.add(Component.translatable(tag(stack).contains(VERSE)
                ? "item.tribalpower.pulse_crossbow.loaded_verse" : "item.tribalpower.pulse_crossbow.loaded")
                .withStyle(net.minecraft.ChatFormatting.AQUA));
    }
}
