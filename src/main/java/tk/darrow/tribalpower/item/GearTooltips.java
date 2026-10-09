package tk.darrow.tribalpower.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;

/**
 * The lines under a Spiritgear piece's rank and voice: what this rank and this voice give this very piece, with the
 * numbers it works to now. Every figure is read from {@link TribalConfig} or the piece's rank at the moment the
 * tooltip is drawn, so a Manifested blade shows Manifested numbers and a pack's rebalance shows through.
 */
public final class GearTooltips {
    private GearTooltips() {}

    /** The rank's own gifts: what the piece pays and how it works at this rank. */
    public static void rank(ItemStack stack, List<Component> lines) {
        int rank = SpiritGear.rank(stack);
        var item = stack.getItem();
        if (item instanceof SpiritweaveArmor) {
            lines.add(grey("item.tribalpower.spiritgear.upkeep", SpiritGear.armorCost(stack), seconds(TribalConfig.armorUpkeepTicks())));
        } else if (item instanceof SpiritgearBladeItem) {
            lines.add(grey("item.tribalpower.spiritgear.cost_blow", SpiritGear.hitCost(stack)));
        } else if (item instanceof SpiritgearRattleItem) {
            lines.add(grey("item.tribalpower.spiritgear.cost_use", SpiritGear.useCost(stack)));
        } else {
            lines.add(grey("item.tribalpower.spiritgear.cost_tool", SpiritGear.mineCost(stack), SpiritGear.useCost(stack)));
            if (rank > 0) lines.add(grey("item.tribalpower.spiritgear.mining", percent(SpiritGear.mining(stack) - 1)));
        }
        if (rank >= 2 && !(item instanceof SpiritweaveArmor)) lines.add(grey("item.tribalpower.spiritgear.spared"));
        if (rank >= 3) lines.add(grey("item.tribalpower.spiritgear.doubled"));
    }

    /** What the bound voice does on this piece, at this rank. Nothing is added for a voice this piece ignores. */
    public static void voice(ItemStack stack, Attunement voice, List<Component> lines) {
        boolean m = SpiritGear.rank(stack) >= 3;
        var item = stack.getItem();
        String v = voice.getSerializedName();
        if (item instanceof SpiritgearBladeItem) {
            lines.add(green("item.tribalpower.spiritgear_blade.echo", number(TribalConfig.bladeEchoDamage()), seconds(TribalConfig.bladeGlowTicks())));
            switch (voice) {
                case EARTH -> add(lines, "blade", v, level(m ? TribalConfig.bladeEarthSlownessLevelManifested() : TribalConfig.bladeEarthSlownessLevel()), seconds(TribalConfig.bladeEarthSlownessTicks()));
                case FIRE -> add(lines, "blade", v, m ? TribalConfig.bladeFireSecondsManifested() : TribalConfig.bladeFireSeconds(), number(m ? TribalConfig.bladeFireDamageManifested() : TribalConfig.bladeFireDamage()));
                case WATER -> add(lines, "blade", v, number(m ? TribalConfig.bladeWaterHealManifested() : TribalConfig.bladeWaterHeal()));
                case AIR -> add(lines, "blade", v, number(m ? TribalConfig.bladeAirSweepManifested() : TribalConfig.bladeAirSweep()));
                case SPIRIT -> add(lines, "blade", v, number(TribalConfig.bladeEchoDamage() + (m ? TribalConfig.bladeSpiritEchoManifested() : TribalConfig.bladeSpiritEcho())));
                case LOOM -> add(lines, "blade", v, number(m ? TribalConfig.bladeLoomPullManifested() : TribalConfig.bladeLoomPull()));
            }
            return;
        }
        if (item instanceof SpiritgearPickaxeItem) {
            switch (voice) {
                case EARTH, FIRE, WATER, LOOM -> add(lines, "pickaxe", v);
                case AIR -> add(lines, "pickaxe", v, level(m ? 2 : 1), seconds(TribalConfig.pickAirHasteTicks()));
                case SPIRIT -> add(lines, "pickaxe", v, m ? TribalConfig.spiritPickGlintRangeManifested() : TribalConfig.spiritPickGlintRange());
            }
            return;
        }
        if (item instanceof SpiritgearShovelItem) {
            switch (voice) {
                case EARTH, FIRE, AIR, LOOM -> add(lines, "shovel", v);
                case WATER -> add(lines, "shovel", v, percent(chance(stack, TribalConfig.shovelWaterClayChance())));
                case SPIRIT -> add(lines, "shovel", v, TribalConfig.spiritShovelGlintRange());
            }
            return;
        }
        if (item instanceof SpiritgearAxeItem) {
            switch (voice) {
                case EARTH, FIRE, AIR, LOOM -> add(lines, "axe", v);
                case WATER -> add(lines, "axe", v, percent(chance(stack, TribalConfig.axeWaterSaplingChance())));
                case SPIRIT -> add(lines, "axe", v, TribalConfig.axeSpiritGlowRange(), seconds(TribalConfig.axeSpiritGlowTicks()));
            }
            return;
        }
        if (item instanceof SpiritgearHoeItem) {
            switch (voice) {
                case EARTH, FIRE, AIR, LOOM -> add(lines, "hoe", v);
                case WATER -> add(lines, "hoe", v, m ? TribalConfig.hoeWaterMoistenRadiusManifested() : TribalConfig.hoeWaterMoistenRadius());
                case SPIRIT -> add(lines, "hoe", v, percent(chance(stack, TribalConfig.hoeSpiritBountyChance())));
            }
            return;
        }
        if (item instanceof SpiritgearShearsItem) {
            switch (voice) {
                case EARTH, FIRE, AIR, LOOM -> add(lines, "shears", v);
                case WATER -> add(lines, "shears", v, percent(m ? 1.0 : TribalConfig.shearsWaterRegrowChance()));
                case SPIRIT -> add(lines, "shears", v, seconds(TribalConfig.shearsSpiritRegenTicks()), m ? TribalConfig.shearsSpiritGlowRangeManifested() : TribalConfig.shearsSpiritGlowRange());
            }
            return;
        }
        if (item instanceof SpiritgearRattleItem) {
            switch (voice) {
                case EARTH, AIR, LOOM -> add(lines, "rattle", v, seconds(TribalConfig.rattleVoiceTicks()));
                case FIRE -> add(lines, "rattle", v, seconds(TribalConfig.rattleFireTicks()));
                case WATER -> add(lines, "rattle", v, number(TribalConfig.rattleWaterBonus()));
                case SPIRIT -> add(lines, "rattle", v);
            }
            return;
        }
        if (item instanceof SpiritweaveArmor armor) {
            String piece = switch (armor.getType()) { case HELMET -> "hood"; case CHESTPLATE -> "robe"; case LEGGINGS -> "leggings"; default -> "boots"; };
            armorVoice(lines, piece, voice, m);
        }
    }

    /** The unlinked piece's own gift; a voice replaces it. */
    public static void unlinked(ItemStack stack, List<Component> lines) {
        var item = stack.getItem();
        if (item instanceof SpiritgearBladeItem) {
            lines.add(green("item.tribalpower.spiritgear_blade.echo", number(TribalConfig.bladeEchoDamage()), seconds(TribalConfig.bladeGlowTicks())));
        } else if (item instanceof SpiritgearRattleItem) {
            rattleHeal(stack, lines);
        } else if (item instanceof SpiritweaveArmor armor) {
            switch (armor.getType()) {
                case HELMET -> lines.add(green("item.tribalpower.spiritweave_hood.plain", seconds(TribalConfig.armorNightVisionTicks())));
                case CHESTPLATE -> lines.add(green("item.tribalpower.spiritweave_robe.plain", seconds(TribalConfig.armorResistanceTicks())));
                case LEGGINGS -> lines.add(green("item.tribalpower.spiritweave_leggings.plain", seconds(TribalConfig.armorSpeedTicks())));
                default -> lines.add(green("item.tribalpower.spiritweave_boots.plain", seconds(TribalConfig.bootsSlowFallTicks())));
            }
        }
    }

    /** The rattle's healing at this rank; the Water voice multiplies it. */
    public static void rattleHeal(ItemStack stack, List<Component> lines) {
        double heal = TribalConfig.rattleHeal() * (1 + SpiritGear.rank(stack) * TribalConfig.rattleRankBonus());
        if (SpiritGear.voice(stack).orElse(null) == Attunement.WATER) heal *= TribalConfig.rattleWaterBonus();
        lines.add(green("item.tribalpower.spiritgear_rattle.heal", number(heal)));
        if (SpiritGear.rank(stack) >= 2) lines.add(green("item.tribalpower.spiritgear_rattle.sickness"));
    }

    private static void armorVoice(List<Component> lines, String piece, Attunement voice, boolean m) {
        String v = voice.getSerializedName();
        switch (piece) {
            case "hood" -> {
                switch (voice) {
                    case EARTH -> add(lines, "hood", v, percent(m ? TribalConfig.earthHoodBlockChanceManifested() : TribalConfig.earthHoodBlockChance()));
                    case FIRE -> add(lines, "hood", v, seconds(TribalConfig.armorFireResistanceTicks()));
                    case WATER -> add(lines, "hood", v, seconds(TribalConfig.armorWaterBreathingTicks()));
                    case AIR -> add(lines, "hood", v, seconds(TribalConfig.armorNightVisionTicks()));
                    case SPIRIT -> add(lines, "hood", v, seconds(TribalConfig.armorNightVisionTicks()), TribalConfig.spiritHoodGlowRange());
                    case LOOM -> add(lines, "hood", v, seconds(TribalConfig.armorLuckTicks()));
                }
            }
            case "robe" -> {
                switch (voice) {
                    case EARTH -> add(lines, "robe", v, level(m ? 2 : 1), seconds(TribalConfig.armorResistanceTicks()));
                    case FIRE -> add(lines, "robe", v, seconds(TribalConfig.armorFireResistanceTicks()), TribalConfig.fireRobeIgniteSeconds());
                    case WATER -> add(lines, "robe", v, seconds(m ? TribalConfig.waterRobeRegenTicksManifested() : TribalConfig.waterRobeRegenTicks()));
                    case SPIRIT -> add(lines, "robe", v, seconds(TribalConfig.armorResistanceTicks()), seconds(TribalConfig.spiritRobeGlowTicks()));
                    case LOOM -> add(lines, "robe", v, percent(m ? TribalConfig.loomRobeRefundChanceManifested() : TribalConfig.loomRobeRefundChance()));
                    case AIR -> { }
                }
            }
            case "leggings" -> {
                switch (voice) {
                    case EARTH, LOOM -> add(lines, "leggings", v);
                    case FIRE -> add(lines, "leggings", v, seconds(TribalConfig.armorSpeedTicks()));
                    case WATER -> add(lines, "leggings", v, seconds(TribalConfig.armorDolphinsGraceTicks()));
                    case AIR -> add(lines, "leggings", v, level(m ? 2 : 1), seconds(TribalConfig.armorSpeedTicks()));
                    case SPIRIT -> add(lines, "leggings", v, seconds(TribalConfig.armorSpeedTicks()));
                }
            }
            default -> {
                switch (voice) {
                    case AIR -> add(lines, "boots", v, seconds(TribalConfig.bootsSlowFallTicks()));
                    case FIRE -> add(lines, "boots", v);
                    case WATER -> add(lines, "boots", v, m ? TribalConfig.waterBootsFreezeRadiusManifested() : TribalConfig.waterBootsFreezeRadius());
                    case LOOM -> add(lines, "boots", v, m ? TribalConfig.loomBootsStitchReachManifested() : TribalConfig.loomBootsStitchReach(), seconds(TribalConfig.loomBootsStitchCooldownTicks()));
                    case EARTH, SPIRIT -> { }
                }
            }
        }
    }

    // ---- formatting

    private static void add(List<Component> lines, String piece, String voice, Object... args) {
        lines.add(green("item.tribalpower.spiritgear_" + piece + ".voice." + voice, args));
    }

    private static Component green(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.DARK_GREEN);
    }

    private static Component grey(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    /** A Manifested piece doubles its voice chances, as {@link SpiritGear#chance} does. */
    private static double chance(ItemStack stack, double base) {
        return SpiritGear.rank(stack) >= 3 ? Math.min(1.0, base * 2) : base;
    }

    public static String number(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    static String seconds(int ticks) {
        return number(ticks / 20.0);
    }

    public static String percent(double fraction) {
        return number(Math.round(fraction * 1000) / 10.0);
    }

    static String level(int level) {
        return switch (level) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> Integer.toString(level); };
    }
}
