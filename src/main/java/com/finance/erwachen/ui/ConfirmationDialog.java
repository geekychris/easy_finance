package com.finance.erwachen.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class ConfirmationDialog extends Dialog {
    
    public ConfirmationDialog() {
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);
        setWidth("400px");
        setResizable(false);
        setDraggable(true);
    }
    
    public static ConfirmationDialog createDeleteAccountDialog(String accountName, Runnable onConfirm) {
        ConfirmationDialog dialog = new ConfirmationDialog();
        
        // Header with warning icon
        Icon warningIcon = VaadinIcon.WARNING.create();
        warningIcon.setColor("var(--lumo-error-color)");
        warningIcon.setSize("24px");
        
        H3 title = new H3("Delete Account");
        title.getStyle().set("margin", "0");
        title.getStyle().set("color", "var(--lumo-error-text-color)");
        
        HorizontalLayout header = new HorizontalLayout(warningIcon, title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(false);
        header.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        
        // Message
        Div message = new Div();
        message.setText("Are you sure you want to delete the account \"" + accountName + "\"?");
        message.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        
        Div warningMessage = new Div();
        warningMessage.setText("This action cannot be undone. The account will be deactivated and hidden from future views, but existing transaction history will be preserved.");
        warningMessage.getStyle().set("font-size", "var(--lumo-font-size-s)");
        warningMessage.getStyle().set("color", "var(--lumo-secondary-text-color)");
        warningMessage.getStyle().set("margin-bottom", "var(--lumo-space-l)");
        
        // Buttons
        Button confirmButton = new Button("Delete Account");
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        confirmButton.addClickListener(e -> {
            onConfirm.run();
            dialog.close();
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> dialog.close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, confirmButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        
        // Layout
        VerticalLayout layout = new VerticalLayout(header, message, warningMessage, buttonLayout);
        layout.setPadding(true);
        layout.setSpacing(false);
        
        dialog.add(layout);
        
        // Focus the cancel button by default for safety
        cancelButton.focus();
        
        return dialog;
    }
    
    public static ConfirmationDialog createGenericConfirmationDialog(
            String title, 
            String message, 
            String confirmText, 
            String cancelText,
            Runnable onConfirm) {
        
        ConfirmationDialog dialog = new ConfirmationDialog();
        
        // Title
        H3 titleComponent = new H3(title);
        titleComponent.getStyle().set("margin", "0 0 var(--lumo-space-m) 0");
        
        // Message
        Div messageDiv = new Div();
        messageDiv.setText(message);
        messageDiv.getStyle().set("margin-bottom", "var(--lumo-space-l)");
        
        // Buttons
        Button confirmButton = new Button(confirmText);
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmButton.addClickListener(e -> {
            onConfirm.run();
            dialog.close();
        });
        
        Button cancelButton = new Button(cancelText);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> dialog.close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, confirmButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        
        // Layout
        VerticalLayout layout = new VerticalLayout(titleComponent, messageDiv, buttonLayout);
        layout.setPadding(true);
        layout.setSpacing(false);
        
        dialog.add(layout);
        
        return dialog;
    }
}