import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { BookingStore } from '../booking.store';
import { BookingService, extractQueryParamsAndUpdateStore } from '../booking.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { CommonModule } from '@angular/common';
import { navigateWithQueryParams } from '../booking.service';
import { RoomTypeOption } from '../interfaces/room';
import { AuthStore } from '../../auth/auth.store';
import { LightRoomDto } from '../../room/room';
import { RoomService } from '../../room/room.service';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'tinn-booking-room-options-page',
    imports: [MatIcon, PageHeaderComponent, CommonModule],
    templateUrl: './booking-room-options-page.component.html',
})
export class BookingRoomOptionsPageComponent implements OnInit {
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly bookingService = inject(BookingService);
    private readonly roomService = inject(RoomService);
    private readonly bookingStore = inject(BookingStore);
    private readonly authStore = inject(AuthStore);
    private readonly toastr = inject(ToastrService);

    public readonly Number = Number;
    public readonly loading = this.bookingStore.loading;
    public readonly roomOptions = this.bookingStore.roomOptions;
    public readonly error = this.bookingStore.error;
    public readonly request = this.bookingStore.request;
    public readonly selectedRoomTypes = this.bookingStore.selectedRoomTypes;
    public readonly selectedRoomIds = this.bookingStore.selectedRoomIds; // For manual booking
    public readonly earlyCheckIn = this.bookingStore.earlyCheckIn;
    public readonly lateCheckOut = this.bookingStore.lateCheckOut;

    // For manual booking (staff only)
    public readonly availableRooms = signal<LightRoomDto[]>([]);

    // Search and filter properties
    public searchText = signal<string>('');
    public floorFilter = signal<number | null>(null);
    public capacityFilter = signal<number | null>(null);

    public readonly isStaff = computed(() => {
        const userProfile = this.authStore.userProfile();
        return userProfile?.roles.includes('EMPLOYEE') || userProfile?.roles.includes('ADMIN') || false;
    });

    public readonly sortedRoomOptions = computed(() => {
        const options = this.roomOptions();

        return [...options].sort((a, b) => {
            const aRecommended = a.recommendedCount > 0 ? 1 : 0;
            const bRecommended = b.recommendedCount > 0 ? 1 : 0;
            if (aRecommended !== bRecommended) {
                return bRecommended - aRecommended;
            }

            const typePriceCompare = this.compareNullablePrices(a.roomType.basePrice, b.roomType.basePrice);
            if (typePriceCompare !== 0) {
                return typePriceCompare;
            }

            const nameCompare = a.roomType.name.localeCompare(b.roomType.name, undefined, {
                sensitivity: 'base',
            });
            if (nameCompare !== 0) {
                return nameCompare;
            }

            const priceCompare = this.compareNullablePrices(a.pricePerNight, b.pricePerNight);
            if (priceCompare !== 0) {
                return priceCompare;
            }

            if (a.roomType.id !== b.roomType.id) {
                return a.roomType.id - b.roomType.id;
            }

            return 0;
        });
    });

    public readonly filteredRooms = computed((): LightRoomDto[] => {
        let rooms = this.availableRooms();
        const search = this.searchText().toLowerCase().trim();
        const floor = this.floorFilter();
        const capacity = this.capacityFilter();

        if (search) {
            rooms = rooms.filter(
                (room) =>
                    room.roomNumber.toString().includes(search) || room.roomType.name.toLowerCase().includes(search),
            );
        }

        if (floor !== null) {
            rooms = rooms.filter((room) => room.floor === floor);
        }

        if (capacity !== null) {
            rooms = rooms.filter((room) => room.roomType.capacity >= capacity);
        }

        return rooms;
    });

    public readonly availableFloors = computed((): number[] => {
        const floors = new Set(this.availableRooms().map((room) => room.floor));
        return Array.from(floors).sort((a, b) => a - b);
    });

    public readonly availableCapacities = computed((): number[] => {
        const capacities = new Set(this.availableRooms().map((room) => room.roomType.capacity));
        return Array.from(capacities).sort((a, b) => a - b);
    });

    public readonly totalGuestsRequired = computed((): number => {
        const req = this.request();
        return Number(req.numAdults) + Number(req.numChildren);
    });

    public readonly currentSelectionSums = computed((): { numRooms: number; capacity: number } => {
        const selected = this.selectedRoomTypes();
        const options = this.roomOptions();

        let totalRooms = 0;
        let totalCapacity = 0;

        selected.forEach((selection) => {
            const roomOption = options.find(
                (opt) =>
                    opt.roomType.id === selection.roomTypeId &&
                    this.pricesMatch(opt.pricePerNight, selection.pricePerNight),
            );
            if (roomOption) {
                totalRooms += selection.quantity;
                totalCapacity += roomOption.roomType.capacity * selection.quantity;
            }
        });

        return {
            numRooms: totalRooms,
            capacity: totalCapacity,
        };
    });

    public readonly manualBookingSelectionSums = computed((): { numRooms: number; capacity: number } => {
        const selectedIds = this.selectedRoomIds();
        const selectedRooms = this.availableRooms().filter((room) => selectedIds.includes(room.id));

        return {
            numRooms: selectedRooms.length,
            capacity: selectedRooms.reduce((sum, room) => sum + room.roomType.capacity, 0),
        };
    });

    public readonly canProceedToCheckout = computed((): boolean => {
        const guestsRequired = this.totalGuestsRequired();

        if (this.isStaff()) {
            // Manual booking: need at least one room selected AND capacity to meet guest count AND rooms <= guests
            const selectedIds = this.selectedRoomIds();
            if (selectedIds.length === 0) {
                return false;
            }

            const selectedRooms = this.availableRooms().filter((room) => selectedIds.includes(room.id));
            const totalCapacity = selectedRooms.reduce((sum, room) => sum + room.roomType.capacity, 0);
            const numRoomsSelected = selectedRooms.length;

            return totalCapacity >= guestsRequired && numRoomsSelected <= guestsRequired;
        } else {
            // Customer booking: need rooms selected and capacity to meet guest count AND rooms <= guests
            const selected = this.selectedRoomTypes();
            const sums = this.currentSelectionSums();
            return selected.length > 0 && sums.capacity >= guestsRequired && sums.numRooms <= guestsRequired;
        }
    });

    public async ngOnInit(): Promise<void> {
        extractQueryParamsAndUpdateStore(this.route, this.bookingStore);
        if (!this.bookingStore.isRequestValid()) {
            this.bookingStore.resetRequest();
            navigateWithQueryParams('/booking/date', this.router, this.bookingStore);
        } else {
            if (this.isStaff()) {
                await this.fetchAvailableRooms();
            } else {
                await this.fetchRoomOptions();
            }
        }
    }

    public incrementRoomTypeQuantity(roomTypeOption: RoomTypeOption): void {
        const currentQuantity = this.getRoomTypeQuantity(roomTypeOption);

        // Prevent adding more than available
        if (currentQuantity >= roomTypeOption.availableCount) {
            return;
        }

        const currentSelected = this.selectedRoomTypes();
        const existingIndex = currentSelected.findIndex(
            (s) =>
                s.roomTypeId === roomTypeOption.roomType.id &&
                this.pricesMatch(s.pricePerNight, roomTypeOption.pricePerNight),
        );

        if (existingIndex !== -1) {
            const updated = [...currentSelected];
            updated[existingIndex] = {
                ...updated[existingIndex],
                quantity: updated[existingIndex].quantity + 1,
            };
            this.bookingStore.setSelectedRoomTypes(updated);
        } else {
            this.bookingStore.setSelectedRoomTypes([
                ...currentSelected,
                {
                    roomTypeId: roomTypeOption.roomType.id,
                    pricePerNight: roomTypeOption.pricePerNight,
                    quantity: 1,
                },
            ]);
        }
    }

    public decrementRoomTypeQuantity(roomTypeOption: RoomTypeOption): void {
        const currentSelected = this.selectedRoomTypes();
        const existingIndex = currentSelected.findIndex(
            (s) =>
                s.roomTypeId === roomTypeOption.roomType.id &&
                this.pricesMatch(s.pricePerNight, roomTypeOption.pricePerNight),
        );

        if (existingIndex !== -1) {
            const updated = [...currentSelected];
            if (updated[existingIndex].quantity > 1) {
                updated[existingIndex] = {
                    ...updated[existingIndex],
                    quantity: updated[existingIndex].quantity - 1,
                };
                this.bookingStore.setSelectedRoomTypes(updated);
            } else {
                updated.splice(existingIndex, 1);
                this.bookingStore.setSelectedRoomTypes(updated);
            }
        }
    }

    public getRoomTypeQuantity(roomTypeOption: RoomTypeOption): number {
        const selection = this.selectedRoomTypes().find(
            (s) =>
                s.roomTypeId === roomTypeOption.roomType.id &&
                this.pricesMatch(s.pricePerNight, roomTypeOption.pricePerNight),
        );
        return selection ? selection.quantity : 0;
    }

    public canAddMoreRooms(roomTypeOption: RoomTypeOption): boolean {
        const currentQuantity = this.getRoomTypeQuantity(roomTypeOption);
        return currentQuantity < roomTypeOption.availableCount;
    }

    public getSelectedRoomsSummary(): Array<{
        roomType: RoomTypeOption['roomType'];
        pricePerNight: number;
        quantity: number;
        totalPrice: number;
    }> {
        const selected = this.selectedRoomTypes();
        const options = this.roomOptions();

        return selected
            .map((selection) => {
                const roomOption = options.find(
                    (opt) =>
                        opt.roomType.id === selection.roomTypeId &&
                        this.pricesMatch(opt.pricePerNight, selection.pricePerNight),
                );
                if (roomOption) {
                    const priceCents = this.priceToCents(roomOption.pricePerNight);
                    return {
                        roomType: roomOption.roomType,
                        quantity: selection.quantity,
                        pricePerNight: roomOption.pricePerNight,
                        totalPrice: priceCents === null ? 0 : (priceCents * selection.quantity) / 100,
                    };
                }
                return null;
            })
            .filter((item) => item !== null) as Array<{
            roomType: RoomTypeOption['roomType'];
            pricePerNight: number;
            quantity: number;
            totalPrice: number;
        }>;
    }

    public roomOptionKey(roomTypeOption: RoomTypeOption): string {
        return this.roomTypePriceKey(roomTypeOption.roomType.id, roomTypeOption.pricePerNight);
    }

    public roomTypePriceKey(roomTypeId: number, pricePerNight: number | null | undefined): string {
        const priceKey = this.priceToCents(pricePerNight);
        return `${roomTypeId}-${priceKey ?? 'na'}`;
    }

    private priceToCents(price: number | null | undefined): number | null {
        if (price === null || price === undefined || Number.isNaN(price)) {
            return null;
        }
        const cents = price * 100;
        return cents >= 0 ? Math.ceil(cents) : Math.floor(cents);
    }

    private compareNullablePrices(a: number | null | undefined, b: number | null | undefined): number {
        const aCents = this.priceToCents(a);
        const bCents = this.priceToCents(b);
        if (aCents === null && bCents === null) {
            return 0;
        }
        if (aCents === null) {
            return 1;
        }
        if (bCents === null) {
            return -1;
        }
        return aCents - bCents;
    }

    private pricesMatch(a: number | null | undefined, b: number | null | undefined): boolean {
        const aCents = this.priceToCents(a);
        const bCents = this.priceToCents(b);
        if (aCents === null && bCents === null) {
            return true;
        }
        return aCents !== null && bCents !== null && aCents === bCents;
    }

    public goBack(): void {
        navigateWithQueryParams('/booking/guests', this.router, this.bookingStore);
    }

    public goForwards(): void {
        navigateWithQueryParams('/booking/checkout', this.router, this.bookingStore);
    }

    public cancelBooking(): void {
        this.bookingStore.resetAll();
        this.router.navigate(['/staff/dashboard']);
    }

    private async fetchRoomOptions(): Promise<void> {
        const request = this.request();
        await this.bookingService.fetchRoomOptions(request);
    }

    private async fetchAvailableRooms(): Promise<void> {
        const request = this.request();
        const rooms = await this.roomService.getAvailableRooms(request.checkInDate!, request.checkOutDate!);
        this.availableRooms.set(rooms);

        if (this.roomService.error()) {
            const error = this.roomService.error();
            const errorMessage = error?.validationErrors?.[0]?.message || error?.message || 'An error occurred';
            this.toastr.error(errorMessage, 'Failed to load available rooms');
        }
    }

    // Manual booking methods
    public toggleRoomSelection(roomId: number): void {
        const currentSelection = this.selectedRoomIds();
        if (currentSelection.includes(roomId)) {
            this.bookingStore.setSelectedRoomIds(currentSelection.filter((id) => id !== roomId));
        } else {
            this.bookingStore.setSelectedRoomIds([...currentSelection, roomId]);
        }
    }

    public isRoomSelected(roomId: number): boolean {
        return this.selectedRoomIds().includes(roomId);
    }

    public getSelectedRoomsCount(): number {
        return this.selectedRoomIds().length;
    }

    public getSelectedRoomsDetails(): LightRoomDto[] {
        const selectedIds = this.selectedRoomIds();
        return this.availableRooms().filter((room) => selectedIds.includes(room.id));
    }

    public resetFilters(): void {
        this.searchText.set('');
        this.floorFilter.set(null);
        this.capacityFilter.set(null);
    }

    public toggleEarlyCheckIn(): void {
        this.bookingStore.setEarlyCheckIn(!this.earlyCheckIn());
    }

    public toggleLateCheckOut(): void {
        this.bookingStore.setLateCheckOut(!this.lateCheckOut());
    }
}
