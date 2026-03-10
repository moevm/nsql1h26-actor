import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { ProfileInfo } from '../../components/profile-info/profile-info';
import { ProfileExperience } from '../../components/profile-experience/profile-experience';
import { ActorsApi } from '../../../../core/services/actors-api';
import { components } from '../../../../shared/api/types';
import { ProfileMedia } from '../../components/profile-media/profile-media';

type Actor = components['schemas']['Actor']

@Component({
  selector: 'app-profile-page',
  imports: [ProfileInfo, ProfileMedia, ProfileExperience],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
})
export class ProfilePage {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly actorsApi = inject(ActorsApi);
  readonly actor = signal<Actor | null>(null);


  constructor() {
    const actorId = this.route.snapshot.queryParamMap.get('actorId') ?? '69aee09a3dcd7a59f2474e42';

    if (!actorId) {
      console.info(
        '[ProfilePage] Add query param actorId to test API call, for example: /profile?actorId=67cdb7849b9c8f2d1e6a1234',
      );
      return;
    }

    this.actorsApi
      .getActorById(actorId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (actor) => {
          this.actor.set(actor);
        },
        error: (error) => console.error('[ProfilePage] Failed to load actor:', error),
      });
  }


}
