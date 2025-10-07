# CSV Import Format Documentation

## Overview
The Erwachen application supports importing transactions from CSV (Comma-Separated Values) files. This feature allows you to bulk import transaction data from bank exports or other financial tools.

## CSV Format Requirements

### Required Columns
The following columns are **required** in your CSV file:
- `Date` - The transaction date
- `Description` - The transaction description
- `Amount` - The transaction amount (positive for income/credits, negative for expenses/debits)

### Optional Columns
The following columns are **optional** but recommended:
- `Account` - The account name (must match an existing account in the system)
- `Category` - The category name (will be auto-created if it doesn't exist)
- `Type` - Transaction type (DEBIT, CREDIT, or TRANSFER)
- `Payee` - The payee/merchant name
- `Memo` - Additional notes or memo

## Supported Date Formats
The import system supports the following date formats:
- `YYYY-MM-DD` (e.g., 2024-01-15)
- `MM/DD/YYYY` (e.g., 01/15/2024)
- `DD/MM/YYYY` (e.g., 15/01/2024)
- `MM-DD-YYYY` (e.g., 01-15-2024)
- `DD-MM-YYYY` (e.g., 15-01-2024)

## CSV Header Example
```csv
Date,Description,Amount,Account,Category,Type,Payee,Memo
```

## Data Rules and Conventions

### Amount Format
- Positive numbers represent income/credits (money coming in)
- Negative numbers represent expenses/debits (money going out)
- Commas in numbers are automatically removed (e.g., "1,234.56" becomes "1234.56")
- Do not include currency symbols ($, €, etc.)

### Account Matching
- If an account name is provided, it must match an existing account name exactly (case-insensitive)
- If no account is provided or the account is not found, the system will use the first available checking account
- If no accounts exist, the import will fail

### Category Handling
- If a category name is provided but doesn't exist, it will be automatically created as an EXPENSE category
- Categories are matched case-insensitive
- If no category is provided, the transaction will be imported without a category

### Transaction Type
- If not specified, the system will automatically determine the type based on the amount:
  - Positive amounts → CREDIT
  - Negative amounts → DEBIT
- Valid values: DEBIT, CREDIT, TRANSFER (case-insensitive)

## Sample CSV File
See `sample_transactions.csv` for a complete example with 15 sample transactions showing various scenarios.

## Import Process

1. **Navigate** to Transactions → Click "Import CSV" button
2. **Upload** your CSV file (max 10MB, .csv extension)
3. **Review** the import results:
   - Successful imports are immediately saved to the database
   - Any errors are displayed with line numbers and descriptions
   - The transaction grid is automatically refreshed

## Error Handling
The import system provides detailed error reporting:
- **File format errors** (invalid CSV structure, missing required columns)
- **Data validation errors** (invalid dates, amounts, etc.)
- **Reference errors** (missing accounts, etc.)
- Each error includes the line number and specific issue

## Tips for Successful Imports

1. **Test with a small file first** - Try importing 5-10 transactions to verify your format
2. **Ensure accounts exist** - Create your accounts in the system before importing
3. **Use consistent date formats** - Stick to one date format throughout your file
4. **Clean your data** - Remove extra spaces, special characters, and ensure amounts are numeric
5. **Check category names** - Use consistent category naming to avoid duplicates

## Limitations
- Maximum file size: 10MB
- Maximum number of transactions per import: No hard limit, but larger files may take longer to process
- Only CSV format is supported (not Excel .xlsx files)
- Time information is not supported - only dates

## Troubleshooting Common Issues

### "Missing required column" error
- Ensure your CSV has columns named exactly: `Date`, `Description`, `Amount`
- Column names are case-insensitive but must be spelled correctly

### "Invalid date format" error
- Check that all dates follow one of the supported formats
- Ensure there are no empty date cells

### "No valid account found" error
- Create at least one account in the system before importing
- Ensure account names in CSV match exactly with existing accounts

### "Invalid amount format" error
- Remove any currency symbols ($, €, etc.)
- Ensure amounts are numeric (e.g., -123.45, 1000.00)
- Remove any text from amount columns