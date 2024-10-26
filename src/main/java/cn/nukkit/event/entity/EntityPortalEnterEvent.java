package cn.nukkit.event.entity;

import cn.nukkit.entity.Entity;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import lombok.Getter;
import lombok.NonNull;

public class EntityPortalEnterEvent extends EntityEvent implements Cancellable {

    @Getter private static final HandlerList handlers = new HandlerList();

    private final PortalType type;
    private @NonNull Position to;

    /**
     * If is allowed to spawn a portal at the destination.
     */
    private boolean canSpawnPortal = true;

    public EntityPortalEnterEvent(@NonNull Entity entity, @NonNull PortalType type, @NonNull Position to) {
        this.entity = entity;
        this.type = type;

        this.to = to;
    }

    public PortalType getPortalType() {
        return type;
    }

    public @NonNull Position getTo() {
        return to;
    }

    public void setTo(@NonNull Position to) {
        this.to = to;
    }

    public boolean canSpawnPortal() {
        return this.canSpawnPortal;
    }

    public void setCanSpawnPortal(boolean canSpawnPortal) {
        this.canSpawnPortal = canSpawnPortal;
    }

    public enum PortalType {
        NETHER,
        END
    }
}
