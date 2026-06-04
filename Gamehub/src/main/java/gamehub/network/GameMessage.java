package gamehub.network;

import java.io.Serializable;

/**
 * All data exchanged over the LAN connection is wrapped in this class.
 * It is sent as a serialized Java object over an ObjectOutputStream.
 */
public class GameMessage implements Serializable {

    // Required by Java serialization – bump this if we change the class fields.
    private static final long serialVersionUID = 1L;

    // ── Message types ─────────────────────────────────────────────────────────
    public enum Type {
        /** Server assigns the client a player color/symbol. Payload: stringPayload = "RED" | "YELLOW" */
        PLAYER_ASSIGN,

        /** A player made a move. Payload: intPayload = column index (Connect4) or cell index (TicTacToe) */
        MOVE,

        /** One side requests a restart. No payload needed. */
        RESTART_REQ,

        /** The other side acknowledges the restart. No payload needed. */
        RESTART_ACK,

        /** A player has disconnected gracefully. */
        DISCONNECT
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private final Type   type;
    private final int    intPayload;
    private final String stringPayload;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Message with no payload (e.g. RESTART_REQ, DISCONNECT). */
    public GameMessage(Type type) {
        this(type, -1, null);
    }

    /** Message with an integer payload (e.g. MOVE). */
    public GameMessage(Type type, int intPayload) {
        this(type, intPayload, null);
    }

    /** Message with a string payload (e.g. PLAYER_ASSIGN). */
    public GameMessage(Type type, String stringPayload) {
        this(type, -1, stringPayload);
    }

    /** Full constructor. */
    public GameMessage(Type type, int intPayload, String stringPayload) {
        this.type          = type;
        this.intPayload    = intPayload;
        this.stringPayload = stringPayload;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Type   getType()          { return type; }
    public int    getIntPayload()    { return intPayload; }
    public String getStringPayload() { return stringPayload; }

    @Override
    public String toString() {
        return "GameMessage{type=" + type
                + ", int=" + intPayload
                + ", str=" + stringPayload + "}";
    }
}