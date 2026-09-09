package tk.darrow.tribalpower.api;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * A block entity that can explain its own state. The Spirit Codex prints these lines
 * (after the generic {@link Diagnostics#report} header) when sneak-used on the block.
 */
public interface Diagnosable {
    List<Component> diagnose(ServerLevel level, BlockPos pos);
}
