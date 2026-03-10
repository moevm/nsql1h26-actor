import { Component, Input, OnChanges, OnDestroy, SimpleChanges, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { MediaApi } from '../../../../core/services/media-api';
import { components } from '../../../../shared/api/types';

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
  private readonly fallbackImage = '/images/profilepic.jpg';
  private photoRequestSub?: Subscription;
  private currentObjectUrl: string | null = null;

  readonly firstPhotoUrl = signal<string | null>(null);

  capitalize(value: string | null | undefined): string {
    if (!value) return '-';

    const normalized = value.trim();
    if (!normalized) return '-';

    return normalized.charAt(0).toUpperCase() + normalized.slice(1);
  }

  getAgeFromDob(date: string): number {
    const birthDate = new Date(date);
    const today = new Date();

    let age = today.getFullYear() - birthDate.getFullYear();

    const monthDiff = today.getMonth() - birthDate.getMonth();

    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }

    return age;
  }

  ageWord(date: string): string {
    const age = this.getAgeFromDob(date);
    const lastDigit = age % 10;
    const lastTwo = age % 100;

    if (lastTwo >= 11 && lastTwo <= 14) return 'лет';
    if (lastDigit === 1) return 'год';
    if (lastDigit >= 2 && lastDigit <= 4) return 'года';
    return age + ' лет';
  }

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
