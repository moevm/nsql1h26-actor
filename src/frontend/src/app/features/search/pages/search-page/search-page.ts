import { Component, computed, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';
import {
  DEFAULT_SEARCH_FILTERS,
  SearchFilters,
  SearchFiltersValue,
} from '../../components/search-filters/search-filters';
import { SearchBar } from '../../components/search-bar/search-bar';
import { ProfileCard } from '../../../../shared/ui/profile-card/profile-card';
import { SearchPagination } from '../../components/search-pagination/search-pagination';

type ActorResult = {
  id: number;
  fullName: string;
};

type SearchRequestParams = {
  q: string;
} & SearchFiltersValue;

@Component({
  selector: 'app-search-page',
  imports: [SearchFilters, SearchBar, ProfileCard, SearchPagination],
  templateUrl: './search-page.html',
  styleUrl: './search-page.scss',
})
export class SearchPage {
  readonly query = signal('');
  readonly filters = signal<SearchFiltersValue>(DEFAULT_SEARCH_FILTERS);

  readonly requestParams = computed<SearchRequestParams>(() => ({
    q: this.query().trim(),
    ...this.filters(),
  }));

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly results = signal<ActorResult[]>([]);

  readonly cardSlots = Array.from({ length: 12 });

  constructor() {
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
              return of<ActorResult[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((actors) => {
        this.results.set(actors);
        this.loading.set(false);
      });
  }

  onFiltersChange(filters: SearchFiltersValue): void {
    this.filters.set(filters);
  }

  private fetchActors(params: SearchRequestParams) {
    const normalizedQuery = params.q.toLowerCase();

    const actors: ActorResult[] = [{ id: 1, fullName: 'John Doe' }];

    return of(actors).pipe(
      map((items) =>
        items.filter((item) => {
          if (!normalizedQuery) {
            return true;
          }
          return item.fullName.toLowerCase().includes(normalizedQuery);
        }),
      ),
    );
  }
}
