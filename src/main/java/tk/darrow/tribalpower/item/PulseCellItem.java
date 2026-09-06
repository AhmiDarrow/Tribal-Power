package tk.darrow.tribalpower.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Portable Spirit Pulse buffer. Fill at a Drumheart; Spiritgear drains cells from inventory.
 */
public class PulseCellItem extends Item {
    public static final int CAPACITY = 200;
    public static final String PULSE_KEY = "Pulse";

    public PulseCellItem(Properties properties) {
        super(properties);
    }

    public static int getPulse(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return Mth.clamp(data.copyTag().getInt(PULSE_KEY), 0, CAPACITY);
    }

    public static void setPulse(ItemStack stack, int amount) {
        int clamped = Mth.clamp(amount, 0, CAPACITY);
        if (clamped <= 0) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(PULSE_KEY));
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(PULSE_KEY, clamped));
    }

    public static int insertPulse(ItemStack stack, int amount, boolean simulate) {
        if (amount <= 0 || !(stack.getItem() instanceof PulseCellItem)) {
            return 0;
        }
        int stored = getPulse(stack);
        int accepted = Math.min(amount, CAPACITY - stored);
        if (!simulate && accepted > 0) {
            setPulse(stack, stored + accepted);
        }
        return accepted;
    }

    public static int extractPulse(ItemStack stack, int amount, boolean simulate) {
        if (amount <= 0 || !(stack.getItem() instanceof PulseCellItem)) {
            return 0;
        }
        int stored = getPulse(stack);
        int taken = Math.min(amount, stored);
        if (!simulate && taken > 0) {
            setPulse(stack, stored - taken);
        }
        return taken;
    }

    public static ItemStack createFilled(int amount) {
        ItemStack stack = new ItemStack(ModItems.PULSE_CELL.get());
        setPulse(stack, amount);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getPulse(stack) / (float) CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float fill = getPulse(stack) / (float) CAPACITY;
        return Mth.hsvToRgb(0.50F, 0.65F, 0.55F + 0.45F * fill);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.pulse_cell.desc", getPulse(stack), CAPACITY));
    }
}
