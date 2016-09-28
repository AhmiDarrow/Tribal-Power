package tk.darrow.tribalpower.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class BlockAmazonite extends Block {

	public BlockAmazonite() {
		super(Material.IRON);
		setUnlocalizedName(Reference.TribalPowerBlocks.AMAZONITEBLOCK.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerBlocks.AMAZONITEBLOCK.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}

}
