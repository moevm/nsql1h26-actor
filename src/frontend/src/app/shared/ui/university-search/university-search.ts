import {
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  Output,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';
import { UniversityService } from '../../../core/services/university-service';
import { components } from '../../api/types';

type EducationItem = components['schemas']['EducationItem'];
type UniversitySearchItem = components['schemas']['UniversitySearchItem'];
type SelectedUniversity = {
  id: string;
  name: string;
  shortName?: string | null;
  oldNames?: string[] | null;
};

@Component({
  selector: 'app-university-search',
  imports: [],
  templateUrl: './university-search.html',
  styleUrl: './university-search.scss',
})
export class UniversitySearch {
  private readonly destroyRef = inject(DestroyRef);
  private readonly universityService = inject(UniversityService);
  private existingSelectedUniversities: SelectedUniversity[] = [];
  private isMultiselect = false;

  private readonly query = signal('');
  readonly universities = signal<UniversitySearchItem[]>([]);
  readonly selectedUniversities = signal<SelectedUniversity[]>([]);
  readonly visibleUniversities = computed(() => {
    const items = this.universities().map((item) => {
      return { id: item.id, name: item.name } as SelectedUniversity;
    });
    if (!this.multiselect) {
      return items;
    }

    const selected = this.selectedUniversities();
    return items.filter((item) => !selected.some((picked) => this.isSameUniversity(item, picked)));
  });

  readonly loading = signal(false);
  readonly isDropdownOpen = signal(false);
  readonly hasQuery = computed(() => this.query().trim().length > 0);

  @Input()
  set alreadyExistsUni(value: EducationItem[] | null) {
    this.existingSelectedUniversities = (value ?? [])
      .map((item) => {
        const id = item.uniId?.trim();
        const name = (item.university?.name ?? item.name ?? '').trim();

        if (!id || !name) {
          return null;
        }

        return {
          id,
          name,
          shortName: item.university?.shortName ?? null,
          oldNames: item.university?.oldNames ?? null,
        } as SelectedUniversity;
      })
      .filter((item): item is SelectedUniversity => item !== null);

    this.applyExistingUniversities();
  }
  @Input() placeholder: string = 'Введите название вуза';
  @Input() getWhenDataReady: boolean = false;
  @Input()
  set multiselect(value: boolean) {
    this.isMultiselect = value;
    this.applyExistingUniversities();
  }

  get multiselect(): boolean {
    return this.isMultiselect;
  }

  @Input()
  set resetVersion(_: number) {
    this.existingSelectedUniversities = [];
    this.query.set('');
    this.universities.set([]);
    this.selectedUniversities.set([]);
    this.isDropdownOpen.set(false);
    this.syncSelectedUniversityWithParent();
  }

  @Output() selectedUniversity = new EventEmitter<string[] | null>();
  @Output() selectedUniversityData = new EventEmitter<UniversitySearchItem[] | null>();

  get inputValue(): string {
    return this.query();
  }

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
      });
  }

  onInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.query.set(value);

    if (!this.multiselect) {
      const selected = this.selectedUniversities()[0];

      const isStillSelected =
        selected && selected.name.trim().toLowerCase() === value.trim().toLowerCase();

      if (!isStillSelected && !this.getWhenDataReady) {
        this.selectedUniversities.set([]);
        this.syncSelectedUniversityWithParent();
      }
    }

    this.isDropdownOpen.set(true);
  }

  onFocus(): void {
    if (this.multiselect) {
      this.query.set('');
    }
    this.isDropdownOpen.set(true);
  }

  onFocusOut(event: FocusEvent): void {
    const currentTarget = event.currentTarget as HTMLElement | null;
    const relatedTarget = event.relatedTarget as Node | null;
    if (currentTarget && relatedTarget && currentTarget.contains(relatedTarget)) {
      return;
    }

    if (this.multiselect) {
      let result = '';
      this.selectedUniversities().forEach((item) => {
        result += item.name + ', ';
      });

      this.query.set(result.slice(0, -2));
    } else {
      this.syncSingleSelectOnFocusOut();
    }

    this.isDropdownOpen.set(false);
  }

  onSelectUniversity(university: UniversitySearchItem): void {
    const value = (university.name ?? '').trim();
    if (!value || !university.id || !university.name) {
      return;
    }

    const selectedItem: SelectedUniversity = {
      id: university.id,
      name: university.name,
      shortName: university.shortName ?? null,
      oldNames: university.oldNames ?? null,
    };

    if (this.multiselect) {
      this.selectedUniversities.set([...this.selectedUniversities(), selectedItem]);

      if (!this.getWhenDataReady) {
        this.syncSelectedUniversityWithParent();
      }

      this.query.set('');
      this.isDropdownOpen.set(true);
      return;
    }

    this.query.set(value);
    this.selectedUniversities.set([selectedItem]);

    if (!this.getWhenDataReady) {
      this.syncSelectedUniversityWithParent();
    }
    
    this.isDropdownOpen.set(false);
  }

  removeUniversity(university: SelectedUniversity): void {
    if (!this.multiselect) {
      return;
    }

    this.selectedUniversities.set(
      this.selectedUniversities().filter((item) => !this.isSameUniversity(item, university)),
    );
    this.syncSelectedUniversityWithParent();
  }

  isUniversitySelected(university: SelectedUniversity): boolean {
    if (this.multiselect) {
      return this.selectedUniversities().some((item) => this.isSameUniversity(item, university));
    }

    const selected = this.selectedUniversities()[0];
    return selected ? selected.id === (university.id ?? null) : false;
  }

  private isSameUniversity(a: SelectedUniversity, b: SelectedUniversity): boolean {
    if (a.id && b.id) {
      return a.id === b.id;
    }

    return (a.name ?? '').trim().toLowerCase() === (b.name ?? '').trim().toLowerCase();
  }

  private syncSingleSelectOnFocusOut(): void {
    const query = this.query().trim();
    if (!query) {
      this.selectedUniversities.set([]);
      this.query.set('');
      this.syncSelectedUniversityWithParent();
      return;
    }

    const matched = this.universities().find(
      (item) => (item.name ?? '').trim().toLowerCase() === query.toLowerCase(),
    );

    if (!matched?.id || !matched?.name) {
      this.selectedUniversities.set([]);
      this.query.set('');
      this.syncSelectedUniversityWithParent();
      return;
    }

    this.selectedUniversities.set([
      {
        id: matched.id,
        name: matched.name,
        shortName: matched.shortName ?? null,
        oldNames: matched.oldNames ?? null,
      },
    ]);
    this.query.set(matched.name);
    this.syncSelectedUniversityWithParent();
  }

  private applyExistingUniversities(): void {
    const selected = this.multiselect
      ? this.existingSelectedUniversities
      : this.existingSelectedUniversities.slice(0, 1);

    this.selectedUniversities.set(selected);
    this.query.set(
      this.multiselect ? selected.map((item) => item.name).join(', ') : (selected[0]?.name ?? ''),
    );
    this.syncSelectedUniversityWithParent();
  }

  private syncSelectedUniversityWithParent(): void {
    const selected = this.selectedUniversities();

    const selectedIds = this.selectedUniversities()
      .map((item) => item.id)
      .filter((id): id is string => Boolean(id));

    const selectedData: UniversitySearchItem[] = selected.map((item) => ({
      id: item.id,
      name: item.name,
      shortName: item.shortName ?? null,
      oldNames: item.oldNames ?? null,
    }));

    this.selectedUniversity.emit(selectedIds.length > 0 ? selectedIds : null);
    this.selectedUniversityData.emit(selectedData.length > 0 ? selectedData : null);
  }
}
