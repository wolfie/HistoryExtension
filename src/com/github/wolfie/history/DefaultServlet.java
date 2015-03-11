package com.github.wolfie.history;

import com.vaadin.server.VaadinServlet;

import javax.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = { "/VAADIN/*" }, asyncSupported = true)
public class DefaultServlet extends VaadinServlet {
    // empty
}
