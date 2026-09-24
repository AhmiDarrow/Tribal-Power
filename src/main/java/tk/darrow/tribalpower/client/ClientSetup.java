package tk.darrow.tribalpower.client;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import tk.darrow.tribalpower.entity.ModEntities;

public final class ClientSetup {
    private ClientSetup() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            var bow = tk.darrow.tribalpower.item.ModItems.PULSE_BOW.get();
            net.minecraft.client.renderer.item.ItemProperties.register(bow,
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull"),
                    (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0F
                            : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F);
            net.minecraft.client.renderer.item.ItemProperties.register(bow,
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("pulling"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1F : 0F);
            var crossbow = tk.darrow.tribalpower.kit.KitRegistry.PULSE_CROSSBOW.get();
            net.minecraft.client.renderer.item.ItemProperties.register(crossbow,
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull"),
                    (stack, level, entity, seed) -> tk.darrow.tribalpower.song.PulseCrossbowItem.loadProgress(stack, entity));
            net.minecraft.client.renderer.item.ItemProperties.register(crossbow,
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("pulling"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1F : 0F);
            net.minecraft.client.renderer.item.ItemProperties.register(crossbow,
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("charged"),
                    (stack, level, entity, seed) -> tk.darrow.tribalpower.song.PulseCrossbowItem.loaded(stack) ? 1F : 0F);
        });
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        tk.darrow.tribalpower.entity.CreatureEntities.ANIMALS.forEach((p,t)->event.registerEntityRenderer(t.get(),context->new LatticeCreatureRenderer<>(context,p)));
        tk.darrow.tribalpower.entity.CreatureEntities.MONSTERS.forEach((p,t)->event.registerEntityRenderer(t.get(),context->new LatticeCreatureRenderer<>(context,p)));
        event.registerEntityRenderer(ModEntities.SPIRIT_WISP.get(), SpiritWispRenderer::new);
        event.registerEntityRenderer(tk.darrow.tribalpower.event.MarchEvents.WANDERING_SPIRIT.get(), WanderingSpiritRenderer::new);
        event.registerEntityRenderer(ModEntities.MARCH_WALKER.get(), MarchWalkerRenderer::new);
        event.registerEntityRenderer(tk.darrow.tribalpower.world.structure.MarchRegistry.THE_UNSUNG.get(), tk.darrow.tribalpower.boss.client.TheUnsungRenderer::new);
        for (var guardian : tk.darrow.tribalpower.guardian.Guardian.values())
            event.registerEntityRenderer(tk.darrow.tribalpower.guardian.GuardianRegistry.ENTITIES.get(guardian).get(),
                    context -> new tk.darrow.tribalpower.guardian.client.GuardianRenderer(context, guardian));
        event.registerEntityRenderer(tk.darrow.tribalpower.tribe.TribeRegistry.TRIBAL_KIN.get(), TribalKinRenderer::new);
        event.registerEntityRenderer(ModEntities.SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.SONIC_BOLT.get(), SonicBoltRenderer::new);
        event.registerBlockEntityRenderer(tk.darrow.tribalpower.blockentity.ModBlockEntities.CAMP_DISPLAY.get(), CampDisplayRenderer::new);
        event.registerBlockEntityRenderer(tk.darrow.tribalpower.blockentity.ModBlockEntities.TRIBAL_BENCH.get(), CampDisplayRenderer::new);
    }
}
