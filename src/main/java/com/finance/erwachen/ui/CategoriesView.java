package com.finance.erwachen.ui;

import com.finance.erwachen.entity.Category;
import com.finance.erwachen.service.CategoryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@PageTitle("Categories")
@Route(value = "categories", layout = MainLayout.class)
@PermitAll
public class CategoriesView extends VerticalLayout {

    private final CategoryService categoryService;
    private Grid<Category> categoriesGrid;

    @Autowired
    public CategoriesView(CategoryService categoryService) {
        this.categoryService = categoryService;

        setSizeFull();
        createHeader();
        createCategoriesGrid();
        refreshData();
    }

    private void createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        H2 title = new H2("Categories");

        HorizontalLayout buttons = new HorizontalLayout();
        Button addCategoryButton = new Button("Add Category", VaadinIcon.PLUS.create());
        addCategoryButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        addCategoryButton.addClickListener(e -> openAddCategoryDialog());
        
        buttons.add(addCategoryButton);
        header.add(title, buttons);

        add(header);
    }

    private void createCategoriesGrid() {
        categoriesGrid = new Grid<>(Category.class, false);
        categoriesGrid.setSizeFull();

        categoriesGrid.addColumn(Category::getName).setHeader("Name").setSortable(true);
        categoriesGrid.addColumn(Category::getType).setHeader("Type").setSortable(true);
        categoriesGrid.addColumn(category -> 
                category.getParent() != null ? category.getParent().getName() : "Root Category")
                .setHeader("Parent").setSortable(true);
        categoriesGrid.addColumn(Category::getDescription).setHeader("Description");
        categoriesGrid.addColumn(Category::getActive).setHeader("Active").setSortable(true);
        
        // Add action buttons column
        categoriesGrid.addComponentColumn(category -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.getElement().setAttribute("title", "Edit Category");
            editButton.addClickListener(e -> openEditCategoryDialog(category));
            
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteButton.getElement().setAttribute("title", "Delete Category");
            deleteButton.addClickListener(e -> confirmDeleteCategory(category));
            
            actions.add(editButton, deleteButton);
            return actions;
        }).setHeader("Actions").setWidth("120px").setFlexGrow(0);

        add(categoriesGrid);
    }

    private void refreshData() {
        List<Category> categories = categoryService.findAllActive();
        categoriesGrid.setItems(categories);
    }

    private void openAddCategoryDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add New Category");
        dialog.setWidth("500px");

        FormLayout formLayout = new FormLayout();
        
        // Form fields
        TextField nameField = new TextField("Name");
        nameField.setRequiredIndicatorVisible(true);
        
        ComboBox<Category.CategoryType> typeCombo = new ComboBox<>("Type");
        typeCombo.setRequiredIndicatorVisible(true);
        typeCombo.setItems(Category.CategoryType.values());
        typeCombo.setValue(Category.CategoryType.EXPENSE);
        
        ComboBox<Category> parentCombo = new ComboBox<>("Parent Category");
        parentCombo.setItems(categoryService.findAllActive());
        parentCombo.setItemLabelGenerator(Category::getName);
        
        TextField descriptionField = new TextField("Description");
        
        formLayout.add(nameField, typeCombo, parentCombo, descriptionField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        
        // Buttons
        Button saveButton = new Button("Save", e -> {
            try {
                if (nameField.isEmpty() || typeCombo.isEmpty()) {
                    Notification.show("Please fill in all required fields", 3000, 
                        Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                Category category = new Category();
                category.setName(nameField.getValue());
                category.setType(typeCombo.getValue());
                category.setParent(parentCombo.getValue());
                category.setDescription(descriptionField.getValue());
                category.setActive(true);
                
                categoryService.save(category);
                refreshData();
                dialog.close();
                
                Notification.show("Category created successfully!", 3000, 
                    Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                System.err.println("Error saving category: " + ex.getMessage());
                ex.printStackTrace();
                Notification.show("Error saving category: " + ex.getMessage(), 5000, 
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
    
    private void openEditCategoryDialog(Category category) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Category");
        dialog.setWidth("500px");

        FormLayout formLayout = new FormLayout();
        
        // Form fields - pre-populated with existing data
        TextField nameField = new TextField("Name");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setValue(category.getName() != null ? category.getName() : "");
        
        ComboBox<Category.CategoryType> typeCombo = new ComboBox<>("Type");
        typeCombo.setRequiredIndicatorVisible(true);
        typeCombo.setItems(Category.CategoryType.values());
        typeCombo.setValue(category.getType());
        
        ComboBox<Category> parentCombo = new ComboBox<>("Parent Category");
        List<Category> availableParents = categoryService.findAllActive().stream()
            .filter(c -> !c.getId().equals(category.getId())) // Exclude self to prevent circular reference
            .collect(java.util.stream.Collectors.toList());
        parentCombo.setItems(availableParents);
        parentCombo.setItemLabelGenerator(Category::getName);
        parentCombo.setValue(category.getParent());
        
        TextField descriptionField = new TextField("Description");
        descriptionField.setValue(category.getDescription() != null ? category.getDescription() : "");
        
        formLayout.add(nameField, typeCombo, parentCombo, descriptionField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        
        // Buttons
        Button saveButton = new Button("Save Changes", e -> {
            try {
                if (nameField.isEmpty() || typeCombo.isEmpty()) {
                    Notification.show("Please fill in all required fields", 3000, 
                        Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                // Update category with new values
                category.setName(nameField.getValue());
                category.setType(typeCombo.getValue());
                category.setParent(parentCombo.getValue());
                category.setDescription(descriptionField.getValue().isEmpty() ? null : descriptionField.getValue());
                
                categoryService.save(category);
                refreshData();
                dialog.close();
                
                Notification.show("Category updated successfully!", 3000, 
                    Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                System.err.println("Error updating category: " + ex.getMessage());
                ex.printStackTrace();
                Notification.show("Error updating category: " + ex.getMessage(), 5000, 
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
    
    private void confirmDeleteCategory(Category category) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Delete Category");
        confirmDialog.setText(String.format(
            "Are you sure you want to delete the category '%s'?\n\n" +
            "This action cannot be undone. Transactions using this category will become uncategorized.",
            category.getName()
        ));
        
        confirmDialog.setCancelable(true);
        confirmDialog.setConfirmText("Delete");
        confirmDialog.setConfirmButtonTheme("error primary");
        
        confirmDialog.addConfirmListener(e -> {
            try {
                categoryService.delete(category);
                refreshData();
                
                Notification.show("Category deleted successfully!", 3000, 
                    Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
            } catch (Exception ex) {
                System.err.println("Error deleting category: " + ex.getMessage());
                ex.printStackTrace();
                Notification.show("Error deleting category: " + ex.getMessage(), 5000, 
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        confirmDialog.open();
    }
}
