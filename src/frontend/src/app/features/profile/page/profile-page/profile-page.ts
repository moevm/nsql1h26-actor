import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { ProfileInfo } from '../../components/profile-info/profile-info';
import { ProfileExperience } from '../../components/profile-experience/profile-experience';
import { ActorsApi } from '../../../../core/services/actors-api';
import { components } from '../../../../shared/api/types';
import { ProfileMedia } from '../../components/profile-media/profile-media';
import { catchError, distinctUntilChanged, filter, map, of, switchMap } from 'rxjs';

type Actor = components['schemas']['Actor'];

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
    this.route.paramMap
      .pipe(
        map((params) => params.get('id')),
        filter((id): id is string => !!id),
        distinctUntilChanged(),
        switchMap((id) =>
          this.actorsApi.getActorById(id).pipe(
            catchError((error) => {
              console.error('[ProfilePage] Failed to load actor:', error);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((actor) => this.actor.set(actor));
  }
}
