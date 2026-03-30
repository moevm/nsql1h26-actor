import { Component, inject, DestroyRef, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ProfileCard } from '../../../shared/ui/profile-card/profile-card';
import { components } from '../../../shared/api/types';
import { ActorsApi } from '../../../core/services/actors-api';
import { fullName } from '../../../shared/utils/actorFormatter';
import { RouterLink } from '@angular/router';

type Actor = components['schemas']['Actor'];

@Component({
  standalone: true,
  selector: 'app-landing-page',
  imports: [CommonModule, ProfileCard, RouterLink],
  templateUrl: './landing-page.html',
  styleUrls: ['./landing-page.scss'],
})
export class LandingPage {
  private readonly destroyRef = inject(DestroyRef);
  private readonly actorsApi = inject(ActorsApi);
  readonly actors = signal<Actor[]>([]);
  readonly actorsLoading = signal(true);
  readonly actorsError = signal<string | null>(null);

  readonly fullName = fullName;

  constructor() {
    this.actorsApi
      .getActorbyLimit(8)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.actors.set(response.actors);
          this.actorsError.set(null);
          this.actorsLoading.set(false);
        },
        error: (error) => {
          console.error('[LandingPage] Failed to load actors:', error);
          this.actors.set([]);
          this.actorsError.set('Не удалось загрузить актеров. Попробуйте обновить страницу.');
          this.actorsLoading.set(false);
        },
      });
  }
}
