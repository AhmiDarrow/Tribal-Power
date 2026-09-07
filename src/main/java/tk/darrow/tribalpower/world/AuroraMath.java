package tk.darrow.tribalpower.world;
/** Shared pure calculations keep weather/time fades bounded, smooth and testable. */
public final class AuroraMath {
    private AuroraMath() {}
    public static float visibility(long dayTime,float partial,float rain,float thunder) {
        double time=(Math.floorMod(dayTime,24000)+Math.clamp(partial,0F,1F))/24000.0;
        double night=Math.clamp((-Math.sin(time*Math.PI*2)-.15)/.65,0,1);
        return (float)(night*night*(3-2*night)*(1-Math.clamp(rain,0F,1F))*(1-Math.clamp(thunder,0F,1F)));
    }
}
