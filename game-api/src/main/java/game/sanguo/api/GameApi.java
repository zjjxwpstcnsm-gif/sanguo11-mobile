package game.sanguo.api;

import java.util.function.Consumer;

/** Called on the configured serial logic thread. Subscriptions never own a platform page. */
public interface GameApi {
    interface Subscription extends AutoCloseable { @Override void close(); }
    StateToken state();
    boolean busy();
    CommandResult execute(GameCommand command);
    GameSnapshot snapshot();
    Subscription subscribe(Consumer<GameEvent> listener);
}
