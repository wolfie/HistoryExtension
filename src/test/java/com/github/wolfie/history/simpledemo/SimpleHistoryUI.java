package com.github.wolfie.history.simpledemo;

import com.github.wolfie.history.HistoryExtension;
import com.github.wolfie.history.HistoryExtension.ErrorListener;
import com.github.wolfie.history.HistoryExtension.PopStateEvent;
import com.github.wolfie.history.HistoryExtension.PopStateListener;
import com.vaadin.annotations.Title;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;

import java.util.HashMap;
import java.util.Map;
import org.vaadin.addonhelpers.AbstractTest;

@SuppressWarnings("serial")
@Title("Simplified HTML5 History Demo")
public class SimpleHistoryUI extends AbstractTest {

    private static final String DATA_KEY = "data";

    private final VerticalLayout layout = new VerticalLayout();
    private HistoryExtension history;
    private TextField url;
    private TextField stateField;

    private void pushState() {
        final Map<String, String> stateMap = new HashMap<String, String>();
        stateMap.put(DATA_KEY, stateField.getValue());
        history.pushState(stateMap, url.getValue());
    }

    @Override
    public Component getTestComponent() {

        history = HistoryExtension.extend(this,

        new PopStateListener() {
            @Override
            public void popState(final PopStateEvent event) {
                final Map<String, String> state = event.getStateAsMap();
                stateField.setValue(state.get(DATA_KEY));
                url.setValue(event.getAddress().toString());
            }
        },

        new ErrorListener() {
            @Override
            public void onError(final HistoryExtension.ErrorEvent event) {
                Notification.show(
                        event.getErrorName() + ": " + event.getMessage(),
                        Notification.Type.ERROR_MESSAGE);
                event.cancel();
            }
        });
        /*
         * we want to initialize the history state data with something we can
         * handle in our popstate listener
         */
//        final Map<String, String> newStateMap = Collections.singletonMap(DATA_KEY, "");
        // FIXME, I guess this is a regression in Vaadin, the map don't serialize anymore like this
        final Map<String, String> newStateMap = new HashMap<String, String>(){};
        newStateMap.put(DATA_KEY, "");
        final Class<?> componentType = newStateMap.getClass().getComponentType();
        System.err.println(componentType);
        System.err.println(componentType);
        history.replaceState(newStateMap, getPage()
                .getLocation().toString());

        layout.addComponent(new Label("Choose some text " + "to store in "
                + "a state. Then choose a new URL to navigate to. After "
                + "you've done this a few times, press the back and "
                + "forward buttons in your browser to see what happens"));

        final FormLayout fl = new FormLayout();
        fl.setSpacing(true);
        fl.setMargin(true);
        layout.addComponent(fl);

        stateField = new TextField("State data");
        fl.addComponent(stateField);

        url = new TextField("URL");
        url.setValue(getPage().getLocation().toString());
        url.setWidth("500px");
        url.addShortcutListener(new ShortcutListener("", KeyCode.ENTER,
                (int[]) null) {
            @Override
            public void handleAction(final Object sender, final Object target) {
                pushState();
            }
        });
        fl.addComponent(url);

        fl.addComponent(new Button("Save and go", new Button.ClickListener() {
            @Override
            public void buttonClick(final ClickEvent event) {
                pushState();
            }
        }));
        return layout;
    }
}