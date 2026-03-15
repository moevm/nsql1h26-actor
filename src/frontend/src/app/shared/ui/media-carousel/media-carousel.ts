import { Component, HostListener, Input, OnChanges, SimpleChanges } from '@angular/core';

export type MediaCarouselItem = {
  id: string;
  url: string;
  caption: string | null;
};

@Component({
  selector: 'app-media-carousel',
  imports: [],
  templateUrl: './media-carousel.html',
  styleUrl: './media-carousel.scss',
})
export class MediaCarousel implements OnChanges {
  @Input() title = '';
  @Input() items: MediaCarouselItem[] = [];
  @Input() mediaType: 'photo' | 'video' = 'photo';
  @Input() itemsPerPage = 4;
  @Input() itemWidth = 115;
  @Input() itemHeight = 190;

  currentPage = 0;
  zoomedItem: MediaCarouselItem | null = null;
  private readonly playingVideoIds = new Set<string>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['items'] || changes['itemsPerPage']) {
      const lastPage = this.getTotalPages() - 1;
      this.currentPage = Math.min(this.currentPage, Math.max(0, lastPage));
    }

    if (changes['items'] || changes['mediaType']) {
      this.playingVideoIds.clear();
      this.closeZoom();
    }
  }

  visibleItems(): MediaCarouselItem[] {
    const perPage = this.getItemsPerPage();
    const start = this.currentPage * perPage;
    return this.items.slice(start, start + perPage);
  }

  hasPrevPage(): boolean {
    return this.currentPage > 0;
  }

  hasNextPage(): boolean {
    return this.currentPage < this.getTotalPages() - 1;
  }

  goPrevPage(): void {
    if (this.hasPrevPage()) {
      this.currentPage -= 1;
    }
  }

  goNextPage(): void {
    if (this.hasNextPage()) {
      this.currentPage += 1;
    }
  }

  isVideoPlaying(itemId: string): boolean {
    return this.playingVideoIds.has(itemId);
  }

  startVideo(itemId: string, videoElement: HTMLVideoElement): void {
    this.playingVideoIds.add(itemId);
    void videoElement.play().catch((error) => {
      this.playingVideoIds.delete(itemId);
      console.error('[MediaCarousel] Failed to start video playback:', error);
    });
  }

  onVideoPlay(itemId: string): void {
    this.playingVideoIds.add(itemId);
  }

  onVideoPause(itemId: string): void {
    this.playingVideoIds.delete(itemId);
  }

  openZoom(item: MediaCarouselItem): void {
    this.zoomedItem = item;
  }

  closeZoom(): void {
    this.zoomedItem = null;
  }

  stopZoomClose(event: Event): void {
    event.stopPropagation();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.zoomedItem) {
      this.closeZoom();
    }
  }

  private getItemsPerPage(): number {
    return Math.max(1, this.itemsPerPage || 1);
  }

  private getTotalPages(): number {
    const perPage = this.getItemsPerPage();
    return Math.max(1, Math.ceil(this.items.length / perPage));
  }
}
