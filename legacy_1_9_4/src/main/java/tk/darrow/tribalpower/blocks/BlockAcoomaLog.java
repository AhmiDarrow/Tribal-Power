package tk.darrow.tribalpower.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import tk.darrow.tribalpower.Reference;
import tk.darrow.tribalpower.TribalPower;

public class BlockAcoomaLog extends Block {

	public BlockAcoomaLog() {
		super(Material.WOOD);
		this.setHardness(2.0F);
		this.setSoundType(SoundType.WOOD);
		setUnlocalizedName(Reference.TribalPowerBlocks.ACOOMALOG.getUnlocalizedName());
		setRegistryName(Reference.TribalPowerBlocks.ACOOMALOG.getRegistryName());
		setCreativeTab(TribalPower.CREATIVE_TAB);
	}

}