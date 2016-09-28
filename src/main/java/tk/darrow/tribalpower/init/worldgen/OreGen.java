package tk.darrow.tribalpower.init.worldgen;

import java.util.Random;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import tk.darrow.tribalpower.handlers.ConfigHandler;
import tk.darrow.tribalpower.init.ModBlocks;

public class OreGen implements IWorldGenerator{
	
	
	
	
	

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator,
			IChunkProvider chunkProvider) {
		switch (world.provider.getDimension()) {
        case -1:
            break;
        case 0:
            amazoniteore(world, random, chunkX * 16, chunkZ * 16);
            fluoriteore(world, random, chunkX * 16, chunkZ * 16);
            chromiteore(world, random, chunkX * 16, chunkZ * 16);
            break;
        case 1:
            break;
            }
		}
	
	private void amazoniteore(World world, Random rand, int chunkX, int chunkZ) {
        for (int k = 0; k < 16; k++)
        {
            int firstBlockXCoord = chunkX + rand.nextInt(16);
            int firstBlockZCoord = chunkZ + rand.nextInt(16);
            //Will be found between y = 0 and y = NextInt()
            int quisqueY = rand.nextInt(50);
            BlockPos quisquePos = new BlockPos(firstBlockXCoord, quisqueY, firstBlockZCoord);
            if (ConfigHandler.enableAmazoniteGeneration)
                //The 10 as the second parameter sets the maximum vein size
                (new WorldGenMinable(ModBlocks.amazoniteore.getDefaultState(), 12)).generate(world, rand, quisquePos);
        }
    }
	
	private void fluoriteore(World world, Random rand, int chunkX, int chunkZ) {
        for (int k = 0; k < 16; k++)
        {
            int firstBlockXCoord = chunkX + rand.nextInt(16);
            int firstBlockZCoord = chunkZ + rand.nextInt(16);
            //Will be found between y = 0 and y = NextInt()
            int quisqueY = rand.nextInt(50);
            BlockPos quisquePos = new BlockPos(firstBlockXCoord, quisqueY, firstBlockZCoord);
            if (ConfigHandler.enableFluoriteGeneration)
                //The 10 as the second parameter sets the maximum vein size
                (new WorldGenMinable(ModBlocks.fluoriteore.getDefaultState(), 12)).generate(world, rand, quisquePos);
        }
    }
	
	private void chromiteore(World world, Random rand, int chunkX, int chunkZ) {
        for (int k = 0; k < 16; k++)
        {
            int firstBlockXCoord = chunkX + rand.nextInt(16);
            int firstBlockZCoord = chunkZ + rand.nextInt(16);
            //Will be found between y = 0 and y = NextInt()
            int quisqueY = rand.nextInt(50);
            BlockPos quisquePos = new BlockPos(firstBlockXCoord, quisqueY, firstBlockZCoord);
            if (ConfigHandler.enableChromiteGeneration)
                //The 10 as the second parameter sets the maximum vein size
                (new WorldGenMinable(ModBlocks.chromiteore.getDefaultState(), 12)).generate(world, rand, quisquePos);
        }
    }

}
