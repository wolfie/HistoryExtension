/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.wolfie.history;

import com.vaadin.annotations.JavaScript;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;
import elemental.json.JsonArray;

/**
 * A link that navigates with push state and also controls the Navigator on the
 * server side.
 *
 * @author mstahv
 */
@JavaScript("pushstatelink.js")
public class PushStateLink extends AbstractJavaScriptComponent {

    private final String viewIndentifier;
    private final String text;

    public PushStateLink(String caption, String viewIdentifier) {
        this.viewIndentifier = viewIdentifier;
        this.text = caption;
            String contextPath = VaadinServlet.getCurrent().getServletContext()
                    .getContextPath();

            getState().setHref(contextPath + "/" + viewIndentifier);
            getState().setText(text);
            addFunction("onClick", new JavaScriptFunction() {
                @Override
                public void call(JsonArray arguments) {
                    // TODO figure out if it would be possible to set the correct value to Page location.
                    Navigator navigator = getUI().getNavigator();
                    navigator.navigateTo(viewIndentifier);
                }
            });
    }

    @Override
    public PushStateLinkState getState() {
        return (PushStateLinkState) super.getState();
    }

}
