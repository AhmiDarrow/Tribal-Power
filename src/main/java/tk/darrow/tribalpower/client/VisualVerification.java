package tk.darrow.tribalpower.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
public final class VisualVerification {
    private static long nextCapture=0;
    private static int captures=0;
    public static void render(RenderLevelStageEvent event) {
        if(Boolean.getBoolean("tribalpower.codexVerification") && event.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL && codex==null) {
            var client=Minecraft.getInstance();
            if(client.player!=null && client.gameMode!=null && client.screen==null) {
                client.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(tk.darrow.tribalpower.item.ModItems.SPIRIT_CODEX.get()));
                client.gameMode.useItem(client.player,net.minecraft.world.InteractionHand.MAIN_HAND);
                if(client.screen instanceof SpiritCodexScreen opened){codex=opened;codexNext=System.currentTimeMillis()+3000;}
            }
        }
        if(!Boolean.getBoolean("tribalpower.visualVerification") || event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.level==null || mc.player==null || captures>=24)return;
        long now=System.currentTimeMillis();if(now<nextCapture)return;nextCapture=now+15000;
        Screenshot.grab(mc.gameDirectory,"tribal-visual-"+(++captures)+".png",mc.getMainRenderTarget(),message->{});
    }
    private static long nextMenuCapture;
    private static int menuCaptures;
    public static void screen(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
        codexTour(event);
        if (!Boolean.getBoolean("tribalpower.visualVerification")
                || !(event.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen)
                || menuCaptures >= 6) return;
        long now = System.currentTimeMillis();
        if (now < nextMenuCapture) return;
        nextMenuCapture = now + 10000;
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "menu-visual-" + (++menuCaptures) + ".png",
                mc.getMainRenderTarget(), message -> {});
    }
    private static SpiritCodexScreen codex;
    private static int codexStage;
    private static long codexNext;
    private static void codexTour(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
        if(!Boolean.getBoolean("tribalpower.codexVerification") || codexStage>=9)return;
        var mc=Minecraft.getInstance();
        if(codex==null && event.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen) {
            codex=new SpiritCodexScreen();mc.setScreen(codex);codexNext=System.currentTimeMillis()+3000;return;
        }
        if(codex==null || System.currentTimeMillis()<codexNext)return;
        codexNext=System.currentTimeMillis()+3500;
        Screenshot.grab(mc.gameDirectory,"codex-"+codexStage+".png",mc.getMainRenderTarget(),message->{});
        codexStage++;
        if(codexStage==3 && mc.screen instanceof net.minecraft.client.gui.screens.ConfirmScreen confirm) {
            for(var child:confirm.children())if(child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getString().equals("Reveal knowledge")){button.onPress();break;}
        } else codex.verificationStage(codexStage);
    }
}
