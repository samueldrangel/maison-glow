package com.maisonglow.view;

/**
 * Implemented by the controller of a screen that can have work in flight (a chat waiting for an answer, a photo being
 * processed) or something pending (an action waiting for a confirmation). The {@link Navigator} calls {@link #dispose}
 * when the screen is discarded, which happens when the session is closed, so that an answer arriving late never shows
 * up in the next session and nothing pending can be confirmed afterwards.
 */
public interface ViewLifecycle {

    /** Discards what is pending and ignores any answer that arrives from now on. Calling it twice does nothing. */
    void dispose();
}
