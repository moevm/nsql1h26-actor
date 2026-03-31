import { Component, Input, OnChanges, OnDestroy, SimpleChanges, signal } from '@angular/core';
import { InputFile } from '../../../../../../shared/ui/input-file/input-file';
import {
  MediaCarousel,
  MediaCarouselItem,
} from '../../../../../../shared/ui/media-carousel/media-carousel';
import { FormControl } from '@angular/forms';
import { Subscription } from 'rxjs';
import { EditableMedia, MediaTypes } from '../../profile-form.model';

type LocalMediaItem = MediaCarouselItem & {
  media: EditableMedia;
};

@Component({
  selector: 'app-profile-form-media',
  imports: [InputFile, MediaCarousel],
  templateUrl: './profile-form-media.html',
  styleUrl: './profile-form-media.scss',
})
export class ProfileFormMedia implements OnChanges, OnDestroy {
  @Input() photos!: FormControl<EditableMedia[]>;
  @Input() videos!: FormControl<EditableMedia[]>;

  readonly photoItems = signal<LocalMediaItem[]>([]);
  readonly videoItems = signal<LocalMediaItem[]>([]);
  readonly pendingPhotoFiles = signal<File[]>([]);
  readonly pendingVideoFiles = signal<File[]>([]);
  readonly photoInputResetVersion = signal(0);
  readonly videoInputResetVersion = signal(0);

  private photoControlSub?: Subscription;
  private videoControlSub?: Subscription;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['photos']) {
      this.bindPhotosControl();
    }

    if (changes['videos']) {
      this.bindVideosControl();
    }
  }

  addPhotos(files: FileList | null): void {
    if (!files) {
      return;
    }

    this.pendingPhotoFiles.update((prev) => [...prev, ...Array.from(files)]);
  }

  addVideos(files: FileList | null): void {
    if (!files) {
      return;
    }

    this.pendingVideoFiles.update((prev) => [...prev, ...Array.from(files)]);
  }

  removePhoto(id: string): void {
    if (!id) {
      return;
    }

    const next = (this.photos.value ?? []).filter((media) => this.getMediaId(media) !== id);
    this.updatePhotoControl(next);
  }

  removeVideo(id: string): void {
    if (!id) {
      return;
    }

    const next = (this.videos.value ?? []).filter((media) => this.getMediaId(media) !== id);
    this.updateVideoControl(next);
  }

  commitPhotos(): void {
    const queued = this.pendingPhotoFiles();
    if (queued.length === 0) {
      return;
    }

    const next = [...(this.photos.value ?? []), ...this.toNewMediaItems(queued, 'photo')];
    this.updatePhotoControl(next);
    this.pendingPhotoFiles.set([]);
    this.photoInputResetVersion.update((version) => version + 1);
  }

  commitVideos(): void {
    const queued = this.pendingVideoFiles();
    if (queued.length === 0) {
      return;
    }

    const next = [...(this.videos.value ?? []), ...this.toNewMediaItems(queued, 'video')];
    this.updateVideoControl(next);
    this.pendingVideoFiles.set([]);
    this.videoInputResetVersion.update((version) => version + 1);
  }

  ngOnDestroy(): void {
    this.photoControlSub?.unsubscribe();
    this.videoControlSub?.unsubscribe();
    this.photoItems().forEach((item) => this.revokeLocalUrl(item.media));
    this.videoItems().forEach((item) => this.revokeLocalUrl(item.media));
  }

  private bindPhotosControl(): void {
    this.photoControlSub?.unsubscribe();
    this.syncPhotoItems(this.photos.value ?? []);
    this.photoControlSub = this.photos.valueChanges.subscribe((items) => {
      this.syncPhotoItems(items ?? []);
    });
  }

  private bindVideosControl(): void {
    this.videoControlSub?.unsubscribe();
    this.syncVideoItems(this.videos.value ?? []);
    this.videoControlSub = this.videos.valueChanges.subscribe((items) => {
      this.syncVideoItems(items ?? []);
    });
  }

  private syncPhotoItems(mediaItems: EditableMedia[]): void {
    const nextItems = this.toCarouselItems(mediaItems);
    this.revokeRemovedLocalUrls(this.photoItems(), nextItems);
    this.photoItems.set(nextItems);
  }

  private syncVideoItems(mediaItems: EditableMedia[]): void {
    const nextItems = this.toCarouselItems(mediaItems);
    this.revokeRemovedLocalUrls(this.videoItems(), nextItems);
    this.videoItems.set(nextItems);
  }

  private revokeRemovedLocalUrls(prevItems: LocalMediaItem[], nextItems: LocalMediaItem[]): void {
    const nextIds = new Set(nextItems.map((item) => item.id));
    prevItems
      .filter((item) => !nextIds.has(item.id))
      .forEach((item) => this.revokeLocalUrl(item.media));
  }

  private toCarouselItems(mediaItems: EditableMedia[]): LocalMediaItem[] {
    return mediaItems.map((media) => ({
      id: this.getMediaId(media),
      url: media.url,
      caption: media.caption ?? (media.kind === 'new' ? media.file.name : null),
      media,
    }));
  }

  private toNewMediaItems(files: File[], type: MediaTypes): EditableMedia[] {
    return files.map((file) => {
      const tempId = `${file.name}-${file.lastModified}-${file.size}-${Math.random().toString(36).slice(2, 9)}`;
      return {
        kind: 'new',
        tempId,
        file,
        url: URL.createObjectURL(file),
        caption: file.name,
        type: type,
      };
    });
  }

  private getMediaId(media: EditableMedia): string {
    return media.kind === 'existing' ? media.id : media.tempId;
  }

  private updatePhotoControl(next: EditableMedia[]): void {
    this.photos.setValue(next);
    this.photos.markAsDirty();
  }

  private updateVideoControl(next: EditableMedia[]): void {
    this.videos.setValue(next);
    this.videos.markAsDirty();
  }

  private revokeLocalUrl(media: EditableMedia): void {
    if (media.kind !== 'new') {
      return;
    }

    URL.revokeObjectURL(media.url);
  }
}
