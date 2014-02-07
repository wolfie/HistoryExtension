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

        public JSONObject getStateAsJson() {
            return json;
        }

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

        public HistoryExtension getSource() {
            return HistoryExtension.this;
        }
    }

    public static interface PopStateListener {
        void popState(PopStateEvent event);
    }

    public static class ErrorEvent {
        public enum Type {
            UNSUPPORTED, METHOD_INVOKE
        }

        private Type type;
        private String name;
        private String message;
        private boolean cancelled;

        public ErrorEvent(Type type, String name, String message) {
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

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    public static interface ErrorListener {
        void onError(ErrorEvent event);
    }

    private final List<PopStateListener> popListeners = new ArrayList<PopStateListener>();
    private final List<ErrorListener> errorListeners = new ArrayList<ErrorListener>();

    private boolean unsupported = false;

    public static HistoryExtension extend(final UI ui,
            final PopStateListener listener) {
        final HistoryExtension extension = new HistoryExtension();
        extension.addPopStateListener(listener);
        extension.extend(ui);
        return extension;
    }

    public HistoryExtension() {
        addFunction("popstate", new JavaScriptFunction() {
            @Override
            public void call(final JSONArray arguments) throws JSONException {
                if (arguments.length() > 0) {
                    try {
                        JSONObject jsonObject = arguments.getJSONObject(0);
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
                ErrorEvent.Type type = ErrorEvent.Type.values()[errorType];
                final String name = arguments.getString(1);
                final String message = arguments.getString(2);

                final ErrorEvent event = new ErrorEvent(type, name, message);
                fireError(event);
            }
        });
    }

    @SuppressWarnings("cast")
    public void extend(final UI ui) {
        super.extend((AbstractClientConnector) ui);
    }

    public void back() {
        callFunction("back");
    }

    public void forward() {
        callFunction("forward");
    }

    @SuppressWarnings("boxing")
    public void go(final int steps) {
        callFunction("go", steps);
    }

    public void pushState(final Map<String, String> nextStateMap,
            final String nextUrl) {
        callFunction("pushState", nextStateMap, nextUrl);
    }

    public void pushState(final JSONObject nextStateJson, final String nextUrl) {
        callFunction("pushState", nextStateJson, nextUrl);
    }

    public void replaceState(final Map<String, String> newStateMap,
            final String newUrl) {
        callFunction("replaceState", newStateMap, newUrl);
    }

    public void replaceState(JSONObject newStateJson, String newUrl) {
        callFunction("replaceState", newStateJson, newUrl);
    }

    public void addPopStateListener(final PopStateListener listener) {
        popListeners.add(listener);
    }

    public boolean removePopStateListener(PopStateListener listener) {
        return popListeners.remove(listener);
    }

    public void addErrorListener(final ErrorListener listener) {
        errorListeners.add(listener);
    }

    protected void fireListeners(JSONObject json) {
        PopStateEvent event = new PopStateEvent(json);
        for (final PopStateListener listener : popListeners) {
            listener.popState(event);
        }
    }

    protected void fireError(ErrorEvent e) {
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
    protected void callFunction(String name, Object... arguments) {
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
