package tk.darrow.tribalpower.kit;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.entity.MarchThreat;

/**
 * A soul urn. Use it on a creature to draw it inside -- anything living but a player, a boss or an elite -- and
 * use it on a block to set the creature back down there. Each capture and each release is a use; woven, copper
 * and manifested urns wear out, a resonant urn never does. An urn only takes a creature while it still has a use
 * left to let it out again.
 */
public class SoulUrnItem extends Item {
    public enum Tier {
        WOVEN, COPPER, MANIFESTED, RESONANT;

        public String id() { return name().toLowerCase(Locale.ROOT) + "_soul_urn"; }

        /** Uses a fresh urn has; -1 is endless. */
        public int uses() {
            return switch (this) {
                case WOVEN -> TribalConfig.wovenUrnUses();
                case COPPER -> TribalConfig.copperUrnUses();
                case MANIFESTED -> TribalConfig.manifestedUrnUses();
                case RESONANT -> -1;
            };
        }
    }

    public static final String SOUL = "Soul", TYPE = "SoulType", USED = "UsesSpent";
    /** What must not come back with the creature: it gets a fresh identity and none of its old motion. */
    private static final List<String> SHED = List.of("UUID", "Motion", "FallDistance", "Passengers", "leash", "Pos");

    private final Tier tier;

    public SoulUrnItem(Tier tier, Properties properties) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    public Tier tier() { return tier; }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
    }

    public static boolean full(ItemStack stack) {
        return tag(stack).contains(SOUL);
    }

    /** Uses left, or -1 when endless. */
    public int usesLeft(ItemStack stack) {
        int total = tier.uses();
        return total < 0 ? -1 : Math.max(0, total - tag(stack).getInt(USED));
    }

    /** Why a creature cannot go in an urn for this player, or null when it can. */
    public static @Nullable String refusal(Entity entity) {
        return refusal(entity, null);
    }

    public static @Nullable String refusal(Entity entity, @Nullable Player player) {
        if (!(entity instanceof LivingEntity living) || entity instanceof Player || !entity.isAlive() || entity.isInvulnerable())
            return "message.tribalpower.soul_urn.not_this";
        // Nobody bottles another player's companion.
        if (player != null && entity instanceof tk.darrow.tribalpower.familiar.Familiar familiar && familiar.isBonded() && !familiar.isOwnedBy(player))
            return "message.tribalpower.soul_urn.not_yours";
        if (player != null && entity instanceof net.minecraft.world.entity.OwnableEntity pet && pet.getOwnerUUID() != null
                && !pet.getOwnerUUID().equals(player.getUUID())) return "message.tribalpower.soul_urn.not_yours";
        if (entity instanceof net.minecraft.world.entity.npc.AbstractVillager villager && villager.isTrading())
            return "message.tribalpower.soul_urn.busy";
        if (entity.getType().is(Tags.EntityTypes.BOSSES) || entity.getType().is(Tags.EntityTypes.CAPTURING_NOT_SUPPORTED)
                || entity instanceof tk.darrow.tribalpower.entity.LatticeMonster monster && monster.profile().boss())
            return "message.tribalpower.soul_urn.boss";
        var health = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (health != null && health.hasModifier(MarchThreat.ELITE)) return "message.tribalpower.soul_urn.elite";
        return null;
    }

    /** Draws a creature into an empty urn. Returns whether it went in. */
    public boolean capture(Player player, ItemStack urn, Entity target) {
        if (full(urn)) {
            player.displayClientMessage(Component.translatable("message.tribalpower.soul_urn.full"), true);
            return false;
        }
        String refused = refusal(target, player);
        if (refused != null) {
            player.displayClientMessage(Component.translatable(refused), true);
            return false;
        }
        int left = usesLeft(urn);
        if (left >= 0 && left < 2) {
            player.displayClientMessage(Component.translatable("message.tribalpower.soul_urn.worn"), true);
            return false;
        }
        CompoundTag soul = new CompoundTag();
        target.ejectPassengers();
        target.stopRiding();
        if (!target.save(soul)) return false;
        SHED.forEach(soul::remove);
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY(0.5), target.getZ(), 16, 0.3, 0.4, 0.3, 0.03);
            level.playSound(null, target.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        target.discard();
        CustomData.update(DataComponents.CUSTOM_DATA, urn, tag -> {
            tag.put(SOUL, soul);
            tag.putString(TYPE, EntityType.getKey(target.getType()).toString());
            tag.putInt(USED, tag.getInt(USED) + 1);
        });
        urn.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        return true;
    }

    /** Sets the held creature down on a block face. Returns whether it came out. */
    public boolean release(Player player, ItemStack urn, ServerLevel level, BlockPos at) {
        CompoundTag soul = tag(urn).getCompound(SOUL).copy();
        Entity entity = EntityType.loadEntityRecursive(soul, level, e -> {
            e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot() + 180, 0);
            return e;
        });
        if (entity == null || !level.addFreshEntity(entity)) {
            player.displayClientMessage(Component.translatable("message.tribalpower.soul_urn.failed"), true);
            return false;
        }
        level.sendParticles(ParticleTypes.SOUL, entity.getX(), entity.getY(0.5), entity.getZ(), 16, 0.3, 0.4, 0.3, 0.03);
        level.playSound(null, at, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
        CustomData.update(DataComponents.CUSTOM_DATA, urn, tag -> {
            tag.remove(SOUL);
            tag.remove(TYPE);
            tag.putInt(USED, tag.getInt(USED) + 1);
        });
        urn.remove(DataComponents.CUSTOM_MODEL_DATA);
        if (usesLeft(urn) == 0 && !player.getAbilities().instabuild) {
            level.playSound(null, at, SoundEvents.DECORATED_POT_SHATTER, SoundSource.PLAYERS, 0.8F, 1.0F);
            urn.shrink(1);
        }
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack urn = context.getItemInHand();
        if (!full(urn) || context.getPlayer() == null) return InteractionResult.PASS;
        if (context.getLevel() instanceof ServerLevel level)
            release(context.getPlayer(), urn, level, context.getClickedPos().relative(context.getClickedFace()));
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!full(stack)) return super.getName(stack);
        var type = EntityType.byString(tag(stack).getString(TYPE));
        return type.<Component>map(t -> Component.translatable("item.tribalpower.soul_urn.holding", super.getName(stack), t.getDescription()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override public boolean isFoil(ItemStack stack) { return full(stack) || tier == Tier.RESONANT; }
    @Override public boolean isBarVisible(ItemStack stack) { return tier != Tier.RESONANT && tag(stack).getInt(USED) > 0; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * usesLeft(stack) / Math.max(1, tier.uses())); }
    @Override public int getBarColor(ItemStack stack) { return Mth.hsvToRgb(0.45F, 0.7F, 0.9F); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(full(stack) ? "item.tribalpower.soul_urn.release" : "item.tribalpower.soul_urn.capture")
                .withStyle(ChatFormatting.GRAY));
        int left = usesLeft(stack);
        lines.add(left < 0 ? Component.translatable("item.tribalpower.soul_urn.endless").withStyle(ChatFormatting.DARK_AQUA)
                : Component.translatable("item.tribalpower.soul_urn.uses", left, tier.uses()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
