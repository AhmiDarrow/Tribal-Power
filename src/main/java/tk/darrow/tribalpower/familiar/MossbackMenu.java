package tk.darrow.tribalpower.familiar;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.entity.LatticeAnimal;

/** The bonded Mossback's saddlebag (entity NBT {@code Saddlebag}); only its owner may keep it open. Nine slots by default, twelve with Deep Pocket. */
public class MossbackMenu extends AbstractContainerMenu {
    public static final int BASE_SLOTS=FamiliarData.BASE_SADDLEBAG;
    public static final int SLOTS=LatticeAnimal.SADDLEBAG_SLOTS;
    private final Container container;
    private final LatticeAnimal animal;
    private final ContainerData data;
    public MossbackMenu(int id,Inventory inventory) { this(id,inventory,new SimpleContainer(SLOTS),new SimpleContainerData(1),null); }
    public MossbackMenu(int id,Inventory inventory,Container container,LatticeAnimal animal) {
        this(id,inventory,container,new ContainerData() {
            @Override public int get(int index) { return animal!=null && animal.lattice().expressed(FamiliarData.Mark.DEEP_POCKET)?1:0; }
            @Override public void set(int index,int value) {}
            @Override public int getCount() { return 1; }
        },animal);
    }
    private MossbackMenu(int id,Inventory inventory,Container container,ContainerData data,LatticeAnimal animal) {
        super(FamiliarRegistry.SADDLEBAG.get(),id);this.container=container;this.animal=animal;this.data=data;
        checkContainerSize(container,SLOTS);
        checkContainerDataCount(data,1);
        container.startOpen(inventory.player);
        for(int i=0;i<BASE_SLOTS;i++)addSlot(new Slot(container,i,8+i*18,20));
        for(int i=0;i<FamiliarData.EXTRA_SADDLEBAG;i++)addSlot(new ExtraSlot(container,BASE_SLOTS+i,62+i*18,40));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,63+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,121));
        addDataSlots(data);
    }
    @Override public boolean stillValid(Player player) {
        if(animal==null)return true;
        return animal.isAlive() && animal.isBonded() && animal.isOwnedBy(player) && player.distanceToSqr(animal)<=64;
    }
    @Override public void removed(Player player) { super.removed(player);container.stopOpen(player); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        Slot slot=slots.get(index);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();
        if(index<SLOTS) { if(!moveItemStackTo(stack,SLOTS,slots.size(),true))return ItemStack.EMPTY; }
        else if(!moveItemStackTo(stack,0,openSlots(),false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);
        return original;
    }
    private boolean extraOpen() { return data.get(0)!=0; }
    private int openSlots() { return extraOpen()?SLOTS:BASE_SLOTS; }

    private final class ExtraSlot extends Slot {
        ExtraSlot(Container container,int index,int x,int y) { super(container,index,x,y); }
        @Override public boolean mayPlace(ItemStack stack) { return extraOpen(); }
        @Override public boolean isActive() { return extraOpen() || hasItem(); }
    }
}
