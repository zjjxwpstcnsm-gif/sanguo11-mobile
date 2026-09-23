package game.sanguo.runtime;

import game.sanguo.api.StateToken;
import game.sanguo.core.World;

/** Migration-only detached read/draft model, NEVER the session's authoritative World.
 * A view is valid for at most one attempted command. Existing UI closures reference this
 * draft; accepted results are copied before installation so retained references cannot write authority. */
public final class LegacyView {
    public final StateToken state;
    public final World draft;
    LegacyView(StateToken state,World draft){this.state=state;this.draft=draft;}
}
