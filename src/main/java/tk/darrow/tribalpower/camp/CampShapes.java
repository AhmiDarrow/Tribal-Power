package tk.darrow.tribalpower.camp;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.block.Block;
public final class CampShapes {
 private static final java.util.Map<String,VoxelShape> SHAPES=java.util.Map.ofEntries(
java.util.Map.entry("wayanchor",Shapes.or(Block.box(2,0,2,14,2,14),Block.box(3,2,3,13,3,13),Block.box(6,3,6,10,13,10),Block.box(4,6,4,12,9,12),Block.box(5,13,5,11,16,11),Block.box(2,3,2,4,12,4),Block.box(12,3,2,14,12,4),Block.box(2,3,12,4,12,14),Block.box(12,3,12,14,12,14)).optimize()),
java.util.Map.entry("hush_totem",Shapes.or(Block.box(2,0,2,14,2,14),Block.box(3,2,3,13,3,13),Block.box(5,3,5,11,10,11),Block.box(3,10,4,13,15,12),Block.box(4,12,3,6,14,4),Block.box(10,12,3,12,14,4),Block.box(1,9,6,15,11,10)).optimize()),
java.util.Map.entry("summoning_cradle",Shapes.or(Block.box(2,0,2,14,2,14),Block.box(3,2,3,13,3,13),Block.box(4,3,4,12,5,12),Block.box(6,5,6,10,9,10),Block.box(2,3,2,4,14,4),Block.box(12,3,2,14,14,4),Block.box(2,3,12,4,14,14),Block.box(12,3,12,14,14,14),Block.box(2,13,2,14,15,4),Block.box(2,13,12,14,15,14)).optimize()),
java.util.Map.entry("grove_tender",Shapes.or(Block.box(2,0,2,14,2,14),Block.box(3,2,3,13,3,13),Block.box(6,3,6,10,13,10),Block.box(3,8,6,13,10,10),Block.box(3,13,3,13,15,13),Block.box(6,15,6,10,16,10)).optimize()),
java.util.Map.entry("spirit_lantern",Shapes.or(Block.box(2,0,2,14,2,14),Block.box(3,2,3,13,3,13),Block.box(5,3,5,11,11,11),Block.box(3,3,3,4,13,4),Block.box(12,3,3,13,13,4),Block.box(3,3,12,4,13,13),Block.box(12,3,12,13,13,13),Block.box(3,12,3,13,14,13),Block.box(6,14,6,10,16,10)).optimize()),
java.util.Map.entry("rain_chime",Shapes.or(Block.box(2,13,2,14,15,14),Block.box(7,0,7,9,14,9),Block.box(3,5,5,4,13,7),Block.box(6,5,5,7,13,7),Block.box(9,5,5,10,13,7),Block.box(12,5,5,13,13,7),Block.box(4,2,4,12,3,12)).optimize()),
java.util.Map.entry("offering_table",Shapes.or(Block.box(1,11,1,15,13,15),Block.box(2,13,2,14,14,14),Block.box(5,14,5,11,15,11),Block.box(2,0,2,4,11,4),Block.box(12,0,2,14,11,4),Block.box(2,0,12,4,11,14),Block.box(12,0,12,14,11,14)).optimize()));
 public static VoxelShape shape(String id){return SHAPES.getOrDefault(id,Shapes.block());}
}
