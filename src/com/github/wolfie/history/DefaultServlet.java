package com.github.wolfie.history;

import javax.servlet.annotation.WebServlet;

import com.vaadin.server.VaadinServlet;

@WebServlet(urlPatterns = { "/VAADIN/*" }, asyncSupported = true)
public class DefaultServlet extends VaadinServlet {
    // empty
}
