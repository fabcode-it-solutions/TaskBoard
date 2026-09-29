import { Component, inject, OnDestroy, OnInit, signal, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import QRCode from 'qrcode';

import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../features/auth/auth.service';
import { QrLoginAttempt, QrTokenResponse } from '../../../features/auth/interfaces/qr-token';

type DialogState = 'loading' | 'qr-display' | 'confirming' | 'success' | 'error';

@Component({
    selector: 'tinn-qr-login-dialog',
    imports: [CommonModule, MatDialogModule, MatIcon],
    templateUrl: './qr-login-dialog.component.html',
})
export class QrLoginDialogComponent implements OnInit, OnDestroy {
    @ViewChild('qrCanvas', { static: false }) private qrCanvas!: ElementRef<HTMLCanvasElement>;

    private readonly dialogRef = inject(MatDialogRef<QrLoginDialogComponent>);
    private readonly authService = inject(AuthService);

    public readonly state = signal<DialogState>('loading');
    public readonly countdown = signal<number>(60);
    public readonly errorMessage = signal<string>('');

    private stompClient: Client | null = null;
    private countdownInterval: ReturnType<typeof setInterval> | null = null;
    private currentToken: QrTokenResponse | null = null;

    public async ngOnInit(): Promise<void> {
        await this.generateQrCode();
    }

    public ngOnDestroy(): void {
        this.cleanup();
    }

    public async regenerate(): Promise<void> {
        this.cleanup();
        await this.generateQrCode();
    }

    public async confirm(): Promise<void> {
        if (!this.currentToken) {
            return;
        }

        this.state.set('loading');

        try {
            await this.authService.confirmQrLogin(this.currentToken.sessionId, true);
            this.state.set('success');
            setTimeout(() => this.dialogRef.close(true), 1500);
        } catch {
            this.errorMessage.set('Failed to confirm login');
            this.state.set('error');
        }
    }

    public async deny(): Promise<void> {
        if (!this.currentToken) {
            return;
        }

        try {
            await this.authService.confirmQrLogin(this.currentToken.sessionId, false);
        } catch {
            // Ignore errors on deny
        }

        this.dialogRef.close(false);
    }

    public close(): void {
        this.dialogRef.close(false);
    }

    private async generateQrCode(): Promise<void> {
        this.state.set('loading');
        this.errorMessage.set('');

        const tokenResponse = await this.authService.generateQrToken();

        if (!tokenResponse) {
            this.errorMessage.set('Failed to generate QR code. Are you logged in as an employee?');
            this.state.set('error');
            return;
        }

        this.currentToken = tokenResponse;
        this.countdown.set(tokenResponse.expiresInSeconds);

        this.connectWebSocket(tokenResponse.sessionId);

        this.startCountdown();

        this.state.set('qr-display');

        setTimeout(() => {
            this.renderQrCode(tokenResponse.token);
        }, 100);
    }

    private renderQrCode(token: string): void {
        if (!this.qrCanvas?.nativeElement) {
            console.error('QR Canvas not available');
            return;
        }

        const qrUrl = `${window.location.origin}${environment.baseUrl}/mobile/qr-auth?token=${token}`;

        QRCode.toCanvas(
            this.qrCanvas.nativeElement,
            qrUrl,
            {
                width: 256,
                margin: 2,
                color: {
                    dark: '#000000',
                    light: '#FFFFFF',
                },
            },
            (error: Error | null | undefined) => {
                if (error) {
                    console.error('Failed to generate QR code:', error);
                    this.errorMessage.set('Failed to generate QR code');
                    this.state.set('error');
                }
            },
        );
    }

    private connectWebSocket(sessionId: string): void {
        const token = this.authService.getToken();
        if (!token) {
            console.error('No auth token available for WebSocket');
            return;
        }

        this.stompClient = new Client({
            webSocketFactory: (): WebSocket => new SockJS(`${environment.wsUrl}/ws`),
            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },
            reconnectDelay: 5000,
            onConnect: (): void => {
                this.stompClient?.subscribe(`/topic/qr-auth/${sessionId}`, (message: IMessage) => {
                    try {
                        const attempt: QrLoginAttempt = JSON.parse(message.body);
                        if (attempt.sessionId === sessionId) {
                            this.state.set('confirming');
                        }
                    } catch (error) {
                        console.error('Error parsing QR auth message:', error);
                    }
                });
            },
            onStompError: (frame): void => {
                console.error('WebSocket error:', frame.headers['message']);
            },
        });

        this.stompClient.activate();
    }

    private startCountdown(): void {
        this.countdownInterval = setInterval(() => {
            const current = this.countdown();
            if (current <= 1) {
                this.cleanup();
                this.errorMessage.set('QR code expired. Please regenerate.');
                this.state.set('error');
            } else {
                this.countdown.set(current - 1);
            }
        }, 1000);
    }

    private cleanup(): void {
        if (this.countdownInterval) {
            clearInterval(this.countdownInterval);
            this.countdownInterval = null;
        }

        if (this.stompClient?.active) {
            this.stompClient.deactivate();
            this.stompClient = null;
        }

        this.currentToken = null;
    }
}
