package tk.darrow.tribalpower.charm;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.item.SpiritgearHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A worn power object. Sneak-use opens the charm slots; use equips into the first empty one. */
public class SpiritCharmItem extends Item {
    public static final String VOICES_KEY = "CharmVoices";
    public static final int LINK_COST = 40;
    public static final int CHORUS_COST = 200;

    public final CharmKind kind;

    public SpiritCharmItem(CharmKind kind, Properties properties) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public static Set<Attunement> voices(ItemStack stack) {
        Set<Attunement> out = new LinkedHashSet<>();
        if (!(stack.getItem() instanceof SpiritCharmItem charm)) return out;
        String raw = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(VOICES_KEY);
        if (raw == null || raw.isEmpty()) {
            if (charm.kind.nativeVoice != null) out.add(charm.kind.nativeVoice);
            return out;
        }
        for (String part : raw.split(",")) {
            if (part.isEmpty()) continue;
            out.add(Attunement.byName(part));
        }
        return out;
    }

    public static void setVoices(ItemStack stack, Set<Attunement> voices) {
        String joined = String.join(",", voices.stream().map(Attunement::getSerializedName).toList());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (joined.isEmpty()) tag.remove(VOICES_KEY);
            else tag.putString(VOICES_KEY, joined);
        });
    }

    public static boolean addVoice(Player player, ItemStack stack, Attunement voice) {
        if (!(stack.getItem() instanceof SpiritCharmItem)) return false;
        Set<Attunement> voices = new LinkedHashSet<>(voices(stack));
        if (voices.contains(voice)) {
            player.displayClientMessage(Component.translatable("message.tribalpower.charm.already",
                    Component.translatable("attunement.tribalpower." + voice.getSerializedName())), true);
            return false;
        }
        if (!player.getAbilities().instabuild && !SpiritgearHelper.tryConsumePulse(player, LINK_COST)) {
            SpiritgearHelper.notifyStarved(player);
            return false;
        }
        voices.add(voice);
        setVoices(stack, voices);
        player.displayClientMessage(Component.translatable("message.tribalpower.charm.linked",
                Component.translatable("attunement.tribalpower." + voice.getSerializedName()), voices.size()), true);
        return true;
    }

    public static int pulseCost(ItemStack stack) {
        int voices = Math.max(1, voices(stack).size());
        return 2 * voices;
    }

    public static boolean grantsFlight(ItemStack stack) {
        return voices(stack).contains(Attunement.AIR);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player.isShiftKeyDown()) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CharmMenu(id, inv, CharmSlots.of(p)),
                    Component.translatable("gui.tribalpower.charm_slots")));
            return InteractionResultHolder.consume(stack);
        }
        if (CharmSlots.of(player).equip(stack)) {
            player.displayClientMessage(Component.translatable("message.tribalpower.charm.equipped"), true);
            return InteractionResultHolder.consume(stack);
        }
        player.displayClientMessage(Component.translatable("message.tribalpower.charm.full"), true);
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.spirit_charm.desc"));
        lines.add(Component.translatable("item.tribalpower." + kind.id + ".boon"));
        List<Attunement> voices = new ArrayList<>(voices(stack));
        if (voices.isEmpty()) {
            lines.add(Component.translatable("item.tribalpower.spirit_charm.silent"));
        } else {
            for (Attunement voice : voices) {
                lines.add(Component.translatable("item.tribalpower.spirit_charm.voice",
                        Component.translatable("attunement.tribalpower." + voice.getSerializedName())));
            }
        }
        if (grantsFlight(stack)) lines.add(Component.translatable("item.tribalpower.spirit_charm.flight"));
        lines.add(Component.translatable("item.tribalpower.spirit_charm.cost", pulseCost(stack)));
    }

    public static boolean imprintChorus(Player player, ItemStack stack, ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (!(stack.getItem() instanceof SpiritCharmItem charm) || charm.kind != CharmKind.CHORUS) return false;
        Set<Attunement> found = new LinkedHashSet<>();
        for (var totem : tk.darrow.tribalpower.lattice.LatticeNetwork.findNearbyTotems(level, pos, 8)) {
            found.add(totem.getAttunement());
        }
        if (found.size() < 3) {
            player.displayClientMessage(Component.translatable("message.tribalpower.charm.chorus_need"), true);
            return false;
        }
        if (!player.getAbilities().instabuild && !SpiritgearHelper.tryConsumePulse(player, CHORUS_COST)) {
            SpiritgearHelper.notifyStarved(player);
            return false;
        }
        setVoices(stack, found);
        player.displayClientMessage(Component.translatable("message.tribalpower.charm.chorus", found.size()), true);
        return true;
    }
}
