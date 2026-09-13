package game.sanguo.core.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Invalid commands are values, not exceptions, and must leave state and the RNG untouched. */
public final class CommandResult {
    public enum Error {
        NONE, WRONG_PHASE, WRONG_FORCE, UNKNOWN_UNIT, DEAD_UNIT, ALREADY_ACTED, STATUS_BLOCKED,
        INVALID_DESTINATION, OCCUPIED, NO_PATH, FRIENDLY_TARGET, OUT_OF_RANGE, UNKNOWN_TACTIC,
        WRONG_WEAPON, INSUFFICIENT_ENERGY, TACTIC_CONDITION, UNKNOWN_STRONGHOLD
    }
    public final boolean ok;
    public final Error error;
    public final String message;
    public final int damage;
    public final List<HexPos> path;
    private CommandResult(Error error, String message, int damage, List<HexPos> path) {
        this.ok = error == Error.NONE; this.error = error; this.message = message; this.damage = damage;
        this.path = Collections.unmodifiableList(new ArrayList<>(path));
    }
    static CommandResult fail(Error error, String message) {
        return new CommandResult(error, message, 0, Collections.emptyList());
    }
    static CommandResult success(String message) {
        return new CommandResult(Error.NONE, message, 0, Collections.emptyList());
    }
    static CommandResult hit(int damage) {
        return new CommandResult(Error.NONE, "Attack resolved", damage, Collections.emptyList());
    }
    static CommandResult moved(List<HexPos> path) {
        return new CommandResult(Error.NONE, "Moved", 0, path);
    }
}
