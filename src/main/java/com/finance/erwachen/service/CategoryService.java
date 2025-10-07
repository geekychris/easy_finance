package com.finance.erwachen.service;

import com.finance.erwachen.entity.Category;
import com.finance.erwachen.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAllActive() {
        return categoryRepository.findByActiveTrueOrderByName();
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    public Optional<Category> findByName(String name) {
        return categoryRepository.findByName(name);
    }

    public List<Category> findRootCategories() {
        return categoryRepository.findByParentIsNullAndActiveTrue();
    }

    public List<Category> findRootCategoriesByType(Category.CategoryType type) {
        return categoryRepository.findRootCategoriesByType(type);
    }

    public List<Category> findSubCategories(Category parent) {
        return categoryRepository.findByParentAndActiveTrue(parent);
    }

    public List<Category> searchByName(String name) {
        return categoryRepository.findByNameContainingIgnoreCase(name);
    }

    public Category save(Category category) {
        logger.info("Saving category: {}", category.getName());
        return categoryRepository.save(category);
    }

    public void delete(Category category) {
        logger.info("Deactivating category: {}", category.getName());
        category.setActive(false);
        categoryRepository.save(category);
    }

    /**
     * Create default categories for a new installation
     */
    public void createDefaultCategories() {
        logger.info("Creating default categories");
        
        if (categoryRepository.count() > 0) {
            logger.info("Categories already exist, skipping default creation");
            return;
        }

        // Income categories
        Category salary = new Category("Salary", Category.CategoryType.INCOME);
        salary.setDescription("Primary employment income");
        categoryRepository.save(salary);

        Category investments = new Category("Investment Income", Category.CategoryType.INCOME);
        investments.setDescription("Dividends, interest, and capital gains");
        categoryRepository.save(investments);

        Category freelance = new Category("Freelance", Category.CategoryType.INCOME);
        freelance.setDescription("Freelance and contract work");
        categoryRepository.save(freelance);

        // Expense categories
        Category housing = new Category("Housing", Category.CategoryType.EXPENSE);
        housing.setDescription("Rent, mortgage, and home-related expenses");
        Category savedHousing = categoryRepository.save(housing);

        Category utilities = new Category("Utilities", Category.CategoryType.EXPENSE, savedHousing);
        utilities.setDescription("Electricity, gas, water, internet");
        categoryRepository.save(utilities);

        Category food = new Category("Food & Dining", Category.CategoryType.EXPENSE);
        food.setDescription("Groceries and restaurant expenses");
        Category savedFood = categoryRepository.save(food);

        Category groceries = new Category("Groceries", Category.CategoryType.EXPENSE, savedFood);
        groceries.setDescription("Grocery store purchases");
        categoryRepository.save(groceries);

        Category dining = new Category("Dining Out", Category.CategoryType.EXPENSE, savedFood);
        dining.setDescription("Restaurants and takeout");
        categoryRepository.save(dining);

        Category transportation = new Category("Transportation", Category.CategoryType.EXPENSE);
        transportation.setDescription("Vehicle and travel expenses");
        Category savedTransportation = categoryRepository.save(transportation);

        Category gas = new Category("Gas & Fuel", Category.CategoryType.EXPENSE, savedTransportation);
        gas.setDescription("Vehicle fuel expenses");
        categoryRepository.save(gas);

        Category maintenance = new Category("Vehicle Maintenance", Category.CategoryType.EXPENSE, savedTransportation);
        maintenance.setDescription("Car repairs and maintenance");
        categoryRepository.save(maintenance);

        Category entertainment = new Category("Entertainment", Category.CategoryType.EXPENSE);
        entertainment.setDescription("Entertainment and leisure activities");
        Category savedEntertainment = categoryRepository.save(entertainment);

        Category streaming = new Category("Streaming Services", Category.CategoryType.EXPENSE, savedEntertainment);
        streaming.setDescription("Netflix, Spotify, etc.");
        categoryRepository.save(streaming);

        Category healthcare = new Category("Healthcare", Category.CategoryType.EXPENSE);
        healthcare.setDescription("Medical and health-related expenses");
        categoryRepository.save(healthcare);

        Category shopping = new Category("Shopping", Category.CategoryType.EXPENSE);
        shopping.setDescription("General shopping and purchases");
        categoryRepository.save(shopping);

        Category education = new Category("Education", Category.CategoryType.EXPENSE);
        education.setDescription("Educational expenses and courses");
        categoryRepository.save(education);

        Category insurance = new Category("Insurance", Category.CategoryType.EXPENSE);
        insurance.setDescription("All types of insurance premiums");
        categoryRepository.save(insurance);

        // Transfer category
        Category transfer = new Category("Transfer", Category.CategoryType.TRANSFER);
        transfer.setDescription("Transfers between accounts");
        categoryRepository.save(transfer);

        logger.info("Created default categories successfully");
    }

    /**
     * Get category hierarchy as a tree structure
     */
    public List<Category> getCategoryTree() {
        return findRootCategories();
    }

    /**
     * Validate category hierarchy to prevent circular references
     */
    public boolean isValidParentChild(Category parent, Category child) {
        if (parent == null || child == null) {
            return true;
        }
        
        if (parent.equals(child)) {
            return false; // Cannot be parent of itself
        }
        
        // Check if parent is already a descendant of child
        return !isDescendant(child, parent);
    }

    private boolean isDescendant(Category ancestor, Category descendant) {
        if (ancestor == null || descendant == null) {
            return false;
        }
        
        Category current = descendant.getParent();
        while (current != null) {
            if (current.equals(ancestor)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Get all descendant categories of a parent category
     */
    public List<Category> getDescendants(Category parent) {
        List<Category> descendants = findSubCategories(parent);
        
        // Recursively add sub-categories
        for (Category subCategory : List.copyOf(descendants)) {
            descendants.addAll(getDescendants(subCategory));
        }
        
        return descendants;
    }
}