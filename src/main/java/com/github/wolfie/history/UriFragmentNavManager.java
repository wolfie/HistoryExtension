package com.github.wolfie.history;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Logger;

import com.github.wolfie.history.HistoryExtension.PopStateEvent;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.Navigator.UriFragmentManager;

/**
 * Stores state in URL fragment, e.g. "host:port#!viewname/some_state". This
 * creates compatible urls with Vaadins default {@link UriFragmentManager}.
 */
public class UriFragmentNavManager extends UriFragmentManager implements HistoryExtension.PopStateListener {

    private static final String FRAGMENT_PREFIX = "!";
    private final HistoryExtension historyExtension;
    private final Map<String, String> emptyStateObject = null;
    private Navigator navigator;
    private URI lastAddress;

    public UriFragmentNavManager(HistoryExtension historyExtension) {
        super(historyExtension.getUI().getPage());
        this.historyExtension = historyExtension;
        lastAddress = historyExtension.getUI().getPage().getLocation();
        this.historyExtension.addPopStateListener(this);
    }

    @Override
    protected String getFragment() {
        return lastAddress.getFragment();
    }

    @Override
    protected void setFragment(String fragment) {
        try {
            URI newLocation = new URI(lastAddress.getScheme(), lastAddress.getUserInfo(), lastAddress.getHost(),
                    lastAddress.getPort(), lastAddress.getPath(), lastAddress.getQuery(), fragment);
            this.historyExtension.pushState(emptyStateObject, newLocation.toString());
            navigator.getUI().getPage().updateLocation(newLocation.toString(), false);
        } catch (URISyntaxException e) {
            Logger.getLogger(getClass().getName()).warning("Could not create url from current: " + lastAddress);
        }

    }

    @Override
    public void setNavigator(final Navigator navigator) {
        super.setNavigator(navigator);
        this.navigator = navigator;
    }

    @Override
    public void popState(final PopStateEvent event) {
        lastAddress = event.getAddress();
        navigator.navigateTo(parseStateFrom(event.getAddress()));
    }

    protected String parseStateFrom(final URI uri) {
        if (uri.getFragment() != null && uri.getFragment().startsWith(FRAGMENT_PREFIX)) {
            return uri.getFragment().substring(FRAGMENT_PREFIX.length());
        } else {
            return "";
        }
    }

}