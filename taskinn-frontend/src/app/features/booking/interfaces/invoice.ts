export interface InvoiceLineItem {
    description: string;
    quantity: number;
    unitPrice: number;
    total: number;
    category: string;
}

export interface InvoicePreview {
    bookingId: number;
    guestName: string;
    items: InvoiceLineItem[];
    subTotal?: number;
    totalAmount: number;
    alreadyPaid: number;
    remainingBalance: number;
}
