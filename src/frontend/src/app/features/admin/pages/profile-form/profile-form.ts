import { Component, DestroyRef, inject, signal } from '@angular/core';
import { catchError, concatMap, finalize, forkJoin, from, map, of, toArray } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ProfileFormInfo } from './components/profile-form-info/profile-form-info';
import { ProfileFormExperience } from './components/profile-form-experience/profile-form-experience';
import { ProfileFormMedia } from './components/profile-form-media/profile-form-media';
import { ActivatedRoute, Router } from '@angular/router';
import { components } from '../../../../shared/api/types';
import {
  NoticeBanner,
  NoticeBannerTone,
  SubmitNotification,
} from '../../../../shared/ui/notice-banner/notice-banner';

import { EditableMedia, DEFAULT_PROFILE_INPUTS, MediaTypes } from './profile-form.model';
import { ActorsApi } from '../../../../core/services/actors-api';
import { CreateActorMediaRequest, MediaApi } from '../../../../core/services/media-api';

import {
  MIN_YEAR,
  toRequiredText,
  toNullableText,
  toNullableNumber,
  getCurrentYear,
  birthDateValidator,
  dobYearsValidator,
  theatreYearsValidator,
} from '../../../../shared/utils/formHeplers';

import { formatDate } from '../../../../shared/utils/actorFormatter';

type Actor = components['schemas']['Actor'];
type ActorCreate = components['schemas']['ActorCreate'];
type ActorCreateResponse = components['schemas']['ActorCreateResponse'];

type ContactLinkItem = components['schemas']['ContactLinkItem'];
type FilmPlayItem = components['schemas']['FilmPlayItem'];
type MediaSourceItem = components['schemas']['PhotoItem'] | components['schemas']['VideoItem'];

type EducationCreateItem = components['schemas']['EducationCreateItem'];
type TheatrePlayItem = components['schemas']['TheatrePlayItem'];

@Component({
  selector: 'app-profile-form',
  imports: [
    ProfileFormInfo,
    ProfileFormMedia,
    ProfileFormExperience,
    ReactiveFormsModule,
    NoticeBanner,
  ],
  templateUrl: './profile-form.html',
  styleUrl: './profile-form.scss',
})
export class ProfileForm {
  private fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly actorsApi = inject(ActorsApi);
  private readonly mediaApi = inject(MediaApi);

  private readonly CURRENT_YEAR = getCurrentYear();
  private readonly EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
  private readonly PHONE_RE = /^\+?[0-9][0-9\s\-()]{7,16}[0-9]$/;
  private readonly URL_RE = /^(https?:\/\/)?([a-z0-9-]+\.)+[a-z]{2,}(\/[^\s]*)?$/i;
  private mediaObjectUrls: string[] = [];

  readonly actor = signal<Actor | null>(null);
  readonly createdActorId = signal<string>('');

  readonly isEditing = signal<boolean>(false);

  readonly isSubmitting = signal(false);
  readonly submitNotification = signal<SubmitNotification | null>(null);

  formatDate = formatDate;

  profile_form = this.fb.group({
    mainPhoto: this.fb.control<EditableMedia | null>(null),
    firstName: [DEFAULT_PROFILE_INPUTS.firstName, Validators.required],
    lastName: [DEFAULT_PROFILE_INPUTS.lastName, Validators.required],
    middleName: [DEFAULT_PROFILE_INPUTS.middleName],
    birthDate: [DEFAULT_PROFILE_INPUTS.birthDate, birthDateValidator(120)],
    height: [DEFAULT_PROFILE_INPUTS.height, [Validators.min(40), Validators.max(250)]],
    weight: [DEFAULT_PROFILE_INPUTS.weight, [Validators.min(20), Validators.max(150)]],
    gender: this.fb.nonNullable.control(DEFAULT_PROFILE_INPUTS.gender),
    hairColor: [DEFAULT_PROFILE_INPUTS.hairColor],
    eyeColor: [DEFAULT_PROFILE_INPUTS.eyeColor],
    bio: [DEFAULT_PROFILE_INPUTS.bio],
    title: this.fb.nonNullable.control(DEFAULT_PROFILE_INPUTS.title),
    phone: [DEFAULT_PROFILE_INPUTS.phone, Validators.pattern(this.PHONE_RE)],
    email: [DEFAULT_PROFILE_INPUTS.email, Validators.pattern(this.EMAIL_RE)],
    contactLinks: this.fb.group({
      web: ['', Validators.pattern(this.URL_RE)],
      vk: ['', Validators.pattern(this.URL_RE)],
      max: ['', Validators.pattern(this.URL_RE)],
      rutube: ['', Validators.pattern(this.URL_RE)],
    }),
    education: this.fb.control<EducationCreateItem[]>([]),
    films: this.fb.array<FormGroup>([]),
    theatrePlayItems: this.fb.array<FormGroup>([]),
    photos: this.fb.control<EditableMedia[]>([]),
    videos: this.fb.control<EditableMedia[]>([]),
    genres: this.fb.nonNullable.control<string[]>(DEFAULT_PROFILE_INPUTS.genres ?? []),
  });

  constructor() {
    this.destroyRef.onDestroy(() => this.clearMediaObjectUrls());

    this.profile_form
      .get('birthDate')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.revalidateTheatreYears();
        this.revalidateFilmsYears();
      });

    this.route.data.subscribe((data) => {
      const actor = data['actor'];

      if (actor) {
        this.actor.set(actor);
        this.isEditing.set(true);

        this.fillForm(actor);
        this.fillExperience(actor);
        this.fillMedia(actor);
      }
    });
  }

  get films(): FormArray<FormGroup> {
    return this.profile_form.get('films') as FormArray<FormGroup>;
  }

  get theatres(): FormArray<FormGroup> {
    return this.profile_form.get('theatrePlayItems') as FormArray<FormGroup>;
  }

  get photos(): FormControl<EditableMedia[]> {
    return this.profile_form.get('photos') as FormControl<EditableMedia[]>;
  }

  get videos(): FormControl<EditableMedia[]> {
    return this.profile_form.get('videos') as FormControl<EditableMedia[]>;
  }

  get mainPhoto(): FormControl<EditableMedia | null> {
    return this.profile_form.get('mainPhoto') as FormControl<EditableMedia | null>;
  }

  private fillForm(actor: Actor): void {
    this.profile_form.patchValue({
      ...actor,
      contactLinks: {
        web: actor.links?.find((l) => l.name === 'web')?.url ?? '',
        vk: actor.links?.find((l) => l.name === 'vk')?.url ?? '',
        max: actor.links?.find((l) => l.name === 'max')?.url ?? '',
        rutube: actor.links?.find((l) => l.name === 'rutube')?.url ?? '',
      },
      education: actor.education?.map((item) => {
        return { uniId: item.uniId! };
      }),
      photos: [],
      videos: [],
      genres: actor.genres ?? [],
    });
  }

  private fillExperience(actor: Actor): void {
    this.profile_form.setControl(
      'films',
      this.fb.array(
        (actor.films ?? []).map((f) =>
          this.fb.group({
            title: [f.title ?? ''],
            year: [
              f.year ?? null,
              [Validators.min(MIN_YEAR), Validators.max(this.CURRENT_YEAR), dobYearsValidator()],
            ],
            role: [f.role ?? ''],
            director: [f.director ?? ''],
          }),
        ),
      ),
    );

    this.profile_form.setControl(
      'theatrePlayItems',
      this.fb.array(
        (actor.theatrePlayItems ?? []).map((t) =>
          this.fb.group({
            name: [t.name ?? ''],
            years: [
              t.years ?? '',
              [theatreYearsValidator(MIN_YEAR, this.CURRENT_YEAR), dobYearsValidator()],
            ],
            plays: this.fb.array(
              (t.plays ?? []).map((p) =>
                this.fb.group({
                  title: [p.title ?? ''],
                  year: [p.year ?? null],
                  role: [p.role ?? ''],
                  director: [p.director ?? ''],
                }),
              ),
            ),
          }),
        ),
      ),
    );
  }

  private fillMedia(actor: Actor): void {
    const actorId = actor.id;
    this.clearMediaObjectUrls();

    if (!actorId) {
      this.photos.setValue([]);
      this.videos.setValue([]);
      this.mainPhoto.setValue(null);
      return;
    }

    forkJoin({
      mainPhoto: this.loadExistingMedia(
        actorId,
        actor.mainPhotoId ? [{ id: actor.mainPhotoId }] : [],
        'photo',
      ),
      photos: this.loadExistingMedia(actorId, actor.photos, 'photo'),
      videos: this.loadExistingMedia(actorId, actor.videos, 'video'),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ mainPhoto, photos, videos }) => {
        this.mainPhoto.setValue(mainPhoto[0]);
        this.photos.setValue(photos);
        this.videos.setValue(videos);
      });
  }

  private loadExistingMedia(
    actorId: string,
    items: MediaSourceItem[] | undefined,
    type: MediaTypes,
  ) {
    const mediaWithId = (items ?? [])
      .map((item) => ({
        id: item.id,
        caption: item.caption ?? null,
      }))
      .filter((item): item is { id: string; caption: string | null } => Boolean(item.id));

    if (mediaWithId.length === 0) {
      return of([] as EditableMedia[]);
    }

    return forkJoin(
      mediaWithId.map((item) =>
        this.mediaApi.getActorMedia(actorId, item.id).pipe(
          map((blob): EditableMedia => {
            const url = URL.createObjectURL(blob);
            this.mediaObjectUrls.push(url);

            return {
              kind: 'existing',
              id: item.id,
              url,
              caption: item.caption,
              type: type,
            };
          }),
          catchError((error) => {
            console.error('[ProfileForm] Failed to load media item:', {
              actorId,
              mediaId: item.id,
              error,
            });
            return of(null);
          }),
        ),
      ),
    ).pipe(map((loaded) => loaded.filter((item): item is EditableMedia => item !== null)));
  }

  private clearMediaObjectUrls(): void {
    this.mediaObjectUrls.forEach((url) => URL.revokeObjectURL(url));
    this.mediaObjectUrls = [];
  }

  private revalidateTheatreYears(): void {
    this.theatres.controls.forEach((theatre) => {
      theatre.get('years')?.updateValueAndValidity({ emitEvent: false });
    });
  }

  private revalidateFilmsYears(): void {
    this.films.controls.forEach((film) => {
      film.get('year')?.updateValueAndValidity({ emitEvent: false });
    });
  }

  getPlays(theatreIndex: number): FormArray<FormGroup> {
    return this.theatres.at(theatreIndex).get('plays') as FormArray<FormGroup>;
  }

  addFilm(): void {
    this.films.push(
      this.fb.group({
        title: [''],
        year: [
          null,
          [Validators.min(MIN_YEAR), Validators.max(this.CURRENT_YEAR), dobYearsValidator()],
        ],
        role: [''],
        director: [''],
      }),
    );
  }

  addTheatre(): void {
    this.theatres.push(
      this.fb.group({
        name: [''],
        years: ['', [theatreYearsValidator(MIN_YEAR, this.CURRENT_YEAR), dobYearsValidator()]],
        plays: this.fb.array([]),
      }),
    );
  }

  addPlay(theatreIndex: number): void {
    this.getPlays(theatreIndex).push(
      this.fb.group({
        title: [''],
        year: [null, [Validators.min(MIN_YEAR), Validators.max(this.CURRENT_YEAR)]],
        role: [''],
        director: [''],
      }),
    );
  }

  removeFilm(index: number): void {
    this.films.removeAt(index);
  }

  removeTheatre(index: number): void {
    this.theatres.removeAt(index);
  }

  removePlay(theatreIndex: number, playIndex: number): void {
    this.getPlays(theatreIndex).removeAt(playIndex);
  }

  // Building payloads
  private buildCreatePayload(): ActorCreate {
    const raw = this.profile_form.getRawValue();

    const links: ContactLinkItem[] = [
      { name: 'web', url: toNullableText(raw.contactLinks.web) ?? undefined },
      { name: 'vk', url: toNullableText(raw.contactLinks.vk) ?? undefined },
      { name: 'max', url: toNullableText(raw.contactLinks.max) ?? undefined },
      { name: 'rutube', url: toNullableText(raw.contactLinks.rutube) ?? undefined },
    ].filter((item) => Boolean(item.url));

    const films: FilmPlayItem[] = raw.films
      .map((film) => ({
        title: toRequiredText(film['title']),
        year: toNullableNumber(film['year']),
        role: toNullableText(film['role']),
        director: toNullableText(film['director']),
      }))
      .filter((item) => Boolean(item.title || item.year !== null || item.role || item.director));

    const theatrePlayItems: TheatrePlayItem[] = raw.theatrePlayItems
      .map((theatre) => ({
        name: toRequiredText(theatre['name']),
        years: toNullableText(theatre['years']),
        plays: ((theatre['plays'] ?? []) as Array<Record<string, unknown>>)
          .map((play) => ({
            title: toRequiredText(play['title']),
            year: toNullableNumber(play['year']),
            role: toNullableText(play['role']),
            director: toNullableText(play['director']),
          }))
          .filter((item) =>
            Boolean(item.title || item.year !== null || item.role || item.director),
          ),
      }))
      .filter((item) => Boolean(item.name || item.years || item.plays.length > 0));

    return {
      firstName: toRequiredText(raw.firstName),
      lastName: toRequiredText(raw.lastName),
      middleName: toNullableText(raw.middleName),
      birthDate: toNullableText(raw.birthDate),
      height: toNullableNumber(raw.height),
      weight: toNullableNumber(raw.weight),
      gender: raw.gender,
      hairColor: toNullableText(raw.hairColor),
      eyeColor: toNullableText(raw.eyeColor),
      bio: toNullableText(raw.bio),
      title: raw.title,
      phone: toNullableText(raw.phone),
      email: toNullableText(raw.email),
      links,
      education: raw.education,
      films,
      theatrePlayItems,
      genres: raw.genres,
    };
  }

  private buildMediaPayload(item: EditableMedia): CreateActorMediaRequest | string {
    if (item.kind === 'new') {
      return {
        file: item.file,
        type: item.type,
        caption: null,
      };
    } else {
      return item.id;
    }
  }

  // Submit helpers
  private uploadMainPhoto(actorId: string) {
    const mainPhoto = this.mainPhoto.value;
    const currentActorMainPhotoId = this.actor()?.mainPhotoId ?? null;

    if (!mainPhoto) {
      if (!currentActorMainPhotoId) {
        return of<string | null>(null);
      }

      return this.mediaApi.deleteActorMedia(actorId, currentActorMainPhotoId).pipe(map(() => null));
    }

    if (mainPhoto.kind !== 'new') {
      return of(mainPhoto.id);
    }

    return this.mediaApi
      .createActorMedia(actorId, {
        file: mainPhoto.file,
        type: 'photo',
        caption: mainPhoto.caption,
      })
      .pipe(
        concatMap((response) => {
          if (!response.mediaId) {
            throw new Error('Failed to upload main photo');
          }

          return of(response.mediaId);
        }),
      );
  }

  private applyMainPhoto(actorId: string) {
    return this.uploadMainPhoto(actorId).pipe(
      concatMap((mainPhotoId) =>
        mainPhotoId
          ? this.actorsApi.updateActor(actorId, { mainPhotoId }).pipe(map(() => actorId))
          : of(actorId),
      ),
    );
  }

  private getMediaForUpload(onlyNew: boolean): EditableMedia[] {
    const { videos, photos } = this.profile_form.getRawValue();
    const currentMedia = [...(videos ?? []), ...(photos ?? [])];

    const newMedia = currentMedia.filter(
      (item): item is Extract<EditableMedia, { kind: 'new' }> => item.kind === 'new',
    );

    if (onlyNew) {
      return newMedia;
    }

    const currentExistingIds = new Set(
      currentMedia
        .filter(
          (item): item is Extract<EditableMedia, { kind: 'existing' }> => item.kind === 'existing',
        )
        .map((item) => item.id),
    );

    const deletedMedia: EditableMedia[] = [
      ...(this.actor()?.videos ?? []).flatMap((item) =>
        item.id && !currentExistingIds.has(item.id)
          ? [
              {
                kind: 'existing' as const,
                id: item.id,
                url: '',
                caption: item.caption ?? null,
                type: 'video' as const,
              },
            ]
          : [],
      ),
      ...(this.actor()?.photos ?? []).flatMap((item) =>
        item.id && !currentExistingIds.has(item.id)
          ? [
              {
                kind: 'existing' as const,
                id: item.id,
                url: '',
                caption: item.caption ?? null,
                type: 'photo' as const,
              },
            ]
          : [],
      ),
    ];

    return [...newMedia, ...deletedMedia];
  }

  private uploadActorMediaSequentially(actorId: string, medias: EditableMedia[]) {
    return from(medias).pipe(
      concatMap((item) => {
        const mediaPayload = this.buildMediaPayload(item);

        if (!mediaPayload) {
          return of(null);
        }

        if (typeof mediaPayload === 'string') {
          return this.mediaApi.deleteActorMedia(actorId, mediaPayload);
        }

        return this.mediaApi.createActorMedia(actorId, mediaPayload);
      }),
      toArray(),
    );
  }

  private extractCreatedActorId(response: ActorCreateResponse): string {
    if (response.status !== 'ok' || !response.id) {
      throw new Error('Failed to create actor');
    }

    this.createdActorId.set(response.id);
    return response.id;
  }

  setSubmitNotification(tone: NoticeBannerTone, message: string): void {
    this.submitNotification.set({ tone, message });
  }

  clearSubmitNotification(): void {
    this.submitNotification.set(null);
  }

  onSubmit(): void {
    if (this.profile_form.invalid || this.isSubmitting()) {
      this.profile_form.markAllAsTouched();
      return;
    }

    const payload = this.buildCreatePayload();

    this.clearSubmitNotification();
    this.isSubmitting.set(true);

    if (!this.isEditing()) {
      const medias = this.getMediaForUpload(true);

      this.actorsApi
        .createActor(payload)
        .pipe(
          map((response) => this.extractCreatedActorId(response)),
          concatMap((actorId) => this.applyMainPhoto(actorId)),
          concatMap((actorId) =>
            this.uploadActorMediaSequentially(actorId, medias).pipe(map(() => actorId)),
          ),
          finalize(() => this.isSubmitting.set(false)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          next: (actorId) => {
            void this.router.navigate(['/profile', actorId]);
          },
          error: (error) => {
            console.error('[ProfileForm] Failed to create actor or upload media:', error);
            this.setSubmitNotification(
              'error',
              'Не удалось создать профиль актера. Попробуйте еще раз.',
            );
          },
        });
    } else {
      const deletedMainPhotoId =
        this.mainPhoto.value === null ? (this.actor()?.mainPhotoId ?? null) : null;

      const medias = this.getMediaForUpload(false).filter(
        (item) =>
          !(deletedMainPhotoId && item.kind === 'existing' && item.id === deletedMainPhotoId),
      );

      this.actorsApi
        .updateActor(this.actor()?.id!, payload)
        .pipe(
          map((response) => response.id!),
          concatMap((actorId) => this.applyMainPhoto(actorId)),
          concatMap((actorId) =>
            this.uploadActorMediaSequentially(actorId, medias).pipe(map(() => actorId)),
          ),
          finalize(() => this.isSubmitting.set(false)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          next: (actorId) => {
            void this.router.navigate(['/profile', actorId]);
          },
          error: (error) => {
            console.error('[ProfileForm] Failed to update actor or upload new media:', error);
            this.setSubmitNotification(
              'error',
              'Не удалось обновить профиль актера. Попробуйте еще раз.',
            );
          },
        });
    }
  }
}
