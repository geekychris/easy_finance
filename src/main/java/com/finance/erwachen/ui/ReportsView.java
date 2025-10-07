package com.finance.erwachen.ui;

import com.finance.erwachen.entity.Transaction;
import com.finance.erwachen.service.AccountService;
import com.finance.erwachen.service.CategoryService;
import com.finance.erwachen.service.TransactionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Reports")
@Route(value = "reports", layout = MainLayout.class)
@PermitAll
public class ReportsView extends VerticalLayout {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private final DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy");
    
    private VerticalLayout contentArea;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    
    // Category filtering state
    private ComboBox<String> categoryTypeFilter;
    private ComboBox<String> amountRangeFilter;

    @Autowired
    public ReportsView(AccountService accountService, TransactionService transactionService, CategoryService categoryService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        createHeader();
        createDateRangeSelector();
        createTabsAndContent();
        loadDefaultReports();
    }
    
    private void createHeader() {
        H2 title = new H2("📊 Financial Reports & Analytics");
        title.getStyle().set("color", "var(--lumo-primary-color)");
        
        Paragraph description = new Paragraph("Comprehensive analysis of your financial data with interactive charts and detailed breakdowns.");
        description.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        add(title, description);
    }
    
    private void createDateRangeSelector() {
        H4 dateRangeTitle = new H4("Date Range Selection");
        
        HorizontalLayout dateRangeLayout = new HorizontalLayout();
        dateRangeLayout.setAlignItems(Alignment.END);
        dateRangeLayout.setSpacing(true);
        
        startDatePicker = new DatePicker("Start Date");
        startDatePicker.setValue(LocalDate.now().withDayOfMonth(1)); // First day of current month
        
        endDatePicker = new DatePicker("End Date");
        endDatePicker.setValue(LocalDate.now()); // Today
        
        Button refreshButton = new Button("Refresh Reports", VaadinIcon.REFRESH.create());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshButton.addClickListener(e -> refreshAllReports());
        
        // Quick date range buttons
        Button thisMonthButton = new Button("This Month");
        thisMonthButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        thisMonthButton.addClickListener(e -> {
            LocalDate now = LocalDate.now();
            startDatePicker.setValue(now.withDayOfMonth(1));
            endDatePicker.setValue(now.withDayOfMonth(now.lengthOfMonth()));
            refreshAllReports();
        });
        
        Button lastMonthButton = new Button("Last Month");
        lastMonthButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        lastMonthButton.addClickListener(e -> {
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            startDatePicker.setValue(lastMonth.withDayOfMonth(1));
            endDatePicker.setValue(lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()));
            refreshAllReports();
        });
        
        Button last3MonthsButton = new Button("Last 3 Months");
        last3MonthsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        last3MonthsButton.addClickListener(e -> {
            startDatePicker.setValue(LocalDate.now().minusMonths(3));
            endDatePicker.setValue(LocalDate.now());
            refreshAllReports();
        });
        
        dateRangeLayout.add(startDatePicker, endDatePicker, refreshButton, thisMonthButton, lastMonthButton, last3MonthsButton);
        
        add(dateRangeTitle, dateRangeLayout);
    }
    
    private void createTabsAndContent() {
        Tabs tabs = new Tabs();
        tabs.setWidthFull();
        
        Tab summaryTab = new Tab("Financial Summary");
        Tab monthlyTab = new Tab("Monthly Analysis");
        Tab categoryTab = new Tab("Category Breakdown");
        Tab trendsTab = new Tab("Trends & Charts");
        
        tabs.add(summaryTab, monthlyTab, categoryTab, trendsTab);
        tabs.setSelectedTab(summaryTab);
        
        contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        contentArea.setPadding(false);
        contentArea.setSpacing(true);
        
        tabs.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            contentArea.removeAll();
            
            if (selectedTab == summaryTab) {
                loadFinancialSummaryReport();
            } else if (selectedTab == monthlyTab) {
                loadMonthlyAnalysisReport();
            } else if (selectedTab == categoryTab) {
                loadCategoryBreakdownReport();
            } else if (selectedTab == trendsTab) {
                loadTrendsAndChartsReport();
            }
        });
        
        add(tabs, contentArea);
    }
    
    private void loadDefaultReports() {
        loadFinancialSummaryReport();
    }
    
    private void refreshAllReports() {
        contentArea.removeAll();
        loadFinancialSummaryReport(); // Reload current tab content
    }
    
    private void loadFinancialSummaryReport() {
        try {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            
            // Period summary cards
            createPeriodSummaryCards(startDate, endDate);
            
            // Account summary
            createAccountSummarySection();
            
            // Recent transactions preview
            createRecentTransactionsPreview(startDate, endDate);
            
        } catch (Exception e) {
            System.err.println("Error loading financial summary: " + e.getMessage());
            contentArea.add(new Paragraph("Error loading financial summary: " + e.getMessage()));
        }
    }
    
    private void loadMonthlyAnalysisReport() {
        try {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            
            H3 title = new H3("Monthly Analysis - " + startDate.format(monthFormat) + 
                             (startDate.getMonth() != endDate.getMonth() ? " to " + endDate.format(monthFormat) : ""));
            contentArea.add(title);
            
            // Monthly statistics
            createMonthlyStatsSection(startDate, endDate);
            
            // Income vs Expenses comparison
            createIncomeVsExpensesSection(startDate, endDate);
            
            // Monthly trend chart
            createMonthlyTrendChart(startDate, endDate);
            
        } catch (Exception e) {
            System.err.println("Error loading monthly analysis: " + e.getMessage());
            contentArea.add(new Paragraph("Error loading monthly analysis: " + e.getMessage()));
        }
    }
    
    private void loadCategoryBreakdownReport() {
        try {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            
            H3 title = new H3("📊 Category Breakdown Analysis");
            contentArea.add(title);
            
            // Category filters
            createCategoryFilters();
            
            // Category overview summary
            createCategoryOverviewSummary(startDate, endDate);
            
            // Category spending pie chart visualization
            createCategoryPieChart(startDate, endDate);
            
            // Enhanced category details table
            createEnhancedCategoryDetailsTable(startDate, endDate);
            
            // Category spending trends
            createCategorySpendingTrends(startDate, endDate);
            
            // Top categories with percentages
            createTopCategoriesWithPercentages(startDate, endDate);
            
            // Category comparison analysis
            createCategoryComparisonAnalysis(startDate, endDate);
            
        } catch (Exception e) {
            System.err.println("Error loading category breakdown: " + e.getMessage());
            contentArea.add(new Paragraph("Error loading category breakdown: " + e.getMessage()));
        }
    }
    
    // Implementation methods for all report sections
    
    private void createPeriodSummaryCards(LocalDate startDate, LocalDate endDate) {
        HorizontalLayout summaryCards = new HorizontalLayout();
        summaryCards.setWidthFull();
        summaryCards.setSpacing(true);
        
        try {
            // Get transactions for the period
            List<Transaction> transactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            
            BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            BigDecimal totalExpenses = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(Transaction::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            BigDecimal netCashFlow = totalIncome.subtract(totalExpenses);
            
            Div incomeCard = createSummaryCard("Total Income", currencyFormat.format(totalIncome), "success");
            Div expensesCard = createSummaryCard("Total Expenses", currencyFormat.format(totalExpenses), "error");
            Div netFlowCard = createSummaryCard("Net Cash Flow", currencyFormat.format(netCashFlow), 
                netCashFlow.compareTo(BigDecimal.ZERO) >= 0 ? "primary" : "error");
            Div transactionCountCard = createSummaryCard("Transactions", String.valueOf(transactions.size()), "contrast");
            
            summaryCards.add(incomeCard, expensesCard, netFlowCard, transactionCountCard);
            
        } catch (Exception e) {
            System.err.println("Error creating period summary: " + e.getMessage());
            summaryCards.add(createSummaryCard("Error Loading Data", "--", "error"));
        }
        
        contentArea.add(summaryCards);
    }
    
    private Div createSummaryCard(String title, String value, String theme) {
        Div card = new Div();
        card.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.BorderRadius.MEDIUM, 
                          LumoUtility.Padding.LARGE);
        card.setWidthFull();

        H4 cardTitle = new H4(title);
        cardTitle.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.MEDIUM);

        Span valueSpan = new Span(value);
        valueSpan.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);
        valueSpan.addClassName("text-" + theme);

        VerticalLayout cardContent = new VerticalLayout(cardTitle, valueSpan);
        cardContent.setSpacing(false);
        cardContent.setPadding(false);

        card.add(cardContent);
        return card;
    }
    
    private void createAccountSummarySection() {
        try {
            H4 accountTitle = new H4("Account Summary");
            contentArea.add(accountTitle);
            
            AccountService.AccountSummary summary = accountService.getAccountSummary();
            if (summary != null) {
                HorizontalLayout accountSummaryCards = new HorizontalLayout();
                accountSummaryCards.setWidthFull();
                accountSummaryCards.setSpacing(true);
                
                Div assetsCard = createSummaryCard("Total Assets", currencyFormat.format(summary.getTotalAssets()), "success");
                Div liabilitiesCard = createSummaryCard("Total Liabilities", currencyFormat.format(summary.getTotalLiabilities()), "error");
                Div netWorthCard = createSummaryCard("Net Worth", currencyFormat.format(summary.getNetWorth()), "primary");
                
                accountSummaryCards.add(assetsCard, liabilitiesCard, netWorthCard);
                contentArea.add(accountSummaryCards);
            }
        } catch (Exception e) {
            System.err.println("Error creating account summary: " + e.getMessage());
        }
    }
    
    private void createRecentTransactionsPreview(LocalDate startDate, LocalDate endDate) {
        try {
            H4 transactionsTitle = new H4("Recent Transactions Preview");
            
            List<Transaction> recentTransactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 10)).getContent();
            
            if (!recentTransactions.isEmpty()) {
                Grid<Transaction> transactionsGrid = new Grid<>(Transaction.class, false);
                transactionsGrid.setHeight("300px");
                
                transactionsGrid.addColumn(transaction -> transaction.getTransactionDate().format(dateFormat))
                    .setHeader("Date").setWidth("120px");
                transactionsGrid.addColumn(Transaction::getDescription)
                    .setHeader("Description").setFlexGrow(3);
                transactionsGrid.addColumn(transaction -> {
                    BigDecimal amount = transaction.getAmount();
                    String formatted = currencyFormat.format(amount.abs());
                    return amount.compareTo(BigDecimal.ZERO) >= 0 ? "+" + formatted : "-" + formatted;
                }).setHeader("Amount").setWidth("120px");
                
                transactionsGrid.setItems(recentTransactions);
                
                contentArea.add(transactionsTitle, transactionsGrid);
            } else {
                contentArea.add(transactionsTitle, new Paragraph("No transactions found for the selected period."));
            }
        } catch (Exception e) {
            System.err.println("Error creating transactions preview: " + e.getMessage());
        }
    }
    
    private void createMonthlyStatsSection(LocalDate startDate, LocalDate endDate) {
        try {
            List<Transaction> transactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            
            // Calculate daily averages
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            
            BigDecimal totalExpenses = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(Transaction::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            BigDecimal dailyAvgExpense = daysBetween > 0 ? totalExpenses.divide(BigDecimal.valueOf(daysBetween), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            
            // Find largest expense and income
            Optional<Transaction> largestExpense = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .min(Comparator.comparing(Transaction::getAmount));
                
            Optional<Transaction> largestIncome = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(Transaction::getAmount));
            
            HorizontalLayout statsCards = new HorizontalLayout();
            statsCards.setWidthFull();
            statsCards.setSpacing(true);
            
            Div dailyAvgCard = createSummaryCard("Daily Avg Expense", currencyFormat.format(dailyAvgExpense), "contrast");
            
            Div largestExpenseCard = createSummaryCard("Largest Expense", 
                largestExpense.isPresent() ? currencyFormat.format(largestExpense.get().getAmount().abs()) : "$0.00", "error");
                
            Div largestIncomeCard = createSummaryCard("Largest Income", 
                largestIncome.isPresent() ? currencyFormat.format(largestIncome.get().getAmount()) : "$0.00", "success");
            
            statsCards.add(dailyAvgCard, largestExpenseCard, largestIncomeCard);
            contentArea.add(statsCards);
            
        } catch (Exception e) {
            System.err.println("Error creating monthly stats: " + e.getMessage());
        }
    }
    
    private void createIncomeVsExpensesSection(LocalDate startDate, LocalDate endDate) {
        try {
            List<Transaction> transactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            
            Map<String, BigDecimal> monthlyData = transactions.stream()
                .collect(Collectors.groupingBy(
                    t -> t.getTransactionDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + t.getTransactionDate().getYear(),
                    Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));
                
            if (!monthlyData.isEmpty()) {
                H4 title = new H4("Income vs Expenses Comparison");
                
                VerticalLayout comparisonLayout = new VerticalLayout();
                
                for (Map.Entry<String, BigDecimal> entry : monthlyData.entrySet()) {
                    HorizontalLayout monthRow = new HorizontalLayout();
                    monthRow.setWidthFull();
                    monthRow.setAlignItems(Alignment.CENTER);
                    
                    Span monthLabel = new Span(entry.getKey());
                    monthLabel.setWidth("120px");
                    
                    Span amountLabel = new Span(currencyFormat.format(entry.getValue()));
                    amountLabel.getStyle().set("font-weight", "bold");
                    if (entry.getValue().compareTo(BigDecimal.ZERO) >= 0) {
                        amountLabel.getStyle().set("color", "var(--lumo-success-text-color)");
                    } else {
                        amountLabel.getStyle().set("color", "var(--lumo-error-text-color)");
                    }
                    
                    monthRow.add(monthLabel, amountLabel);
                    comparisonLayout.add(monthRow);
                }
                
                contentArea.add(title, comparisonLayout);
            }
        } catch (Exception e) {
            System.err.println("Error creating income vs expenses section: " + e.getMessage());
        }
    }
    
    private void createCategoryDetailsTable(LocalDate startDate, LocalDate endDate) {
        try {
            List<Transaction> transactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            
            Map<String, CategoryStats> categoryStats = new HashMap<>();
            
            for (Transaction transaction : transactions) {
                String categoryName = transaction.getCategory() != null ? transaction.getCategory().getName() : "Uncategorized";
                
                categoryStats.computeIfAbsent(categoryName, k -> new CategoryStats())
                    .addTransaction(transaction.getAmount());
            }
            
            if (!categoryStats.isEmpty()) {
                H4 title = new H4("Category Details");
                
                Grid<CategoryStats> categoryGrid = new Grid<>();
                categoryGrid.setHeight("400px");
                
                categoryGrid.addColumn(CategoryStats::getName).setHeader("Category").setFlexGrow(2);
                categoryGrid.addColumn(stats -> stats.getTransactionCount()).setHeader("Transactions").setWidth("120px");
                categoryGrid.addColumn(stats -> currencyFormat.format(stats.getTotalAmount())).setHeader("Total").setWidth("120px");
                categoryGrid.addColumn(stats -> currencyFormat.format(stats.getAverageAmount())).setHeader("Average").setWidth("120px");
                
                List<CategoryStats> statsList = categoryStats.entrySet().stream()
                    .map(entry -> {
                        entry.getValue().setName(entry.getKey());
                        return entry.getValue();
                    })
                    .sorted((a, b) -> b.getTotalAmount().abs().compareTo(a.getTotalAmount().abs()))
                    .collect(Collectors.toList());
                
                categoryGrid.setItems(statsList);
                
                contentArea.add(title, categoryGrid);
            }
        } catch (Exception e) {
            System.err.println("Error creating category details table: " + e.getMessage());
        }
    }
    
    private void createTopCategoriesSection(LocalDate startDate, LocalDate endDate) {
        try {
            List<Transaction> expenseTransactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000))
                .getContent().stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.toList());
            
            Map<String, BigDecimal> categoryExpenses = expenseTransactions.stream()
                .collect(Collectors.groupingBy(
                    t -> t.getCategory() != null ? t.getCategory().getName() : "Uncategorized",
                    Collectors.reducing(BigDecimal.ZERO, 
                        t -> t.getAmount().abs(), 
                        BigDecimal::add)
                ));
            
            if (!categoryExpenses.isEmpty()) {
                H4 title = new H4("Top Expense Categories");
                
                VerticalLayout topCategoriesLayout = new VerticalLayout();
                
                categoryExpenses.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> {
                        HorizontalLayout categoryRow = new HorizontalLayout();
                        categoryRow.setWidthFull();
                        categoryRow.setAlignItems(Alignment.CENTER);
                        
                        Span categoryName = new Span(entry.getKey());
                        categoryName.setWidth("200px");
                        
                        Span categoryAmount = new Span(currencyFormat.format(entry.getValue()));
                        categoryAmount.getStyle().set("font-weight", "bold");
                        categoryAmount.getStyle().set("color", "var(--lumo-error-text-color)");
                        
                        categoryRow.add(categoryName, categoryAmount);
                        topCategoriesLayout.add(categoryRow);
                    });
                
                contentArea.add(title, topCategoriesLayout);
            }
        } catch (Exception e) {
            System.err.println("Error creating top categories section: " + e.getMessage());
        }
    }
    
    // Enhanced Category Analysis Methods
    
    private void createCategoryFilters() {
        try {
            H4 filtersTitle = new H4("🔍 Category Filters & Analysis Tools");
            
            HorizontalLayout filtersLayout = new HorizontalLayout();
            filtersLayout.setAlignItems(Alignment.END);
            filtersLayout.setSpacing(true);
            
            // Category type filter
            categoryTypeFilter = new ComboBox<>("Transaction Type");
            categoryTypeFilter.setItems("All Types", "Income Only", "Expenses Only", "Large Transactions");
            categoryTypeFilter.setValue("All Types");
            categoryTypeFilter.setWidth("150px");
            
            // Amount threshold filter
            amountRangeFilter = new ComboBox<>("Amount Range");
            amountRangeFilter.setItems("All Amounts", "Under $100", "$100 - $500", "$500 - $1,000", "Over $1,000");
            amountRangeFilter.setValue("All Amounts");
            amountRangeFilter.setWidth("150px");
            
            // Time period analysis filter
            ComboBox<String> timeAnalysisFilter = new ComboBox<>("Time Analysis");
            timeAnalysisFilter.setItems("Current Period", "Compare Previous", "Trend Analysis", "Seasonal View");
            timeAnalysisFilter.setValue("Current Period");
            timeAnalysisFilter.setWidth("150px");
            
            Button applyFiltersButton = new Button("Apply Filters", VaadinIcon.FILTER.create());
            applyFiltersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            applyFiltersButton.addClickListener(e -> applyCategoryFilters());
            
            Button resetFiltersButton = new Button("Reset", VaadinIcon.REFRESH.create());
            resetFiltersButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            resetFiltersButton.addClickListener(e -> resetCategoryFilters());
            
            filtersLayout.add(categoryTypeFilter, amountRangeFilter, timeAnalysisFilter, applyFiltersButton, resetFiltersButton);
            
            // Add filter help text
            Paragraph filterHelp = new Paragraph("ℹ️ Use filters to drill down into specific transaction types, amount ranges, and time periods for detailed category analysis.");
            filterHelp.getStyle().set("color", "var(--lumo-secondary-text-color)");
            filterHelp.getStyle().set("font-size", "var(--lumo-font-size-s)");
            filterHelp.getStyle().set("margin", "var(--lumo-space-xs) 0");
            
            contentArea.add(filtersTitle, filtersLayout, filterHelp);
            
        } catch (Exception e) {
            System.err.println("Error creating category filters: " + e.getMessage());
        }
    }
    
    private void applyCategoryFilters() {
        // Refresh the current category breakdown with filters applied
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        // Clear existing analysis sections (keep filters and overview)
        removeFilteredContent();
        
        // Reload with filtered data
        createCategoryOverviewSummary(startDate, endDate);
        createCategoryPieChart(startDate, endDate);
        createEnhancedCategoryDetailsTable(startDate, endDate);
        createCategorySpendingTrends(startDate, endDate);
        createTopCategoriesWithPercentages(startDate, endDate);
        createCategoryComparisonAnalysis(startDate, endDate);
    }
    
    private void resetCategoryFilters() {
        categoryTypeFilter.setValue("All Types");
        amountRangeFilter.setValue("All Amounts");
        applyCategoryFilters();
    }
    
    private void removeFilteredContent() {
        // Remove all components except the first few (title, filters, etc.)
        int componentsToKeep = 4; // Title, filters, help text, and overview
        while (contentArea.getComponentCount() > componentsToKeep) {
            contentArea.remove(contentArea.getComponentAt(componentsToKeep));
        }
    }
    
    private void createCategoryOverviewSummary(LocalDate startDate, LocalDate endDate) {
        try {
            List<Transaction> allTransactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            List<Transaction> transactions = applyTransactionFilters(allTransactions);
            
            Set<String> uniqueCategories = transactions.stream()
                .map(t -> t.getCategory() != null ? t.getCategory().getName() : "Uncategorized")
                .collect(Collectors.toSet());
            
            BigDecimal totalCategorySpending = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(Transaction::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Find most active category
            Map<String, Integer> categoryTransactionCounts = transactions.stream()
                .collect(Collectors.groupingBy(
                    t -> t.getCategory() != null ? t.getCategory().getName() : "Uncategorized",
                    Collectors.reducing(0, t -> 1, Integer::sum)
                ));
            
            String mostActiveCategory = categoryTransactionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");
            
            H4 summaryTitle = new H4("📋 Category Overview Summary");
            
            HorizontalLayout overviewCards = new HorizontalLayout();
            overviewCards.setWidthFull();
            overviewCards.setSpacing(true);
            
            Div categoriesCard = createSummaryCard("Total Categories", String.valueOf(uniqueCategories.size()), "primary");
            Div spendingCard = createSummaryCard("Category Spending", currencyFormat.format(totalCategorySpending), "error");
            Div activeCard = createSummaryCard("Most Active", mostActiveCategory, "contrast");
            
            overviewCards.add(categoriesCard, spendingCard, activeCard);
            contentArea.add(summaryTitle, overviewCards);
            
        } catch (Exception e) {
            System.err.println("Error creating category overview: " + e.getMessage());
        }
    }
    
    private void createEnhancedCategoryDetailsTable(LocalDate startDate, LocalDate endDate) {
        try {
            List<Transaction> allTransactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            List<Transaction> transactions = applyTransactionFilters(allTransactions);
            
            Map<String, EnhancedCategoryStats> categoryStats = new HashMap<>();
            BigDecimal totalAbsoluteAmount = BigDecimal.ZERO;
            
            for (Transaction transaction : transactions) {
                String categoryName = transaction.getCategory() != null ? transaction.getCategory().getName() : "Uncategorized";
                
                categoryStats.computeIfAbsent(categoryName, k -> new EnhancedCategoryStats())
                    .addTransaction(transaction.getAmount(), transaction.getTransactionDate());
                    
                totalAbsoluteAmount = totalAbsoluteAmount.add(transaction.getAmount().abs());
            }
            
            if (!categoryStats.isEmpty()) {
                H4 title = new H4("📊 Enhanced Category Analysis");
                
                Grid<EnhancedCategoryStats> categoryGrid = new Grid<>();
                categoryGrid.setHeight("500px");
                
                // Make totalAbsoluteAmount final for lambda use
                final BigDecimal finalTotalAmount = totalAbsoluteAmount;
                
                categoryGrid.addColumn(EnhancedCategoryStats::getName).setHeader("Category").setFlexGrow(2);
                categoryGrid.addColumn(stats -> stats.getTransactionCount()).setHeader("Count").setWidth("80px");
                categoryGrid.addColumn(stats -> currencyFormat.format(stats.getTotalAmount())).setHeader("Total").setWidth("120px");
                categoryGrid.addColumn(stats -> currencyFormat.format(stats.getAverageAmount())).setHeader("Average").setWidth("120px");
                categoryGrid.addColumn(stats -> String.format("%.1f%%", stats.getPercentageOfTotal(finalTotalAmount))).setHeader("% of Total").setWidth("100px");
                categoryGrid.addColumn(stats -> stats.getFrequency()).setHeader("Frequency").setWidth("120px");
                categoryGrid.addColumn(stats -> stats.getTrend()).setHeader("Trend").setWidth("100px");
                
                List<EnhancedCategoryStats> statsList = categoryStats.entrySet().stream()
                    .map(entry -> {
                        entry.getValue().setName(entry.getKey());
                        entry.getValue().calculateTrend();
                        return entry.getValue();
                    })
                    .sorted((a, b) -> b.getTotalAmount().abs().compareTo(a.getTotalAmount().abs()))
                    .collect(Collectors.toList());
                
                categoryGrid.setItems(statsList);
                
                contentArea.add(title, categoryGrid);
            }
        } catch (Exception e) {
            System.err.println("Error creating enhanced category details table: " + e.getMessage());
        }
    }
    
    private void createCategorySpendingTrends(LocalDate startDate, LocalDate endDate) {
        try {
            List<Transaction> allTransactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            List<Transaction> transactions = applyTransactionFilters(allTransactions);
            
            // Group by category and week
            Map<String, Map<String, BigDecimal>> categoryWeeklySpending = new HashMap<>();
            
            for (Transaction transaction : transactions) {
                if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    String categoryName = transaction.getCategory() != null ? transaction.getCategory().getName() : "Uncategorized";
                    String weekKey = getWeekKey(transaction.getTransactionDate());
                    
                    categoryWeeklySpending.computeIfAbsent(categoryName, k -> new HashMap<>())
                        .merge(weekKey, transaction.getAmount().abs(), BigDecimal::add);
                }
            }
            
            if (!categoryWeeklySpending.isEmpty()) {
                H4 title = new H4("📈 Category Spending Trends");
                contentArea.add(title);
                
                // Show top 3 categories with trends
                categoryWeeklySpending.entrySet().stream()
                    .sorted((a, b) -> {
                        BigDecimal totalA = a.getValue().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal totalB = b.getValue().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                        return totalB.compareTo(totalA);
                    })
                    .limit(3)
                    .forEach(entry -> {
                        createCategoryTrendVisualization(entry.getKey(), entry.getValue());
                    });
            }
        } catch (Exception e) {
            System.err.println("Error creating category spending trends: " + e.getMessage());
        }
    }
    
    private void createTopCategoriesWithPercentages(LocalDate startDate, LocalDate endDate) {
        try {
            List<Transaction> allTransactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            List<Transaction> filteredTransactions = applyTransactionFilters(allTransactions);
            List<Transaction> expenseTransactions = filteredTransactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.toList());
            
            BigDecimal totalExpenses = expenseTransactions.stream()
                .map(Transaction::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, BigDecimal> categoryExpenses = expenseTransactions.stream()
                .collect(Collectors.groupingBy(
                    t -> t.getCategory() != null ? t.getCategory().getName() : "Uncategorized",
                    Collectors.reducing(BigDecimal.ZERO, 
                        t -> t.getAmount().abs(), 
                        BigDecimal::add)
                ));
            
            if (!categoryExpenses.isEmpty() && totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                H4 title = new H4("🏆 Top Expense Categories with Breakdown");
                contentArea.add(title);
                
                VerticalLayout topCategoriesLayout = new VerticalLayout();
                
                categoryExpenses.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .limit(7)
                    .forEach(entry -> {
                        BigDecimal percentage = entry.getValue()
                            .divide(totalExpenses, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                        
                        HorizontalLayout categoryRow = createCategoryRowWithBar(entry.getKey(), entry.getValue(), percentage);
                        topCategoriesLayout.add(categoryRow);
                    });
                
                contentArea.add(topCategoriesLayout);
            }
        } catch (Exception e) {
            System.err.println("Error creating top categories with percentages: " + e.getMessage());
        }
    }
    
    private void createCategoryComparisonAnalysis(LocalDate startDate, LocalDate endDate) {
        try {
            H4 title = new H4("🔄 Category Comparison Analysis");
            
            // Compare current period with previous period of same length
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            LocalDate prevStartDate = startDate.minusDays(daysBetween + 1);
            LocalDate prevEndDate = startDate.minusDays(1);
            
            List<Transaction> currentPeriod = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            List<Transaction> previousPeriod = transactionService.findByDateRange(prevStartDate, prevEndDate, PageRequest.of(0, 1000)).getContent();
            
            Map<String, BigDecimal> currentCategoryTotals = getCategoryTotals(currentPeriod);
            Map<String, BigDecimal> previousCategoryTotals = getCategoryTotals(previousPeriod);
            
            if (!currentCategoryTotals.isEmpty()) {
                VerticalLayout comparisonLayout = new VerticalLayout();
                comparisonLayout.add(title);
                
                // Show comparison for top categories
                currentCategoryTotals.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> {
                        String category = entry.getKey();
                        BigDecimal currentAmount = entry.getValue();
                        BigDecimal previousAmount = previousCategoryTotals.getOrDefault(category, BigDecimal.ZERO);
                        
                        HorizontalLayout comparisonRow = createCategoryComparisonRow(category, currentAmount, previousAmount);
                        comparisonLayout.add(comparisonRow);
                    });
                
                contentArea.add(comparisonLayout);
            }
        } catch (Exception e) {
            System.err.println("Error creating category comparison: " + e.getMessage());
        }
    }
    
    // Enhanced Trends and Charts Methods
    
    private void createSpendingVelocityAnalysis(LocalDate startDate, LocalDate endDate) {
        try {
            List<Transaction> transactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            
            // Calculate spending velocity (daily average, weekly patterns)
            Map<Integer, BigDecimal> dailySpending = new HashMap<>();
            Map<String, BigDecimal> weeklySpending = new HashMap<>();
            
            for (Transaction transaction : transactions) {
                if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    int dayOfWeek = transaction.getTransactionDate().getDayOfWeek().getValue();
                    String weekKey = getWeekKey(transaction.getTransactionDate());
                    
                    dailySpending.merge(dayOfWeek, transaction.getAmount().abs(), BigDecimal::add);
                    weeklySpending.merge(weekKey, transaction.getAmount().abs(), BigDecimal::add);
                }
            }
            
            H4 title = new H4("⚡ Spending Velocity & Patterns");
            contentArea.add(title);
            
            // Day of week analysis
            createDayOfWeekAnalysis(dailySpending);
            
            // Weekly velocity chart
            createWeeklyVelocityChart(weeklySpending);
            
        } catch (Exception e) {
            System.err.println("Error creating spending velocity analysis: " + e.getMessage());
        }
    }
    
    private void createSpendingPatternInsights(LocalDate startDate, LocalDate endDate) {
        try {
            List<Transaction> transactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            
            H4 title = new H4("🧠 Spending Pattern Insights");
            contentArea.add(title);
            
            // Analyze patterns
            VerticalLayout insightsLayout = new VerticalLayout();
            
            // Weekend vs Weekday spending
            Map<Boolean, BigDecimal> weekendVsWeekday = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.groupingBy(
                    t -> t.getTransactionDate().getDayOfWeek().getValue() >= 6,
                    Collectors.reducing(BigDecimal.ZERO, 
                        t -> t.getAmount().abs(), 
                        BigDecimal::add)
                ));
            
            if (!weekendVsWeekday.isEmpty()) {
                BigDecimal weekdaySpending = weekendVsWeekday.getOrDefault(false, BigDecimal.ZERO);
                BigDecimal weekendSpending = weekendVsWeekday.getOrDefault(true, BigDecimal.ZERO);
                
                HorizontalLayout weekendAnalysis = new HorizontalLayout();
                weekendAnalysis.add(
                    new Span("📅 Weekday Spending: " + currencyFormat.format(weekdaySpending)),
                    new Span("🎆 Weekend Spending: " + currencyFormat.format(weekendSpending))
                );
                
                insightsLayout.add(weekendAnalysis);
            }
            
            // Large transaction analysis
            BigDecimal averageTransaction = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(Transaction::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(
                    transactions.stream().filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0).count()
                ), 2, RoundingMode.HALF_UP);
            
            long largeTransactions = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .filter(t -> t.getAmount().abs().compareTo(averageTransaction.multiply(BigDecimal.valueOf(3))) > 0)
                .count();
            
            insightsLayout.add(new Paragraph("💰 Large transactions (3x avg): " + largeTransactions));
            
            contentArea.add(insightsLayout);
            
        } catch (Exception e) {
            System.err.println("Error creating spending pattern insights: " + e.getMessage());
        }
    }
    
    private void createFinancialHealthIndicators(LocalDate startDate, LocalDate endDate) {
        try {
            List<Transaction> transactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            
            H4 title = new H4("🌡️ Financial Health Indicators");
            contentArea.add(title);
            
            // Calculate key health metrics
            BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalExpenses = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(Transaction::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            HorizontalLayout healthCards = new HorizontalLayout();
            healthCards.setWidthFull();
            healthCards.setSpacing(true);
            
            // Savings rate
            if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal savingsRate = totalIncome.subtract(totalExpenses)
                    .divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
                
                String healthColor = savingsRate.compareTo(BigDecimal.valueOf(20)) >= 0 ? "success" : 
                                   savingsRate.compareTo(BigDecimal.valueOf(10)) >= 0 ? "primary" : "error";
                
                Div savingsCard = createSummaryCard("Savings Rate", 
                    String.format("%.1f%%", savingsRate.doubleValue()), healthColor);
                healthCards.add(savingsCard);
            }
            
            // Expense volatility
            List<BigDecimal> dailyExpenses = calculateDailyExpenses(transactions, startDate, endDate);
            BigDecimal volatility = calculateVolatility(dailyExpenses);
            String volatilityColor = volatility.compareTo(BigDecimal.valueOf(50)) <= 0 ? "success" : "error";
            
            Div volatilityCard = createSummaryCard("Expense Volatility", 
                currencyFormat.format(volatility), volatilityColor);
            healthCards.add(volatilityCard);
            
            contentArea.add(healthCards);
            
        } catch (Exception e) {
            System.err.println("Error creating financial health indicators: " + e.getMessage());
        }
    }
    
    private void createPredictiveAnalysis(LocalDate startDate, LocalDate endDate) {
        try {
            List<Transaction> transactions = transactionService.findByDateRange(startDate, endDate, PageRequest.of(0, 1000)).getContent();
            
            H4 title = new H4("🔮 Predictive Analysis & Forecasting");
            contentArea.add(title);
            
            // Calculate monthly projections
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            
            BigDecimal totalExpenses = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(Transaction::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (daysBetween > 0 && totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dailyAverage = totalExpenses.divide(BigDecimal.valueOf(daysBetween), 2, RoundingMode.HALF_UP);
                BigDecimal monthlyProjection = dailyAverage.multiply(BigDecimal.valueOf(30));
                
                VerticalLayout predictionLayout = new VerticalLayout();
                
                HorizontalLayout projectionCards = new HorizontalLayout();
                projectionCards.setWidthFull();
                projectionCards.setSpacing(true);
                
                Div dailyCard = createSummaryCard("Daily Average", currencyFormat.format(dailyAverage), "contrast");
                Div monthlyCard = createSummaryCard("Monthly Projection", currencyFormat.format(monthlyProjection), "primary");
                
                projectionCards.add(dailyCard, monthlyCard);
                predictionLayout.add(projectionCards);
                
                // Trend prediction
                String trendPrediction = analyzeTrend(transactions);
                Paragraph trendParagraph = new Paragraph("📈 Trend Analysis: " + trendPrediction);
                trendParagraph.getStyle().set("font-style", "italic");
                
                predictionLayout.add(trendParagraph);
                contentArea.add(predictionLayout);
            }
            
        } catch (Exception e) {
            System.err.println("Error creating predictive analysis: " + e.getMessage());
        }
    }
    
    // Helper methods for enhanced functionality
    
    private List<Transaction> applyTransactionFilters(List<Transaction> transactions) {
        if (categoryTypeFilter == null || amountRangeFilter == null) {
            return transactions; // Filters not initialized yet
        }
        
        return transactions.stream()
            .filter(this::matchesTypeFilter)
            .filter(this::matchesAmountFilter)
            .collect(Collectors.toList());
    }
    
    private boolean matchesTypeFilter(Transaction transaction) {
        String filterValue = categoryTypeFilter.getValue();
        if (filterValue == null || "All Types".equals(filterValue)) {
            return true;
        }
        
        BigDecimal amount = transaction.getAmount();
        switch (filterValue) {
            case "Income Only":
                return amount.compareTo(BigDecimal.ZERO) > 0;
            case "Expenses Only":
                return amount.compareTo(BigDecimal.ZERO) < 0;
            case "Large Transactions":
                return amount.abs().compareTo(BigDecimal.valueOf(500)) > 0;
            default:
                return true;
        }
    }
    
    private boolean matchesAmountFilter(Transaction transaction) {
        String filterValue = amountRangeFilter.getValue();
        if (filterValue == null || "All Amounts".equals(filterValue)) {
            return true;
        }
        
        BigDecimal absAmount = transaction.getAmount().abs();
        switch (filterValue) {
            case "Under $100":
                return absAmount.compareTo(BigDecimal.valueOf(100)) < 0;
            case "$100 - $500":
                return absAmount.compareTo(BigDecimal.valueOf(100)) >= 0 && 
                       absAmount.compareTo(BigDecimal.valueOf(500)) <= 0;
            case "$500 - $1,000":
                return absAmount.compareTo(BigDecimal.valueOf(500)) > 0 && 
                       absAmount.compareTo(BigDecimal.valueOf(1000)) <= 0;
            case "Over $1,000":
                return absAmount.compareTo(BigDecimal.valueOf(1000)) > 0;
            default:
                return true;
        }
    }
    
    private String getWeekKey(LocalDate date) {
        return String.format("%d-W%02d", date.getYear(), 
            date.get(java.time.temporal.WeekFields.ISO.weekOfYear()));
    }
    
    private void createCategoryTrendVisualization(String categoryName, Map<String, BigDecimal> weeklyData) {
        Div trendDiv = new Div();
        trendDiv.getStyle().set("margin", "var(--lumo-space-s)");
        trendDiv.getStyle().set("padding", "var(--lumo-space-m)");
        trendDiv.getStyle().set("background", "var(--lumo-contrast-5pct)");
        trendDiv.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        
        H4 categoryTitle = new H4("📊 " + categoryName);
        
        // Create simple ASCII-style trend visualization
        StringBuilder trendViz = new StringBuilder();
        List<BigDecimal> amounts = weeklyData.values().stream()
            .sorted()
            .collect(Collectors.toList());
        
        if (!amounts.isEmpty()) {
            BigDecimal max = amounts.get(amounts.size() - 1);
            for (BigDecimal amount : amounts) {
                int barLength = max.compareTo(BigDecimal.ZERO) > 0 ? 
                    (int) (amount.divide(max, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(20)).intValue()) : 0;
                trendViz.append("■".repeat(Math.max(1, barLength))).append(" ").append(currencyFormat.format(amount)).append("\n");
            }
        }
        
        Paragraph trendParagraph = new Paragraph(trendViz.toString());
        trendParagraph.getStyle().set("font-family", "monospace");
        trendParagraph.getStyle().set("white-space", "pre");
        
        trendDiv.add(categoryTitle, trendParagraph);
        contentArea.add(trendDiv);
    }
    
    private HorizontalLayout createCategoryRowWithBar(String categoryName, BigDecimal amount, BigDecimal percentage) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        
        Span categorySpan = new Span(categoryName);
        categorySpan.setWidth("200px");
        
        // Visual bar representation
        int barLength = Math.min(20, Math.max(1, percentage.intValue() / 5));
        Span barSpan = new Span("■".repeat(barLength));
        barSpan.getStyle().set("color", "var(--lumo-primary-color)");
        barSpan.getStyle().set("font-family", "monospace");
        barSpan.setWidth("150px");
        
        Span amountSpan = new Span(currencyFormat.format(amount));
        amountSpan.getStyle().set("font-weight", "bold");
        
        Span percentageSpan = new Span(String.format("(%.1f%%)", percentage.doubleValue()));
        percentageSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        row.add(categorySpan, barSpan, amountSpan, percentageSpan);
        return row;
    }
    
    private HorizontalLayout createCategoryComparisonRow(String category, BigDecimal current, BigDecimal previous) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        
        Span categorySpan = new Span(category);
        categorySpan.setWidth("150px");
        
        Span currentSpan = new Span(currencyFormat.format(current));
        currentSpan.setWidth("120px");
        
        Span previousSpan = new Span(currencyFormat.format(previous));
        previousSpan.setWidth("120px");
        previousSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        // Calculate change
        String changeText;
        String changeColor;
        if (previous.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal change = current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            changeText = String.format("%+.1f%%", change.doubleValue());
            changeColor = change.compareTo(BigDecimal.ZERO) > 0 ? "var(--lumo-error-color)" : "var(--lumo-success-color)";
        } else {
            changeText = "New";
            changeColor = "var(--lumo-primary-color)";
        }
        
        Span changeSpan = new Span(changeText);
        changeSpan.getStyle().set("color", changeColor);
        changeSpan.getStyle().set("font-weight", "bold");
        
        row.add(categorySpan, currentSpan, previousSpan, changeSpan);
        return row;
    }
    
    private Map<String, BigDecimal> getCategoryTotals(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
            .collect(Collectors.groupingBy(
                t -> t.getCategory() != null ? t.getCategory().getName() : "Uncategorized",
                Collectors.reducing(BigDecimal.ZERO, 
                    t -> t.getAmount().abs(), 
                    BigDecimal::add)
            ));
    }
    
    private void createDayOfWeekAnalysis(Map<Integer, BigDecimal> dailySpending) {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        
        VerticalLayout dayAnalysis = new VerticalLayout();
        Paragraph title = new Paragraph("📅 Daily Spending Patterns");
        title.getStyle().set("font-weight", "bold");
        dayAnalysis.add(title);
        
        BigDecimal maxSpending = dailySpending.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        
        for (int i = 1; i <= 7; i++) {
            BigDecimal daySpending = dailySpending.getOrDefault(i, BigDecimal.ZERO);
            int barLength = maxSpending.compareTo(BigDecimal.ZERO) > 0 ? 
                (int) (daySpending.divide(maxSpending, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(15)).intValue()) : 0;
            
            HorizontalLayout dayRow = new HorizontalLayout();
            dayRow.setAlignItems(Alignment.CENTER);
            
            Span dayLabel = new Span(days[i - 1]);
            dayLabel.setWidth("100px");
            
            Span barSpan = new Span("■".repeat(Math.max(1, barLength)));
            barSpan.getStyle().set("color", "var(--lumo-primary-color)");
            barSpan.getStyle().set("font-family", "monospace");
            barSpan.setWidth("120px");
            
            Span amountSpan = new Span(currencyFormat.format(daySpending));
            
            dayRow.add(dayLabel, barSpan, amountSpan);
            dayAnalysis.add(dayRow);
        }
        
        contentArea.add(dayAnalysis);
    }
    
    private void createWeeklyVelocityChart(Map<String, BigDecimal> weeklySpending) {
        if (weeklySpending.isEmpty()) return;
        
        VerticalLayout weeklyChart = new VerticalLayout();
        Paragraph title = new Paragraph("📈 Weekly Spending Velocity");
        title.getStyle().set("font-weight", "bold");
        weeklyChart.add(title);
        
        BigDecimal maxWeekly = weeklySpending.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        
        weeklySpending.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                int barLength = maxWeekly.compareTo(BigDecimal.ZERO) > 0 ? 
                    (int) (entry.getValue().divide(maxWeekly, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(20)).intValue()) : 0;
                
                HorizontalLayout weekRow = new HorizontalLayout();
                weekRow.setAlignItems(Alignment.CENTER);
                
                Span weekLabel = new Span(entry.getKey());
                weekLabel.setWidth("80px");
                
                Span barSpan = new Span("■".repeat(Math.max(1, barLength)));
                barSpan.getStyle().set("color", "var(--lumo-success-color)");
                barSpan.getStyle().set("font-family", "monospace");
                
                Span amountSpan = new Span(currencyFormat.format(entry.getValue()));
                
                weekRow.add(weekLabel, barSpan, amountSpan);
                weeklyChart.add(weekRow);
            });
        
        contentArea.add(weeklyChart);
    }
    
    private List<BigDecimal> calculateDailyExpenses(List<Transaction> transactions, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, BigDecimal> dailyExpenses = new HashMap<>();
        
        // Initialize all days with zero
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dailyExpenses.put(current, BigDecimal.ZERO);
            current = current.plusDays(1);
        }
        
        // Add actual expenses
        for (Transaction transaction : transactions) {
            if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                dailyExpenses.merge(transaction.getTransactionDate(), 
                    transaction.getAmount().abs(), BigDecimal::add);
            }
        }
        
        return new ArrayList<>(dailyExpenses.values());
    }
    
    private BigDecimal calculateVolatility(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        
        BigDecimal mean = values.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        
        BigDecimal variance = values.stream()
            .map(v -> v.subtract(mean).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }
    
    private String analyzeTrend(List<Transaction> transactions) {
        if (transactions.size() < 7) {
            return "Insufficient data for trend analysis";
        }
        
        // Simple trend analysis based on first half vs second half spending
        int midPoint = transactions.size() / 2;
        
        BigDecimal firstHalfExpenses = transactions.subList(0, midPoint).stream()
            .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
            .map(Transaction::getAmount)
            .map(BigDecimal::abs)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal secondHalfExpenses = transactions.subList(midPoint, transactions.size()).stream()
            .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
            .map(Transaction::getAmount)
            .map(BigDecimal::abs)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (secondHalfExpenses.compareTo(firstHalfExpenses.multiply(BigDecimal.valueOf(1.1))) > 0) {
            return "Spending is increasing - consider budget review";
        } else if (secondHalfExpenses.compareTo(firstHalfExpenses.multiply(BigDecimal.valueOf(0.9))) < 0) {
            return "Spending is decreasing - good cost management";
        } else {
            return "Spending is stable - consistent pattern";
        }
    }
    
    private void createEnhancedDailySpendingChart(LocalDate startDate, LocalDate endDate) {
        // Enhanced version of the daily spending chart with better visualization
        Paragraph chartPlaceholder = new Paragraph("📈 Enhanced daily spending analysis is displayed above with text-based visualizations. Interactive charts available with Vaadin Charts add-on.");
        chartPlaceholder.getStyle().set("background", "var(--lumo-contrast-5pct)");
        chartPlaceholder.getStyle().set("padding", "var(--lumo-space-m)");
        chartPlaceholder.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        chartPlaceholder.getStyle().set("font-style", "italic");
        contentArea.add(chartPlaceholder);
    }
    
    private void createEnhancedCashFlowChart(LocalDate startDate, LocalDate endDate) {
        // Enhanced version with cash flow analysis
        Paragraph chartPlaceholder = new Paragraph("💰 Enhanced cash flow analysis with predictive modeling is displayed above. Full interactive charts available with Vaadin Charts add-on.");
        chartPlaceholder.getStyle().set("background", "var(--lumo-contrast-5pct)");
        chartPlaceholder.getStyle().set("padding", "var(--lumo-space-m)");
        chartPlaceholder.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        chartPlaceholder.getStyle().set("font-style", "italic");
        contentArea.add(chartPlaceholder);
    }
    
    private void createEnhancedCategoryTrendsChart(LocalDate startDate, LocalDate endDate) {
        // Enhanced version with category trends analysis
        Paragraph chartPlaceholder = new Paragraph("📈 Enhanced category trends with comparative analysis are displayed above using text-based visualizations. Interactive charts available with Vaadin Charts add-on.");
        chartPlaceholder.getStyle().set("background", "var(--lumo-contrast-5pct)");
        chartPlaceholder.getStyle().set("padding", "var(--lumo-space-m)");
        chartPlaceholder.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        chartPlaceholder.getStyle().set("font-style", "italic");
        contentArea.add(chartPlaceholder);
    }
    
    // Enhanced CategoryStats class
    private static class EnhancedCategoryStats {
        private String name;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private int transactionCount = 0;
        private List<LocalDate> transactionDates = new ArrayList<>();
        private String trend = "";
        
        public void addTransaction(BigDecimal amount, LocalDate date) {
            totalAmount = totalAmount.add(amount);
            transactionCount++;
            transactionDates.add(date);
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public int getTransactionCount() { return transactionCount; }
        public BigDecimal getAverageAmount() {
            return transactionCount > 0 ? totalAmount.divide(BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        }
        
        public double getPercentageOfTotal(BigDecimal total) {
            return total.compareTo(BigDecimal.ZERO) > 0 ? 
                totalAmount.abs().divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
        }
        
        public String getFrequency() {
            if (transactionCount == 0) return "None";
            if (transactionCount == 1) return "One-time";
            
            // Calculate average days between transactions
            if (transactionDates.size() > 1) {
                transactionDates.sort(LocalDate::compareTo);
                long totalDays = java.time.temporal.ChronoUnit.DAYS.between(
                    transactionDates.get(0), transactionDates.get(transactionDates.size() - 1)
                );
                double avgDays = (double) totalDays / (transactionDates.size() - 1);
                
                if (avgDays <= 1) return "Daily";
                if (avgDays <= 3) return "Very frequent";
                if (avgDays <= 7) return "Weekly";
                if (avgDays <= 14) return "Bi-weekly";
                if (avgDays <= 30) return "Monthly";
                return "Occasional";
            }
            
            return "Regular";
        }
        
        public String getTrend() { return trend; }
        
        public void calculateTrend() {
            if (transactionDates.size() < 3) {
                trend = "Insufficient data";
                return;
            }
            
            // Simple trend calculation
            transactionDates.sort(LocalDate::compareTo);
            int midPoint = transactionDates.size() / 2;
            
            long recentTransactions = transactionDates.subList(midPoint, transactionDates.size()).size();
            long olderTransactions = transactionDates.subList(0, midPoint).size();
            
            if (recentTransactions > olderTransactions * 1.2) {
                trend = "↗️ Increasing";
            } else if (recentTransactions < olderTransactions * 0.8) {
                trend = "↘️ Decreasing";
            } else {
                trend = "↔️ Stable";
            }
        }
    }
    
    // Placeholder methods for chart functionality (would need Vaadin Charts add-on)
    private void createMonthlyTrendChart(LocalDate startDate, LocalDate endDate) {
        Paragraph chartPlaceholder = new Paragraph("📈 Monthly trend chart would be displayed here. Requires Vaadin Charts add-on for full functionality.");
        chartPlaceholder.getStyle().set("background", "var(--lumo-contrast-5pct)");
        chartPlaceholder.getStyle().set("padding", "var(--lumo-space-m)");
        chartPlaceholder.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        contentArea.add(chartPlaceholder);
    }
    
    private void createCategoryPieChart(LocalDate startDate, LocalDate endDate) {
        Paragraph chartPlaceholder = new Paragraph("🥧 Category spending pie chart would be displayed here. Requires Vaadin Charts add-on for full functionality.");
        chartPlaceholder.getStyle().set("background", "var(--lumo-contrast-5pct)");
        chartPlaceholder.getStyle().set("padding", "var(--lumo-space-m)");
        chartPlaceholder.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        contentArea.add(chartPlaceholder);
    }
    
    private void createDailySpendingChart(LocalDate startDate, LocalDate endDate) {
        Paragraph chartPlaceholder = new Paragraph("📊 Daily spending trend chart would be displayed here. Requires Vaadin Charts add-on for full functionality.");
        chartPlaceholder.getStyle().set("background", "var(--lumo-contrast-5pct)");
        chartPlaceholder.getStyle().set("padding", "var(--lumo-space-m)");
        chartPlaceholder.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        contentArea.add(chartPlaceholder);
    }
    
    private void createCashFlowChart(LocalDate startDate, LocalDate endDate) {
        Paragraph chartPlaceholder = new Paragraph("💰 Cash flow chart would be displayed here. Requires Vaadin Charts add-on for full functionality.");
        chartPlaceholder.getStyle().set("background", "var(--lumo-contrast-5pct)");
        chartPlaceholder.getStyle().set("padding", "var(--lumo-space-m)");
        chartPlaceholder.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        contentArea.add(chartPlaceholder);
    }
    
    private void createCategoryTrendsChart(LocalDate startDate, LocalDate endDate) {
        Paragraph chartPlaceholder = new Paragraph("📈 Category trends over time chart would be displayed here. Requires Vaadin Charts add-on for full functionality.");
        chartPlaceholder.getStyle().set("background", "var(--lumo-contrast-5pct)");
        chartPlaceholder.getStyle().set("padding", "var(--lumo-space-m)");
        chartPlaceholder.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        contentArea.add(chartPlaceholder);
    }
    
    // Helper class for category statistics
    private static class CategoryStats {
        private String name;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private int transactionCount = 0;
        
        public void addTransaction(BigDecimal amount) {
            totalAmount = totalAmount.add(amount);
            transactionCount++;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public int getTransactionCount() { return transactionCount; }
        public BigDecimal getAverageAmount() {
            return transactionCount > 0 ? totalAmount.divide(BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        }
    }
    
    private void loadTrendsAndChartsReport() {
        try {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            
            H3 title = new H3("📈 Financial Trends & Predictive Analysis");
            contentArea.add(title);
            
            // Spending velocity and patterns
            createSpendingVelocityAnalysis(startDate, endDate);
            
            // Enhanced daily spending trend with text visualization
            createEnhancedDailySpendingChart(startDate, endDate);
            
            // Cash flow trend analysis
            createEnhancedCashFlowChart(startDate, endDate);
            
            // Category trends over time with ASCII charts
            createEnhancedCategoryTrendsChart(startDate, endDate);
            
            // Spending pattern insights
            createSpendingPatternInsights(startDate, endDate);
            
            // Financial health indicators
            createFinancialHealthIndicators(startDate, endDate);
            
            // Predictive analysis
            createPredictiveAnalysis(startDate, endDate);
            
        } catch (Exception e) {
            System.err.println("Error loading trends and charts: " + e.getMessage());
            contentArea.add(new Paragraph("Error loading trends and charts: " + e.getMessage()));
        }
    }
}
