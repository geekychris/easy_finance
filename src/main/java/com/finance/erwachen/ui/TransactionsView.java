package com.finance.erwachen.ui;

import com.finance.erwachen.entity.Transaction;
import com.finance.erwachen.entity.Account;
import com.finance.erwachen.entity.Category;
import com.finance.erwachen.service.TransactionService;
import com.finance.erwachen.service.AccountService;
import com.finance.erwachen.service.CategoryService;
import com.finance.erwachen.service.CsvImportService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.html.Hr;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Transactions")
@Route(value = "transactions", layout = MainLayout.class)
@PermitAll
public class TransactionsView extends VerticalLayout {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final CsvImportService csvImportService;
    private Grid<Transaction> transactionsGrid;
    private TextField searchField;
    private Button bulkDeleteButton;
    
    // Filter state variables
    private DatePicker startDateFilter;
    private DatePicker endDateFilter;
    private TextField descriptionFilter;
    private ComboBox<Account> accountFilter;
    private ComboBox<Category> categoryFilter;
    private TextField payeeFilter;
    private TextField memoFilter;
    private NumberField minAmountFilter;
    private NumberField maxAmountFilter;
    private ComboBox<Transaction.TransactionType> typeFilter;
    private ComboBox<Transaction.TransactionStatus> statusFilter;
    private Checkbox groupByMonthCheckbox;
    private Details filtersSection;
    
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private final DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy");

    @Autowired
    public TransactionsView(TransactionService transactionService, 
                           AccountService accountService,
                           CategoryService categoryService,
                           CsvImportService csvImportService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.csvImportService = csvImportService;
        
        System.out.println("TransactionsView constructor started");

        setSizeFull();
        createHeader();
        createSearchAndFilters();
        createTransactionsGrid();
        loadFilterPreferences();
        refreshData();
        
        System.out.println("TransactionsView constructor completed successfully");
    }

    private void createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        H2 title = new H2("Transactions");

        HorizontalLayout buttons = new HorizontalLayout();
        Button addTransactionButton = new Button("Add Transaction", VaadinIcon.PLUS.create());
        addTransactionButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        addTransactionButton.addClickListener(e -> openAddTransactionDialog());
        
        Button importCsvButton = new Button("Import CSV", VaadinIcon.UPLOAD.create());
        importCsvButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        importCsvButton.addClickListener(e -> openImportCsvDialog());
        
        bulkDeleteButton = new Button("Delete Selected", VaadinIcon.TRASH.create());
        bulkDeleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        bulkDeleteButton.setEnabled(false);
        bulkDeleteButton.addClickListener(e -> confirmBulkDeleteTransactions());
        
        buttons.add(addTransactionButton, importCsvButton, bulkDeleteButton);
        header.add(title, buttons);

        add(header);
    }

    private void createSearchAndFilters() {
        VerticalLayout filtersContainer = new VerticalLayout();
        filtersContainer.setWidthFull();
        filtersContainer.setPadding(false);
        filtersContainer.setSpacing(true);

        // Basic search bar (always visible)
        HorizontalLayout basicSearchLayout = new HorizontalLayout();
        basicSearchLayout.setWidthFull();
        basicSearchLayout.setAlignItems(Alignment.CENTER);

        searchField = new TextField();
        searchField.setPlaceholder("Search transactions...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> applyFilters());
        searchField.setWidthFull();

        // Group by month toggle
        groupByMonthCheckbox = new Checkbox("Group by Month");
        groupByMonthCheckbox.addValueChangeListener(e -> applyFilters());

        basicSearchLayout.add(searchField, groupByMonthCheckbox);
        
        // Advanced filters section (expandable)
        createAdvancedFiltersSection();
        
        filtersContainer.add(basicSearchLayout, filtersSection);
        add(filtersContainer);
    }
    
    private void createAdvancedFiltersSection() {
        VerticalLayout filtersContent = new VerticalLayout();
        filtersContent.setPadding(true);
        filtersContent.setSpacing(true);
        
        // Time range filters
        HorizontalLayout timeRangeLayout = new HorizontalLayout();
        timeRangeLayout.setWidthFull();
        timeRangeLayout.setAlignItems(Alignment.END);
        
        startDateFilter = new DatePicker("Start Date");
        startDateFilter.addValueChangeListener(e -> applyFilters());
        startDateFilter.setWidth("150px");
        
        endDateFilter = new DatePicker("End Date");
        endDateFilter.addValueChangeListener(e -> applyFilters());
        endDateFilter.setWidth("150px");
        
        // Quick date range buttons
        Button thisMonthBtn = new Button("This Month", e -> {
            LocalDate now = LocalDate.now();
            startDateFilter.setValue(now.withDayOfMonth(1));
            endDateFilter.setValue(now.withDayOfMonth(now.lengthOfMonth()));
        });
        thisMonthBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        
        Button lastMonthBtn = new Button("Last Month", e -> {
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            startDateFilter.setValue(lastMonth.withDayOfMonth(1));
            endDateFilter.setValue(lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()));
        });
        lastMonthBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        
        Button last3MonthsBtn = new Button("Last 3 Months", e -> {
            LocalDate now = LocalDate.now();
            startDateFilter.setValue(now.minusMonths(3));
            endDateFilter.setValue(now);
        });
        last3MonthsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        
        timeRangeLayout.add(startDateFilter, endDateFilter, thisMonthBtn, lastMonthBtn, last3MonthsBtn);
        
        // Text-based filters (Description, Payee, Memo)
        HorizontalLayout textFiltersLayout = new HorizontalLayout();
        textFiltersLayout.setWidthFull();
        textFiltersLayout.setAlignItems(Alignment.END);
        
        descriptionFilter = new TextField("Description Contains");
        descriptionFilter.setPlaceholder("Filter by description...");
        descriptionFilter.addValueChangeListener(e -> applyFilters());
        descriptionFilter.setWidth("200px");
        descriptionFilter.setClearButtonVisible(true);
        
        payeeFilter = new TextField("Payee Contains");
        payeeFilter.setPlaceholder("Filter by payee...");
        payeeFilter.addValueChangeListener(e -> applyFilters());
        payeeFilter.setWidth("180px");
        payeeFilter.setClearButtonVisible(true);
        
        memoFilter = new TextField("Memo Contains");
        memoFilter.setPlaceholder("Filter by memo...");
        memoFilter.addValueChangeListener(e -> applyFilters());
        memoFilter.setWidth("180px");
        memoFilter.setClearButtonVisible(true);
        
        textFiltersLayout.add(descriptionFilter, payeeFilter, memoFilter);
        
        // Account and Category filters  
        HorizontalLayout accountCategoryLayout = new HorizontalLayout();
        accountCategoryLayout.setWidthFull();
        accountCategoryLayout.setAlignItems(Alignment.END);
        
        accountFilter = new ComboBox<>("Account");
        accountFilter.setItems(accountService.findAllActive());
        accountFilter.setItemLabelGenerator(Account::getAccountName);
        accountFilter.addValueChangeListener(e -> applyFilters());
        accountFilter.setWidth("200px");
        accountFilter.setClearButtonVisible(true);
        
        categoryFilter = new ComboBox<>("Category");
        categoryFilter.setItems(categoryService.findAllActive());
        categoryFilter.setItemLabelGenerator(Category::getName);
        categoryFilter.addValueChangeListener(e -> applyFilters());
        categoryFilter.setWidth("200px");
        categoryFilter.setClearButtonVisible(true);
        
        accountCategoryLayout.add(accountFilter, categoryFilter);
        
        // Transaction Type and Status filters
        HorizontalLayout typeStatusLayout = new HorizontalLayout();
        typeStatusLayout.setWidthFull();
        typeStatusLayout.setAlignItems(Alignment.END);
        
        typeFilter = new ComboBox<>("Transaction Type");
        typeFilter.setItems(Transaction.TransactionType.values());
        typeFilter.addValueChangeListener(e -> applyFilters());
        typeFilter.setWidth("150px");
        typeFilter.setClearButtonVisible(true);
        
        statusFilter = new ComboBox<>("Status");
        statusFilter.setItems(Transaction.TransactionStatus.values());
        statusFilter.addValueChangeListener(e -> applyFilters());
        statusFilter.setWidth("120px");
        statusFilter.setClearButtonVisible(true);
        
        typeStatusLayout.add(typeFilter, statusFilter);
        
        // Amount range filters
        HorizontalLayout amountRangeLayout = new HorizontalLayout();
        amountRangeLayout.setWidthFull();
        amountRangeLayout.setAlignItems(Alignment.END);
        
        minAmountFilter = new NumberField("Min Amount ($)");
        minAmountFilter.addValueChangeListener(e -> applyFilters());
        minAmountFilter.setWidth("150px");
        
        maxAmountFilter = new NumberField("Max Amount ($)");
        maxAmountFilter.addValueChangeListener(e -> applyFilters());
        maxAmountFilter.setWidth("150px");
        
        // Quick amount filter buttons
        Button under100Btn = new Button("Under $100", e -> {
            minAmountFilter.clear();
            maxAmountFilter.setValue(100.0);
        });
        under100Btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        
        Button over500Btn = new Button("Over $500", e -> {
            minAmountFilter.setValue(500.0);
            maxAmountFilter.clear();
        });
        over500Btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        
        amountRangeLayout.add(minAmountFilter, maxAmountFilter, under100Btn, over500Btn);
        
        // Action buttons
        HorizontalLayout actionLayout = new HorizontalLayout();
        actionLayout.setJustifyContentMode(JustifyContentMode.START);
        
        Button clearFiltersBtn = new Button("Clear All Filters", VaadinIcon.ERASER.create());
        clearFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearFiltersBtn.addClickListener(e -> clearAllFilters());
        
        Button applyFiltersBtn = new Button("Apply Filters", VaadinIcon.FILTER.create());
        applyFiltersBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyFiltersBtn.addClickListener(e -> applyFilters());
        
        actionLayout.add(clearFiltersBtn, applyFiltersBtn);
        
        // Add all filter sections to content
        filtersContent.add(
            new H3("📅 Time Range"),
            timeRangeLayout,
            new Hr(),
            new H3("📝 Text Filters"),
            textFiltersLayout,
            new Hr(),
            new H3("🏦 Account & Category"),
            accountCategoryLayout,
            new Hr(),
            new H3("🌿️ Type & Status"),
            typeStatusLayout,
            new Hr(),
            new H3("💰 Amount Range"),
            amountRangeLayout,
            new Hr(),
            actionLayout
        );
        
        // Create expandable details section
        filtersSection = new Details("Advanced Filters", filtersContent);
        filtersSection.setWidthFull();
    }
    
    private void createSelectionToolbar() {
        HorizontalLayout selectionToolbar = new HorizontalLayout();
        selectionToolbar.setWidthFull();
        selectionToolbar.setJustifyContentMode(JustifyContentMode.START);
        selectionToolbar.setAlignItems(Alignment.CENTER);
        selectionToolbar.getStyle().set("padding-bottom", "var(--lumo-space-s)");
        
        Button selectAllButton = new Button("Select All", VaadinIcon.CHECK_SQUARE_O.create());
        selectAllButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        selectAllButton.addClickListener(e -> {
            transactionsGrid.asMultiSelect().select(transactionsGrid.getListDataView().getItems().toList());
        });
        
        Button clearSelectionButton = new Button("Clear Selection", VaadinIcon.SQUARE_SHADOW.create());
        clearSelectionButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        clearSelectionButton.addClickListener(e -> transactionsGrid.deselectAll());
        
        Span selectionInfo = new Span("Select transactions using checkboxes, then use bulk actions");
        selectionInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");
        selectionInfo.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        selectionToolbar.add(selectAllButton, clearSelectionButton, selectionInfo);
        
        add(selectionToolbar);
    }

    private void createTransactionsGrid() {
        transactionsGrid = new Grid<>(Transaction.class, false);
        transactionsGrid.setSizeFull();
        
        // Enable multi-select
        transactionsGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        
        // Add selection listener to enable/disable bulk delete button
        transactionsGrid.addSelectionListener(selection -> {
            int selectedCount = selection.getAllSelectedItems().size();
            bulkDeleteButton.setEnabled(selectedCount > 0);
            if (selectedCount > 0) {
                bulkDeleteButton.setText(String.format("Delete Selected (%d)", selectedCount));
            } else {
                bulkDeleteButton.setText("Delete Selected");
            }
        });

        transactionsGrid.addColumn(transaction -> transaction.getTransactionDate().format(dateFormat))
                .setHeader("Date").setSortable(true).setWidth("120px");
        transactionsGrid.addColumn(Transaction::getDescription)
                .setHeader("Description").setSortable(true).setFlexGrow(3);
        
        // FIX: Safely handle lazy-loaded Account relationship
        transactionsGrid.addColumn(transaction -> {
            try {
                return transaction.getAccount() != null ? transaction.getAccount().getAccountName() : "Unknown";
            } catch (Exception e) {
                // Handle lazy initialization exception
                System.err.println("Could not access account for transaction: " + e.getMessage());
                return "Account unavailable";
            }
        }).setHeader("Account").setSortable(true).setFlexGrow(2);
        
        // FIX: Safely handle lazy-loaded Category relationship
        transactionsGrid.addColumn(transaction -> {
            try {
                return transaction.getCategory() != null ? transaction.getCategory().getName() : "Uncategorized";
            } catch (Exception e) {
                // Handle lazy initialization exception
                System.err.println("Could not access category for transaction: " + e.getMessage());
                return "Category unavailable";
            }
        }).setHeader("Category").setSortable(true).setFlexGrow(2);
        
        transactionsGrid.addColumn(transaction -> {
            BigDecimal amount = transaction.getAmount();
            String formatted = currencyFormat.format(amount.abs());
            return amount.compareTo(BigDecimal.ZERO) >= 0 ? "+" + formatted : "-" + formatted;
        }).setHeader("Amount").setSortable(true).setWidth("120px");
        transactionsGrid.addColumn(Transaction::getType)
                .setHeader("Type").setSortable(true).setWidth("100px");
        transactionsGrid.addColumn(Transaction::getStatus)
                .setHeader("Status").setSortable(true).setWidth("100px");
        
        // Add action buttons column
        transactionsGrid.addComponentColumn(transaction -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.getElement().setAttribute("title", "Edit Transaction");
            editButton.addClickListener(e -> openEditTransactionDialog(transaction));
            
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteButton.getElement().setAttribute("title", "Delete Transaction");
            deleteButton.addClickListener(e -> confirmDeleteTransaction(transaction));
            
            actions.add(editButton, deleteButton);
            return actions;
        }).setHeader("Actions").setWidth("120px").setFlexGrow(0);

        // Add selection toolbar
        createSelectionToolbar();
        
        add(transactionsGrid);
    }

    private void refreshData() {
        applyFilters();
    }
    
    private void applyFilters() {
        try {
            System.out.println("Applying filters and loading transactions...");
            
            // Get all transactions first (we'll expand this later for database-level filtering)
            List<Transaction> allTransactions = transactionService.findAll(PageRequest.of(0, 1000)).getContent();
            
            // Apply filters
            List<Transaction> filteredTransactions = filterTransactions(allTransactions);
            
            System.out.println("Filtered to " + filteredTransactions.size() + " transactions");
            
            // Check if we should group by month
            if (groupByMonthCheckbox.getValue()) {
                displayTransactionsByMonth(filteredTransactions);
            } else {
                transactionsGrid.setItems(filteredTransactions);
            }
            
            // Save filter preferences
            saveFilterPreferences();
            
        } catch (Exception e) {
            System.err.println("Error applying filters: " + e.getMessage());
            e.printStackTrace();
            transactionsGrid.setItems();
        }
    }
    
    private List<Transaction> filterTransactions(List<Transaction> transactions) {
        return transactions.stream()
                .filter(this::matchesSearchFilter)
                .filter(this::matchesDateFilter)
                .filter(this::matchesDescriptionFilter)
                .filter(this::matchesAccountFilter)
                .filter(this::matchesCategoryFilter)
                .filter(this::matchesPayeeFilter)
                .filter(this::matchesMemoFilter)
                .filter(this::matchesAmountFilter)
                .filter(this::matchesTypeFilter)
                .filter(this::matchesStatusFilter)
                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate())) // Sort by date descending
                .collect(Collectors.toList());
    }
    
    private boolean matchesSearchFilter(Transaction transaction) {
        String searchTerm = searchField.getValue();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return true;
        }
        
        searchTerm = searchTerm.toLowerCase();
        return (transaction.getDescription() != null && transaction.getDescription().toLowerCase().contains(searchTerm)) ||
               (transaction.getPayee() != null && transaction.getPayee().toLowerCase().contains(searchTerm)) ||
               (transaction.getMemo() != null && transaction.getMemo().toLowerCase().contains(searchTerm)) ||
               transaction.getAmount().toString().contains(searchTerm);
    }
    
    private boolean matchesDateFilter(Transaction transaction) {
        LocalDate startDate = startDateFilter.getValue();
        LocalDate endDate = endDateFilter.getValue();
        LocalDate transactionDate = transaction.getTransactionDate();
        
        if (startDate != null && transactionDate.isBefore(startDate)) {
            return false;
        }
        
        if (endDate != null && transactionDate.isAfter(endDate)) {
            return false;
        }
        
        return true;
    }
    
    private boolean matchesDescriptionFilter(Transaction transaction) {
        String descriptionText = descriptionFilter.getValue();
        if (descriptionText == null || descriptionText.trim().isEmpty()) {
            return true;
        }
        
        return transaction.getDescription() != null && 
               transaction.getDescription().toLowerCase().contains(descriptionText.toLowerCase());
    }
    
    private boolean matchesAccountFilter(Transaction transaction) {
        Account selectedAccount = accountFilter.getValue();
        if (selectedAccount == null) {
            return true;
        }
        
        return transaction.getAccount() != null && 
               transaction.getAccount().getId().equals(selectedAccount.getId());
    }
    
    private boolean matchesCategoryFilter(Transaction transaction) {
        Category selectedCategory = categoryFilter.getValue();
        if (selectedCategory == null) {
            return true;
        }
        
        return transaction.getCategory() != null && 
               transaction.getCategory().getId().equals(selectedCategory.getId());
    }
    
    private boolean matchesPayeeFilter(Transaction transaction) {
        String payeeText = payeeFilter.getValue();
        if (payeeText == null || payeeText.trim().isEmpty()) {
            return true;
        }
        
        return transaction.getPayee() != null && 
               transaction.getPayee().toLowerCase().contains(payeeText.toLowerCase());
    }
    
    private boolean matchesMemoFilter(Transaction transaction) {
        String memoText = memoFilter.getValue();
        if (memoText == null || memoText.trim().isEmpty()) {
            return true;
        }
        
        return transaction.getMemo() != null && 
               transaction.getMemo().toLowerCase().contains(memoText.toLowerCase());
    }
    
    private boolean matchesAmountFilter(Transaction transaction) {
        Double minAmount = minAmountFilter.getValue();
        Double maxAmount = maxAmountFilter.getValue();
        BigDecimal transactionAmount = transaction.getAmount().abs();
        
        if (minAmount != null && transactionAmount.compareTo(BigDecimal.valueOf(minAmount)) < 0) {
            return false;
        }
        
        if (maxAmount != null && transactionAmount.compareTo(BigDecimal.valueOf(maxAmount)) > 0) {
            return false;
        }
        
        return true;
    }
    
    private boolean matchesTypeFilter(Transaction transaction) {
        Transaction.TransactionType selectedType = typeFilter.getValue();
        if (selectedType == null) {
            return true;
        }
        
        return transaction.getType() == selectedType;
    }
    
    private boolean matchesStatusFilter(Transaction transaction) {
        Transaction.TransactionStatus selectedStatus = statusFilter.getValue();
        if (selectedStatus == null) {
            return true;
        }
        
        return transaction.getStatus() == selectedStatus;
    }
    
    private void clearAllFilters() {
        searchField.clear();
        startDateFilter.clear();
        endDateFilter.clear();
        descriptionFilter.clear();
        accountFilter.clear();
        categoryFilter.clear();
        payeeFilter.clear();
        memoFilter.clear();
        minAmountFilter.clear();
        maxAmountFilter.clear();
        typeFilter.clear();
        statusFilter.clear();
        groupByMonthCheckbox.setValue(false);
        applyFilters();
    }
    
    private void displayTransactionsByMonth(List<Transaction> transactions) {
        // Group transactions by month
        Map<YearMonth, List<Transaction>> transactionsByMonth = transactions.stream()
            .collect(Collectors.groupingBy(
                transaction -> YearMonth.from(transaction.getTransactionDate()),
                LinkedHashMap::new, // Maintain order
                Collectors.toList()
            ));
        
        // Clear the existing grid content
        transactionsGrid.setItems();
        
        // Create a flattened list with month headers for display
        List<TransactionOrHeader> displayItems = new ArrayList<>();
        
        // Sort months in descending order (newest first)
        transactionsByMonth.entrySet().stream()
            .sorted(Map.Entry.<YearMonth, List<Transaction>>comparingByKey().reversed())
            .forEach(entry -> {
                YearMonth month = entry.getKey();
                List<Transaction> monthTransactions = entry.getValue();
                
                // Calculate monthly statistics
                BigDecimal monthlyIncome = monthTransactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal monthlyExpenses = monthTransactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal monthlyNet = monthlyIncome.add(monthlyExpenses);
                
                // Add month header
                displayItems.add(new TransactionOrHeader(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    monthTransactions.size(),
                    monthlyIncome,
                    monthlyExpenses,
                    monthlyNet,
                    true
                ));
                
                // Add transactions for this month
                monthTransactions.stream()
                    .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                    .forEach(transaction -> displayItems.add(new TransactionOrHeader(transaction)));
            });
        
        updateGridForGroupedDisplay(displayItems);
    }
    
    private void updateGridForGroupedDisplay(List<TransactionOrHeader> items) {
        // For monthly grouping, we'll create a simpler approach using the existing grid structure
        // First, create the month summary information as separate components above the grid
        
        // Remove existing month summaries if any
        removeOldMonthlySummaries();
        
        // Create monthly summaries
        List<Transaction> allTransactions = items.stream()
            .filter(item -> !item.isHeader())
            .map(TransactionOrHeader::getTransaction)
            .collect(Collectors.toList());
        
        createMonthlySummaryComponents(items);
        
        // Set the transactions in the regular grid
        transactionsGrid.setItems(allTransactions);
    }
    
    private void removeOldMonthlySummaries() {
        // Remove any existing monthly summary components
        getChildren()
            .filter(component -> component.getId().isPresent() && 
                   component.getId().get().startsWith("monthly-summary"))
            .collect(Collectors.toList())
            .forEach(this::remove);
    }
    
    private void createMonthlySummaryComponents(List<TransactionOrHeader> items) {
        VerticalLayout summariesContainer = new VerticalLayout();
        summariesContainer.setId("monthly-summary-container");
        summariesContainer.setPadding(true);
        summariesContainer.setSpacing(true);
        
        H3 summaryTitle = new H3("📅 Monthly Summary");
        summariesContainer.add(summaryTitle);
        
        // Create month cards
        HorizontalLayout monthCardsLayout = new HorizontalLayout();
        monthCardsLayout.setWidthFull();
        monthCardsLayout.setSpacing(true);
        
        items.stream()
            .filter(TransactionOrHeader::isHeader)
            .forEach(header -> {
                VerticalLayout monthCard = createMonthCard(
                    header.getHeaderText(),
                    header.getTransactionCount(),
                    header.getMonthlyIncome(),
                    header.getMonthlyExpenses(),
                    header.getMonthlyNet()
                );
                monthCardsLayout.add(monthCard);
            });
        
        summariesContainer.add(monthCardsLayout);
        
        // Insert before the transactions grid
        int gridIndex = 0;
        for (int i = 0; i < getComponentCount(); i++) {
            if (getComponentAt(i) == transactionsGrid) {
                gridIndex = i;
                break;
            }
        }
        addComponentAtIndex(gridIndex, summariesContainer);
    }
    
    private VerticalLayout createMonthCard(String monthName, int transactionCount, 
                                         BigDecimal income, BigDecimal expenses, BigDecimal net) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle().set("background", "var(--lumo-contrast-5pct)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        card.setWidth("250px");
        
        // Month name and transaction count
        H4 monthHeader = new H4(monthName);
        monthHeader.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");
        monthHeader.getStyle().set("color", "var(--lumo-primary-color)");
        
        Paragraph transactionCountText = new Paragraph(transactionCount + " transactions");
        transactionCountText.getStyle().set("margin", "0 0 var(--lumo-space-m) 0");
        transactionCountText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        transactionCountText.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        // Financial summary
        HorizontalLayout incomeRow = new HorizontalLayout();
        incomeRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        incomeRow.setWidthFull();
        incomeRow.add(
            new Span("↑ Income:"),
            new Span(currencyFormat.format(income))
        );
        incomeRow.getStyle().set("color", "var(--lumo-success-color)");
        
        HorizontalLayout expensesRow = new HorizontalLayout();
        expensesRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        expensesRow.setWidthFull();
        expensesRow.add(
            new Span("↓ Expenses:"),
            new Span(currencyFormat.format(expenses.abs()))
        );
        expensesRow.getStyle().set("color", "var(--lumo-error-color)");
        
        HorizontalLayout netRow = new HorizontalLayout();
        netRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        netRow.setWidthFull();
        netRow.getStyle().set("border-top", "1px solid var(--lumo-contrast-20pct)");
        netRow.getStyle().set("padding-top", "var(--lumo-space-xs)");
        netRow.getStyle().set("font-weight", "bold");
        
        Span netLabel = new Span("= Net:");
        Span netValue = new Span(currencyFormat.format(net));
        
        if (net.compareTo(BigDecimal.ZERO) >= 0) {
            netRow.getStyle().set("color", "var(--lumo-success-color)");
        } else {
            netRow.getStyle().set("color", "var(--lumo-error-color)");
        }
        
        netRow.add(netLabel, netValue);
        
        card.add(monthHeader, transactionCountText, incomeRow, expensesRow, netRow);
        return card;
    }
    
    // Filter Persistence Methods
    private void saveFilterPreferences() {
        // Save filter preferences to browser's localStorage
        getElement().executeJs(
            "localStorage.setItem('transactionFilters', JSON.stringify({" +
            "  startDate: arguments[0]," +
            "  endDate: arguments[1]," +
            "  category: arguments[2]," +
            "  minAmount: arguments[3]," +
            "  maxAmount: arguments[4]," +
            "  type: arguments[5]," +
            "  status: arguments[6]," +
            "  groupByMonth: arguments[7]" +
            "}));",
            startDateFilter.getValue() != null ? startDateFilter.getValue().toString() : null,
            endDateFilter.getValue() != null ? endDateFilter.getValue().toString() : null,
            categoryFilter.getValue() != null ? categoryFilter.getValue().getName() : null,
            minAmountFilter.getValue(),
            maxAmountFilter.getValue(),
            typeFilter.getValue() != null ? typeFilter.getValue().toString() : null,
            statusFilter.getValue() != null ? statusFilter.getValue().toString() : null,
            groupByMonthCheckbox.getValue()
        );
    }
    
    private void loadFilterPreferences() {
        // Load filter preferences from browser's localStorage (simplified version)
        getElement().executeJs(
            "const filters = JSON.parse(localStorage.getItem('transactionFilters') || '{}');" +
            "if (filters.groupByMonth === true) {" +
            "  return 'true';" +
            "} else {" +
            "  return 'false';" +
            "}"
        ).then(result -> {
            if (result.asString().equals("true")) {
                groupByMonthCheckbox.setValue(true);
            } else {
                groupByMonthCheckbox.setValue(false);
            }
        });
    }
    
    private void clearFilterPreferences() {
        // Clear saved filter preferences
        getElement().executeJs("localStorage.removeItem('transactionFilters');");
    }
    
    // Helper class for displaying mixed transaction and header data
    private static class TransactionOrHeader {
        private Transaction transaction;
        private String headerText;
        private int transactionCount;
        private BigDecimal monthlyIncome;
        private BigDecimal monthlyExpenses;
        private BigDecimal monthlyNet;
        private boolean isHeader;
        
        // Constructor for header
        public TransactionOrHeader(String headerText, int transactionCount, 
                                 BigDecimal monthlyIncome, BigDecimal monthlyExpenses, 
                                 BigDecimal monthlyNet, boolean isHeader) {
            this.headerText = headerText;
            this.transactionCount = transactionCount;
            this.monthlyIncome = monthlyIncome;
            this.monthlyExpenses = monthlyExpenses;
            this.monthlyNet = monthlyNet;
            this.isHeader = isHeader;
        }
        
        // Constructor for transaction
        public TransactionOrHeader(Transaction transaction) {
            this.transaction = transaction;
            this.isHeader = false;
        }
        
        public boolean isHeader() { return isHeader; }
        public Transaction getTransaction() { return transaction; }
        public String getHeaderText() { return headerText; }
        public int getTransactionCount() { return transactionCount; }
        public BigDecimal getMonthlyIncome() { return monthlyIncome; }
        public BigDecimal getMonthlyExpenses() { return monthlyExpenses; }
        public BigDecimal getMonthlyNet() { return monthlyNet; }
    }

    private void openAddTransactionDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add New Transaction");
        dialog.setWidth("600px");

        FormLayout formLayout = new FormLayout();
        
        // Form fields
        TextField descriptionField = new TextField("Description");
        descriptionField.setRequiredIndicatorVisible(true);
        
        BigDecimalField amountField = new BigDecimalField("Amount");
        amountField.setRequiredIndicatorVisible(true);
        
        DatePicker dateField = new DatePicker("Date");
        dateField.setRequiredIndicatorVisible(true);
        dateField.setValue(java.time.LocalDate.now());
        
        ComboBox<Account> accountCombo = new ComboBox<>("Account");
        accountCombo.setRequiredIndicatorVisible(true);
        accountCombo.setItems(accountService.findAllActive());
        accountCombo.setItemLabelGenerator(Account::getAccountName);
        
        ComboBox<Category> categoryCombo = new ComboBox<>("Category");
        categoryCombo.setItems(categoryService.findAllActive());
        categoryCombo.setItemLabelGenerator(Category::getName);
        
        ComboBox<Transaction.TransactionType> typeCombo = new ComboBox<>("Type");
        typeCombo.setRequiredIndicatorVisible(true);
        typeCombo.setItems(Transaction.TransactionType.values());
        typeCombo.setValue(Transaction.TransactionType.DEBIT);
        
        TextField payeeField = new TextField("Payee");
        TextField memoField = new TextField("Memo");
        
        formLayout.add(descriptionField, amountField, dateField, accountCombo, 
                      categoryCombo, typeCombo, payeeField, memoField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        
        // Buttons
        Button saveButton = new Button("Save", e -> {
            try {
                if (descriptionField.isEmpty() || amountField.isEmpty() || 
                    dateField.isEmpty() || accountCombo.isEmpty()) {
                    Notification.show("Please fill in all required fields", 3000, 
                        Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                Transaction transaction = new Transaction();
                transaction.setDescription(descriptionField.getValue());
                transaction.setAmount(amountField.getValue());
                transaction.setTransactionDate(dateField.getValue());
                transaction.setAccount(accountCombo.getValue());
                transaction.setCategory(categoryCombo.getValue());
                transaction.setType(typeCombo.getValue());
                transaction.setPayee(payeeField.getValue());
                transaction.setMemo(memoField.getValue());
                transaction.setStatus(Transaction.TransactionStatus.PENDING);
                
                transactionService.save(transaction);
                refreshData();
                dialog.close();
                
                Notification.show("Transaction created successfully!", 3000, 
                    Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                System.err.println("Error saving transaction: " + ex.getMessage());
                ex.printStackTrace();
                Notification.show("Error saving transaction: " + ex.getMessage(), 5000, 
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(formLayout);
        dialog.open();
    }
    
    private void openImportCsvDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Import Transactions from CSV");
        dialog.setWidth("600px");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        
        // Instructions
        Paragraph instructions = new Paragraph(
            "Upload a CSV file with your transaction data. The CSV should have columns: " +
            "Date, Description, Amount (required), and optionally: Account, Category, Type, Payee, Memo.\n\n" +
            "Supported date formats: YYYY-MM-DD, MM/DD/YYYY, DD/MM/YYYY"
        );
        instructions.getStyle().set("white-space", "pre-line");
        instructions.getStyle().set("background", "var(--lumo-contrast-5pct)");
        instructions.getStyle().set("padding", "var(--lumo-space-m)");
        instructions.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        
        // File upload component
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv", "text/csv", "application/csv");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(10 * 1024 * 1024); // 10MB limit
        upload.setDropLabel(new com.vaadin.flow.component.html.Span("Drop CSV file here or click to browse"));
        
        // Upload success handler
        upload.addSucceededListener(event -> {
            try {
                System.out.println("CSV import started for file: " + event.getFileName());
                CsvImportService.CsvImportResult result = csvImportService.importTransactions(buffer.getInputStream());
                
                if (result.isSuccess()) {
                    refreshData();
                    dialog.close();
                    
                    String message = String.format(
                        "Import completed successfully!\n" +
                        "• Imported: %d transactions\n" +
                        "• Errors: %d", 
                        result.getSuccessCount(), 
                        result.getErrorCount()
                    );
                    
                    if (result.getErrorCount() > 0) {
                        // Show detailed error dialog if there were errors
                        showImportResultDialog(message, result.getErrors());
                    } else {
                        Notification.show(message, 5000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    }
                } else {
                    showImportErrorDialog("Import failed", result.getErrors());
                }
                
            } catch (Exception e) {
                System.err.println("Error during CSV import: " + e.getMessage());
                e.printStackTrace();
                showImportErrorDialog("Import failed", List.of("Unexpected error: " + e.getMessage()));
            }
        });
        
        // Upload failed handler
        upload.addFailedListener(event -> {
            System.err.println("CSV upload failed: " + event.getReason().getMessage());
            Notification.show("Upload failed: " + event.getReason().getMessage(), 5000, 
                Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });
        
        dialogLayout.add(instructions, upload);
        
        // Buttons
        Button closeButton = new Button("Close", e -> dialog.close());
        
        dialog.getFooter().add(closeButton);
        dialog.add(dialogLayout);
        dialog.open();
    }
    
    private void showImportResultDialog(String message, List<String> errors) {
        Dialog resultDialog = new Dialog();
        resultDialog.setHeaderTitle("Import Results");
        resultDialog.setWidth("700px");
        resultDialog.setHeight("500px");
        
        VerticalLayout layout = new VerticalLayout();
        
        // Success message
        Paragraph successMsg = new Paragraph(message);
        successMsg.getStyle().set("white-space", "pre-line");
        successMsg.getStyle().set("font-weight", "bold");
        
        layout.add(successMsg);
        
        if (!errors.isEmpty()) {
            H3 errorHeader = new H3("Errors encountered:");
            layout.add(errorHeader);
            
            // Create scrollable area for errors
            VerticalLayout errorList = new VerticalLayout();
            errorList.getStyle().set("background", "var(--lumo-contrast-5pct)");
            errorList.getStyle().set("padding", "var(--lumo-space-s)");
            errorList.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
            errorList.getStyle().set("max-height", "300px");
            errorList.getStyle().set("overflow-y", "auto");
            
            for (String error : errors) {
                Paragraph errorMsg = new Paragraph("• " + error);
                errorMsg.getStyle().set("margin", "0");
                errorMsg.getStyle().set("color", "var(--lumo-error-text-color)");
                errorList.add(errorMsg);
            }
            
            layout.add(errorList);
        }
        
        Button closeButton = new Button("Close", e -> resultDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        resultDialog.getFooter().add(closeButton);
        resultDialog.add(layout);
        resultDialog.open();
    }
    
    private void showImportErrorDialog(String title, List<String> errors) {
        Dialog errorDialog = new Dialog();
        errorDialog.setHeaderTitle(title);
        errorDialog.setWidth("600px");
        errorDialog.setHeight("400px");
        
        VerticalLayout layout = new VerticalLayout();
        
        // Error list
        for (String error : errors) {
            Paragraph errorMsg = new Paragraph("• " + error);
            errorMsg.getStyle().set("color", "var(--lumo-error-text-color)");
            layout.add(errorMsg);
        }
        
        Button closeButton = new Button("Close", e -> errorDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        errorDialog.getFooter().add(closeButton);
        errorDialog.add(layout);
        errorDialog.open();
    }
    
    private void openEditTransactionDialog(Transaction transaction) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Transaction");
        dialog.setWidth("600px");

        FormLayout formLayout = new FormLayout();
        
        // Form fields - pre-populated with existing data
        TextField descriptionField = new TextField("Description");
        descriptionField.setRequiredIndicatorVisible(true);
        descriptionField.setValue(transaction.getDescription() != null ? transaction.getDescription() : "");
        
        BigDecimalField amountField = new BigDecimalField("Amount");
        amountField.setRequiredIndicatorVisible(true);
        amountField.setValue(transaction.getAmount());
        
        DatePicker dateField = new DatePicker("Date");
        dateField.setRequiredIndicatorVisible(true);
        dateField.setValue(transaction.getTransactionDate());
        
        ComboBox<Account> accountCombo = new ComboBox<>("Account");
        accountCombo.setRequiredIndicatorVisible(true);
        accountCombo.setItems(accountService.findAllActive());
        accountCombo.setItemLabelGenerator(Account::getAccountName);
        accountCombo.setValue(transaction.getAccount());
        
        ComboBox<Category> categoryCombo = new ComboBox<>("Category");
        categoryCombo.setItems(categoryService.findAllActive());
        categoryCombo.setItemLabelGenerator(Category::getName);
        categoryCombo.setValue(transaction.getCategory());
        
        ComboBox<Transaction.TransactionType> typeCombo = new ComboBox<>("Type");
        typeCombo.setRequiredIndicatorVisible(true);
        typeCombo.setItems(Transaction.TransactionType.values());
        typeCombo.setValue(transaction.getType());
        
        TextField payeeField = new TextField("Payee");
        payeeField.setValue(transaction.getPayee() != null ? transaction.getPayee() : "");
        
        TextField memoField = new TextField("Memo");
        memoField.setValue(transaction.getMemo() != null ? transaction.getMemo() : "");
        
        ComboBox<Transaction.TransactionStatus> statusCombo = new ComboBox<>("Status");
        statusCombo.setItems(Transaction.TransactionStatus.values());
        statusCombo.setValue(transaction.getStatus());
        
        formLayout.add(descriptionField, amountField, dateField, accountCombo, 
                      categoryCombo, typeCombo, payeeField, memoField, statusCombo);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        
        // Buttons
        Button saveButton = new Button("Save Changes", e -> {
            try {
                if (descriptionField.isEmpty() || amountField.isEmpty() || 
                    dateField.isEmpty() || accountCombo.isEmpty()) {
                    Notification.show("Please fill in all required fields", 3000, 
                        Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                // Update transaction with new values
                transaction.setDescription(descriptionField.getValue());
                transaction.setAmount(amountField.getValue());
                transaction.setTransactionDate(dateField.getValue());
                transaction.setAccount(accountCombo.getValue());
                transaction.setCategory(categoryCombo.getValue());
                transaction.setType(typeCombo.getValue());
                transaction.setPayee(payeeField.getValue().isEmpty() ? null : payeeField.getValue());
                transaction.setMemo(memoField.getValue().isEmpty() ? null : memoField.getValue());
                transaction.setStatus(statusCombo.getValue());
                
                transactionService.save(transaction);
                refreshData();
                dialog.close();
                
                Notification.show("Transaction updated successfully!", 3000, 
                    Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                System.err.println("Error updating transaction: " + ex.getMessage());
                ex.printStackTrace();
                Notification.show("Error updating transaction: " + ex.getMessage(), 5000, 
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(formLayout);
        dialog.open();
    }
    
    private void confirmDeleteTransaction(Transaction transaction) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Delete Transaction");
        confirmDialog.setText(String.format(
            "Are you sure you want to delete this transaction?\n\n" +
            "Description: %s\n" +
            "Amount: %s\n" +
            "Date: %s\n\n" +
            "This action cannot be undone.",
            transaction.getDescription(),
            currencyFormat.format(transaction.getAmount()),
            transaction.getTransactionDate().format(dateFormat)
        ));
        
        confirmDialog.setCancelable(true);
        confirmDialog.setConfirmText("Delete");
        confirmDialog.setConfirmButtonTheme("error primary");
        
        confirmDialog.addConfirmListener(e -> {
            try {
                transactionService.delete(transaction);
                refreshData();
                
                Notification.show("Transaction deleted successfully!", 3000, 
                    Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                System.err.println("Error deleting transaction: " + ex.getMessage());
                ex.printStackTrace();
                Notification.show("Error deleting transaction: " + ex.getMessage(), 5000, 
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        confirmDialog.open();
    }
    
    private void confirmBulkDeleteTransactions() {
        Set<Transaction> selectedTransactions = transactionsGrid.getSelectedItems();
        
        if (selectedTransactions.isEmpty()) {
            Notification.show("No transactions selected", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader(String.format("Delete %d Transaction(s)", selectedTransactions.size()));
        
        // Create summary of selected transactions
        StringBuilder transactionSummary = new StringBuilder();
        transactionSummary.append(String.format("Are you sure you want to delete %d selected transaction(s)?\n\n", selectedTransactions.size()));
        
        transactionSummary.append("Selected transactions:\n");
        int count = 0;
        for (Transaction transaction : selectedTransactions) {
            if (count < 10) { // Show first 10 transactions
                transactionSummary.append(String.format("• %s - %s (%s)\n", 
                    transaction.getTransactionDate().format(dateFormat),
                    transaction.getDescription(),
                    currencyFormat.format(transaction.getAmount())));
            } else {
                transactionSummary.append(String.format("... and %d more transaction(s)\n", selectedTransactions.size() - 10));
                break;
            }
            count++;
        }
        
        transactionSummary.append("\nThis action cannot be undone.");
        
        confirmDialog.setText(transactionSummary.toString());
        confirmDialog.setCancelable(true);
        confirmDialog.setConfirmText("Delete All");
        confirmDialog.setConfirmButtonTheme("error primary");
        
        confirmDialog.addConfirmListener(e -> {
            try {
                // Delete all selected transactions
                for (Transaction transaction : selectedTransactions) {
                    transactionService.delete(transaction);
                }
                
                // Clear selection and refresh data
                transactionsGrid.deselectAll();
                refreshData();
                
                Notification.show(
                    String.format("Successfully deleted %d transaction(s)!", selectedTransactions.size()), 
                    4000, 
                    Notification.Position.BOTTOM_START
                ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                System.err.println("Error during bulk delete: " + ex.getMessage());
                ex.printStackTrace();
                Notification.show("Error deleting transactions: " + ex.getMessage(), 5000, 
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        confirmDialog.open();
    }
}
