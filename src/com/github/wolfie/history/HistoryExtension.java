package com.github.wolfie.history;

import java.net.URI;
import java.net.URISyntaxException;
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
 * An extension that allows server-side control over the HTML5
 * <code>history</code> JavaScript object, and also allows listening to the
 * {@link PopStateEvent}.
 * 
 * @author Henrik Paul
 */
@JavaScript("historyextension.js")
public class HistoryExtension extends AbstractJavaScriptExtension {

    /**
     * An event class that carries application state data for the currently
     * selected browser history entry
     */
    public class PopStateEvent {
        private Map<String, String> map = null;
        private final JSONObject json;
        private final String stringAddress;
        private URI address;

        private PopStateEvent(final JSONObject json, final String address) {
            this.json = json;
            this.stringAddress = address;
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
                final String[] names = JSONObject.getNames(json);
                if (names != null) {
                    for (final String key : names) {
                        try {
                            tempMap.put(key, json.getString(key));
                        } catch (final JSONException e) {
                            throw new RuntimeException("org.json.JSONObject "
                                    + "seems to have a bug, as it's "
                                    + "returning keys on objects "
                                    + "that don't exist.", e);
                        }
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

        /**
         * Returns the address to which it was changed, as it reads in the
         * browser location bar.
         * <p>
         * <em>Note:</em>
         * <code>{@link UI#getPage() getPage()}.getLocation()</code> is not
         * properly updated on a state push/replace/pop event, and therefore
         * will probably not return the correct value (at least in Vaadin
         * 7.1.10).
         */
        public URI getAddress() {
            if (address == null) {
                try {
                    address = new URI(stringAddress);
                } catch (final URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            return address;
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
     * @see HistoryExtension#addErrorListener(ErrorListener)
     */
    public static class ErrorEvent {

        /** An enum describing the type of error that occurred */
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
        private final String stringAddress;
        private boolean cancelled;
        private URI address;

        private ErrorEvent(final Type type, final String name,
                final String message, final String address) {
            this.type = type;
            this.name = name;
            this.message = message;
            this.stringAddress = address;
        }

        /** Returns the type of error that occurred */
        public Type getType() {
            return type;
        }

        /** The name of the error that occurred, as given by the browser. */
        public String getErrorName() {
            return name;
        }

        /** The descriptive message of that error, as given by the browser. */
        public String getMessage() {
            return message;
        }

        /**
         * Returns the address as it reads in the browser location bar when the
         * error occurred.
         * <p>
         * <em>Note:</em>
         * <code>{@link UI#getPage() getPage()}.getLocation()</code> is not
         * properly updated on a state push/replace/pop event, and therefore
         * will probably not return the correct value (at least in Vaadin
         * 7.1.10).
         */
        public URI getAddress() {
            if (address == null) {
                try {
                    address = new URI(stringAddress);
                } catch (final URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            return address;
        }

        /**
         * Cancels the error message.
         * <p>
         * If a fired {@link ErrorEvent} is not cancelled by a listener,
         * {@link HistoryExtension} will throw a {@link RuntimeException} that
         * describes what happened.
         * <p>
         * Cancelling an {@link ErrorEvent} lets the {@link HistoryExtension}
         * know that the error has been dealt with, and no additional noise
         * needs to be raised regarding it.
         * <p>
         * All {@link ErrorListener ErrorListeners} still are called, even if an
         * event gets called.
         * 
         * @see #isCancelled()
         */
        public void cancel() {
            cancelled = true;
        }

        /**
         * Checks whether this error has been cancelled or not.
         * 
         * @see #cancel()
         */
        public boolean isCancelled() {
            return cancelled;
        }
    }

    /**
     * An listener interface that allows the server code to listen to
     * asynchronously happened client-side errors.
     * <p>
     * The {@link ErrorListener} should call {@link ErrorEvent#cancel()} on an
     * event that has explicitly been dealt with, otherwise
     * {@link HistoryExtension} will throw a {@link RuntimeException}.
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

    /** Creates a new {@link HistoryExtension} */
    public HistoryExtension() {
        addFunction("popstate", new JavaScriptFunction() {
            @Override
            public void call(final JSONArray arguments) throws JSONException {
                if (arguments.length() > 0) {
                    try {
                        final JSONObject state = arguments.getJSONObject(0);
                        final String address = arguments.getString(1);
                        fireListeners(state, address);
                    } catch (final JSONException e) {
                        throw new RuntimeException(e);
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
                final String address = arguments.getString(3);

                final ErrorEvent event = new ErrorEvent(type, name, message,
                        address);
                fireError(event);
            }
        });
    }

    /** Extend a {@link UI} with this {@link HistoryExtension} */
    public void extend(final UI ui) {
        @SuppressWarnings("cast")
        final AbstractClientConnector acc = ui;
        super.extend(acc);
    }

    /**
     * Tells the browser to go back one step in its history stack.
     */
    public void back() {
        callFunction("back");
    }

    /**
     * Tells the browser to go forward one step in its history stack.
     */
    public void forward() {
        callFunction("forward");
    }

    /**
     * Tells the browser to go forward or backwards a certain amount of steps in
     * its history stack.
     * <p>
     * Negative values go backwards, positive values go forwards.
     */
    @SuppressWarnings("boxing")
    public void go(final int steps) {
        callFunction("go", steps);
    }

    /**
     * Pushes a state object, represented by a {@link Map Map<String, String>},
     * in the browser's history stack.
     * <p>
     * <em>Note:</em> The state should represent the state of the application as
     * it will become, once the URL has changed. So, for example, a select
     * widget has the value "foo", and the user changes that selection to the
     * value "bar". In this case, we want the state object to reflect that value
     * "bar", and perhaps change the url to "/bar".
     * <p>
     * In that case, we would write code similar to the following:
     * 
     * <code><pre>
     * Map&lt;String, String&gt; stateMap = new HashMap&lt;String, String&gt;();
     * stateMap.put("userChoice", "bar");
     * extension.pushState(stateMap, "/bar");
     * </pre></code>
     * 
     * @param nextStateMap
     *            The state representing the <strong>upcoming</strong>
     *            application state
     * @param nextUrl
     *            A URI string of what will be displayed in the browser's
     *            location bar
     * @see PopStateListener
     * @see PopStateEvent#getStateAsMap()
     */
    public void pushState(final Map<String, String> nextStateMap,
            final String nextUrl) {
        callFunction("pushState", nextStateMap, nextUrl);
    }

    /**
     * Pushes a state object, represented by a {@link JSONObject}, in the
     * browser's history stack.
     * <p>
     * <em>Note:</em> The state should represent the state of the application as
     * it will become, once the URL has changed. So, for example, a select
     * widget has the value "foo", and the user changes that selection to the
     * value "bar". In this case, we want the state object to reflect that value
     * "bar", and perhaps change the url to "/bar".
     * <p>
     * In that case, we would write code similar to the following:
     * 
     * <code><pre>
     * JSONObject stateJson = new JSONObject();
     * stateJson.put("userChoice", "bar");
     * extension.pushState(stateJson, "/bar");
     * </pre></code>
     * 
     * @param nextStateJson
     *            The state representing the <strong>upcoming</strong>
     *            application state
     * @param nextUrl
     *            A URI string of what will be displayed in the browser's
     *            location bar
     * @see PopStateListener
     * @see PopStateEvent#getStateAsMap()
     */
    public void pushState(final JSONObject nextStateJson, final String nextUrl) {
        callFunction("pushState", nextStateJson, nextUrl);
    }

    /**
     * Similar to {@link #pushState(Map, String)}, but instead of adding a new
     * history entry in the browser, it replaces the current one.
     * <p>
     * Works perfectly for tracking runtime progress (e.g. scrolling down a
     * page, or displaying the progress while watching a movie).
     * <p>
     * It usually is a good idea to initialize your application with a call to
     * this method, so that your application gets a "proper" state object when
     * the user presses back to this initial state.
     * 
     * @param newStateMap
     *            The state representing the <strong>upcoming</strong>
     *            application state
     * @param newUrl
     *            A URI string of what will be displayed in the browser's
     *            location bar
     * @see PopStateListener
     * @see PopStateEvent#getStateAsMap()
     */
    public void replaceState(final Map<String, String> newStateMap,
            final String newUrl) {
        callFunction("replaceState", newStateMap, newUrl);
    }

    /**
     * Similar to {@link #pushState(JSONObject, String)}, but instead of adding
     * a new history entry in the browser, it replaces the current one.
     * <p>
     * Works perfectly for tracking runtime progress (e.g. scrolling down a
     * page, or displaying the progress while watching a movie).
     * <p>
     * It usually is a good idea to initialize your application with a call to
     * this method, so that your application gets a "proper" state object when
     * the user presses back to this initial state.
     * 
     * @param newStateJson
     *            The state representing the <strong>upcoming</strong>
     *            application state
     * @param newUrl
     *            A URI string of what will be displayed in the browser's
     *            location bar
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

    private void fireListeners(final JSONObject state, final String address) {
        final PopStateEvent event = new PopStateEvent(state, address);
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
