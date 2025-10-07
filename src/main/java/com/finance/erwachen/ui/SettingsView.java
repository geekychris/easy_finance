package com.finance.erwachen.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PageTitle("Settings")
@Route(value = "settings", layout = MainLayout.class)
@PermitAll
public class SettingsView extends VerticalLayout {

    public SettingsView() {
        setSizeFull();
        
        H2 title = new H2("Settings");
        Paragraph description = new Paragraph("Application settings and configuration options will be available here.");
        description.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        add(title, description);
    }
}