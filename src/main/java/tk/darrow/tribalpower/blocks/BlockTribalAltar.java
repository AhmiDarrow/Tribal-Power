package tk.darrow.tribalpower.blocks;

import com.sun.org.apache.xerces.internal.impl.xpath.XPath.Axis;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class BlockTribalAltar extends Block {
	
	//private static final AxisAlignedBB BOUNDING_BOX = new AxisAlignedBB(0.0625 * 1, y1, z1, x2, y2, z2)
	
	public BlockTribalAltar() {
		super(Material.IRON);
		setUnlocalizedName(Reference.TribalPowerBlocks.TRIBALALTAR.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerBlocks.TRIBALALTAR.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
		setLightLevel(0.8F);
	}
	
	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}
	
	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

}
