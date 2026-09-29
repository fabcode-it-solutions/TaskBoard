export interface NavigationSequence {
    id: number;
    name: string;
    roomNumbers: string[];
    createdAt: string;
}

export interface NavigationStep {
    fromRoomNumber: string;
    toRoomNumber: string;
    fromFloor: number;
    toFloor: number;
    isCrossFloor: boolean;
    stepIndex: number;
}
