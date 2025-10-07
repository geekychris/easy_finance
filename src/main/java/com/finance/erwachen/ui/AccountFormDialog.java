package com.finance.erwachen.ui;

import com.finance.erwachen.entity.Account;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.validator.StringLengthValidator;

import java.math.BigDecimal;
import java.util.function.Consumer;

public class AccountFormDialog extends Dialog {
    
    private final BeanValidationBinder<Account> binder;
    private final Consumer<Account> saveHandler;
    private Account currentAccount;
    
    // Form fields
    private TextField accountNameField;
    private TextField accountNumberField;
    private ComboBox<Account.AccountType> accountTypeField;
    private ComboBox<Account.BankProvider> bankProviderField;
    private BigDecimalField balanceField;
    private TextArea descriptionField;
    
    // Buttons
    private Button saveButton;
    private Button cancelButton;

    public AccountFormDialog(Consumer<Account> saveHandler) {
        this.saveHandler = saveHandler;
        this.binder = new BeanValidationBinder<>(Account.class);
        
        initializeDialog();
        createFormLayout();
        createButtonLayout();
        setupValidation();
        
        setWidth("500px");
        setResizable(true);
        setDraggable(true);
    }
    
    private void initializeDialog() {
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);
    }
    
    private void createFormLayout() {
        H2 title = new H2("Account Details");
        
        FormLayout formLayout = new FormLayout();
        
        // Account Name
        accountNameField = new TextField("Account Name");
        accountNameField.setRequired(true);
        accountNameField.setRequiredIndicatorVisible(true);
        accountNameField.setWidthFull();
        
        // Account Number
        accountNumberField = new TextField("Account Number");
        accountNumberField.setRequired(true);
        accountNumberField.setRequiredIndicatorVisible(true);
        accountNumberField.setWidthFull();
        
        // Account Type
        accountTypeField = new ComboBox<>("Account Type");
        accountTypeField.setItems(Account.AccountType.values());
        accountTypeField.setItemLabelGenerator(this::formatAccountType);
        accountTypeField.setRequired(true);
        accountTypeField.setRequiredIndicatorVisible(true);
        accountTypeField.setWidthFull();
        
        // Bank Provider
        bankProviderField = new ComboBox<>("Bank Provider");
        bankProviderField.setItems(Account.BankProvider.values());
        bankProviderField.setItemLabelGenerator(this::formatBankProvider);
        bankProviderField.setRequired(true);
        bankProviderField.setRequiredIndicatorVisible(true);
        bankProviderField.setWidthFull();
        
        // Initial Balance
        balanceField = new BigDecimalField("Initial Balance");
        balanceField.setValue(BigDecimal.ZERO);
        balanceField.setWidthFull();
        balanceField.setPrefixComponent(new com.vaadin.flow.component.html.Span("$"));
        
        // Description
        descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();
        descriptionField.setMaxHeight("100px");
        
        formLayout.add(
            accountNameField, accountNumberField,
            accountTypeField, bankProviderField,
            balanceField, descriptionField
        );
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("600px", 2)
        );
        
        add(title, formLayout);
    }
    
    private void createButtonLayout() {
        saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> save());
        
        cancelButton = new Button("Cancel");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        
        add(buttonLayout);
    }
    
    private void setupValidation() {
        // Bind fields
        binder.forField(accountNameField)
            .withValidator(new StringLengthValidator("Account name must be between 1 and 255 characters", 1, 255))
            .bind(Account::getAccountName, Account::setAccountName);
        
        binder.forField(accountNumberField)
            .withValidator(new StringLengthValidator("Account number must be between 1 and 255 characters", 1, 255))
            .withValidator(this::validateUniqueAccountNumber)
            .bind(Account::getAccountNumber, Account::setAccountNumber);
        
        binder.forField(accountTypeField)
            .withValidator(type -> type != null, "Please select an account type")
            .bind(Account::getAccountType, Account::setAccountType);
        
        binder.forField(bankProviderField)
            .withValidator(provider -> provider != null, "Please select a bank provider")
            .bind(Account::getBankProvider, Account::setBankProvider);
        
        binder.forField(balanceField)
            .withValidator(balance -> balance != null, "Balance cannot be empty")
            .bind(Account::getBalance, Account::setBalance);
        
        binder.forField(descriptionField)
            .bind(Account::getDescription, Account::setDescription);
        
        // Enable save button only when form is valid
        binder.addStatusChangeListener(event -> 
            saveButton.setEnabled(binder.isValid())
        );
    }
    
    private ValidationResult validateUniqueAccountNumber(String accountNumber, 
                                                        com.vaadin.flow.data.binder.ValueContext context) {
        // Note: This is a basic validation - in a real app, you'd check against the database
        // excluding the current account if editing
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return ValidationResult.error("Account number is required");
        }
        
        // Add more validation logic here if needed (e.g., format checks)
        return ValidationResult.ok();
    }
    
    private String formatAccountType(Account.AccountType type) {
        if (type == null) return "";
        return switch (type) {
            case CHECKING -> "Checking";
            case SAVINGS -> "Savings";
            case CREDIT_CARD -> "Credit Card";
            case INVESTMENT -> "Investment";
            case LOAN -> "Loan";
        };
    }
    
    private String formatBankProvider(Account.BankProvider provider) {
        if (provider == null) return "";
        return switch (provider) {
            case SCHWAB -> "Charles Schwab";
            case BANK_OF_AMERICA -> "Bank of America";
            case MANUAL -> "Manual Entry";
        };
    }
    
    public void openForNew() {
        currentAccount = new Account();
        currentAccount.setBalance(BigDecimal.ZERO);
        binder.readBean(currentAccount);
        
        getDialogTitle().setText("Add New Account");
        open();
        accountNameField.focus();
    }
    
    public void openForEdit(Account account) {
        currentAccount = new Account();
        // Copy values to avoid modifying the original until save
        currentAccount.setId(account.getId());
        currentAccount.setAccountName(account.getAccountName());
        currentAccount.setAccountNumber(account.getAccountNumber());
        currentAccount.setAccountType(account.getAccountType());
        currentAccount.setBankProvider(account.getBankProvider());
        currentAccount.setBalance(account.getBalance());
        currentAccount.setDescription(account.getDescription());
        currentAccount.setActive(account.getActive());
        currentAccount.setCreatedAt(account.getCreatedAt());
        currentAccount.setUpdatedAt(account.getUpdatedAt());
        currentAccount.setLastSyncDate(account.getLastSyncDate());
        
        binder.readBean(currentAccount);
        
        getDialogTitle().setText("Edit Account");
        // Make account number read-only when editing
        accountNumberField.setReadOnly(true);
        open();
        accountNameField.focus();
    }
    
    private void save() {
        try {
            binder.writeBean(currentAccount);
            saveHandler.accept(currentAccount);
            showSuccessNotification();
            close();
        } catch (ValidationException e) {
            showErrorNotification("Please correct the validation errors");
        }
    }
    
    private void showSuccessNotification() {
        Notification notification = Notification.show(
            currentAccount.getId() == null ? "Account created successfully" : "Account updated successfully"
        );
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
    
    @Override
    public void close() {
        // Reset form state
        accountNumberField.setReadOnly(false);
        binder.readBean(new Account());
        super.close();
    }
    
    private H2 getDialogTitle() {
        return (H2) getChildren().filter(component -> component instanceof H2).findFirst().orElse(null);
    }
}