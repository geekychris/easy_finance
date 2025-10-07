package com.finance.erwachen.service;

import com.finance.erwachen.entity.Account;
import com.finance.erwachen.entity.Category;
import com.finance.erwachen.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CsvImportService {

    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private CategoryService categoryService;

    public CsvImportResult importTransactions(InputStream csvInputStream) {
        CsvImportResult result = new CsvImportResult();
        List<String> errors = new ArrayList<>();
        List<Transaction> successfulTransactions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvInputStream))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                result.setSuccess(false);
                result.getErrors().add("CSV file is empty");
                return result;
            }
            
            // Parse header to determine column positions
            String[] headers = headerLine.split(",");
            Map<String, Integer> columnMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnMap.put(headers[i].trim().toLowerCase(), i);
            }
            
            // Validate required columns
            String[] requiredColumns = {"date", "description", "amount"};
            for (String required : requiredColumns) {
                if (!columnMap.containsKey(required)) {
                    result.setSuccess(false);
                    result.getErrors().add("Missing required column: " + required);
                    return result;
                }
            }
            
            String line;
            int lineNumber = 2; // Starting from line 2 (after header)
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    lineNumber++;
                    continue;
                }
                
                try {
                    Transaction transaction = parseCsvLine(line, columnMap, lineNumber);
                    if (transaction != null) {
                        transactionService.save(transaction);
                        successfulTransactions.add(transaction);
                        result.incrementSuccessCount();
                    }
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    result.incrementErrorCount();
                }
                
                lineNumber++;
            }
            
        } catch (IOException e) {
            result.setSuccess(false);
            result.getErrors().add("Error reading CSV file: " + e.getMessage());
            return result;
        }
        
        result.setSuccess(true);
        result.setErrors(errors);
        result.setImportedTransactions(successfulTransactions);
        
        return result;
    }
    
    private Transaction parseCsvLine(String line, Map<String, Integer> columnMap, int lineNumber) throws Exception {
        String[] values = parseCsvValues(line);
        
        Transaction transaction = new Transaction();
        
        // Parse date (required)
        String dateStr = getColumnValue(values, columnMap, "date");
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new Exception("Date is required but was empty");
        }
        
        LocalDate date;
        try {
            // Try multiple date formats
            date = parseDate(dateStr.trim());
            transaction.setTransactionDate(date);
        } catch (Exception e) {
            throw new Exception("Invalid date format: " + dateStr + ". Expected format: YYYY-MM-DD or MM/DD/YYYY");
        }
        
        // Parse description (required)
        String description = getColumnValue(values, columnMap, "description");
        if (description == null || description.trim().isEmpty()) {
            throw new Exception("Description is required but was empty");
        }
        transaction.setDescription(description.trim());
        
        // Parse amount (required)
        String amountStr = getColumnValue(values, columnMap, "amount");
        if (amountStr == null || amountStr.trim().isEmpty()) {
            throw new Exception("Amount is required but was empty");
        }
        
        try {
            BigDecimal amount = new BigDecimal(amountStr.trim().replace(",", ""));
            transaction.setAmount(amount);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid amount format: " + amountStr);
        }
        
        // Parse account (try to find existing or use default)
        String accountName = getColumnValue(values, columnMap, "account");
        Account account = null;
        if (accountName != null && !accountName.trim().isEmpty()) {
            account = findAccountByName(accountName.trim());
        }
        if (account == null) {
            // Try to find any active checking account as default
            List<Account> accounts = accountService.findAllActive();
            account = accounts.stream()
                .filter(acc -> acc.getAccountType() == Account.AccountType.CHECKING)
                .findFirst()
                .orElse(accounts.isEmpty() ? null : accounts.get(0));
        }
        if (account == null) {
            throw new Exception("No valid account found. Please ensure at least one account exists.");
        }
        transaction.setAccount(account);
        
        // Parse category (optional)
        String categoryName = getColumnValue(values, columnMap, "category");
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            Category category = findOrCreateCategory(categoryName.trim());
            transaction.setCategory(category);
        }
        
        // Parse type (optional, default based on amount)
        String typeStr = getColumnValue(values, columnMap, "type");
        Transaction.TransactionType type;
        if (typeStr != null && !typeStr.trim().isEmpty()) {
            try {
                type = Transaction.TransactionType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Default based on amount if invalid type provided
                type = transaction.getAmount().compareTo(BigDecimal.ZERO) >= 0 ? 
                    Transaction.TransactionType.CREDIT : Transaction.TransactionType.DEBIT;
            }
        } else {
            // Default based on amount
            type = transaction.getAmount().compareTo(BigDecimal.ZERO) >= 0 ? 
                Transaction.TransactionType.CREDIT : Transaction.TransactionType.DEBIT;
        }
        transaction.setType(type);
        
        // Parse payee (optional)
        String payee = getColumnValue(values, columnMap, "payee");
        if (payee != null && !payee.trim().isEmpty()) {
            transaction.setPayee(payee.trim());
        }
        
        // Parse memo (optional)
        String memo = getColumnValue(values, columnMap, "memo");
        if (memo != null && !memo.trim().isEmpty()) {
            transaction.setMemo(memo.trim());
        }
        
        // Set default status
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        
        return transaction;
    }
    
    private String[] parseCsvValues(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                values.add(currentValue.toString());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(ch);
            }
        }
        values.add(currentValue.toString());
        
        return values.toArray(new String[0]);
    }
    
    private String getColumnValue(String[] values, Map<String, Integer> columnMap, String columnName) {
        Integer index = columnMap.get(columnName.toLowerCase());
        if (index != null && index < values.length) {
            String value = values[index];
            // Remove surrounding quotes if present
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                value = value.substring(1, value.length() - 1);
            }
            return value;
        }
        return null;
    }
    
    private LocalDate parseDate(String dateStr) {
        // Try different date formats
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ignored) {
            }
        }
        
        throw new RuntimeException("Unable to parse date: " + dateStr);
    }
    
    private Account findAccountByName(String accountName) {
        return accountService.findAllActive().stream()
            .filter(account -> account.getAccountName().equalsIgnoreCase(accountName))
            .findFirst()
            .orElse(null);
    }
    
    private Category findOrCreateCategory(String categoryName) {
        // Try to find existing category
        Category existing = categoryService.findAllActive().stream()
            .filter(category -> category.getName().equalsIgnoreCase(categoryName))
            .findFirst()
            .orElse(null);
            
        if (existing != null) {
            return existing;
        }
        
        // Create new category if not found
        Category newCategory = new Category();
        newCategory.setName(categoryName);
        newCategory.setType(Category.CategoryType.EXPENSE); // Default to expense
        newCategory.setActive(true);
        newCategory.setDescription("Auto-created from CSV import");
        
        return categoryService.save(newCategory);
    }
    
    // Result class for import operations
    public static class CsvImportResult {
        private boolean success;
        private int successCount = 0;
        private int errorCount = 0;
        private List<String> errors = new ArrayList<>();
        private List<Transaction> importedTransactions = new ArrayList<>();
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public int getSuccessCount() { return successCount; }
        public void incrementSuccessCount() { this.successCount++; }
        
        public int getErrorCount() { return errorCount; }
        public void incrementErrorCount() { this.errorCount++; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public List<Transaction> getImportedTransactions() { return importedTransactions; }
        public void setImportedTransactions(List<Transaction> importedTransactions) { 
            this.importedTransactions = importedTransactions; 
        }
    }
}