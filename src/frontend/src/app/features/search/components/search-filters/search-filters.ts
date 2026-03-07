import { DestroyRef, Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { debounceTime, startWith } from 'rxjs';

export type SearchFiltersValue = {
  gender: '' | 'male' | 'female';
  age_from: number | null;
  age_to: number | null;
  height_from: number | null;
  height_to: number | null;
  weight_from: number | null;
  weight_to: number | null;
  activity_years_from: number | null;
  activity_years_to: number | null;
  university: string;
  theatre: string;
  actor_rank: string;
  hair_color: string;
  eye_color: string;
  genre_drama: boolean;
  genre_comedy: boolean;
  genre_tragedy: boolean;
  genre_melodrama: boolean;
  genre_tragicomedy: boolean;
  genre_musical: boolean;
  genre_opera: boolean;
  genre_ballet: boolean;
  genre_monodrama: boolean;
};

export const DEFAULT_SEARCH_FILTERS: SearchFiltersValue = {
  gender: '',
  age_from: null,
  age_to: null,
  height_from: null,
  height_to: null,
  weight_from: null,
  weight_to: null,
  activity_years_from: null,
  activity_years_to: null,
  university: '',
  theatre: '',
  actor_rank: '',
  hair_color: '',
  eye_color: '',
  genre_drama: false,
  genre_comedy: false,
  genre_tragedy: false,
  genre_melodrama: false,
  genre_tragicomedy: false,
  genre_musical: false,
  genre_opera: false,
  genre_ballet: false,
  genre_monodrama: false,
};

const toNumber = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') {
    return null;
  }

  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

const rangeValidator = (fromKey: string, toKey: string, errorKey: string): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    const fromValue = toNumber(control.get(fromKey)?.value);
    const toValue = toNumber(control.get(toKey)?.value);

    if (fromValue === null || toValue === null) {
      return null;
    }

    return fromValue <= toValue ? null : { [errorKey]: true };
  };
};

@Component({
  selector: 'app-search-filters',
  imports: [ReactiveFormsModule],
  templateUrl: './search-filters.html',
  styleUrl: './search-filters.scss',
})
export class SearchFilters implements OnInit {
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  @Output() filtersChange = new EventEmitter<SearchFiltersValue>();

  isAdvancedOpen = false;
  isGenresOpen = false;

  search_form = this.fb.group(
    {
      gender: [DEFAULT_SEARCH_FILTERS.gender, [Validators.required]],
      age_from: [DEFAULT_SEARCH_FILTERS.age_from, [Validators.min(0), Validators.max(120)]],
      age_to: [DEFAULT_SEARCH_FILTERS.age_to, [Validators.min(0), Validators.max(120)]],
      height_from: [DEFAULT_SEARCH_FILTERS.height_from, [Validators.min(40), Validators.max(250)]],
      height_to: [DEFAULT_SEARCH_FILTERS.height_to, [Validators.min(40), Validators.max(250)]],
      weight_from: [DEFAULT_SEARCH_FILTERS.weight_from, [Validators.min(20), Validators.max(150)]],
      weight_to: [DEFAULT_SEARCH_FILTERS.weight_to, [Validators.min(20), Validators.max(150)]],
      activity_years_from: [DEFAULT_SEARCH_FILTERS.activity_years_from, [Validators.min(1900), Validators.max(2026)]],
      activity_years_to: [DEFAULT_SEARCH_FILTERS.activity_years_to, [Validators.min(1900), Validators.max(2026)]],
      university: [DEFAULT_SEARCH_FILTERS.university],
      theatre: [DEFAULT_SEARCH_FILTERS.theatre],
      actor_rank: [DEFAULT_SEARCH_FILTERS.actor_rank],
      hair_color: [DEFAULT_SEARCH_FILTERS.hair_color],
      eye_color: [DEFAULT_SEARCH_FILTERS.eye_color],
      genre_drama: [DEFAULT_SEARCH_FILTERS.genre_drama],
      genre_comedy: [DEFAULT_SEARCH_FILTERS.genre_comedy],
      genre_tragedy: [DEFAULT_SEARCH_FILTERS.genre_tragedy],
      genre_melodrama: [DEFAULT_SEARCH_FILTERS.genre_melodrama],
      genre_tragicomedy: [DEFAULT_SEARCH_FILTERS.genre_tragicomedy],
      genre_musical: [DEFAULT_SEARCH_FILTERS.genre_musical],
      genre_opera: [DEFAULT_SEARCH_FILTERS.genre_opera],
      genre_ballet: [DEFAULT_SEARCH_FILTERS.genre_ballet],
      genre_monodrama: [DEFAULT_SEARCH_FILTERS.genre_monodrama],
    },
    {
      validators: [
        rangeValidator('age_from', 'age_to', 'ageRangeInvalid'),
        rangeValidator('height_from', 'height_to', 'heightRangeInvalid'),
        rangeValidator('weight_from', 'weight_to', 'weightRangeInvalid'),
        rangeValidator('activity_years_from', 'activity_years_to', 'activityRangeInvalid'),
      ],
    },
  );

  ngOnInit(): void {
    this.search_form.valueChanges
      .pipe(startWith(this.search_form.getRawValue()), debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.emitFiltersIfValid());
  }

  isRangeInvalid(errorKey: string, fromKey: string, toKey: string): boolean {
    const fromControl = this.search_form.get(fromKey);
    const toControl = this.search_form.get(toKey);

    if (!fromControl || !toControl) {
      return false;
    }

    return (
      this.search_form.hasError(errorKey) &&
      (fromControl.dirty || fromControl.touched || toControl.dirty || toControl.touched)
    );
  }

  hasControlError(controlName: string, errorKey: string): boolean {
    const control = this.search_form.get(controlName);
    if (!control) {
      return false;
    }

    return control.hasError(errorKey) && (control.dirty || control.touched);
  }

  toggleAdvanced(): void {
    this.isAdvancedOpen = !this.isAdvancedOpen;
  }

  toggleGenres(): void {
    this.isGenresOpen = !this.isGenresOpen;
  }

  onReset(): void {
    this.search_form.reset(DEFAULT_SEARCH_FILTERS);
    this.isAdvancedOpen = false;
    this.isGenresOpen = false;
    this.emitFiltersIfValid();
  }

  onSubmit(): void {
    if (this.search_form.invalid) {
      this.search_form.markAllAsTouched();
      return;
    }

    this.emitFiltersIfValid();
  }

  private emitFiltersIfValid(): void {
    if (this.search_form.invalid) {
      return;
    }

    const raw = this.search_form.getRawValue();
    const filters: SearchFiltersValue = {
      gender: raw.gender === 'female' ? 'female' : raw.gender === 'male' ? 'male' : '',
      age_from: toNumber(raw.age_from),
      age_to: toNumber(raw.age_to),
      height_from: toNumber(raw.height_from),
      height_to: toNumber(raw.height_to),
      weight_from: toNumber(raw.weight_from),
      weight_to: toNumber(raw.weight_to),
      activity_years_from: toNumber(raw.activity_years_from),
      activity_years_to: toNumber(raw.activity_years_to),
      university: raw.university ?? '',
      theatre: raw.theatre ?? '',
      actor_rank: raw.actor_rank ?? '',
      hair_color: raw.hair_color ?? '',
      eye_color: raw.eye_color ?? '',
      genre_drama: !!raw.genre_drama,
      genre_comedy: !!raw.genre_comedy,
      genre_tragedy: !!raw.genre_tragedy,
      genre_melodrama: !!raw.genre_melodrama,
      genre_tragicomedy: !!raw.genre_tragicomedy,
      genre_musical: !!raw.genre_musical,
      genre_opera: !!raw.genre_opera,
      genre_ballet: !!raw.genre_ballet,
      genre_monodrama: !!raw.genre_monodrama,
    };

    this.filtersChange.emit(filters);
  }
}
