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
  readonly actor = signal<Actor | null>(null);

  constructor() {
    const actor = this.route.snapshot.data['actor'];
    if (actor) {
      this.actor.set(actor);
    }
  }
}
