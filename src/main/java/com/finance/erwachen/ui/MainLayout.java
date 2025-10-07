package com.finance.erwachen.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainLayout extends AppLayout {

    private H2 viewTitle;

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        H1 appName = new H1("Erwachen Finance");
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        // Simple approach - just use VaadinIcons directly
        nav.addItem(new SideNavItem("🏠 Dashboard", DashboardView.class));
        nav.addItem(new SideNavItem("💳 Accounts", AccountsView.class));
        nav.addItem(new SideNavItem("💱 Transactions", TransactionsView.class));
        nav.addItem(new SideNavItem("🏷️ Categories", CategoriesView.class));
        nav.addItem(new SideNavItem("📊 Reports", ReportsView.class));
        nav.addItem(new SideNavItem("⚙️ Settings", SettingsView.class));

        return nav;
    }

    private Footer createFooter() {
        Footer layout = new Footer();
        
        // User info
        String username = getCurrentUsername();
        Avatar avatar = new Avatar(username);
        avatar.setThemeName("xsmall");
        avatar.getStyle().set("margin-inline-end", "var(--lumo-space-s)");

        MenuBar userMenu = new MenuBar();
        userMenu.setThemeName("tertiary-inline contrast");

        MenuItem userName = userMenu.addItem("");
        Div div = new Div();
        div.add(avatar);
        div.add(username);
        div.add(VaadinIcon.CHEVRON_DOWN.create()); // Use VaadinIcon instead
        div.getElement().getStyle().set("display", "flex");
        div.getElement().getStyle().set("align-items", "center");
        div.getElement().getStyle().set("gap", "var(--lumo-space-s)");
        userName.add(div);
        
        userName.getSubMenu().addItem("Sign out", e -> {
            // Implement logout
            getUI().ifPresent(ui -> ui.getPage().setLocation("/logout"));
        });

        layout.add(userMenu);
        return layout;
    }

    private String getCurrentUsername() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else {
                return principal.toString();
            }
        } catch (Exception e) {
            return "User";
        }
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        try {
            if (getContent() == null) {
                return "Dashboard"; // Default title
            }
            PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
            return title == null ? "Dashboard" : title.value();
        } catch (Exception e) {
            return "Dashboard"; // Fallback title
        }
    }

}