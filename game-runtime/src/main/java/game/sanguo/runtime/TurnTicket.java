package game.sanguo.runtime;

import game.sanguo.api.StateToken;

/** Immutable initial capture and compare-and-set token for an isolated turn computation. */
public final class TurnTicket {
    public final StateToken expected;
    private final byte[] initial;
    TurnTicket(StateToken expected,byte[] initial){this.expected=expected;this.initial=initial.clone();}
    public byte[] initial(){return initial.clone();}
}
