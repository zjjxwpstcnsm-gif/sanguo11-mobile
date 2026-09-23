package game.sanguo.mobile;

import game.sanguo.core.World;
import java.util.function.Supplier;

/** Temporary native forms adapter: passing a result is forbidden because it executes too early. */
interface LegacyCommandSink {
    World.Result execute(World expectedDraft, Supplier<World.Result> operation);
}
