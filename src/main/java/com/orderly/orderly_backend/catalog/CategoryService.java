package com.orderly.orderly_backend.catalog;

import com.orderly.orderly_backend.auth.api.UserRegisteredEvent;
import com.orderly.orderly_backend.catalog.dto.req.CreateCategoryRequest;
import com.orderly.orderly_backend.catalog.dto.req.ReorderCategoriesRequest;
import com.orderly.orderly_backend.catalog.dto.req.UpdateCategoryRequest;
import com.orderly.orderly_backend.catalog.dto.resp.CategoryDefinitionDto;
import com.orderly.orderly_backend.catalog.dto.resp.CategoryListResponse;
import com.orderly.orderly_backend.catalog.dto.resp.FieldDefinitionDto;
import com.orderly.orderly_backend.exception.NotFoundException;
import com.orderly.orderly_backend.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
class CategoryService {

    private final CategoryDefinitionRepository categoryRepo;

    @Transactional(readOnly = true)
    CategoryListResponse listCategories(UUID userId) {
        return new CategoryListResponse(
                categoryRepo.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId)
                        .stream().map(this::toDto).toList()
        );
    }

    CategoryDefinitionDto createCategory(CreateCategoryRequest req, UUID userId) {
        int nextSortOrder = categoryRepo.findMaxSortOrderByUserId(userId) + 1;

        List<FieldDefinition> fields = req.fieldDefinitions().stream()
                .map(f -> new FieldDefinition(SlugUtils.toSlug(f.label()), f.label(), f.type(), f.options(), f.required()))
                .toList();

        CategoryDefinition saved = categoryRepo.save(CategoryDefinition.builder()
                .name(req.name())
                .icon(req.icon())
                .fieldDefinitions(fields)
                .sortOrder(nextSortOrder)
                .userId(userId)
                .build());

        return toDto(saved);
    }

    /** In place update */
    CategoryDefinitionDto updateCategory(UUID id, UpdateCategoryRequest req, UUID userId) {
        CategoryDefinition category = categoryRepo.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Category not found."));

        // Generate key for new fields. Keep existing for fields that already have a key
        List<FieldDefinition> fields = req.fieldDefinitions().stream()
                .map(f -> {
                    String key = (f.key() != null && !f.key().isBlank())
                            ? f.key()
                            : SlugUtils.toSlug(f.label());
                    return new FieldDefinition(key, f.label(), f.type(), f.options(), f.required());
                })
                .toList();

        category.setName(req.name());
        category.setIcon(req.icon());
        category.setFieldDefinitions(fields);

        return toDto(categoryRepo.save(category));
    }

    void deleteCategory(UUID id, UUID userId) {
        CategoryDefinition category = categoryRepo.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Category not found."));
        categoryRepo.delete(category);
    }

    CategoryListResponse reorderCategories(ReorderCategoriesRequest req, UUID userId) {
        List<CategoryDefinition> categories = categoryRepo.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId);

        // ** Both count mismatch AND foreign IDs → 400 (not 403/404), for enumeration prevention **
        if (req.orderedIds().size() != categories.size()) {
            throw new ValidationException("orderedIds must contain exactly the user's current categories.");
        }

        Set<UUID> userCategoryIds = categories.stream()
                .map(CategoryDefinition::getId).collect(Collectors.toSet());
        for (UUID orderedId : req.orderedIds()) {
            if (!userCategoryIds.contains(orderedId)) {
                throw new ValidationException("orderedIds contains an unknown or foreign category ID.");
            }
        }

        Map<UUID, CategoryDefinition> byId = categories.stream()
                .collect(Collectors.toMap(CategoryDefinition::getId, c -> c));

        List<UUID> orderedIds = req.orderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            byId.get(orderedIds.get(i)).setSortOrder(i);
        }

        categoryRepo.saveAll(categories);

        return new CategoryListResponse(
                categoryRepo.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId)
                        .stream().map(this::toDto).toList()
        );
    }

    /**
     * Seeds default categories for a new user on registration.
     * <p>
     * Transactional synchronous call. Used to seed default categories for users on {@code AuthService.register()}.
     * If seeding fails the entire registration transaction rolls back 
     */
    @EventListener
    void onUserRegistered(UserRegisteredEvent event) {
        categoryRepo.saveAll(buildDefaultCategories(event.userId()));
    }

    // --- Private helpers ---

    private CategoryDefinitionDto toDto(CategoryDefinition c) {
        List<FieldDefinitionDto> fields = c.getFieldDefinitions() == null ? List.of() :
                c.getFieldDefinitions().stream()
                        .map(f -> new FieldDefinitionDto(f.key(), f.label(), f.type(), f.options(), f.required()))
                        .toList();
        return new CategoryDefinitionDto(c.getId(), c.getName(), c.getIcon(), c.getSortOrder(), fields);
    }

    private List<CategoryDefinition> buildDefaultCategories(UUID userId) {
        return List.of(
                buildCategory("Movies", "Film", userId, 0, List.of(
                        new FieldDefinition("director", "Director", "string", null, null),
                        new FieldDefinition("year", "Year", "number", null, null),
                        new FieldDefinition("genre", "Genre", "select",
                                List.of("Action", "Comedy", "Drama", "Horror", "Sci-Fi", "Thriller", "Other"), null)
                )),
                buildCategory("Books", "BookOpen", userId, 1, List.of(
                        new FieldDefinition("author", "Author", "string", null, null),
                        new FieldDefinition("year", "Year", "number", null, null),
                        new FieldDefinition("genre", "Genre", "select",
                                List.of("Fiction", "Non-Fiction", "Sci-Fi", "Fantasy", "Biography", "Other"), null)
                )),
                buildCategory("Restaurants", "UtensilsCrossed", userId, 2, List.of(
                        new FieldDefinition("cuisine", "Cuisine", "string", null, null),
                        new FieldDefinition("price_range", "Price Range", "select",
                                List.of("$", "$$", "$$$", "$$$$"), null),
                        new FieldDefinition("location", "Location", "string", null, null)
                ))
        );
    }

    private CategoryDefinition buildCategory(String name, String icon, UUID userId, int sortOrder,
            List<FieldDefinition> fields) {
        return CategoryDefinition.builder()
                .name(name)
                .icon(icon)
                .userId(userId)
                .sortOrder(sortOrder)
                .fieldDefinitions(fields)
                .build();
    }
}
