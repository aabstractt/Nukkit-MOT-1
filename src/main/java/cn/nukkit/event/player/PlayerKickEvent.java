package cn.nukkit.event.player;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.lang.TextContainer;

public class PlayerKickEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public enum Reason {
        NEW_CONNECTION,
        KICKED_BY_ADMIN,
        NOT_WHITELISTED,
        IP_BANNED,
        NAME_BANNED,
        INVALID_PVE,
        INVALID_PVP,
        LOGIN_TIMEOUT,
        SERVER_FULL,
        FLYING_DISABLED,
        INVALID_PACKET,
        UNKNOWN;

        @Override
        public String toString() {
            return this.name();
        }
    }

    public static HandlerList getHandlers() {
        return handlers;
    }

    protected TextContainer quitMessage;

    protected final Reason reason;
    protected final String reasonString;

    private final String extraData;

    public PlayerKickEvent(Player player, String reason, String quitMessage) {
        this(player, Reason.UNKNOWN, reason, new TextContainer(quitMessage));
    }

    public PlayerKickEvent(Player player, String reason, TextContainer quitMessage) {
        this(player, Reason.UNKNOWN, reason, quitMessage);
    }

    public PlayerKickEvent(Player player, Reason reason, TextContainer quitMessage) {
        this(player, reason, reason.toString(), quitMessage);
    }

    public PlayerKickEvent(Player player, Reason reason, String quitMessage) {
        this(player, reason, new TextContainer(quitMessage));
    }

    public PlayerKickEvent(Player player, Reason reason, String reasonString, TextContainer quitMessage) {
        this(player, reason, reasonString, quitMessage, "");
    }

    public PlayerKickEvent(Player player, Reason reason, String reasonString, TextContainer quitMessage, String extraData) {
        this.player = player;
        this.quitMessage = quitMessage;
        this.reason = reason;
        this.reasonString = reasonString;
        this.extraData = extraData;
    }

    public String getReason() {
        return reasonString;
    }

    public Reason getReasonEnum() {
        return this.reason;
    }

    public TextContainer getQuitMessage() {
        return quitMessage;
    }

    public void setQuitMessage(TextContainer quitMessage) {
        this.quitMessage = quitMessage;
    }

    public void setQuitMessage(String joinMessage) {
        this.setQuitMessage(new TextContainer(joinMessage));
    }

    public String getExtraData() {
        return extraData;
    }
}
