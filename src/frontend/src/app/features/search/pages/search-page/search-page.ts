import { Component, inject, DestroyRef, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';
import { SearchFilters } from '../../components/search-filters/search-filters';
import { SearchBar } from '../../components/search-bar/search-bar';
import { ProfileCard } from '../../../../shared/ui/profile-card/profile-card';
import { SearchPagination } from '../../components/search-pagination/search-pagination';
import { ActorsApi } from '../../../../core/services/actors-api';
import { operations, components } from '../../../../shared/api/types';
import {
  SearchFiltersValue,
  DEFAULT_SEARCH_FILTERS,
} from '../../components/search-filters/search-filters.model';
import { fullName } from '../../../../shared/utils/actorFormatter';

type Actor = components['schemas']['Actor'];

type SearchRequestParams = {
  q: string;
  page: number;
} & SearchFiltersValue;

type SearchCriteriaParams = {
  q: string;
} & SearchFiltersValue;

type ActorsSearchQuery = NonNullable<operations['v1ActorsGet']['parameters']['query']>;

@Component({
  selector: 'app-search-page',
  imports: [SearchFilters, SearchBar, ProfileCard, SearchPagination, RouterLink],
  templateUrl: './search-page.html',
  styleUrl: './search-page.scss',
})
export class SearchPage {
  private readonly actorsApi = inject(ActorsApi);
  private readonly destroyRef = inject(DestroyRef);
  readonly fullName = fullName;

  readonly query = signal('');
  readonly filters = signal<SearchFiltersValue>(DEFAULT_SEARCH_FILTERS);

  readonly criteriaParams = computed<SearchCriteriaParams>(() => ({
    q: this.query().trim(),
    ...this.filters(),
  }));

  readonly requestParams = computed<SearchRequestParams>(() => ({
    q: this.query().trim(),
    page: this.currentPage(),
    ...this.filters(),
  }));

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly actors = signal<Actor[]>([]);

  readonly actorsCount = signal(0);
  readonly currentPage = signal(1);
  readonly pageSize = 12;

  constructor() {
    toObservable(this.query)
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.currentPage() !== 1) {
          this.currentPage.set(1);
        }
      });

    toObservable(this.criteriaParams)
      .pipe(
        debounceTime(350),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
        switchMap((params) =>
          this.fetchActorsCount(params).pipe(
            catchError(() => {
              return of(0);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((count) => this.actorsCount.set(count));

    toObservable(this.requestParams)
      .pipe(
        debounceTime(350),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
        tap(() => {
          console.log('Fetching actors with params:', this.requestParams());
          this.loading.set(true);
          this.error.set(null);
        }),
        switchMap((params) =>
          this.fetchActors(params).pipe(
            catchError(() => {
              this.error.set('Не удалось загрузить результаты');
              return of<Actor[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((actors) => {
        this.actors.set(actors);
        this.loading.set(false);
      });
  }

  onFiltersChange(filters: SearchFiltersValue): void {
    this.filters.set(filters);
    this.currentPage.set(1);
  }

  private fetchActors(params: SearchRequestParams) {
    const backendQuery = this.buildBackendQuery(
      params,
      this.pageSize,
      (Math.max(1, params.page) - 1) * this.pageSize,
    );

    return this.actorsApi.getActors(backendQuery);
  }

  private fetchActorsCount(params: SearchCriteriaParams) {
    const backendQuery = this.buildBackendQuery(params, 999, 0);
    return this.actorsApi.getActors(backendQuery).pipe(map((actors) => actors.length));
  }

  private buildBackendQuery(
    params: SearchCriteriaParams,
    limit: number,
    offset: number,
  ): ActorsSearchQuery {
    return {
      ...(params.gender ? { gender: params.gender } : {}),
      ...(params.age_from != null ? { ageFrom: params.age_from } : {}),
      ...(params.age_to != null ? { ageTo: params.age_to } : {}),
      ...(params.weight_from != null ? { weightMin: params.weight_from } : {}),
      ...(params.weight_to != null ? { weightMax: params.weight_to } : {}),
      ...(params.activity_years_from != null ? { activityYearFrom: params.activity_years_from } : {}),
      ...(params.activity_years_to != null ? { activityYearTo: params.activity_years_to } : {}),
      ...(params.university_id ? { universityId: params.university_id } : {}),
      ...(params.theatre ? { theatre: params.theatre } : {}),
      ...(params.hair_color ? { hairColor: params.hair_color } : {}),
      ...(params.eye_color ? { eyeColor: params.eye_color } : {}),
      ...(this.mapTitle(params.actor_rank) ? { title: this.mapTitle(params.actor_rank) } : {}),
      ...(params.q ? { name: params.q } : {}),
      ...(this.mapGenres(params).length > 0 ? { genres: this.mapGenres(params) } : {}),
      limit,
      offset,
    };
  }

  private mapTitle(value: string): ActorsSearchQuery['title'] | undefined {
    if (value === 'honored' || value === 'national' || value === 'none') {
      return value;
    }
    return undefined;
  }

  private mapGenres(filters: SearchFiltersValue): string[] {
    const items: string[] = [];
    if (filters.genre_drama) items.push('драма');
    if (filters.genre_comedy) items.push('комедия');
    if (filters.genre_tragedy) items.push('трагедия');
    if (filters.genre_melodrama) items.push('мелодрама');
    if (filters.genre_tragicomedy) items.push('трагикомедия');
    if (filters.genre_musical) items.push('мюзикл');
    if (filters.genre_opera) items.push('опера');
    if (filters.genre_ballet) items.push('балет');
    if (filters.genre_monodrama) items.push('монодрама');
    return items;
  }
}
