package com.finance.erwachen.ui;

import com.finance.erwachen.entity.Account;
import com.finance.erwachen.entity.Transaction;
import com.finance.erwachen.service.AccountService;
import com.finance.erwachen.service.TransactionService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@PageTitle("Dashboard")
@Route(value = "", layout = MainLayout.class)
@PermitAll
public class DashboardView extends VerticalLayout {
    
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Autowired
    public DashboardView(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
        
        try {
            createHeader();
            createSummaryCards();
            createContentArea();
        } catch (Exception e) {
            // Fallback content if there are any loading issues
            removeAll();
            add(createErrorFallback(e));
        }
    }
    
    private void createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle().set("background", "var(--lumo-contrast-5pct)");
        
        H1 title = new H1("Dashboard");
        title.getStyle().set("margin", "0");
        title.getStyle().set("color", "var(--lumo-header-text-color)");
        
        Span welcomeText = new Span("Welcome back! Here's your financial overview.");
        welcomeText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        VerticalLayout titleSection = new VerticalLayout(title, welcomeText);
        titleSection.setPadding(false);
        titleSection.setSpacing(false);
        
        Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> refreshDashboard());
        
        header.add(titleSection, refreshButton);
        add(header);
    }
    
    private void createSummaryCards() {
        HorizontalLayout cardLayout = new HorizontalLayout();
        cardLayout.setWidthFull();
        cardLayout.setPadding(true);
        cardLayout.setSpacing(true);
        
        AccountService.AccountSummary summary = accountService.getAccountSummary();
        
        // Total Accounts Card
        cardLayout.add(createSummaryCard(
            "Total Accounts", 
            String.valueOf(summary.getTotalAccounts()),
            VaadinIcon.CREDIT_CARD,
            "var(--lumo-primary-color)"
        ));
        
        // Total Assets Card
        cardLayout.add(createSummaryCard(
            "Total Assets", 
            currencyFormat.format(summary.getTotalAssets()),
            VaadinIcon.TRENDING_UP,
            "var(--lumo-success-color)"
        ));
        
        // Total Liabilities Card
        cardLayout.add(createSummaryCard(
            "Total Liabilities", 
            currencyFormat.format(summary.getTotalLiabilities()),
            VaadinIcon.TRENDING_DOWN,
            "var(--lumo-error-color)"
        ));
        
        // Net Worth Card
        cardLayout.add(createSummaryCard(
            "Net Worth", 
            currencyFormat.format(summary.getNetWorth()),
            VaadinIcon.DOLLAR,
            summary.getNetWorth().compareTo(BigDecimal.ZERO) >= 0 ? 
                "var(--lumo-success-color)" : "var(--lumo-error-color)"
        ));
        
        add(cardLayout);
    }
    
    private Component createSummaryCard(String title, String value, VaadinIcon iconType, String color) {
        Div card = new Div();
        card.addClassName("dashboard-card");
        card.getStyle()
            .set("background", "var(--lumo-base-color)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("box-shadow", "var(--lumo-box-shadow-s)")
            .set("padding", "var(--lumo-space-l)")
            .set("border-left", "4px solid " + color)
            .set("min-width", "200px");
        
        HorizontalLayout content = new HorizontalLayout();
        content.setAlignItems(Alignment.CENTER);
        content.setJustifyContentMode(JustifyContentMode.BETWEEN);
        content.setWidthFull();
        
        VerticalLayout textContent = new VerticalLayout();
        textContent.setPadding(false);
        textContent.setSpacing(false);
        
        Span titleSpan = new Span(title);
        titleSpan.getStyle()
            .set("font-size", "var(--lumo-font-size-s)")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-weight", "500");
        
        Span valueSpan = new Span(value);
        valueSpan.getStyle()
            .set("font-size", "var(--lumo-font-size-xl)")
            .set("font-weight", "600")
            .set("color", "var(--lumo-body-text-color)");
        
        textContent.add(titleSpan, valueSpan);
        
        Icon icon = iconType.create();
        icon.setSize("2em");
        icon.setColor(color);
        
        content.add(textContent, icon);
        card.add(content);
        
        return card;
    }
    
    private void createContentArea() {
        HorizontalLayout mainContent = new HorizontalLayout();
        mainContent.setSizeFull();
        mainContent.setPadding(true);
        mainContent.setSpacing(true);
        
        // Left column - Recent Transactions
        VerticalLayout leftColumn = new VerticalLayout();
        leftColumn.setPadding(false);
        leftColumn.setWidth("60%");
        leftColumn.add(createRecentTransactionsPanel());
        
        // Right column - Account Overview
        VerticalLayout rightColumn = new VerticalLayout();
        rightColumn.setPadding(false);
        rightColumn.setWidth("40%");
        rightColumn.add(createAccountOverviewPanel());
        
        mainContent.add(leftColumn, rightColumn);
        add(mainContent);
    }
    
    private Component createRecentTransactionsPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(true);
        
        // Panel header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        
        H3 title = new H3("Recent Transactions");
        title.getStyle().set("margin", "0");
        
        RouterLink viewAllLink = new RouterLink("View All", TransactionsView.class);
        viewAllLink.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        header.add(title, viewAllLink);
        
        // Transactions grid
        Grid<Transaction> grid = new Grid<>(Transaction.class, false);
        grid.setHeight("300px");
        grid.setAllRowsVisible(true);
        
        grid.addColumn(transaction -> transaction.getTransactionDate().format(dateFormat))
            .setHeader("Date").setWidth("120px");
        grid.addColumn(Transaction::getDescription)
            .setHeader("Description").setFlexGrow(1);
        grid.addColumn(transaction -> currencyFormat.format(transaction.getAmount()))
            .setHeader("Amount").setWidth("100px");
        
        // Load recent transactions
        List<Transaction> recentTransactions = transactionService.getRecentTransactions(10);
        grid.setItems(recentTransactions);
        
        Div panelCard = new Div();
        panelCard.getStyle()
            .set("background", "var(--lumo-base-color)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("box-shadow", "var(--lumo-box-shadow-s)")
            .set("padding", "var(--lumo-space-l)");
        
        panel.add(header, grid);
        panelCard.add(panel);
        
        return panelCard;
    }
    
    private Component createAccountOverviewPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(true);
        
        // Panel header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        
        H3 title = new H3("Account Overview");
        title.getStyle().set("margin", "0");
        
        RouterLink viewAllLink = new RouterLink("Manage", AccountsView.class);
        viewAllLink.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        header.add(title, viewAllLink);
        
        // Account list
        VerticalLayout accountList = new VerticalLayout();
        accountList.setPadding(false);
        accountList.setSpacing(false);
        
        List<Account> accounts = accountService.findAllActive();
        
        if (accounts.isEmpty()) {
            Div emptyState = new Div();
            emptyState.setText("No accounts found. Add your first account to get started.");
            emptyState.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("padding", "var(--lumo-space-l)");
            accountList.add(emptyState);
        } else {
            // Group accounts by type
            Map<Account.AccountType, List<Account>> accountsByType = accounts.stream()
                .collect(Collectors.groupingBy(Account::getAccountType));
            
            accountsByType.forEach((type, accountsOfType) -> {
                // Type header
                H4 typeHeader = new H4(formatAccountType(type));
                typeHeader.getStyle()
                    .set("margin", "var(--lumo-space-m) 0 var(--lumo-space-s) 0")
                    .set("color", "var(--lumo-primary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("text-transform", "uppercase")
                    .set("font-weight", "600");
                
                accountList.add(typeHeader);
                
                // Accounts of this type
                accountsOfType.forEach(account -> {
                    accountList.add(createAccountItem(account));
                });
            });
        }
        
        Div panelCard = new Div();
        panelCard.getStyle()
            .set("background", "var(--lumo-base-color)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("box-shadow", "var(--lumo-box-shadow-s)")
            .set("padding", "var(--lumo-space-l)");
        
        panel.add(header, accountList);
        panelCard.add(panel);
        
        return panelCard;
    }
    
    private Component createAccountItem(Account account) {
        HorizontalLayout item = new HorizontalLayout();
        item.setWidthFull();
        item.setAlignItems(Alignment.CENTER);
        item.setJustifyContentMode(JustifyContentMode.BETWEEN);
        item.getStyle()
            .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
            .set("border-radius", "var(--lumo-border-radius-s)")
            .set("margin-bottom", "var(--lumo-space-xs)");
        
        item.addClickListener(e -> {
            // Could navigate to account details or transactions filtered by account
        });
        
        item.getStyle().set("cursor", "pointer");
        
        // Account info
        VerticalLayout accountInfo = new VerticalLayout();
        accountInfo.setPadding(false);
        accountInfo.setSpacing(false);
        
        Span accountName = new Span(account.getAccountName());
        accountName.getStyle()
            .set("font-weight", "500")
            .set("font-size", "var(--lumo-font-size-s)");
        
        Span accountNumber = new Span(account.getAccountNumber());
        accountNumber.getStyle()
            .set("font-size", "var(--lumo-font-size-xs)")
            .set("color", "var(--lumo-secondary-text-color)");
        
        accountInfo.add(accountName, accountNumber);
        
        // Balance
        Span balance = new Span(currencyFormat.format(account.getBalance()));
        balance.getStyle()
            .set("font-weight", "600")
            .set("color", account.getBalance().compareTo(BigDecimal.ZERO) >= 0 ? 
                "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)");
        
        item.add(accountInfo, balance);
        
        // Hover effect
        item.getElement().addEventListener("mouseenter", e -> {
            item.getStyle().set("background", "var(--lumo-contrast-5pct)");
        });
        item.getElement().addEventListener("mouseleave", e -> {
            item.getStyle().set("background", "transparent");
        });
        
        return item;
    }
    
    private String formatAccountType(Account.AccountType type) {
        return switch (type) {
            case CHECKING -> "Checking";
            case SAVINGS -> "Savings";
            case CREDIT_CARD -> "Credit Cards";
            case INVESTMENT -> "Investments";
            case LOAN -> "Loans";
        };
    }
    
    private void refreshDashboard() {
        getUI().ifPresent(ui -> ui.getPage().reload());
    }
    
    private Component createErrorFallback(Exception e) {
        VerticalLayout fallback = new VerticalLayout();
        fallback.setSizeFull();
        fallback.setPadding(true);
        fallback.setSpacing(true);
        fallback.setAlignItems(Alignment.CENTER);
        fallback.setJustifyContentMode(JustifyContentMode.CENTER);
        
        Icon errorIcon = VaadinIcon.WARNING.create();
        errorIcon.setSize("3em");
        errorIcon.setColor("var(--lumo-error-color)");
        
        H2 errorTitle = new H2("Dashboard Loading Issue");
        errorTitle.getStyle().set("color", "var(--lumo-error-text-color)");
        
        Paragraph errorMessage = new Paragraph(
            "There was an issue loading the dashboard. The application is starting up. Please refresh the page in a moment."
        );
        errorMessage.getStyle().set("text-align", "center");
        errorMessage.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        Button refreshButton = new Button("Refresh Page", VaadinIcon.REFRESH.create());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshButton.addClickListener(e2 -> refreshDashboard());
        
        fallback.add(errorIcon, errorTitle, errorMessage, refreshButton);
        
        return fallback;
    }
}
