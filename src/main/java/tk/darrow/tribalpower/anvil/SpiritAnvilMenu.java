package tk.darrow.tribalpower.anvil;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;

/** The vanilla anvil menu under its own type, so the Spirit Anvil's repair applies here and nowhere else. */
public class SpiritAnvilMenu extends AnvilMenu {
    public SpiritAnvilMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public SpiritAnvilMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(id, inventory, access);
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.getBlock() instanceof SpiritAnvilBlock;
    }

    @Override
    public MenuType<?> getType() {
        return SpiritAnvil.MENU.get();
    }
}
