package tk.darrow.tribalpower.verification;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** GameTestHelper.makeMockServerPlayerInLevel is deprecated for removal in NeoForge 21.1. */
final class VerificationPlayers {
    private VerificationPlayers() {}

    @SuppressWarnings({"removal", "deprecation"})
    static ServerPlayer inLevel(GameTestHelper h) {
        return h.makeMockServerPlayerInLevel();
    }
}
