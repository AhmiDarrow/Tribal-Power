package tk.darrow.tribalpower.camp.identity;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Right-click another player to invite them to your camp (never consumed). Used with no camp it points to
 * {@code /tribalpower camp create}; used on air it reports your camp.
 */
public class CampCharterItem extends Item {
    /** Ticks between invitations from one charter, so a held-down right-click cannot flood the invitee's chat. */
    public static final int INVITE_COOLDOWN=40;
    public CampCharterItem(Properties properties) { super(properties); }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand) {
        if(!(target instanceof Player invitee) || player.getCooldowns().isOnCooldown(this))return InteractionResult.PASS;
        if(player instanceof ServerPlayer holder) { CampCommands.inviteFromCharter(holder,invitee);holder.getCooldowns().addCooldown(this,INVITE_COOLDOWN); }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(player instanceof ServerPlayer holder) {
            var camp=Camps.campOf(holder.server,holder.getUUID());
            holder.displayClientMessage(camp==null?Component.translatable("message.tribalpower.camp.no_camp"):Component.translatable("message.tribalpower.camp.charter_info",camp.name,camp.members.size()),true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.camp_charter.desc"));
    }
}
