package tuwien.taskinn.datagenerator;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tuwien.taskinn.entity.InventoryItem;
import tuwien.taskinn.repo.InventoryItemRepository;

@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class InventoryItemDataGenerator {

    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Generates a predefined set of minibar inventory items if none exist in the database.
     *
     * @return A list of all InventoryItem entities in the database (either pre-existing or newly created).
     */
    public List<InventoryItem> generateData() {
        if (inventoryItemRepository.count() > 0) {
            log.info("Inventory items already exist. Skipping generation.");
            return inventoryItemRepository.findAll();
        }

        log.info("Generating minibar inventory item data...");

        List<InventoryItem> itemsToCreate = List.of(
                // Beverages - Soft Drinks
                createItem("Coca Cola", "Beverages", "bottle", "5.00", 200, 50),
                createItem("Coca Cola Zero", "Beverages", "bottle", "5.00", 150, 40),
                createItem("Sprite", "Beverages", "bottle", "5.00", 150, 40),
                createItem("Schweppes Tonic Water", "Beverages", "bottle", "4.50", 100, 25),

                // Beverages - Water & Juice
                createItem("Evian Water 500ml", "Beverages", "bottle", "4.00", 300, 75),
                createItem("San Pellegrino Sparkling", "Beverages", "bottle", "5.50", 200, 50),

                // Beverages - Energy Drinks
                createItem("Red Bull", "Beverages", "can", "6.50", 150, 40),

                // Beverages - Alcohol
                createItem("Corona Beer", "Alcohol", "bottle", "7.00", 80, 20),
                createItem("Heineken Beer", "Alcohol", "bottle", "7.00", 80, 20),
                createItem("Mini Chardonnay", "Alcohol", "bottle", "14.00", 40, 10),
                createItem("Absolut Vodka 50ml", "Alcohol", "bottle", "10.00", 50, 15),
                createItem("Jack Daniels 50ml", "Alcohol", "bottle", "12.00", 50, 15),

                // Snacks - Chips & Crisps
                createItem("Pringles Original", "Snacks", "can", "5.50", 100, 25),
                createItem("Doritos Nacho Cheese", "Snacks", "bag", "5.00", 80, 20),

                // Snacks - Nuts & Trail Mix
                createItem("Mixed Nuts Premium", "Snacks", "pack", "7.00", 100, 25),
                createItem("Salted Cashews", "Snacks", "pack", "8.00", 80, 20),

                // Snacks - Chocolate & Sweets
                createItem("Toblerone", "Snacks", "bar", "6.00", 80, 20),
                createItem("Lindt Excellence Dark", "Snacks", "bar", "7.00", 60, 15),
                createItem("M&Ms Peanut", "Snacks", "pack", "5.00", 100, 25),

                // Snacks - Cookies & Biscuits
                createItem("Oreo Cookies", "Snacks", "pack", "4.50", 80, 20));
        List<InventoryItem> savedItems = inventoryItemRepository.saveAll(itemsToCreate);

        log.info("Inventory item data generation complete. Created {} new items.", savedItems.size());

        return savedItems;
    }

    private InventoryItem createItem(String name, String category, String unit, String price, int stock,
            int reorderStock) {
        return InventoryItem.builder()
                .name(name)
                .category(category)
                .unitOfMeasurement(unit)
                .unitPrice(new BigDecimal(price))
                .currentStock(stock)
                .deleted(false)
                .reorderStock(reorderStock)
                .build();
    }
}
