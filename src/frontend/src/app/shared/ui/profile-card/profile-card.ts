import { Component, Input, OnChanges, OnDestroy, SimpleChanges, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { MediaApi } from '../../../core/services/media-api';
import { getAgeFromDob, ageWord } from '../../utils/actorFormatter';

@Component({
  selector: 'app-profile-card',
  imports: [],
  templateUrl: './profile-card.html',
  styleUrl: './profile-card.scss',
})
export class ProfileCard implements OnChanges, OnDestroy {
  private readonly mediaApi = inject(MediaApi);

  @Input() fullName?: string;
  @Input() birthDate?: string;
  @Input() education?: string;
  @Input() actorId?: string;
  @Input() imageId?: string;

  private imageRequestSub?: Subscription;
  private currentObjectUrl: string | null = null;
  readonly imageSrc = signal<string | null>(null);

  private readonly defaults = {
    fullName: 'TEST TEST TEST',
    birthDay: '1970-01-01',
    education: 'TEST',
    imageSrc: '/images/profile.jpg',
  };

  get resolvedFullName(): string {
    return this.fullName?.trim() || this.defaults.fullName;
  }

  get resolvedAge(): string {
    const birthDate = this.birthDate?.trim() || this.defaults.birthDay;
    if (!birthDate) {
      return '-';
    }

    const age = getAgeFromDob(birthDate);
    const suffixOrAgeLabel = ageWord(birthDate);
    return /^\d+\s/.test(suffixOrAgeLabel) ? suffixOrAgeLabel : `${age} ${suffixOrAgeLabel}`;
  }

  get resolvedEducation(): string {
    return this.education?.trim() || this.defaults.education;
  }

  get resolvedImageAlt(): string {
    return this.resolvedFullName;
  }

  get resolvedImageSrc(): string {
    return this.imageSrc() ?? this.defaults.imageSrc;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['actorId'] || changes['imageId']) {
      this.loadImage();
    }
  }

  ngOnDestroy(): void {
    this.imageRequestSub?.unsubscribe();
    this.clearObjectUrl();
  }

  private loadImage(): void {
    this.imageRequestSub?.unsubscribe();
    this.clearObjectUrl();

    const actorId = this.actorId?.trim();
    const imageId = this.imageId?.trim();

    if (!actorId || !imageId) {
      return;
    }

    this.imageRequestSub = this.mediaApi.getActorMedia(actorId, imageId).subscribe({
      next: (imageBlob) => {
        const objectUrl = URL.createObjectURL(imageBlob);
        this.currentObjectUrl = objectUrl;
        this.imageSrc.set(objectUrl);
      },
      error: (error) => {
        console.error('[ProfileCard] Failed to load actor image:', error);
        this.imageSrc.set(null);
      },
    });
  }

  private clearObjectUrl(): void {
    if (this.currentObjectUrl) {
      URL.revokeObjectURL(this.currentObjectUrl);
      this.currentObjectUrl = null;
    }

    this.imageSrc.set(null);
  }
}
