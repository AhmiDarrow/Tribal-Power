package tk.darrow.tribalpower.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class BlockChromiteOre extends Block {

	public BlockChromiteOre() {
		super(Material.ROCK);
		this.setHardness(6.0F);
		this.setResistance(5.0F);
		this.setLightLevel(0.3F);
		this.setSoundType(blockSoundType.STONE);
		this.setHarvestLevel("pickaxe", 2);
		this.setCreativeTab(TribalPower.CREATIVE_TAB);
		setUnlocalizedName(Reference.TribalPowerBlocks.CHROMITEORE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerBlocks.CHROMITEORE.getRegistryName());

	}
}
