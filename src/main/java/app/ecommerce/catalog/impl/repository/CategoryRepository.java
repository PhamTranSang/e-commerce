package app.ecommerce.catalog.impl.repository;

import app.ecommerce.catalog.impl.entity.CategoryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    Optional<CategoryEntity> findByCategoryIdAndIsActiveTrue(final UUID categoryId);

    /** Whether the given category has at least one child (i.e. it is not a leaf). */
    @Query("SELECT COUNT(c) > 0 FROM CategoryEntity c WHERE c.parent.categoryId = :categoryId")
    boolean hasChildren(@Param("categoryId") final UUID categoryId);

    List<CategoryEntity> findByIsActiveTrue();

    Page<CategoryEntity> findAllByIsActiveTrue(final Pageable pageable);

    /**
     * Whether a category with the same name already exists under the given parent
     * ({@code parentId == null} means the root level), optionally excluding one category
     * (used on rename to skip the row being renamed).
     *
     * <p>Uses {@code LOWER(...)} to match the case-insensitive unique indexes
     * {@code uq_category_root_name} / {@code uq_category_child_name}.
     */
    @Query("""
        SELECT COUNT(c) > 0 FROM CategoryEntity c
        WHERE LOWER(c.categoryName) = LOWER(:categoryName)
          AND (
                (:parentId IS NULL AND c.parent IS NULL)
             OR (:parentId IS NOT NULL AND c.parent.categoryId = :parentId)
              )
          AND (:excludeCategoryId IS NULL OR c.categoryId <> :excludeCategoryId)
        """)
    boolean existsSiblingWithName(
        @Param("categoryName") final String categoryName,
        @Param("parentId") final UUID parentId,
        @Param("excludeCategoryId") final UUID excludeCategoryId
    );
}
