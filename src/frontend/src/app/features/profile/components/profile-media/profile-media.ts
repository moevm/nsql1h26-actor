import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  WritableSignal,
  inject,
  signal,
} from '@angular/core';
import { Subscription, catchError, forkJoin, map, of } from 'rxjs';
import { MediaApi } from '../../../../core/services/media-api';
import { components } from '../../../../shared/api/types';
import { MediaCarousel, MediaCarouselItem } from '../../../../shared/ui/media-carousel/media-carousel';

type PhotoItem = components['schemas']['PhotoItem'];
type VideoItem = components['schemas']['VideoItem'];
type MediaSourceItem = PhotoItem | VideoItem;
type LoadedMediaBlob = { id: string; blob: Blob; caption: string | null };

@Component({
  selector: 'app-profile-media',
  imports: [MediaCarousel],
  templateUrl: './profile-media.html',
  styleUrl: './profile-media.scss',
})
export class ProfileMedia implements OnChanges, OnDestroy {
  @Input() actorId: string | undefined = undefined;
  @Input() photos: PhotoItem[] | undefined = undefined;
  @Input() videos: VideoItem[] | undefined = undefined;

  private readonly mediaApi = inject(MediaApi);
  private photoRequestSub?: Subscription;
  private videoRequestSub?: Subscription;
  private photoObjectUrls: string[] = [];
  private videoObjectUrls: string[] = [];

  readonly photoItems = signal<MediaCarouselItem[]>([]);
  readonly videoItems = signal<MediaCarouselItem[]>([]);
  readonly photosLoading = signal(false);
  readonly videosLoading = signal(false);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['actorId'] || changes['photos']) {
      this.loadAllPhotos();
    }

    if (changes['actorId'] || changes['videos']) {
      this.loadAllVideos();
    }
  }

  ngOnDestroy(): void {
    this.photoRequestSub?.unsubscribe();
    this.videoRequestSub?.unsubscribe();
    this.clearPhotoObjectUrls();
    this.clearVideoObjectUrls();
  }

  private loadAllPhotos(): void {
    this.photoRequestSub?.unsubscribe();
    this.clearPhotoObjectUrls();

    this.photoRequestSub = this.loadAllMediaType({
      sourceItems: this.photos,
      targetItems: this.photoItems,
      loadingSignal: this.photosLoading,
      mediaKind: 'photo',
      setObjectUrls: (urls) => {
        this.photoObjectUrls = urls;
      },
    });
  }

  private loadAllVideos(): void {
    this.videoRequestSub?.unsubscribe();
    this.clearVideoObjectUrls();

    this.videoRequestSub = this.loadAllMediaType({
      sourceItems: this.videos,
      targetItems: this.videoItems,
      loadingSignal: this.videosLoading,
      mediaKind: 'video',
      setObjectUrls: (urls) => {
        this.videoObjectUrls = urls;
      },
    });
  }

  private loadAllMediaType(args: {
    sourceItems: MediaSourceItem[] | undefined;
    targetItems: WritableSignal<MediaCarouselItem[]>;
    loadingSignal: WritableSignal<boolean>;
    mediaKind: 'photo' | 'video';
    setObjectUrls: (urls: string[]) => void;
  }): Subscription | undefined {
    const { sourceItems, targetItems, loadingSignal, mediaKind, setObjectUrls } = args;

    targetItems.set([]);
    setObjectUrls([]);

    const actorId = this.actorId;
    const mediaEntries = (sourceItems ?? [])
      .map((media) => ({ id: media.id, caption: media.caption ?? null }))
      .filter((media): media is { id: string; caption: string | null } => Boolean(media.id));

    if (!actorId || mediaEntries.length === 0) {
      loadingSignal.set(false);
      return undefined;
    }

    loadingSignal.set(true);

    const requests = mediaEntries.map((media) =>
      this.mediaApi.getActorMedia(actorId, media.id).pipe(
        map(
          (blob): LoadedMediaBlob => ({
            id: media.id,
            blob,
            caption: media.caption,
          }),
        ),
        catchError((error) => {
          console.error(`[ProfileMedia] Failed to load ${mediaKind}:`, {
            actorId,
            mediaId: media.id,
            error,
          });
          return of(null);
        }),
      ),
    );

    return forkJoin(requests).subscribe((results) => {
      const loadedItems = results
        .filter((item): item is LoadedMediaBlob => item !== null)
        .map((item): MediaCarouselItem => {
          const url = URL.createObjectURL(item.blob);
          return {
            id: item.id,
            url,
            caption: item.caption,
          };
        });

      setObjectUrls(loadedItems.map((item) => item.url));
      targetItems.set(loadedItems);
      loadingSignal.set(false);
    });
  }

  private clearPhotoObjectUrls(): void {
    this.revokeObjectUrls(this.photoObjectUrls);
    this.photoObjectUrls = [];
  }

  private clearVideoObjectUrls(): void {
    this.revokeObjectUrls(this.videoObjectUrls);
    this.videoObjectUrls = [];
  }

  private revokeObjectUrls(urls: string[]): void {
    urls.forEach((url) => URL.revokeObjectURL(url));
  }
}
