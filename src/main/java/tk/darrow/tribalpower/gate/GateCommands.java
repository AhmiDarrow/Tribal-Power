package tk.darrow.tribalpower.gate;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

/** {@code /tribalpower gate list|unlink} (design 3.1 section 8). */
public final class GateCommands {
    private GateCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tribalpower").then(Commands.literal("gate")
                .then(Commands.literal("list").executes(GateCommands::list))
                .then(Commands.literal("unlink").then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(c -> unlink(c, StringArgumentType.getString(c, "name")))))));
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GateSavedData data = GateSavedData.get(player.server);
        List<GateSavedData.Gate> gates = data.owned(player.getUUID());
        if (gates.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("message.tribalpower.gate.none"));
            return 0;
        }
        for (GateSavedData.Gate gate : gates) {
            GateSavedData.Gate partner = data.gate(gate.partner());
            context.getSource().sendSuccess(() -> Component.translatable("message.tribalpower.gate.list_entry",
                    Component.literal(gate.name()).withStyle(ChatFormatting.AQUA),
                    gate.dimension(), gate.pos().getX(), gate.pos().getY(), gate.pos().getZ(),
                    partner == null
                            ? Component.translatable("message.tribalpower.gate.list_unlinked").withStyle(ChatFormatting.GRAY)
                            : Component.literal(partner.name()).withStyle(ChatFormatting.GREEN)), false);
        }
        return gates.size();
    }

    private static int unlink(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GateSavedData data = GateSavedData.get(player.server);
        GateSavedData.Gate gate = data.byName(player.getUUID(), name);
        if (gate == null) {
            context.getSource().sendFailure(Component.translatable("message.tribalpower.gate.unknown", name));
            return 0;
        }
        GateSavedData.Gate partner = data.unlink(gate.id());
        context.getSource().sendSuccess(() -> partner == null
                ? Component.translatable("message.tribalpower.gate.already_unlinked", gate.name())
                : Component.translatable("message.tribalpower.gate.unlinked_pair", gate.name(), partner.name()), false);
        return 1;
    }
}
