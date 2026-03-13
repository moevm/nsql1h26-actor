import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  inject,
  signal,
} from '@angular/core';
import { Subscription } from 'rxjs';
import { MediaApi } from '../../../../core/services/media-api';
import { components } from '../../../../shared/api/types';
import { capitalize, getAgeFromDob, ageWord } from '../../../../shared/utils/actorFormatter';
import { AuthSessionService } from '../../../../core/services/auth-session-service';

type Actor = components['schemas']['Actor'];

@Component({
  selector: 'app-profile-info',
  imports: [],
  templateUrl: './profile-info.html',
  styleUrl: './profile-info.scss',
})
export class ProfileInfo implements OnChanges, OnDestroy {
  @Input() actor: Actor | null = null;
  private readonly mediaApi = inject(MediaApi);
  readonly authSessionService = inject(AuthSessionService);
  private readonly fallbackImage = '/images/profile.jpg';
  private photoRequestSub?: Subscription;
  private currentObjectUrl: string | null = null;
  readonly capitalize = capitalize;

  readonly firstPhotoUrl = signal<string | null>(null);

  private readonly genderLabels: Record<NonNullable<Actor['gender']>, string> = {
    male: 'Мужчина',
    female: 'Женщина',
  };

  get genderLabel(): string {
    const gender = this.actor?.gender;
    return gender ? this.genderLabels[gender] : '-';
  }

  get fullEducationString(): string {
    let result = '';

    this.actor?.education?.forEach((item, index, arr) => {
      if (index !== arr.length - 1) {
        result += item.university?.name + ', ';
      }
      result += item.university?.name;
    });

    return result;
  }

  get profileImageSrc(): string {
    return this.firstPhotoUrl() ?? this.fallbackImage;
  }

  get ageLabel(): string {
    const birthDate = this.actor?.birthDate;
    if (!birthDate) {
      return '-';
    }

    const age = getAgeFromDob(birthDate);
    const suffixOrAgeLabel = ageWord(birthDate);
    return /^\d+\s/.test(suffixOrAgeLabel) ? suffixOrAgeLabel : `${age} ${suffixOrAgeLabel}`;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['actor']) {
      this.loadFirstPhoto();
    }
  }

  ngOnDestroy(): void {
    this.photoRequestSub?.unsubscribe();
    this.clearFirstPhotoUrl();
  }

  private loadFirstPhoto(): void {
    this.photoRequestSub?.unsubscribe();
    this.clearFirstPhotoUrl();

    const actorId = this.actor?.id;
    const firstPhotoId = this.actor?.photos?.[0]?.id;

    if (!actorId || !firstPhotoId) {
      return;
    }

    this.photoRequestSub = this.mediaApi.getActorMedia(actorId, firstPhotoId).subscribe({
      next: (photoBlob) => {
        const objectUrl = URL.createObjectURL(photoBlob);
        this.currentObjectUrl = objectUrl;
        this.firstPhotoUrl.set(objectUrl);
      },
      error: (error) => {
        console.error('[ProfileInfo] Failed to load first actor photo:', error);
        this.firstPhotoUrl.set(null);
      },
    });
  }

  private clearFirstPhotoUrl(): void {
    if (this.currentObjectUrl) {
      URL.revokeObjectURL(this.currentObjectUrl);
      this.currentObjectUrl = null;
    }

    this.firstPhotoUrl.set(null);
  }
}
