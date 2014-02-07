package com.github.wolfie.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.wolfie.history.HistoryExtension.ErrorEvent.Type;
import com.sun.xml.internal.txw2.IllegalAnnotationException;
import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.ui.JavaScriptFunction;
import com.vaadin.ui.UI;

/**
 * @author Henrik Paul
 */
@JavaScript("historyextension.js")
public class HistoryExtension extends AbstractJavaScriptExtension {
    public class PopStateEvent {
        private Map<String, String> map = null;
        private final JSONObject json;

        public PopStateEvent(final JSONObject json) {
            this.json = json;
        }

        /**
         * Returns the state data as an {@link JSONObject}. Never
         * <code>null</code>.
         */
        public JSONObject getStateAsJson() {
            return json;
        }

        /**
         * Returns the state data as an <strong>unmodifiable</strong>
         * {@link Map Map<String, String>}. Never <code>null</code>.
         */
        public Map<String, String> getStateAsMap() {
            if (map == null) {
                final LinkedHashMap<String, String> tempMap = new LinkedHashMap<String, String>();
                for (final String key : JSONObject.getNames(json)) {
                    try {
                        tempMap.put(key, json.getString(key));
                    } catch (final JSONException e) {
                        throw new RuntimeException("org.json.JSONObject "
                                + "seems to have a bug, as it's "
                                + "returning keys on objects "
                                + "that don't exist.", e);
                    }
                }
                map = Collections.unmodifiableMap(tempMap);
            }
            return map;
        }

        /**
         * Returns the {@link HistoryExtension} instance from which this event
         * was fired.
         */
        public HistoryExtension getSource() {
            return HistoryExtension.this;
        }
    }

    /**
     * An interface that allows external code to listen to browser history
     * changes.
     * 
     * @see HistoryExtension#addPopStateListener(PopStateListener)
     * @see HistoryExtension#removePopStateListener(PopStateListener)
     */
    public static interface PopStateListener {
        /**
         * A state was popped off the browser's history stack
         * 
         * @param event
         *            The event object describing the application state
         * @see HistoryExtension#pushState(JSONObject, String)
         * @see HistoryExtension#pushState(Map, String)
         */
        void popState(PopStateEvent event);
    }

    /**
     * An event that carries information about an error that occurred on the
     * client side.
     * <p>
     * <em>Note:</em> if an ErrorEvent is not {@link #cancel() cancelled},
     * {@link HistoryExtension} will raise a runtime exception.
     * 
     * @see #cancel()
     */
    public static class ErrorEvent {
        public enum Type {
            /**
             * HTML 5 history functionality is unsupported by the user's
             * browser.
             */
            UNSUPPORTED,

            /** A client-side error occurred when trying to execute the command. */
            METHOD_INVOKE
        }

        private final Type type;
        private final String name;
        private final String message;
        private boolean cancelled;

        public ErrorEvent(final Type type, final String name,
                final String message) {
            this.type = type;
            this.name = name;
            this.message = message;
        }

        public Type getType() {
            return type;
        }

        public String getErrorName() {
            return name;
        }

        public String getMessage() {
            return message;
        }

        /**
         * TODO
         * 
         * @see #isCancelled()
         */
        public void cancel() {
            cancelled = true;
        }

        /**
         * TODO
         * 
         * @return
         * @see #cancel()
         */
        public boolean isCancelled() {
            return cancelled;
        }
    }

    /**
     * TODO
     * 
     * @see HistoryExtension#addErrorListener(ErrorListener)
     * @see HistoryExtension#removeErrorListener(ErrorListener)
     */
    public static interface ErrorListener {
        void onError(ErrorEvent event);
    }

    private final List<PopStateListener> popListeners = new ArrayList<PopStateListener>();
    private final List<ErrorListener> errorListeners = new ArrayList<ErrorListener>();

    /**
     * A flag that is set <em>asynchronously</em>. It denotes whether the
     * browser supports HTML 5 history manipulation or not.
     * 
     * @see ErrorEvent.Type#UNSUPPORTED
     */
    private boolean unsupported = false;

    /**
     * A convenience method to extend a UI with a properly configured
     * {@link HistoryExtension}.
     */
    public static HistoryExtension extend(final UI ui,
            final PopStateListener listener) {
        final HistoryExtension extension = new HistoryExtension();
        extension.addPopStateListener(listener);
        extension.extend(ui);
        return extension;
    }

    /**
     * A convenience method to extend a UI with a properly configured
     * {@link HistoryExtension}.
     */
    public static HistoryExtension extend(final UI ui,
            final PopStateListener popStateListener,
            final ErrorListener errorListener) {
        final HistoryExtension extension = extend(ui, popStateListener);
        extension.addErrorListener(errorListener);
        return extension;
    }

    public HistoryExtension() {
        addFunction("popstate", new JavaScriptFunction() {
            @Override
            public void call(final JSONArray arguments) throws JSONException {
                if (arguments.length() > 0) {
                    try {
                        final JSONObject jsonObject = arguments
                                .getJSONObject(0);
                        fireListeners(jsonObject);
                    } catch (final JSONException e) {
                        arguments.toString();
                    }
                }
            }
        });

        addFunction("error", new JavaScriptFunction() {
            @Override
            public void call(final JSONArray arguments) throws JSONException {
                final int errorType = arguments.getInt(0);
                final ErrorEvent.Type type = ErrorEvent.Type.values()[errorType];
                final String name = arguments.getString(1);
                final String message = arguments.getString(2);

                final ErrorEvent event = new ErrorEvent(type, name, message);
                fireError(event);
            }
        });
    }

    public void extend(final UI ui) {
        @SuppressWarnings("cast")
        final AbstractClientConnector acc = (AbstractClientConnector) ui;
        super.extend(acc);
    }

    /**
     * TODO
     */
    public void back() {
        callFunction("back");
    }

    /**
     * TODO
     */
    public void forward() {
        callFunction("forward");
    }

    /**
     * TODO
     * 
     * @param steps
     */
    @SuppressWarnings("boxing")
    public void go(final int steps) {
        callFunction("go", steps);
    }

    /**
     * TODO
     * 
     * @param nextStateMap
     * @param nextUrl
     * @see PopStateListener
     * @see PopStateEvent#getStateAsMap()
     */
    public void pushState(final Map<String, String> nextStateMap,
            final String nextUrl) {
        callFunction("pushState", nextStateMap, nextUrl);
    }

    /**
     * TODO
     * 
     * @param nextStateJson
     * @param nextUrl
     * @see PopStateListener
     * @see PopStateEvent#getStateAsMap()
     */
    public void pushState(final JSONObject nextStateJson, final String nextUrl) {
        callFunction("pushState", nextStateJson, nextUrl);
    }

    public void replaceState(final Map<String, String> newStateMap,
            final String newUrl) {
        callFunction("replaceState", newStateMap, newUrl);
    }

    /**
     * TODO
     * 
     * @param newStateJson
     * @param newUrl
     * @see PopStateListener
     * @see PopStateEvent#getStateAsMap()
     */
    public void replaceState(final JSONObject newStateJson, final String newUrl) {
        callFunction("replaceState", newStateJson, newUrl);
    }

    /**
     * Adds a {@link PopStateListener}
     * 
     * @throws IllegalArgumentException
     *             if <code>listener</code> is <code>null</code>
     */
    public void addPopStateListener(final PopStateListener listener)
            throws IllegalArgumentException {
        if (listener == null) {
            throw new IllegalAnnotationException("listener may not be null");
        }
        popListeners.add(listener);
    }

    /**
     * Removes a {@link PopStateListener}
     * 
     * @return <code>true</code> if the listener was successfully found and
     *         removed, otherwise <code>false</code>
     */
    public boolean removePopStateListener(final PopStateListener listener) {
        return popListeners.remove(listener);
    }

    /**
     * Adds an {@link ErrorListener}
     * 
     * @throws IllegalArgumentException
     *             if <code>listener</code> is <code>null</code>
     */
    public void addErrorListener(final ErrorListener listener)
            throws IllegalArgumentException {
        if (listener == null) {
            throw new IllegalAnnotationException("listener may not be null");
        }
        errorListeners.add(listener);
    }

    /**
     * Removes an {@link ErrorListener}
     * 
     * @return <code>true</code> if the listener was successfully found and
     *         removed, otherwise <code>false</code>
     */
    public boolean removeErrorListener(final ErrorListener listener) {
        return errorListeners.remove(listener);
    }

    private void fireListeners(final JSONObject state) {
        final PopStateEvent event = new PopStateEvent(state);
        for (final PopStateListener listener : popListeners) {
            listener.popState(event);
        }
    }

    private void fireError(final ErrorEvent e) {
        for (final ErrorListener listener : errorListeners) {
            listener.onError(e);
        }

        if (!e.isCancelled()) {
            throw new RuntimeException(e.getErrorName() + ": " + e.getMessage());
        }

        if (e.getType() == Type.UNSUPPORTED) {
            unsupported = true;
        }
    }

    @Override
    protected void callFunction(final String name, final Object... arguments) {
        /*
         * This method is overridden to stop all client-side calls if the API is
         * not supported.
         * 
         * Otherwise we'd get a lot of unnecessary function calls that will
         * simply end up failing.
         */

        if (!unsupported) {
            super.callFunction(name, arguments);
        } else {
            Logger.getLogger(getClass().getName()).warning(
                    "PushState is unsupported by the client "
                            + "browser. Ignoring RPC call for "
                            + getClass().getSimpleName() + "." + name);
        }
    }
}
