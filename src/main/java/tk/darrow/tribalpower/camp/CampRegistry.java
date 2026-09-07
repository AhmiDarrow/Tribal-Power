package tk.darrow.tribalpower.camp;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;
import java.util.*;

public final class CampRegistry {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("tribalpower");
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("tribalpower");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,"tribalpower");
    public static final Map<String,DeferredBlock<CampBlock>> DEVICES=new LinkedHashMap<>();
    static {
        for(String id:List.of("wayanchor","hush_totem","summoning_cradle","grove_tender","spirit_lantern","rain_chime","offering_table")) {
            var block=BLOCKS.register(id,()->new CampBlock(BlockBehaviour.Properties.of().strength(3F).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(s->s.getValue(CampBlock.LIT)?(id.equals("spirit_lantern")?15:5):0)));
            DEVICES.put(id,block);ITEMS.registerSimpleBlockItem(id,block);
        }
    }
    public static final DeferredItem<BoundEffigyItem> EFFIGY=ITEMS.register("binding_effigy",()->new BoundEffigyItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CampBlockEntity>> TYPE=ENTITIES.register("camp_device",()->BlockEntityType.Builder.of(CampBlockEntity::new,DEVICES.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    private CampRegistry(){}
}
