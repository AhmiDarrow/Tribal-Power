package tk.darrow.tribalpower.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import tk.darrow.tribalpower.item.GearSettingsPayload;
import tk.darrow.tribalpower.item.SpiritGear;
import tk.darrow.tribalpower.item.SpiritStaffItem;
import tk.darrow.tribalpower.item.SpiritweaveArmor;

import java.util.ArrayList;
import java.util.List;

/**
 * One small screen for what a player wears: each worn Spiritweave piece on or off (off means no effect and no Pulse
 * drawn), the hood's ley goggles, the Sixfold Staff's voice, which vault the satchel opens, and the Pulse HUD.
 */
public class GearScreen extends Screen {
    private static final int W = 220, ROW = 24;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final List<Component> labels = new ArrayList<>();
    private int top, height0;

    public GearScreen() {
        super(Component.translatable("gui.tribalpower.gear.title"));
    }

    @Override
    protected void init() {
        labels.clear();
        var player = Minecraft.getInstance().player;
        if (player == null) { onClose(); return; }
        List<Runnable> rows = new ArrayList<>();
        int x = (width - W) / 2;
        boolean anyArmor = false;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack piece = player.getItemBySlot(slot);
            if (!(piece.getItem() instanceof SpiritweaveArmor)) continue;
            anyArmor = true;
            final int ordinal = slot.ordinal();
            rows.add(() -> addRow(x, piece.getHoverName(), CycleButton.onOffBuilder(!SpiritGear.abilitiesOff(piece))
                    .displayOnlyValue().create(0, 0, 60, 20, Component.empty(), (button, on) ->
                            PacketDistributor.sendToServer(new GearSettingsPayload(GearSettingsPayload.ARMOR, ordinal, on)))));
        }
        if (!anyArmor) rows.add(() -> addRow(x, Component.translatable("gui.tribalpower.gear.none"), null));
        ItemStack hood = player.getItemBySlot(EquipmentSlot.HEAD);
        if (SpiritGear.goggles(hood)) {
            rows.add(() -> addRow(x, Component.translatable("gui.tribalpower.gear.goggles"), CycleButton.onOffBuilder(SpiritGear.gogglesOpen(hood))
                    .displayOnlyValue().create(0, 0, 60, 20, Component.empty(), (button, on) ->
                            PacketDistributor.sendToServer(new GearSettingsPayload(GearSettingsPayload.GOGGLES, 0, on)))));
        }
        ItemStack staff = player.getMainHandItem().getItem() instanceof SpiritStaffItem ? player.getMainHandItem()
                : player.getOffhandItem().getItem() instanceof SpiritStaffItem ? player.getOffhandItem() : ItemStack.EMPTY;
        if (!staff.isEmpty()) {
            Component voice = Component.translatable("spell.tribalpower." + SpiritStaffItem.element(staff).getSerializedName());
            rows.add(() -> addRow(x, Component.translatable("gui.tribalpower.gear.staff", voice),
                    Button.builder(Component.translatable("gui.tribalpower.gear.next"), b -> {
                        PacketDistributor.sendToServer(new GearSettingsPayload(GearSettingsPayload.STAFF_VOICE, 0, false));
                        // the staff's stack reaches the client a tick later; rebuild then so the label follows it
                        Minecraft.getInstance().tell(this::rebuild);
                    }).size(60, 20).build()));
        }
        rows.add(() -> addRow(x, Component.translatable("gui.tribalpower.gear.vault"),
                Button.builder(Component.translatable("gui.tribalpower.gear.switch"), b ->
                        PacketDistributor.sendToServer(new GearSettingsPayload(GearSettingsPayload.VAULT, 0, false))).size(60, 20).build()));
        rows.add(() -> addRow(x, Component.translatable("gui.tribalpower.gear.hud"), CycleButton.onOffBuilder(!PulseHud.hidden)
                .displayOnlyValue().create(0, 0, 60, 20, Component.empty(), (button, on) -> PulseHud.hidden = !on)));
        height0 = 30 + rows.size() * ROW + 32;
        top = (height - height0) / 2;
        rowY = top + 28;
        for (Runnable row : rows) row.run();
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x + (W - 80) / 2, top + height0 - 26, 80, 20).build());
    }

    private int rowY;

    private void addRow(int x, Component label, net.minecraft.client.gui.components.AbstractWidget control) {
        labels.add(label);
        if (control != null) {
            control.setX(x + W - 66);
            control.setY(rowY + 2);
            addRenderableWidget(control);
        }
        rowY += ROW;
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = (width - W) / 2;
        g.fill(x, top, x + W, top + height0, 0xFF101B22);
        g.renderOutline(x, top, W, height0, 0xFFB58A58);
        g.fill(x + 1, top + 1, x + W - 1, top + 18, 0xFF20333A);
        g.drawString(font, title, x + 8, top + 6, 0xFFE7DCC1, false);
        int y = top + 28;
        for (Component label : labels) {
            g.drawString(font, label, x + 8, y + 8, 0xFFE7DCC1, false);
            y += ROW;
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (TribalKeys.GEAR.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
}
