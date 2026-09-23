package tk.darrow.tribalpower.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.SpiritGear;
import tk.darrow.tribalpower.item.SpiritweaveArmor;

/**
 * Totem-bound Spiritgear and Spiritweave glow in their voice's colour. Items carry a full-bright glow layer
 * (tint 1) over their trims and around their silhouette; worn Spiritweave lights its trims the same way.
 * An unbound piece shows no glow, and the glow grows stronger with the piece's rank.
 */
public final class VoiceGlow {
    private static final ResourceLocation[] ARMOUR = {
            ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/models/armor/spiritweave_layer_1_glow.png"),
            ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/models/armor/spiritweave_layer_2_glow.png")};
    private static final ResourceLocation GOGGLES =
            ResourceLocation.fromNamespaceAndPath("tribalpower", "textures/models/armor/spiritweave_goggles.png");

    private VoiceGlow() {}

    /** Neon colour of each voice. */
    public static int colour(Attunement voice) {
        return switch (voice) {
            case EARTH -> 0x6CFF6A;
            case FIRE -> 0xFF6A1F;
            case WATER -> 0x33C8FF;
            case AIR -> 0xC4FFEE;
            case SPIRIT -> 0xC06BFF;
            case LOOM -> 0x3FFFE0;
        };
    }

    /** Glow strength by rank: a bound plain piece glimmers, a Manifested one blazes. */
    private static float strength(ItemStack stack) {
        return 0.55F + 0.15F * Math.clamp(SpiritGear.rank(stack), 0, 3);
    }

    /** ARGB for the glow layer, or fully clear when the piece answers no voice. */
    public static int tint(ItemStack stack) {
        return SpiritGear.voice(stack).map(voice -> ((int) (strength(stack) * 255) << 24) | colour(voice)).orElse(0);
    }

    public static void items(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 1 ? tint(stack) : 0xFFFFFFFF,
                ModItems.SPIRITGEAR_PICKAXE.get(), ModItems.SPIRITGEAR_AXE.get(), ModItems.SPIRITGEAR_SHOVEL.get(),
                ModItems.SPIRITGEAR_BLADE.get(), ModItems.SPIRITGEAR_SHEARS.get(), ModItems.SPIRITGEAR_HOE.get(),
                ModItems.SPIRITWEAVE_HOOD.get(), ModItems.SPIRITWEAVE_ROBE.get(),
                ModItems.SPIRITWEAVE_LEGGINGS.get(), ModItems.SPIRITWEAVE_BOOTS.get());
    }

    /** Adds the worn-trim glow to every humanoid that can wear armour, players included. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void layers(EntityRenderersEvent.AddLayers event) {
        var models = event.getEntityModels();
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof LivingEntityRenderer renderer)
                renderer.addLayer(new Worn(renderer, new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                        new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))));
        }
        for (var type : event.getEntityTypes()) {
            if (event.getRenderer(type) instanceof LivingEntityRenderer renderer && renderer.getModel() instanceof HumanoidModel)
                renderer.addLayer(new Worn(renderer, new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                        new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))));
        }
    }

    /** The trims of worn Spiritweave, drawn again full-bright in the voice's colour. */
    static final class Worn<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
        /** Hoisted: this ran once per entity per frame, for every humanoid on screen. */
        private static final EquipmentSlot[] WORN_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET};
        private final HumanoidModel<T> inner, outer;

        Worn(RenderLayerParent<T, M> parent, HumanoidModel<T> inner, HumanoidModel<T> outer) {
            super(parent);
            this.inner = inner;
            this.outer = outer;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            for (EquipmentSlot slot : WORN_SLOTS) {
                ItemStack stack = entity.getItemBySlot(slot);
                if (!(stack.getItem() instanceof SpiritweaveArmor)) continue;
                boolean legs = slot == EquipmentSlot.LEGS;
                HumanoidModel<T> model = legs ? inner : outer;
                getParentModel().copyPropertiesTo(model);
                model.setAllVisible(false);
                switch (slot) {
                    case HEAD -> { model.head.visible = true; model.hat.visible = true; }
                    case CHEST -> { model.body.visible = true; model.rightArm.visible = true; model.leftArm.visible = true; }
                    case LEGS -> { model.body.visible = true; model.rightLeg.visible = true; model.leftLeg.visible = true; }
                    case FEET -> { model.rightLeg.visible = true; model.leftLeg.visible = true; }
                    default -> { }
                }
                if (slot == EquipmentSlot.HEAD && SpiritGear.goggles(stack)) {
                    model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GOGGLES)), 0xF000F0,
                            OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                }
                int tint = tint(stack);
                if ((tint >>> 24) == 0) continue;
                // Additive and full-bright: the colour is the brightness, so scale it by the rank's strength.
                float s = (tint >>> 24) / 255F;
                int r = (int) (((tint >> 16) & 255) * s), g = (int) (((tint >> 8) & 255) * s), b = (int) ((tint & 255) * s);
                model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(ARMOUR[legs ? 1 : 0])), 0xF000F0,
                        OverlayTexture.NO_OVERLAY, 0xFF000000 | r << 16 | g << 8 | b);
            }
        }
    }
}
