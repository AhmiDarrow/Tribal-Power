package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tk.darrow.tribalpower.familiar.MossbackMenu;

/** A moss-green nine-slot saddlebag panel, drawn with fills in the same quiet style as {@link StationScreen}. */
public class MossbackScreen extends AbstractContainerScreen<MossbackMenu> {
    public MossbackScreen(MossbackMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=176;imageHeight=133;inventoryLabelY=39;
    }
    @Override protected void renderBg(GuiGraphics g,float partialTick,int mouseX,int mouseY) {
        int x=leftPos,y=topPos;
        g.fill(x,y,x+176,y+133,0xFF101B22);
        g.renderOutline(x,y,176,133,0xFFB58A58);
        g.fill(x+1,y+1,x+175,y+16,0xFF1F3A2E);
        g.fill(x+6,y+18,x+170,y+40,0xFF14262C);
        for(var slot:menu.slots) {
            g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xFF081317);
            g.renderOutline(x+slot.x-1,y+slot.y-1,18,18,0xFF385456);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        g.drawString(font,title,8,5,0xFFE7DCC1,false);
        g.drawString(font,playerInventoryTitle,8,inventoryLabelY,0xFF98ACA5,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        super.render(g,mouseX,mouseY,partialTick);renderTooltip(g,mouseX,mouseY);
    }
}
