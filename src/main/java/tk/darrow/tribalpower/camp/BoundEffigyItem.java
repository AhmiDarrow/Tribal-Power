package tk.darrow.tribalpower.camp;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.registries.BuiltInRegistries;
import tk.darrow.tribalpower.blockentity.RitualBrazierBlockEntity;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.api.pulse.Attunement;
import java.util.*;

/** A non-lethal imprint renewed by a three-voice ritual; remaining summons travel with the item. */
public class BoundEffigyItem extends Item {
    public static final int MAX_USES=512;
    public BoundEffigyItem(Properties properties){super(properties);}
    public static Set<String> allowed() {
        Set<String> ids=new HashSet<>(List.of("minecraft:zombie","minecraft:skeleton","minecraft:spider","minecraft:creeper","minecraft:cow","minecraft:sheep","minecraft:pig","minecraft:chicken"));
        for(var profile:tk.darrow.tribalpower.entity.CreatureProfile.values())ids.add("tribalpower:"+profile.id);
        return ids;
    }
    private static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static String target(ItemStack stack){String id=data(stack).getString("BoundSpirit");return allowed().contains(id)?id:"";}
    public static int remaining(ItemStack stack){return stack.getItem() instanceof BoundEffigyItem&&!target(stack).isEmpty()?Math.clamp(data(stack).getInt("Summons"),0,MAX_USES):0;}
    public static void bind(ItemStack stack,String id,int uses){var tag=data(stack);tag.putString("BoundSpirit",allowed().contains(id)?id:"");tag.putInt("Summons",Math.clamp(uses,0,MAX_USES));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    public static void spend(ItemStack stack){bind(stack,target(stack),remaining(stack)-1);}
    public static Component targetName(ItemStack stack){var id=ResourceLocation.tryParse(target(stack));return id==null?Component.translatable("message.tribalpower.effigy.unbound"):BuiltInRegistries.ENTITY_TYPE.get(id).getDescription();}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand) {
        if(!(target instanceof Mob)||!player.isShiftKeyDown())return InteractionResult.PASS;
        String id=BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        if(!allowed().contains(id))return InteractionResult.FAIL;
        if(!player.level().isClientSide) {
            if(remaining(stack)>0){player.displayClientMessage(Component.translatable("message.tribalpower.effigy.still_bound"),true);return InteractionResult.FAIL;}
            bind(stack,id,0);player.displayClientMessage(Component.translatable("message.tribalpower.effigy.imprinted",targetName(stack)),true);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof RitualBrazierBlockEntity brazier))return InteractionResult.PASS;
        Player player=context.getPlayer();if(player==null)return InteractionResult.FAIL;
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        ItemStack effigy=context.getItemInHand();var level=context.getLevel();var pos=context.getClickedPos();
        int cloth=0;for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(ModItems.SPIRITWEAVE.get()))cloth+=player.getInventory().getItem(i).getCount();
        boolean ready=!target(effigy).isEmpty()&&remaining(effigy)==0&&brazier.seal().is(ModItems.SPIRIT_SEAL.get())&&!level.hasNeighborSignal(pos)
                &&LatticeNetwork.hasAttunement(level,pos,8,Attunement.EARTH)&&LatticeNetwork.hasAttunement(level,pos,8,Attunement.AIR)&&LatticeNetwork.hasAttunement(level,pos,8,Attunement.SPIRIT)
                &&cloth>=3&&LatticeNetwork.extractPulseNearby(level,pos,8,200,true)>=200;
        if(!ready){player.displayClientMessage(Component.translatable("message.tribalpower.effigy.need_bind"),true);return InteractionResult.FAIL;}
        LatticeNetwork.extractPulseNearby(level,pos,8,200,false);
        int owed=3;for(int i=0;i<player.getInventory().getContainerSize()&&owed>0;i++){var item=player.getInventory().getItem(i);if(item.is(ModItems.SPIRITWEAVE.get())){int n=Math.min(owed,item.getCount());item.shrink(n);owed-=n;}}
        bind(effigy,target(effigy),MAX_USES);player.getInventory().setChanged();
        CampHooks.award((ServerLevel)level,player.getUUID(),"bind_effigy");
        tk.darrow.tribalpower.effect.SpiritEffects.ring((ServerLevel)level,pos.getCenter(),Attunement.SPIRIT,2,24);
        player.displayClientMessage(Component.translatable("message.tribalpower.effigy.bound",MAX_USES),true);
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        tooltip.add(targetName(stack));tooltip.add(Component.translatable("message.tribalpower.effigy.threads",remaining(stack),MAX_USES));
        tooltip.add(Component.translatable("message.tribalpower.effigy.imprint_hint"));
        tooltip.add(Component.translatable("message.tribalpower.effigy.renew_hint"));
    }
    @Override public boolean isBarVisible(ItemStack stack){return !target(stack).isEmpty();}
    @Override public int getBarWidth(ItemStack stack){return Math.round(13F*remaining(stack)/MAX_USES);}
    @Override public int getBarColor(ItemStack stack){return 0x70DECF;}
}
