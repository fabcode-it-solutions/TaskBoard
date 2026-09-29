package tuwien.taskinn.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tuwien.taskinn.entity.InventoryItem;
import tuwien.taskinn.dto.response.InventoryItemSelectionResponseDto;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    /**
     * Find an inventory item by its name
     *
     * @param name the item name
     * @return an Optional containing the item if found
     */
    Optional<InventoryItem> findByName(String name);

    /**
     * Check if an inventory item exists with the given name
     *
     * @param name the item name
     * @return true if an item with the name exists
     */
    boolean existsByName(String name);

    /**
     * Find all non-deleted inventory items
     *
     * @return list of all non-deleted items
     */
    List<InventoryItem> findByDeletedFalse();

    /**
     * Find a non-deleted inventory item by ID
     *
     * @param id the item ID
     * @return an Optional containing the item if found and not deleted
     */
    Optional<InventoryItem> findByIdAndDeletedFalse(Long id);

    /**
     * Check if a non-deleted inventory item exists with the given name
     *
     * @param name the item name
     * @return true if a non-deleted item with the name exists
     */
    boolean existsByNameAndDeletedFalse(String name);

    /**
     * Find all non-deleted inventory items by IDs
     *
     * @param ids the item IDs
     * @return list of non-deleted items matching the IDs
     */
    List<InventoryItem> findByIdInAndDeletedFalse(Iterable<Long> ids);

    /**
     * Check if an active (non-deleted) inventory item exists with the given name, excluding a specific item ID
     *
     * @param name the item name
     * @param excludeId the item ID to exclude from the check (for updates)
     * @return true if another active item with the name exists
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM InventoryItem i WHERE i.name = :name AND i.deleted = false AND (:excludeId IS NULL OR i.id != :excludeId)")
    boolean existsActiveItemByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * Retrieves the first inventory item when sorted alphabetically by name.
     *
     * @return an Optional containing the first InventoryItem, or empty if no items exist.
     */
    Optional<InventoryItem> findTopByOrderByNameAsc();

    /**
     * Retrieves a lightweight projection of all inventory items, consisting only of IDs and names.
     *
     * @return a list of InventoryItemSelectionResponseDto containing the ID and name of every item, ordered by name.
     */
    @Query("SELECT new tuwien.taskinn.dto.response.InventoryItemSelectionResponseDto(i.id, i.name) "
            + "FROM InventoryItem i " + "ORDER BY i.name ASC")
    List<InventoryItemSelectionResponseDto> findAllIdsAndNames();
}
