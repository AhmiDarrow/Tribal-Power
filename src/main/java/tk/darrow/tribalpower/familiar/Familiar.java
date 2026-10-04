package tk.darrow.tribalpower.familiar;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import tk.darrow.tribalpower.entity.CreatureProfile;

/** Bonded lattice creature: the three gentle animals and the six tameable remnants. */
public interface Familiar {
    CreatureProfile profile();
    FamiliarData lattice();
    Optional<UUID> ownerUUID();
    boolean isBonded();
    boolean isOwnedBy(Entity entity);
    Player getOwner();
    void bond(Player owner);
    boolean isSitting();
    void setSitting(boolean sitting);
    boolean unableToMoveToOwner();
    /** {@link FamiliarData#pack()} as last synced, readable on the client. */
    int syncedStats();
    /** {@link FamiliarData#voice()} as last synced, readable on the client; null before it is attuned. */
    tk.darrow.tribalpower.api.pulse.Attunement syncedVoice();
    /** Rolls its wild threads if it has none yet, so a voice set now is saved with them. */
    void ensureLattice(net.minecraft.util.RandomSource random, boolean march);
    /** Pushes {@link #lattice()} to its attributes and to the client. */
    void applyLattice();
    default Mob asMob() { return (Mob) this; }
}
