package tk.darrow.tribalpower.client;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
public final class MarchSkyEffects extends DimensionSpecialEffects {
    public MarchSkyEffects() { super(224,true,SkyType.NORMAL,false,false); }
    @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float brightness) { return color.multiply(.10+brightness*.72,.13+brightness*.77,.20+brightness*.8); }
    @Override public boolean isFoggyAt(int x,int y) { return false; }
    @Override public float[] getSunriseColor(float time,float partial) {
        float[] base=super.getSunriseColor(time,partial);
        return base==null?null:new float[]{.34F,.56F,.63F,base[3]*.65F};
    }
}
