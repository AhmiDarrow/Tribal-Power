package tk.darrow.tribalpower.tribe;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /tribalpower standing [player]} prints all nine standings. */
public final class TribeCommands {
    private TribeCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tribalpower")
                .then(Commands.literal("standing")
                        .executes(ctx -> report(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> report(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static int report(CommandSourceStack source, ServerPlayer player) {
        source.sendSuccess(() -> Component.translatable("message.tribalpower.standing.header", player.getDisplayName())
                .append(TribeStanding.report(source.getServer(), player.getUUID())), false);
        return 1;
    }
}
