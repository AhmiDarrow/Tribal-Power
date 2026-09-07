package tk.darrow.tribalpower.client;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import tk.darrow.tribalpower.world.*;
/** Camera-centered sky ribbons, rendered before terrain so mountains and roofs occlude them. */
public final class AuroraSky {
    private AuroraSky() {}
    public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY || !SkyConfig.ENABLED.get())return;
        var mc=Minecraft.getInstance();var level=mc.level;if(level==null)return;
        boolean march=level.dimension().equals(ModDimensions.THE_MARCH);
        if(march?!SkyConfig.MARCH.get():!level.dimension().equals(Level.OVERWORLD)||!SkyConfig.OVERWORLD.get())return;
        if(event.getCamera().getFluidInCamera()!=FogType.NONE)return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float alpha=AuroraMath.visibility(level.getDayTime(),partial,level.getRainLevel(partial),level.getThunderLevel(partial))*SkyConfig.INTENSITY.get().floatValue();
        if(alpha<.005F)return;
        double time=(level.getGameTime()+(double)partial)/20D;
        int segments=48*(SkyConfig.QUALITY.get()+1);
        var matrix=new Matrix4f(event.getModelViewMatrix());matrix.m30(0).m31(0).m32(0);
        var oldShader=RenderSystem.getShader();float[] oldColor=RenderSystem.getShaderColor().clone();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.setShaderColor(1,1,1,1);
        try {
            var buffer=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            // Nine small constellations evoke the old tribes without replacing vanilla stars.
            for(int group=0;group<9;group++)for(int star=0;star<3;star++) {
                double angle=group*Math.PI*2/9+star*.055;
                float x=(float)(Math.cos(angle)*155),z=(float)(Math.sin(angle)*155),y=85+(float)Math.sin(group*2.4+star)*18;
                float size=star==1?.35F:.22F;
                float r=group%3==0?1F:.62F,g=.86F,blue=group%3==0?.65F:1F;
                for(int corner=0;corner<4;corner++) {
                    float dx=(corner==0||corner==3?-size:size),dy=corner<2?-size:size;
                    buffer.addVertex(matrix,x+(float)Math.sin(angle)*dx,y+dy,z-(float)Math.cos(angle)*dx).setColor(r,g,blue,alpha*(march?.8F:.45F));
                }
            }
            for(int band=0;band<3;band++)for(int i=0;i<segments;i++)for(int row=0;row<5;row++) {
                vertex(buffer,matrix,band,i/(float)segments,row/5F,time,alpha,march);
                vertex(buffer,matrix,band,(i+1)/(float)segments,row/5F,time,alpha,march);
                vertex(buffer,matrix,band,(i+1)/(float)segments,(row+1)/5F,time,alpha,march);
                vertex(buffer,matrix,band,i/(float)segments,(row+1)/5F,time,alpha,march);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();
            RenderSystem.setShaderColor(oldColor[0],oldColor[1],oldColor[2],oldColor[3]);
            if(oldShader!=null)RenderSystem.setShader(()->oldShader);
        }
    }
    private static void vertex(BufferBuilder b,Matrix4f matrix,int band,float u,float v,double time,float strength,boolean march) {
        double angle=u*Math.PI*1.65+band*1.7;
        double wave=Math.sin(u*14+time*.08+band)*9+Math.sin(u*31-time*.04)*3;
        double radius=170+band*12+Math.sin(u*18+time*.06)*9;
        float y=(float)(48+band*16+wave+v*(38+Math.sin(u*22+time*.04)*9));
        float edge=(float)Math.pow(Math.sin(u*Math.PI),.7);
        float curtain=(float)Math.sin(v*Math.PI);
        float filaments=.62F+.38F*(float)Math.pow(Math.sin(u*110+time*.055),2);
        float a=strength*edge*curtain*filaments*.38F;
        float r=(march?.32F:.16F)+v*.30F,g=.85F-v*.4F,blue=.65F+v*.30F;
        b.addVertex(matrix,(float)(Math.cos(angle)*radius),y,(float)(Math.sin(angle)*radius)).setColor(r,g,blue,a);
    }
}
