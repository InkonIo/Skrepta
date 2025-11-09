package com.skrepta.skreptajava.category.service;

import com.skrepta.skreptajava.auth.exception.ResourceNotFoundException;
import com.skrepta.skreptajava.category.dto.CategoryRequest;
import com.skrepta.skreptajava.category.dto.CategoryResponse;
import com.skrepta.skreptajava.category.dto.CategoryStatusRequest;
import com.skrepta.skreptajava.category.entity.Category;
import com.skrepta.skreptajava.category.repository.CategoryRepository;
import com.skrepta.skreptajava.shop.entity.Shop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryIconService categoryIconService;

    @Transactional
    public CategoryResponse uploadCategoryIcon(Long categoryId, MultipartFile file) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        // Удаляем старую иконку, если она есть
        if (category.getIcon() != null && !category.getIcon().isEmpty()) {
            try {
                categoryIconService.deleteCategoryIcon(category.getIcon());
            } catch (Exception e) {
                log.warn("Failed to delete old icon: {}", e.getMessage());
            }
        }

        // Загружаем новую иконку
        String iconUrl;
        try {
            iconUrl = categoryIconService.uploadCategoryIcon(file, categoryId);
        } catch (IOException e) {
            log.error("Failed to upload category icon: {}", e.getMessage());
            throw new RuntimeException("Failed to upload category icon", e);
        }

        // Обновляем URL иконки в базе
        category.setIcon(iconUrl);
        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Category with name " + request.getName() + " already exists.");
        }

        Category parent = getParentCategory(request.getParentId());
        
        // Генерируем slug
        String slug = generateSlug(request.getName());
        
        // Проверяем уникальность slug, если занят - добавляем timestamp
        if (categoryRepository.findBySlug(slug).isPresent()) {
            slug = slug + "-" + System.currentTimeMillis();
            log.warn("Slug collision detected, using: {}", slug);
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
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

        // Если есть привязанные магазины, очищаем связь, чтобы не нарушать целостность БД
        // Если есть привязанные магазины, мы должны удалить категорию из коллекции магазинов
        // Это необходимо, потому что Shop является владельцем связи (нет mappedBy)
        if (!category.getShops().isEmpty()) {
            log.warn("Category {} has {} associated shops. Removing category from shops' collections before deletion.", category.getId(), category.getShops().size());
            for (Shop shop : category.getShops()) {
                shop.getCategories().remove(category);
            }
            // Очистка коллекции в Category не нужна, так как Shop является владельцем
        }

        // Удаляем иконку из S3 перед удалением категории
        if (category.getIcon() != null && !category.getIcon().isEmpty()) {
            try {
                categoryIconService.deleteCategoryIcon(category.getIcon());
            } catch (Exception e) {
                log.warn("Failed to delete icon from S3: {}", e.getMessage());
            }
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

    @Transactional
    public CategoryResponse updateCategoryStatus(Long id, CategoryStatusRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        category.setIsActive(request.getIsActive());
        categoryRepository.save(category);

        return CategoryResponse.fromEntity(category);
    }

    private Category getParentCategory(Long parentId) {
        if (parentId == null || parentId == 0) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + parentId));
    }

    /**
     * ✅ ИСПРАВЛЕННАЯ ГЕНЕРАЦИЯ SLUG С ТРАНСЛИТЕРАЦИЕЙ
     * Теперь русские буквы преобразуются в латиницу, а не удаляются
     */
    private String generateSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        String translit = name
                .toLowerCase()
                .trim()
                // Русские буквы → латиница
                .replace("а", "a").replace("б", "b").replace("в", "v")
                .replace("г", "g").replace("д", "d").replace("е", "e")
                .replace("ё", "e").replace("ж", "zh").replace("з", "z")
                .replace("и", "i").replace("й", "y").replace("к", "k")
                .replace("л", "l").replace("м", "m").replace("н", "n")
                .replace("о", "o").replace("п", "p").replace("р", "r")
                .replace("с", "s").replace("т", "t").replace("у", "u")
                .replace("ф", "f").replace("х", "h").replace("ц", "ts")
                .replace("ч", "ch").replace("ш", "sh").replace("щ", "shch")
                .replace("ъ", "").replace("ы", "y").replace("ь", "")
                .replace("э", "e").replace("ю", "yu").replace("я", "ya")
                // Казахские буквы
                .replace("ә", "a").replace("ғ", "g").replace("қ", "k")
                .replace("ң", "n").replace("ө", "o").replace("ұ", "u")
                .replace("ү", "u").replace("һ", "h").replace("і", "i")
                // Украинские буквы
                .replace("є", "ye").replace("і", "i").replace("ї", "yi").replace("ґ", "g");

        // Все не-буквы/цифры → дефис
        translit = translit.replaceAll("[^a-z0-9]+", "-");
        
        // Убираем дефисы в начале и конце
        translit = translit.replaceAll("^-+|-+$", "");

        // Если после всех преобразований получилась пустая строка
        if (translit.isEmpty()) {
            // Генерируем slug на основе времени + случайное число
            translit = "category-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
            log.warn("Generated fallback slug for name '{}': {}", name, translit);
        }

        return translit;
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