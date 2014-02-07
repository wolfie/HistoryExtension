package com.github.wolfie.history.simpledemo;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.wolfie.history.HistoryExtension;
import com.github.wolfie.history.HistoryExtension.PopStateEvent;
import com.github.wolfie.history.HistoryExtension.PopStateListener;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class SimpleHistoryUI extends UI {

    @WebServlet(urlPatterns = { "/simple/*" }, asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = SimpleHistoryUI.class)
    public static class Servlet extends VaadinServlet {
        // default implementation is fine.
    }

    private static final int MAX_LOG_ENTRIES = 10;

    private static final String BUTTON_CLICKS_KEY = "clicks";
    private static final String URL_KEY = "url";

    private HistoryExtension history;

    private Button button;
    private TextField textField;
    private VerticalLayout layout;
    private VerticalLayout logLayout;

    @Override
    protected void init(final VaadinRequest request) {
        layout = new VerticalLayout();
        layout.setMargin(true);
        setContent(layout);

        history = HistoryExtension.extend(this, new PopStateListener() {
            @Override
            public void popState(final PopStateEvent event) {
                try {
                    /*
                     * You could also use event.getStateAsMap, which returns a
                     * Map<String, String>
                     */
                    resetState(event.getStateAsJson());
                } catch (final JSONException e) {
                    // this won't happen
                    throw new RuntimeException(e);
                }
            }

            @SuppressWarnings("boxing")
            private void resetState(final JSONObject state)
                    throws JSONException {
                /*
                 * Extract what we need from the state, and apply changes to the
                 * UI
                 */
                button.setData(state.getInt(BUTTON_CLICKS_KEY));
                textField.setValue(state.getString(URL_KEY));
                updateButtonCaption();

                log("PopStateEvent detected: clicks: "
                        + state.getInt(BUTTON_CLICKS_KEY) + ", url: "
                        + state.getString(URL_KEY));
            }
        });

        history.addErrorListener(new HistoryExtension.ErrorListener() {
            @Override
            public void onError(final HistoryExtension.ErrorEvent event) {
                /*
                 * in case something goes wrong (e.g. the browser doesn't
                 * support pushState)
                 */
                log(event.getErrorName() + ": " + event.getMessage());

                /*
                 * We handled the error message, the extension doesn't need to
                 * throw an exception.
                 */
                event.cancel();
            }
        });

        createStatefulButton();
        createTextFieldForUrl();
        createNavButtons();
        createLogArea();

        // store initial state.
        updateState();
    }

    private void createTextFieldForUrl() {
        textField = new TextField("Save state, and go to url:");
        textField.setImmediate(true);
        textField.addShortcutListener(new ShortcutListener("", KeyCode.ENTER,
                (int[]) null) {
            @Override
            public void handleAction(final Object sender, final Object target) {
                updateState();
            }
        });
        textField.setWidth("300px");

        // generate initial URL for textfield
        final URI location = getPage().getLocation();
        String url = location.getPath();
        if (location.getQuery() != null) {
            url += "?" + location.getQuery();
        }
        textField.setValue(url);

        layout.addComponent(textField);
    }

    private void createStatefulButton() {
        button = new Button("", new Button.ClickListener() {
            @Override
            @SuppressWarnings("boxing")
            public void buttonClick(final ClickEvent event) {
                button.setData(((Integer) button.getData()) + 1);
                updateButtonCaption();
            }
        });
        button.setData(Integer.valueOf(0));
        layout.addComponent(button);
        updateButtonCaption();
    }

    private void createNavButtons() {
        final HorizontalLayout hl = new HorizontalLayout();
        hl.setSpacing(true);
        layout.addComponent(hl);

        hl.addComponent(new Button("< Back", new Button.ClickListener() {
            @Override
            public void buttonClick(final ClickEvent event) {
                history.back();
            }
        }));

        hl.addComponent(new Button("Forward >", new Button.ClickListener() {
            @Override
            public void buttonClick(final ClickEvent event) {
                history.forward();
            }
        }));
    }

    private void updateButtonCaption() {
        button.setCaption("This button has been pressed " + button.getData()
                + " time(s)");
    }

    private void updateState() {
        final String url = textField.getValue();
        final String clicks = button.getData().toString();

        // serialize what we need to rebuild the page state
        final Map<String, String> state = new HashMap<String, String>();
        state.put(BUTTON_CLICKS_KEY, clicks);
        state.put(URL_KEY, url);

        // send the current state and the next url to the client
        history.pushState(state, url);

        log("Saving state with url: \"" + url + "\" and buttonClicks: "
                + clicks);
    }

    private void createLogArea() {
        logLayout = new VerticalLayout();
        logLayout.setCaption("Log:");
        layout.addComponent(logLayout);
    }

    private void log(final String string) {
        logLayout.addComponent(new Label(string), 0);

        final Iterator<Component> iterator = logLayout.iterator();
        for (int i = 0; iterator.hasNext(); i++) {
            iterator.next();
            if (i >= MAX_LOG_ENTRIES) {
                iterator.remove();
            }
        }
    }
}