package com.finance.erwachen.ui;

import com.finance.erwachen.entity.Account;
import com.finance.erwachen.service.AccountService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@PageTitle("Accounts")
@Route(value = "accounts", layout = MainLayout.class)
@PermitAll
public class AccountsView extends VerticalLayout {

    private final AccountService accountService;
    private Grid<Account> accountsGrid;
    private AccountFormDialog accountFormDialog;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @Autowired
    public AccountsView(AccountService accountService) {
        this.accountService = accountService;
        this.accountFormDialog = new AccountFormDialog(this::saveAccount);

        setSizeFull();
        createHeader();
        createAccountsGrid();
        createContextMenu();
        refreshData();
    }

    private void createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        H2 title = new H2("Accounts");

        HorizontalLayout buttons = new HorizontalLayout();
        Button syncAllButton = new Button("Sync All", VaadinIcon.REFRESH.create());
        syncAllButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        syncAllButton.addClickListener(e -> {
            accountService.syncAllAccounts();
            refreshData();
        });

        Button addAccountButton = new Button("Add Account", VaadinIcon.PLUS.create());
        addAccountButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        addAccountButton.addClickListener(e -> accountFormDialog.openForNew());
        
        buttons.add(syncAllButton, addAccountButton);
        header.add(title, buttons);

        add(header);
    }

    private void createAccountsGrid() {
        accountsGrid = new Grid<>(Account.class, false);
        accountsGrid.setSizeFull();

        accountsGrid.addColumn(Account::getAccountName).setHeader("Account Name").setSortable(true);
        accountsGrid.addColumn(Account::getAccountNumber).setHeader("Account Number").setSortable(true);
        accountsGrid.addColumn(Account::getAccountType).setHeader("Type").setSortable(true);
        accountsGrid.addColumn(account -> account.getBankProvider().name()).setHeader("Bank").setSortable(true);
        accountsGrid.addColumn(account -> currencyFormat.format(account.getBalance()))
                .setHeader("Balance").setSortable(true);
        accountsGrid.addColumn(account -> account.getLastSyncDate().format(dateFormat))
                .setHeader("Last Sync").setSortable(true);
        accountsGrid.addColumn(Account::getDescription).setHeader("Description");

        add(accountsGrid);
    }

    private void createContextMenu() {
        GridContextMenu<Account> contextMenu = accountsGrid.addContextMenu();
        
        contextMenu.addItem("Edit", e -> {
            e.getItem().ifPresent(account -> accountFormDialog.openForEdit(account));
        });
        
        contextMenu.addItem("Delete", e -> {
            e.getItem().ifPresent(this::deleteAccount);
        });
        
        contextMenu.add(new com.vaadin.flow.component.html.Hr());
        
        contextMenu.addItem("Sync Account", e -> {
            e.getItem().ifPresent(this::syncAccount);
        });
    }
    
    private void saveAccount(Account account) {
        try {
            // Validate unique account number
            if (account.getId() == null) {
                // New account
                if (accountService.accountNumberExists(account.getAccountNumber())) {
                    showErrorNotification("Account number already exists: " + account.getAccountNumber());
                    return;
                }
            } else {
                // Editing existing account
                if (accountService.accountNumberExistsExcludingId(account.getAccountNumber(), account.getId())) {
                    showErrorNotification("Account number already exists: " + account.getAccountNumber());
                    return;
                }
            }
            
            accountService.save(account);
            refreshData();
            showSuccessNotification(account.getId() == null ? "Account created successfully" : "Account updated successfully");
        } catch (Exception e) {
            showErrorNotification("Error saving account: " + e.getMessage());
        }
    }
    
    private void deleteAccount(Account account) {
        ConfirmationDialog dialog = ConfirmationDialog.createDeleteAccountDialog(
            account.getAccountName(),
            () -> {
                try {
                    accountService.delete(account);
                    refreshData();
                    showSuccessNotification("Account " + account.getAccountName() + " has been deactivated");
                } catch (Exception e) {
                    showErrorNotification("Error deleting account: " + e.getMessage());
                }
            }
        );
        dialog.open();
    }
    
    private void syncAccount(Account account) {
        try {
            accountService.syncAccount(account);
            refreshData();
            showSuccessNotification("Account " + account.getAccountName() + " synchronized successfully");
        } catch (Exception e) {
            showErrorNotification("Error syncing account: " + e.getMessage());
        }
    }
    
    private void showSuccessNotification(String message) {
        Notification notification = Notification.show(message);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void refreshData() {
        List<Account> accounts = accountService.findAllActive();
        accountsGrid.setItems(accounts);
    }
}
