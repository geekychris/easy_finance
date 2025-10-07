package com.finance.erwachen.repository;

import com.finance.erwachen.entity.Category;
import com.finance.erwachen.entity.Category.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrue();

    List<Category> findByActiveTrueOrderByName();

    List<Category> findByType(CategoryType type);

    List<Category> findByTypeAndActiveTrue(CategoryType type);

    Optional<Category> findByName(String name);

    List<Category> findByParentIsNull();

    List<Category> findByParentIsNullAndActiveTrue();

    List<Category> findByParent(Category parent);

    List<Category> findByParentAndActiveTrue(Category parent);

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.type = :type AND c.active = true ORDER BY c.name")
    List<Category> findRootCategoriesByType(@Param("type") CategoryType type);

    @Query("SELECT c FROM Category c WHERE c.name ILIKE %:name% AND c.active = true")
    List<Category> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT COUNT(c) FROM Category c WHERE c.parent = :parent AND c.active = true")
    Long countActiveSubCategories(@Param("parent") Category parent);
}