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
/** Camera-centered sky ribbons over the March night (and Overworld). Drawn after the skybox so mountains occlude them. */
public final class AuroraSky {
    private AuroraSky() {}
    public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY || !SkyConfig.ENABLED.get())return;
        var mc=Minecraft.getInstance();var level=mc.level;if(level==null)return;
        boolean march=level.dimension().equals(ModDimensions.THE_MARCH);
        if(march?!SkyConfig.MARCH.get():!level.dimension().equals(Level.OVERWORLD)||!SkyConfig.OVERWORLD.get())return;
        if(event.getCamera().getFluidInCamera()!=FogType.NONE)return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        boolean agreed=march && tk.darrow.tribalpower.quest.QuestStatePayload.latest.agreed();
        float alpha=AuroraMath.visibility(level.getDayTime(),partial,agreed?0:level.getRainLevel(partial),agreed?0:level.getThunderLevel(partial))*SkyConfig.INTENSITY.get().floatValue();
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
            // Each grid point is shared by four quads; it is worked out once, the trig per column and the colour per row.
            for(int row=0;row<=5;row++) {
                float v=row/5F;
                CURTAIN[row]=(float)Math.sin(v*Math.PI);
                RED[row]=(march?.32F:.16F)+v*.30F;GREEN[row]=.85F-v*.4F;BLUE[row]=.65F+v*.30F;
            }
            for(int band=0;band<3;band++) {
                for(int i=0;i<=segments;i++)column(band,i,i/(float)segments,time);
                for(int i=0;i<segments;i++)for(int row=0;row<5;row++) {
                    vertex(buffer,matrix,i,row,alpha);
                    vertex(buffer,matrix,i+1,row,alpha);
                    vertex(buffer,matrix,i+1,row+1,alpha);
                    vertex(buffer,matrix,i,row+1,alpha);
                }
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();
            RenderSystem.setShaderColor(oldColor[0],oldColor[1],oldColor[2],oldColor[3]);
            if(oldShader!=null)RenderSystem.setShader(()->oldShader);
        }
    }
    /** Scratch for one band's columns and the five rows, filled each frame on the render thread; sized for the detailed quality. */
    private static final int MAX_COLUMNS=48*3+1;
    private static final float[] X=new float[MAX_COLUMNS],Z=new float[MAX_COLUMNS],EDGE=new float[MAX_COLUMNS],FILAMENTS=new float[MAX_COLUMNS];
    private static final double[] BASE=new double[MAX_COLUMNS],RISE=new double[MAX_COLUMNS];
    private static final float[] CURTAIN=new float[6],RED=new float[6],GREEN=new float[6],BLUE=new float[6];
    private static void column(int band,int i,float u,double time) {
        double angle=u*Math.PI*1.65+band*1.7;
        double wave=Math.sin(u*14+time*.08+band)*9+Math.sin(u*31-time*.04)*3;
        double radius=170+band*12+Math.sin(u*18+time*.06)*9;
        BASE[i]=48+band*16+wave;RISE[i]=38+Math.sin(u*22+time*.04)*9;
        EDGE[i]=(float)Math.pow(Math.sin(u*Math.PI),.7);
        FILAMENTS[i]=.62F+.38F*(float)Math.pow(Math.sin(u*110+time*.055),2);
        X[i]=(float)(Math.cos(angle)*radius);Z[i]=(float)(Math.sin(angle)*radius);
    }
    private static void vertex(BufferBuilder b,Matrix4f matrix,int i,int row,float strength) {
        float v=row/5F;
        float y=(float)(BASE[i]+v*RISE[i]);
        float a=strength*EDGE[i]*CURTAIN[row]*FILAMENTS[i]*.38F;
        b.addVertex(matrix,X[i],y,Z[i]).setColor(RED[row],GREEN[row],BLUE[row],a);
    }
}
