package tk.darrow.tribalpower.blocks;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.init.ModBlocks;
import tk.darrow.tribalpower.init.ModItems;

public class BlockFluoriteOre extends Block {
	
	public BlockFluoriteOre() {
		super(Material.ROCK);
		this.setHardness(6.0F);
		this.setResistance(5.0F);
		this.setLightLevel(0.3F);
		this.setSoundType(blockSoundType.STONE);
		this.setHarvestLevel("pickaxe", 2);
		this.setCreativeTab(TribalPower.CREATIVE_TAB);
		setUnlocalizedName(Reference.TribalPowerBlocks.FLUORITEORE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerBlocks.FLUORITEORE.getRegistryName());

	}
	
	 @Nullable
	 public Item getItemDropped(IBlockState state, Random rand, int fortune)
	    {
	        return ModItems.fluorite;
	    }
	 
	 public int quantityDropped(Random random)
	    {
	        return 2 + random.nextInt(3);
	    }

}
