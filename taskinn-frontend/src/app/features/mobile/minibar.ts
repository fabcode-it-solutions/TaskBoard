export interface InventoryItemDto {
    id: number;
    name: string;
    category: string;
    unitOfMeasurement: string;
    unitPrice: number;
    currentStock: number;
    reorderStock: number;
    createdAt?: string;
    updatedAt?: string;
}

export interface InventoryItemRequestDto {
    name: string;
    category: string;
    unitOfMeasurement: string;
    unitPrice: number;
    currentStock: number;
    reorderStock: number;
}

export interface MinibarConsumptionItem {
    itemId: number;
    quantity: number;
}
