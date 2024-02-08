package cn.nukkit.event.entity;

import cn.nukkit.entity.Entity;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.level.Location;
import lombok.NonNull;

public class EntityPortalEnterEvent extends EntityEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final PortalType type;
    private @NonNull Location to;

    public static HandlerList getHandlers() {
        return handlers;
    }

    public EntityPortalEnterEvent(Entity entity, PortalType type) {
        this.entity = entity;
        this.type = type;

        this.to = entity.getLocation();
    }

    public PortalType getPortalType() {
        return type;
    }

    public @NonNull Location getTo() {
        return to;
    }

    public void setTo(@NonNull Location to) {
        this.to = to;
    }

    public enum PortalType {
        NETHER,
        END
    }
}
