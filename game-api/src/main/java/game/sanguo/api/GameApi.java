package game.sanguo.api;

import java.util.function.Consumer;

/** Called on the configured serial logic thread. Subscriptions never own a platform page. */
public interface GameApi {
    interface Subscription extends AutoCloseable { @Override void close(); }
    StateToken state();
    boolean busy();
    CommandResult execute(GameCommand command);
    CommandResult execute(ContestCommand command);
    DeploymentPreview preview(DeploymentCommand command);
    CommandResult execute(DeploymentCommand command);
    DiplomacyPreview preview(DiplomacyCommand command);
    CommandResult execute(DiplomacyCommand command);
    ConstructionPreview preview(ConstructionCommand command);
    CommandResult execute(ConstructionCommand command);
    TransportPreview preview(TransportCommand command);
    CommandResult execute(TransportCommand command);
    CityActionPreview preview(CityActionCommand command);
    CommandResult execute(CityActionCommand command);
    TradePreview preview(TradeCommand command);
    CommandResult execute(TradeCommand command);
    ProductionPreview preview(ProductionCommand command);
    CommandResult execute(ProductionCommand command);
    GameSnapshot snapshot();
    Subscription subscribe(Consumer<GameEvent> listener);
}
