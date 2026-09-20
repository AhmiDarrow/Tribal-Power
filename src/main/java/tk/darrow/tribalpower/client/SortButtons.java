package tk.darrow.tribalpower.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import tk.darrow.tribalpower.storage.InventorySorter;
import tk.darrow.tribalpower.storage.SortPayload;

import java.util.List;

/**
 * A tidy button on every inventory worth tidying — the player's pack, chests, barrels, shulkers, hoppers and
 * the mod's own caches, whoever built the screen. The button sits in the empty right end of the label row
 * above its own slots, and follows the screen if it shifts (the recipe book, say).
 */
public final class SortButtons {
    private static final int SIZE = 10;

    private SortButtons() {}

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!SkyConfig.SORT_BUTTONS.get()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || screen instanceof CreativeModeInventoryScreen) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        for (boolean container : new boolean[]{false, true}) {
            List<Slot> group = InventorySorter.group(screen.getMenu(), player, container);
            if (group.isEmpty()) continue;
            int right = group.stream().mapToInt(slot -> slot.x).max().orElse(0) + 16;
            int top = group.stream().mapToInt(slot -> slot.y).min().orElse(0);
            event.addListener(new SortButton(screen, right - SIZE, top - SIZE - 2, container, group));
        }
    }

    private static final class SortButton extends AbstractWidget {
        private final AbstractContainerScreen<?> screen;
        private final int offsetX, offsetY;
        private final boolean container;
        /**
         * Which slots this button would move. The menu's slot list never changes while the screen is
         * open, so this is settled once: deciding it again every frame meant a slot-by-slot scan behind
         * every drawn frame, which is not what a button's border colour is worth.
         */
        private final List<Slot> group;
        private boolean live;
        private long checkedAt = Long.MIN_VALUE;

        private SortButton(AbstractContainerScreen<?> screen, int offsetX, int offsetY, boolean container,
                           List<Slot> group) {
            super(screen.getGuiLeft() + offsetX, screen.getGuiTop() + offsetY, SIZE, SIZE,
                    Component.translatable("gui.tribalpower.sort"));
            this.screen = screen;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.container = container;
            this.group = group;
            setTooltip(Tooltip.create(Component.translatable(
                    container ? "gui.tribalpower.sort.container" : "gui.tribalpower.sort.inventory")));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // The screen can move after init (the recipe book slides it), so follow it every frame.
            setX(screen.getGuiLeft() + offsetX);
            setY(screen.getGuiTop() + offsetY);
            boolean hover = live() && isHovered();
            graphics.fill(getX(), getY(), getX() + SIZE, getY() + SIZE, hover ? 0xFF20333A : 0xFF101B22);
            graphics.renderOutline(getX(), getY(), SIZE, SIZE, live() ? (hover ? 0xFFE7DCC1 : 0xFFB58A58) : 0xFF3A4A50);
            int ink = live() ? (hover ? 0xFFFFF4D8 : 0xFFE7DCC1) : 0xFF6A7A80;
            // Three bars, longest first: the usual mark for putting things in order.
            for (int row = 0; row < 3; row++) {
                graphics.fill(getX() + 2, getY() + 2 + row * 2, getX() + 8 - row * 2, getY() + 3 + row * 2, ink);
            }
        }

        /** Is there anything in there to tidy? Re-read a few times a second, not a few times a frame. */
        private boolean live() {
            long now = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
            if (now != checkedAt) {
                checkedAt = now;
                live = false;
                for (Slot slot : group) {
                    if (slot.hasItem()) { live = true; break; }
                }
            }
            return live;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (!live()) return;
            PacketDistributor.sendToServer(new SortPayload(container));
            Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.BUNDLE_INSERT, 1.2F));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
