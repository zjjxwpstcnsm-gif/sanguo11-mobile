package game.sanguo.mobile;

import android.app.Activity;
import android.os.Bundle;

/** Test Lab status-protocol adapter. The original touch probe owns all assertions. */
public final class FirebaseColdStartInstrumentation extends NativeColdStartInstrumentation {
    private boolean listOnly;

    @Override public void onCreate(Bundle arguments) {
        listOnly = arguments != null && "true".equals(arguments.getString("log"));
        super.onCreate(arguments);
    }

    private Bundle status() {
        Bundle status = new Bundle();
        status.putString("id", "InstrumentationTestRunner");
        status.putString("class", getClass().getName());
        status.putString("test", "normalColdStartToNativeGame");
        status.putInt("numtests", 1);
        status.putInt("current", 1);
        return status;
    }

    @Override public void onStart() {
        sendStatus(1, status());
        if (listOnly) {
            sendStatus(0, status());
            super.finish(Activity.RESULT_OK, new Bundle());
            return;
        }
        super.onStart();
    }

    @Override public void finish(int resultCode, Bundle results) {
        String stream = results == null ? "" : results.getString("stream", "");
        boolean passed = resultCode == Activity.RESULT_OK
                && stream.startsWith("PASS COLD_START scoped new-game UI checks=");
        Bundle status = status();
        status.putString("stream", stream);
        if (!passed) status.putString("stack", stream.isEmpty()
                ? "Cold-start probe terminated without an explicit PASS" : stream);
        sendStatus(passed ? 0 : -2, status);
        // Android test parsers read per-test status; -1 means the runner completed.
        super.finish(Activity.RESULT_OK, results == null ? new Bundle() : results);
    }
}
