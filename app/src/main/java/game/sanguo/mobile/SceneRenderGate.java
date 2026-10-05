package game.sanguo.mobile;

import java.util.Objects;
import java.util.function.Consumer;

/** Owner-thread presentation lifecycle. A covered window retains its last frame,
 * but must not compete with a focused dialog's renderer or Android's UI renderer.
 * These flags never change a session, command, camera, preference or save. */
final class SceneRenderGate {
    private final Thread owner = Thread.currentThread();
    private final Consumer<Boolean> changed;
    private boolean resumed = true, focused = true, visible = true, closed;

    SceneRenderGate(Consumer<Boolean> changed) { this.changed = Objects.requireNonNull(changed); }
    boolean active() { return !closed && resumed && focused && visible; }
    void resumed(boolean value) { update(0, value); }
    void focused(boolean value) { update(1, value); }
    void visible(boolean value) { update(2, value); }
    void close() { update(3, true); }

    private void update(int flag, boolean value) {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Render lifecycle owner thread required");
        if (closed) return;
        boolean before = active();
        switch (flag) {
            case 0: resumed = value; break;
            case 1: focused = value; break;
            case 2: visible = value; break;
            case 3: closed = true; break;
            default: throw new AssertionError(flag);
        }
        boolean after = active();
        if (before != after) changed.accept(after);
    }
}
