package tk.darrow.tribalpower.proxy;

import tk.darrow.tribalpower.init.ModBlocks;
import tk.darrow.tribalpower.init.ModItems;

public class ClientProxy implements CommonProxy {

	@Override
	public void init() {
		
		ModItems.registerRenders();
		ModBlocks.registerRenders();
		
	}
}

