export interface NumberSettingProps {
    kind: 'NUMBER';
    value: number;

    min?: number;
    max?: number;
    step?: number;
}

export interface BooleanSettingProps {
    kind: 'BOOLEAN';
    value: boolean;
}

export type SettingKindType = {
    NUMBER: number;
    BOOLEAN: boolean;
};

export type SettingProps = NumberSettingProps | BooleanSettingProps;
export type SettingValue = SettingKindType[keyof SettingKindType];

export interface NamedSetting<S extends string, N extends string> {
    namespace: S;
    name: N;
}

export interface PackedUserSetting {
    namespace: string;
    name: string;
    content: string;
}
