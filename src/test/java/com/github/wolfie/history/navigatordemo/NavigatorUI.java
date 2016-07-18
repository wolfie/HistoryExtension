package com.github.wolfie.history.navigatordemo;

import com.github.wolfie.history.HistoryExtension;
import com.github.wolfie.history.tabledemo.AboutView;
import com.github.wolfie.history.tabledemo.MyPojo;
import com.github.wolfie.history.tabledemo.TableView;
import com.github.wolfie.history.tabledemo.TableView.TableSelectionListener;
import com.vaadin.annotations.Title;
import com.vaadin.navigator.NavigationStateManager;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import java.net.URI;
import org.vaadin.addonhelpers.AbstractTest;

@SuppressWarnings("serial")
@Title("Navigator Integration Example")
public class NavigatorUI extends AbstractTest implements ViewDisplay {

    private Navigator navigator;
    private HistoryExtension history;
    private TabSheet tabSheet;

    private final TableView tableView = new TableView(
            new TableSelectionListener() {
        @Override
        public void tableSelectionChanged(final MyPojo selectedPojo) {
            if (selectedPojo != null) {
                navigator.navigateTo("table/" + selectedPojo.getId());
            } else {
                navigator.navigateTo("");
            }
        }
    });

    private final AboutView aboutView = new AboutView();
    private String contextPath;

    @Override
    public void showView(final View view) {
        tabSheet.setSelectedTab((Component) view);

        if (view == tableView) {
            final String[] args = navigator.getState().split("/");
            if (args.length > 1) {
                final String id = args[1];
                try {
                    tableView.select(Integer.parseInt(id));
                } catch (final NumberFormatException e) {
                    e.printStackTrace();
                }
            } else {
                tableView.select(-1);
            }
        }
    }

    @Override
    public Component getTestComponent() {
        contextPath = VaadinServlet.getCurrent().getServletContext()
                .getContextPath();

        history = new HistoryExtension();
        history.extend(this);

        history.addPopStateListener(new HistoryExtension.PopStateListener() {
            @Override
            public void popState(HistoryExtension.PopStateEvent event) {
                if(navigator.getCurrentView() == tableView) {
                    URI address = event.getAddress();
                    final String path = address.getPath();
                }
            }
        });

        final NavigationStateManager pushStateManager = history
                .createNavigationStateManager(contextPath + "/" + getClass().getName());
        navigator = new Navigator(this, pushStateManager, this);
        navigator.addView("", tableView);
        navigator.addView("table", tableView);
        navigator.addView("about", aboutView);

        tabSheet = new TabSheet();
        tabSheet.addTab(tableView, "Table View");
        tabSheet.addTab(aboutView, "About this Demo");
        tabSheet.setSizeFull();
        tabSheet.addSelectedTabChangeListener(new SelectedTabChangeListener() {
            @Override
            public void selectedTabChange(final SelectedTabChangeEvent event) {
                if (tabSheet.getSelectedTab() == tableView) {
                    final MyPojo selected = tableView.getSelected();
                    if (selected != null) {
                        navigator.navigateTo("table/" + selected.getId());
                    } else {
                        navigator.navigateTo("table");
                    }
                } else {
                    navigator.navigateTo("about");
                }
            }
        });
        return tabSheet;
    }
}
