package com.github.wolfie.history;

import java.net.URI;
import java.util.Map;
import java.util.logging.Logger;

import com.github.wolfie.history.HistoryExtension.PopStateEvent;
import com.vaadin.navigator.NavigationStateManager;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.Page;
import com.vaadin.ui.UI;

/**
 * Stores state in URL path, e.g. "host:port/viewname/some_state" 
 */
final class NavManager implements NavigationStateManager,
        HistoryExtension.PopStateListener {

	private final HistoryExtension historyExtension;
	private final Map emptyStateObject = null;
    private Navigator navigator;
    private String state = null;
    private final String urlRoot;
    private String query;

    public NavManager(HistoryExtension historyExtension, final String urlRoot) {
        this.historyExtension = historyExtension;
		this.urlRoot = urlRoot;
        this.historyExtension.addPopStateListener(this);
    }

    @Override
    public String getState() {
        if (state == null) {
            state = parseStateFrom(navigator.getUI());
        }
        return state;
    }

    @Override
    public void setState(final String state) {
        this.state = state;
        final String pushStateUrl = urlRoot + "/" + state
                + (query != null ? "?" + query : "");

        this.historyExtension.pushState(emptyStateObject, pushStateUrl);
    }

    @Override
    public void setNavigator(final Navigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void popState(final PopStateEvent event) {
        state = parseStateFrom(event.getAddress());
        navigator.navigateTo(state);
    }

    private String parseStateFrom(final UI ui) {
        if (ui != null) {
            final Page page = ui.getPage();
            if (page != null) {
                return parseStateFrom(page.getLocation());
            } else {
                Logger.getLogger(getClass().getName()).warning(
                        "Could not parse a proper state string: "
                        + "Page was null");
            }
        } else {
            Logger.getLogger(getClass().getName()).warning(
                    "Could not parse a proper state string: "
                    + "UI was null");
        }
        return "";
    }

    private String parseStateFrom(final URI uri) {
        final String path = uri.getPath();
        if (!path.startsWith(urlRoot)) {
            Logger.getLogger(getClass().getName()).warning(
                    "URI " + uri + " doesn't start with the urlRoot "
                    + urlRoot);
            return "";
        }

        String parsedState = path.substring(urlRoot.length());
        if (parsedState.startsWith("/")) {
            parsedState = parsedState.substring(1);
        }
        query = uri.getQuery();
        return parsedState;
    }
}