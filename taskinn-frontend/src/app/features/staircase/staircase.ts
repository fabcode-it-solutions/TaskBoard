export interface StaircaseDto {
    id: number;
    floor: number;
    x: number;
    y: number;
    name: string | null;
    active: boolean;
}

export interface StaircaseRequestDto {
    id?: number;
    floor: number;
    x: number;
    y: number;
    name?: string | null;
    active: boolean;
}
