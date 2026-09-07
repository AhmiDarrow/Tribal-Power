package tk.darrow.tribalpower.entity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
public enum CreatureProfile {
    DAWN_STAG("dawn_stag", true, false, 24, 0.25, 0, 1, 0.8F, 1.625F, "none", "dawn_velvet", 0x405d55, 0x7effcb),
    LANTERN_FOX("lantern_fox", true, false, 14, 0.3, 0, 0, 0.8F, 0.875F, "none", "lantern_down", 0x603e68, 0x7affe1),
    MOSSBACK("mossback", true, false, 30, 0.15, 0, 6, 1.3F, 1.0F, "none", "mossback_scale", 0x344f44, 0xc4ffad),
    ASHBOUND("ashbound", false, false, 24, 0.23, 4, 2, 0.8F, 1.438F, "ember", "ember_heart", 0x473c42, 0xffb46c),
    ROOTBOUND("rootbound", false, false, 38, 0.18, 6, 5, 0.8F, 1.438F, "root", "knotted_root", 0x494735, 0xb1e0a0),
    REED_STALKER("reed_stalker", false, false, 22, 0.3, 4, 1, 0.8F, 1.188F, "venom", "reed_fang", 0x294b4a, 0xa2e8b9),
    SHARDBACK("shardback", false, false, 36, 0.18, 5, 8, 1.3F, 1.188F, "shove", "prism_carapace", 0x455272, 0x87fff0),
    HOLLOW_SENTINEL("hollow_sentinel", false, false, 44, 0.2, 6, 6, 0.8F, 1.812F, "weaken", "sentinel_sigil", 0x313f53, 0xbafbe8),
    STORM_MOTH("storm_moth", false, true, 20, 0.24, 4, 0, 0.65F, 1.438F, "gust", "storm_wing", 0x42415e, 0xadffff),
    CINDER_IMP("cinder_imp", false, false, 18, 0.27, 4, 1, 0.8F, 1.25F, "bolt", "cinder_knot", 0x563943, 0xffcb89),
    MOURNING_BELL("mourning_bell", false, true, 28, 0.2, 4, 3, 0.65F, 1.312F, "chill", "bell_fragment", 0x364760, 0xb6e4ff),
    RIFT_HOUND("rift_hound", false, false, 28, 0.34, 5, 2, 0.8F, 0.875F, "hound", "rift_tooth", 0x323848, 0xb5a3ff),
    ECHO_WEAVER("echo_weaver", false, false, 26, 0.25, 4, 3, 1.3F, 0.688F, "weave", "echo_silk", 0x463b59, 0xe4baff);
    public final String id,attack,reagent;
    public final boolean animal,flying;
    public final double health,speed,damage,armor;
    public final float width,height;
    public final int color,glow;
    CreatureProfile(String id,boolean animal,boolean flying,double health,double speed,double damage,double armor,float width,float height,String attack,String reagent,int color,int glow) {
        this.id=id;this.animal=animal;this.flying=flying;this.health=health;this.speed=speed;this.damage=damage;this.armor=armor;this.width=width;this.height=height;this.attack=attack;this.reagent=reagent;this.color=color;this.glow=glow;
    }
    public static CreatureProfile of(EntityType<?> type) {
        String id=BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        for(var profile:values()) if(profile.id.equals(id)) return profile;
        throw new IllegalArgumentException("Unknown Tribal creature: "+id);
    }
    public boolean ranged() { return attack.equals("gust") || attack.equals("bolt") || attack.equals("chill"); }
}
