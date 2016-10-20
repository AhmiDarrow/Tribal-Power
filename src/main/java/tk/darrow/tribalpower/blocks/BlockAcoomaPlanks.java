package tk.darrow.tribalpower.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class BlockAcoomaPlanks extends Block {

	public BlockAcoomaPlanks() {
		super(Material.WOOD);
		setUnlocalizedName(Reference.TribalPowerBlocks.ACOOMAPLANKS.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerBlocks.ACOOMAPLANKS.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}
}
