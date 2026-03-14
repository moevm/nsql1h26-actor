import { Component, DestroyRef, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';
import { UniversityService } from '../../../core/services/university-service';
import { components } from '../../api/types';

type UniversitySearchItem = components['schemas']['UniversitySearchItem'];

@Component({
  selector: 'app-university-search',
  imports: [],
  templateUrl: './university-search.html',
  styleUrl: './university-search.scss',
})
export class UniversitySearch {
  private readonly universityService = inject(UniversityService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly query = signal('');
  readonly universities = signal<UniversitySearchItem[]>([]);
  readonly selectedUniversityId = signal<string | null>(null);
  readonly loading = signal(false);

  readonly datalistId = 'universities-list';

  @Input() placeholder = 'Введите название университета';

  @Input()
  set resetVersion(_: number) {
    this.query.set('');
    this.universities.set([]);
    this.syncSelectedUniversityId('');
  }

  get inputValue(): string {
    return this.query();
  }

  @Output() selectedUniversityIdChange = new EventEmitter<string | null>();

  constructor() {
    toObservable(this.query)
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((rawQuery) => {
          const q = rawQuery.trim();
          if (!q) {
            this.loading.set(false);
            return of<UniversitySearchItem[]>([]);
          }

          this.loading.set(true);
          return this.universityService
            .getUniversityByName({
              q,
              limit: 10,
            })
            .pipe(catchError(() => of<UniversitySearchItem[]>([])));
        }),
        tap(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((items) => {
        this.universities.set(items);
        this.syncSelectedUniversityId(this.query());
      });
  }

  onInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.query.set(value);
    this.syncSelectedUniversityId(value);
  }

  private syncSelectedUniversityId(value: string): void {
    const normalized = value.trim().toLowerCase();
    if (!normalized) {
      this.selectedUniversityId.set(null);
      this.selectedUniversityIdChange.emit(null);
      return;
    }

    const match = this.universities().find((u) => (u.name ?? '').trim().toLowerCase() === normalized);
    const selectedId = match?.id ?? null;
    this.selectedUniversityId.set(selectedId);
    this.selectedUniversityIdChange.emit(selectedId);
  }
}
