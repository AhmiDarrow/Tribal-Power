package tk.darrow.tribalpower.lattice;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A machine whose six faces can be set to input, output, both or none from its UI (or sneak-empty-hand).
 */
public interface HasSideIo {
    SideIo sideIo();

    int[] inputSlots(Direction face);

    int[] outputSlots(Direction face);

    static boolean cycle(Player player, BlockEntity be, Direction face) {
        if (!(be instanceof HasSideIo io)) return false;
        if (player.isSpectator() || !player.mayBuild() || face == null) return false;
        if (be instanceof tk.darrow.tribalpower.camp.Ownership.Owned owned
                && !tk.darrow.tribalpower.camp.Ownership.check(player.level(), owned.owner(), player)) return false;
        var mode = io.sideIo().cycle(face);
        be.setChanged();
        if (be.getLevel() instanceof net.minecraft.server.level.ServerLevel server)
            server.sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.tribalpower.io.face",
                face.getSerializedName(), mode.label()), true);
        return true;
    }

    static boolean insert(int slot, ItemStack stack, Direction face, HasSideIo io,
                          java.util.function.BiPredicate<Integer, ItemStack> canPlace) {
        return face != null && io.sideIo().get(face).insert() && canPlace.test(slot, stack);
    }

    static boolean extract(int slot, Direction face, HasSideIo io, java.util.function.IntPredicate output) {
        return face != null && io.sideIo().get(face).extract() && output.test(slot);
    }
}
