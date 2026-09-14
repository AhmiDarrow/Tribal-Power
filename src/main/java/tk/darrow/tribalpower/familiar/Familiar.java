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
    default Mob asMob() { return (Mob) this; }
}
