export interface QrTokenResponse {
    token: string;
    sessionId: string;
    expiresInSeconds: number;
}

export interface QrTokenExchangeRequest {
    token: string;
}

export interface QrLoginConfirmRequest {
    sessionId: string;
    confirmed: boolean;
}

export interface QrLoginAttempt {
    sessionId: string;
    message: string;
}
