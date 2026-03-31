import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';

export type NoticeBannerTone = 'success' | 'error';

export type SubmitNotification = {
  tone: NoticeBannerTone;
  message: string;
};

@Component({
  selector: 'app-notice-banner',
  imports: [],
  templateUrl: './notice-banner.html',
  styleUrl: './notice-banner.scss',
})
export class NoticeBanner implements OnChanges, OnDestroy {
  @Input({ required: true }) message!: string;
  @Input() tone: NoticeBannerTone = 'success';
  @Input() autoCloseMs: number | null = 3000;
  @Output() closeBanner = new EventEmitter<void>();

  private closeTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['message'] || changes['autoCloseMs']) {
      this.startAutoCloseTimer();
    }
  }

  ngOnDestroy(): void {
    this.clearAutoCloseTimer();
  }

  onClose(): void {
    this.clearAutoCloseTimer();
    this.closeBanner.emit();
  }

  private startAutoCloseTimer(): void {
    this.clearAutoCloseTimer();

    if (this.autoCloseMs === null || this.autoCloseMs <= 0) {
      return;
    }

    this.closeTimer = window.setTimeout(() => {
      this.closeTimer = null;
      this.closeBanner.emit();
    }, this.autoCloseMs);
  }

  private clearAutoCloseTimer(): void {
    if (this.closeTimer === null) {
      return;
    }

    clearTimeout(this.closeTimer);
    this.closeTimer = null;
  }
}
