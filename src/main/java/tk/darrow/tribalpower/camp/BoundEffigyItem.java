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
    /** The vanilla kinds a cradle can call onto its dry floor: animals, monsters and bats. Water creatures never stand there. */
    private static final Set<MobCategory> VANILLA_KINDS=EnumSet.of(MobCategory.CREATURE,MobCategory.MONSTER,MobCategory.AMBIENT);
    /** Never: the bosses and the boss-like, the trader, and the evoker, whose totems a cradle must not farm. Villagers and golems are MISC. */
    private static final Set<String> VANILLA_NEVER=Set.of("minecraft:ender_dragon","minecraft:wither","minecraft:warden","minecraft:elder_guardian",
            "minecraft:evoker","minecraft:illusioner","minecraft:giant","minecraft:wandering_trader");
    /** Asked for every rendered effigy's bar each frame, so the list is built once, on first use, after the registries are filled. */
    private static final class Allowed {
        static final Set<String> IDS;
        static {
            Set<String> ids=new HashSet<>();
            for(var type:BuiltInRegistries.ENTITY_TYPE){
                var id=BuiltInRegistries.ENTITY_TYPE.getKey(type);
                if(id.getNamespace().equals("minecraft")&&VANILLA_KINDS.contains(type.getCategory())&&!VANILLA_NEVER.contains(id.toString()))ids.add(id.toString());
            }
            // The Codex promises bosses can't be imprinted or called from a Cradle.
            for(var profile:tk.darrow.tribalpower.entity.CreatureProfile.values())if(!profile.boss())ids.add("tribalpower:"+profile.id);
            IDS=Set.copyOf(ids);
        }
    }
    public static Set<String> allowed() { return Allowed.IDS; }
    private static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    /** Read in place (never handed to a writer): the durability bar asks per rendered stack per frame. */
    private static CompoundTag read(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).getUnsafe();}
    public static String target(ItemStack stack){String id=read(stack).getString("BoundSpirit");return allowed().contains(id)?id:"";}
    public static int remaining(ItemStack stack){return stack.getItem() instanceof BoundEffigyItem&&!target(stack).isEmpty()?Math.clamp(read(stack).getInt("Summons"),0,MAX_USES):0;}
    public static void bind(ItemStack stack,String id,int uses){var tag=data(stack);tag.putString("BoundSpirit",allowed().contains(id)?id:"");tag.putInt("Summons",Math.clamp(uses,0,MAX_USES));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    public static void spend(ItemStack stack){bind(stack,target(stack),remaining(stack)-1);}
    public static Component targetName(ItemStack stack){String target=target(stack);var id=target.isEmpty()?null:ResourceLocation.tryParse(target);return id==null||!BuiltInRegistries.ENTITY_TYPE.containsKey(id)?Component.translatable("message.tribalpower.effigy.unbound"):BuiltInRegistries.ENTITY_TYPE.get(id).getDescription();}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand) {
        if(!(target instanceof Mob)||!player.isShiftKeyDown())return InteractionResult.PASS;
        String id=BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        if(!allowed().contains(id)||target.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES))return InteractionResult.FAIL;
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
                &&cloth>=3&&LatticeNetwork.tryExtractPulseNearby(level,pos,8,200);
        if(!ready){player.displayClientMessage(Component.translatable("message.tribalpower.effigy.need_bind"),true);return InteractionResult.FAIL;}
        int owed=3;for(int i=0;i<player.getInventory().getContainerSize()&&owed>0;i++){var item=player.getInventory().getItem(i);if(item.isEmpty()||!item.is(ModItems.SPIRITWEAVE.get()))continue;int n=Math.min(owed,item.getCount());item.shrink(n);if(item.isEmpty())player.getInventory().removeItem(item);owed-=n;}
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
