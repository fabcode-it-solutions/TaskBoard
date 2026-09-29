export interface MinibarStatisticsResponse {
    salesByEarnings: Record<string, number>;
    salesByQuantity: Record<string, number>;
    // Minibar item -> day -> sales (quantity)
    dailyItemSales: Record<number, Record<string, number>>;
}

export interface InventoryItemSelectionResponse {
    id: number;
    name: string;
}
