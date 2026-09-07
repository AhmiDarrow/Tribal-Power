package tk.darrow.tribalpower.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
public final class VisualVerification {
    private static long nextCapture=0;
    private static int captures=0;
    public static void render(RenderLevelStageEvent event) {
        if(!Boolean.getBoolean("tribalpower.visualVerification") || event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.level==null || mc.player==null || captures>=24)return;
        long now=System.currentTimeMillis();if(now<nextCapture)return;nextCapture=now+15000;
        Screenshot.grab(mc.gameDirectory,"tribal-visual-"+(++captures)+".png",mc.getMainRenderTarget(),message->{});
    }
    private static long nextMenuCapture;
    private static int menuCaptures;
    public static void screen(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
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
}
