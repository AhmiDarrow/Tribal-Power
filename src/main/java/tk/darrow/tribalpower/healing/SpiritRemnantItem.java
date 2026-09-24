package tk.darrow.tribalpower.healing;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

/**
 * What a bonded familiar leaves when it falls: the spirit of it, held until a Healing Circle calls it back. It
 * keeps the familiar's lattice, owner and name; its pouch and saddlebag spill where it fell, as always, and are
 * not kept, so nothing comes back twice.
 */
public class SpiritRemnantItem extends Item {
    public static final String FAMILIAR = "Familiar", TYPE = "Type";
    /** What must not survive into the revived body. */
    private static final List<String> SHED = List.of("UUID", "Health", "DeathTime", "HurtTime", "HurtByTimestamp", "Fire",
            "active_effects", "Pouch", "Saddlebag", "ArmorItems", "HandItems", "body_armor_item", "leash", "Passengers",
            "Motion", "FallDistance", "LastOwnerAttacker", "Sitting", "Brain");

    public SpiritRemnantItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Mob familiar) {
        CompoundTag body = new CompoundTag();
        familiar.saveWithoutId(body);
        SHED.forEach(body::remove);
        String type = EntityType.getKey(familiar.getType()).toString();
        body.putString("id", type);
        ItemStack stack = new ItemStack(HealingRegistry.SPIRIT_REMNANT.get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.put(FAMILIAR, body);
            tag.putString(TYPE, type);
        });
        if (familiar.hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, familiar.getCustomName());
        return stack;
    }

    /** Calls the familiar back at a spot, whole. Returns it, or null when the remnant holds nothing. */
    public static @Nullable LivingEntity revive(ServerLevel level, BlockPos at, ItemStack remnant) {
        CustomData data = remnant.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.getUnsafe().contains(FAMILIAR)) return null;
        CompoundTag body = data.getUnsafe().getCompound(FAMILIAR).copy();
        Entity entity = EntityType.loadEntityRecursive(body, level, e -> {
            e.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, e.getYRot(), e.getXRot());
            return e;
        });
        if (!(entity instanceof LivingEntity living)) return null;
        living.setHealth(living.getMaxHealth());
        if (!level.addFreshEntity(living)) return null;
        return living;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return super.getName(stack);
        var type = EntityType.byString(data.getUnsafe().getString(TYPE));
        return type.<Component>map(t -> Component.translatable("item.tribalpower.spirit_remnant.named", t.getDescription()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.spirit_remnant.desc").withStyle(ChatFormatting.GRAY));
    }
}
