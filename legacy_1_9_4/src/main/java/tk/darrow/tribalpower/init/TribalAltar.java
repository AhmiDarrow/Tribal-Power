package tk.darrow.tribalpower.init;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TribalAltar {

	// Basic Alchemy
	@SubscribeEvent
	public void acoomawood(RightClickBlock event) {
		// checking block placement and inventory reqs for
		if (event.getItemStack() != null && event.getWorld().getBlockState(event.getPos()).getBlock() != null) {
			if (event.getWorld().getBlockState(event.getPos()).getBlock().equals(ModBlocks.tribalaltar)
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(Items.STICK, 4, 0))
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(ModItems.spiritessence, 1, 0))) {
				// taking ingredients
				event.getEntityPlayer().inventory.clearMatchingItems(ModItems.spiritessence, 0, 1, null);
				event.getEntityPlayer().inventory.clearMatchingItems(Items.STICK, 0, 4, null);

				event.getEntityPlayer().inventory.addItemStackToInventory(new ItemStack(ModBlocks.acoomalog));

				// Cool Effects
				event.getWorld().spawnParticle(EnumParticleTypes.FLAME, event.getPos().getX(),
						event.getPos().getY(), event.getPos().getZ(), 0, 0, 0, null);

				// cool
				event.getWorld().playSound(event.getEntityPlayer(), event.getPos().getX(), event.getPos().getY(),
						event.getPos().getZ(), SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 0.2F, 3.0F);
			}
		}
	}
	@SubscribeEvent
	public void spiritdiamond(RightClickBlock event) {
		// checking block placement and inventory reqs for
		if (event.getItemStack() != null && event.getWorld().getBlockState(event.getPos()).getBlock() != null) {
			if (event.getWorld().getBlockState(event.getPos()).getBlock().equals(ModBlocks.tribalaltar)
					&& event.getItemStack().getItem().equals(ModItems.tribalseal)
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(Items.DIAMOND, 1, 0))
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(ModItems.spiritessence, 8, 0))) {
				// taking ingredients
				event.getEntityPlayer().inventory.clearMatchingItems(Items.DIAMOND, 0, 1, null);
				event.getEntityPlayer().inventory.clearMatchingItems(ModItems.spiritessence, 0, 8, null);
				
				// giving outcome
				event.getEntityPlayer().inventory.addItemStackToInventory(new ItemStack(ModItems.spiritdiamond));

				// Cool Effects
				event.getWorld().spawnParticle(EnumParticleTypes.FLAME, event.getPos().getX(),
						event.getPos().getY(), event.getPos().getZ(), 0, 0, 0, null);

				// cool
				event.getWorld().playSound(event.getEntityPlayer(), event.getPos().getX(), event.getPos().getY(),
						event.getPos().getZ(), SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 0.2F, 3.0F);
			}
		}
	}
	@SubscribeEvent
	public void ghasttear(RightClickBlock event) {
		// checking block placement and inventory reqs for
		if (event.getItemStack() != null && event.getWorld().getBlockState(event.getPos()).getBlock() != null) {
			if (event.getWorld().getBlockState(event.getPos()).getBlock().equals(ModBlocks.tribalaltar)
					&& event.getItemStack().getItem().equals(ModItems.tribalseal)
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(Items.GOLDEN_CARROT, 1, 0))
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(ModItems.spiritessence, 8, 0))) {
				// taking ingredients
				event.getEntityPlayer().inventory.clearMatchingItems(Items.GOLDEN_CARROT, 0, 1, null);
				event.getEntityPlayer().inventory.clearMatchingItems(ModItems.spiritessence, 0, 8, null);
				
				// giving outcome
				event.getEntityPlayer().inventory.addItemStackToInventory(new ItemStack(Items.GHAST_TEAR));

				// Cool Effects
				event.getWorld().spawnParticle(EnumParticleTypes.FLAME, event.getPos().getX(),
						event.getPos().getY(), event.getPos().getZ(), 0, 0, 0, null);

				// cool
				event.getWorld().playSound(event.getEntityPlayer(), event.getPos().getX(), event.getPos().getY(),
						event.getPos().getZ(), SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 0.2F, 3.0F);
			}
		}
	}
	@SubscribeEvent
	public void witherskull(RightClickBlock event) {
		// checking block placement and inventory reqs for
		if (event.getItemStack() != null && event.getWorld().getBlockState(event.getPos()).getBlock() != null) {
			if (event.getWorld().getBlockState(event.getPos()).getBlock().equals(ModBlocks.tribalaltar)
					&& event.getItemStack().getItem().equals(ModItems.tribalseal)
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(Items.SKULL, 1, 0))
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(ModItems.spiritdiamond, 8, 0))) {
				// taking ingredients
				event.getEntityPlayer().inventory.clearMatchingItems(Items.SKULL, 0, 1, null);
				event.getEntityPlayer().inventory.clearMatchingItems(ModItems.spiritdiamond, 0, 8, null);
				
				// giving outcome
				event.getEntityPlayer().inventory.addItemStackToInventory(new ItemStack(Items.SKULL, 1, 1));

				// Cool Effects
				event.getWorld().spawnParticle(EnumParticleTypes.FLAME, event.getPos().getX(),
						event.getPos().getY(), event.getPos().getZ(), 0, 0, 0, null);

				// cool
				event.getWorld().playSound(event.getEntityPlayer(), event.getPos().getX(), event.getPos().getY(),
						event.getPos().getZ(), SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 0.2F, 3.0F);
			}
		}
	}
	
	

	// Basic Runes
	@SubscribeEvent
	public void firerune(RightClickBlock event) {
		// checking block placement and inventory reqs for
		if (event.getItemStack() != null && event.getWorld().getBlockState(event.getPos()).getBlock() != null) {
			if (event.getWorld().getBlockState(event.getPos()).getBlock().equals(ModBlocks.tribalaltar)
					&& event.getItemStack().getItem().equals(ModItems.tribalseal)
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(ModItems.blankrune, 1, 0))
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(Items.BLAZE_POWDER, 4, 0))
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(Items.BLAZE_ROD, 1, 0))) {
				// taking ingredients
				event.getEntityPlayer().inventory.clearMatchingItems(ModItems.blankrune, 0, 1, null);
				event.getEntityPlayer().inventory.clearMatchingItems(Items.BLAZE_POWDER, 0, 4, null);
				event.getEntityPlayer().inventory.clearMatchingItems(Items.BLAZE_ROD, 0, 1, null);
				// giving outcome
				event.getEntityPlayer().inventory.addItemStackToInventory(new ItemStack(ModItems.firerune));

				// Cool Effects
				event.getWorld().spawnParticle(EnumParticleTypes.FLAME, event.getPos().getX(),
						event.getPos().getY(), event.getPos().getZ(), 0, 0, 0, null);

				// cool
				event.getWorld().playSound(event.getEntityPlayer(), event.getPos().getX(), event.getPos().getY(),
						event.getPos().getZ(), SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 0.2F, 3.0F);
			}
		}
	}

	@SubscribeEvent
	public void airrune(RightClickBlock event) {
		// checking block placement and inventory reqs for
		if (event.getItemStack() != null && event.getWorld().getBlockState(event.getPos()).getBlock() != null) {
			if (event.getWorld().getBlockState(event.getPos()).getBlock().equals(ModBlocks.tribalaltar)
					&& event.getItemStack().getItem().equals(ModItems.tribalseal)
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(ModItems.blankrune, 1, 0))
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(ModItems.airessence, 4, 0))
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(Items.SPECTRAL_ARROW, 1, 0))) {
				// taking ingredients
				event.getEntityPlayer().inventory.clearMatchingItems(ModItems.blankrune, 0, 1, null);
				event.getEntityPlayer().inventory.clearMatchingItems(ModItems.airessence, 0, 4, null);
				event.getEntityPlayer().inventory.clearMatchingItems(Items.SPECTRAL_ARROW, 0, 1, null);
				// giving outcome
				event.getEntityPlayer().inventory.addItemStackToInventory(new ItemStack(ModItems.airrune));

				// Cool Effects
				event.getWorld().spawnParticle(EnumParticleTypes.FLAME, event.getPos().getX(),
						event.getPos().getY(), event.getPos().getZ(), 0, 0, 0, null);

				// cool
				event.getWorld().playSound(event.getEntityPlayer(), event.getPos().getX(), event.getPos().getY(),
						event.getPos().getZ(), SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 0.2F, 3.0F);
			}
		}
	}

	@SubscribeEvent
	public void earthrune(RightClickBlock event) {
		// checking block placement and inventory reqs for
		if (event.getItemStack() != null && event.getWorld().getBlockState(event.getPos()).getBlock() != null) {
			if (event.getWorld().getBlockState(event.getPos()).getBlock().equals(ModBlocks.tribalaltar)
					&& event.getItemStack().getItem().equals(ModItems.tribalseal)
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(ModItems.blankrune, 1, 0))
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(Items.SLIME_BALL, 4, 0))
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(Items.POISONOUS_POTATO, 4, 0))) {
				// taking ingredients
				event.getEntityPlayer().inventory.clearMatchingItems(ModItems.blankrune, 0, 1, null);
				event.getEntityPlayer().inventory.clearMatchingItems(Items.SLIME_BALL, 0, 4, null);
				event.getEntityPlayer().inventory.clearMatchingItems(Items.POISONOUS_POTATO, 0, 1, null);
				// giving outcome
				event.getEntityPlayer().inventory.addItemStackToInventory(new ItemStack(ModItems.earthrune));

				// Cool Effects
				event.getWorld().spawnParticle(EnumParticleTypes.FLAME, event.getPos().getX(),
						event.getPos().getY(), event.getPos().getZ(), 0, 0, 0, null);

				// cool
				event.getWorld().playSound(event.getEntityPlayer(), event.getPos().getX(), event.getPos().getY(),
						event.getPos().getZ(), SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 0.2F, 3.0F);
			}
		}
	}

	@SubscribeEvent
	public void waterrune(RightClickBlock event) {
		// checking block placement and inventory reqs for
		if (event.getItemStack() != null && event.getWorld().getBlockState(event.getPos()).getBlock() != null) {
			if (event.getWorld().getBlockState(event.getPos()).getBlock().equals(ModBlocks.tribalaltar)
					&& event.getItemStack().getItem().equals(ModItems.tribalseal)
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(ModItems.blankrune, 1, 0))
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(Items.PRISMARINE_SHARD, 4, 0))
					&& event.getEntityPlayer().inventory.hasItemStack(new ItemStack(Items.PRISMARINE_CRYSTALS, 4, 0))) {
				// taking ingredients
				event.getEntityPlayer().inventory.clearMatchingItems(ModItems.blankrune, 0, 1, null);
				event.getEntityPlayer().inventory.clearMatchingItems(Items.PRISMARINE_CRYSTALS, 0, 4, null);
				event.getEntityPlayer().inventory.clearMatchingItems(Items.PRISMARINE_SHARD, 0, 4, null);
				// giving outcome
				event.getEntityPlayer().inventory.addItemStackToInventory(new ItemStack(ModItems.waterrune));

				// Cool Effects
				event.getWorld().spawnParticle(EnumParticleTypes.FLAME, event.getPos().getX(),
						event.getPos().getY(), event.getPos().getZ(), 0, 0, 0, null);

				// cool
				event.getWorld().playSound(event.getEntityPlayer(), event.getPos().getX(), event.getPos().getY(),
						event.getPos().getZ(), SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 0.2F, 3.0F);
			}
		}
	}

}
