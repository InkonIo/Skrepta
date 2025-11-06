package com.skrepta.skreptajava.category.service;

import com.skrepta.skreptajava.auth.exception.ResourceNotFoundException;
import com.skrepta.skreptajava.category.dto.CategoryRequest;
import com.skrepta.skreptajava.category.dto.CategoryResponse;
import com.skrepta.skreptajava.category.entity.Category;
import com.skrepta.skreptajava.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Category with name " + request.getName() + " already exists.");
        }

        Category parent = getParentCategory(request.getParentId());

        Category category = Category.builder()
                .name(request.getName())
                .slug(generateSlug(request.getName()))
                .parent(parent)
                .icon(request.getIcon())
                .position(request.getPosition())
                .isActive(request.getIsActive())
                .build();

        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        if (!category.getName().equals(request.getName()) && categoryRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Category with name " + request.getName() + " already exists.");
        }

        Category parent = getParentCategory(request.getParentId());

        category.setName(request.getName());
        category.setSlug(generateSlug(request.getName()));
        category.setParent(parent);
        category.setIcon(request.getIcon());
        category.setPosition(request.getPosition());
        category.setIsActive(request.getIsActive());

        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        if (!category.getShops().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with associated shops.");
        }

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllRootCategories() {
        return categoryRepository.findAll().stream()
                .filter(c -> c.getParent() == null)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return mapToResponse(category);
    }

    private Category getParentCategory(Long parentId) {
        // Treat null or 0 as no parent (root category)
        if (parentId == null || parentId == 0) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + parentId));
    }

    private String generateSlug(String name) {
        String slug = StringUtils.trimAllWhitespace(name).toLowerCase(Locale.ROOT);
        // Simple slug generation. For production, consider a more robust library.
        return slug.replaceAll("[^a-z0-9\\-]", "");
    }

    private CategoryResponse mapToResponse(Category category) {
        List<CategoryResponse> children = List.of();
        
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            children = category.getChildren().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .icon(category.getIcon())
                .position(category.getPosition())
                .isActive(category.getIsActive())
                .children(children)
                .build();
    }
}