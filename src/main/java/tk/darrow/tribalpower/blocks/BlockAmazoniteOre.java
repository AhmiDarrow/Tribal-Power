package tk.darrow.tribalpower.blocks;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.init.ModItems;

public class BlockAmazoniteOre extends Block {

	public BlockAmazoniteOre() {
		super(Material.ROCK);
		this.setHardness(6.0F);
		this.setResistance(5.0F);
		this.setLightLevel(0.3F);
		this.setSoundType(blockSoundType.STONE);
		this.setHarvestLevel("pickaxe", 2);
		this.setCreativeTab(TribalPower.CREATIVE_TAB);
		setUnlocalizedName(Reference.TribalPowerBlocks.AMAZONITEORE.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerBlocks.AMAZONITEORE.getRegistryName());

	}

	@Nullable
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		if (fortune > 3) {
			fortune = 3;
		}

		return rand.nextInt(5 - fortune * 3) == 0 ? ModItems.spiritessence : Item.getItemFromBlock(this);
	}

}
