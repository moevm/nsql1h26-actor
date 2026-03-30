import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

export const MIN_YEAR = 1926;
export function getCurrentYear(): number {
  return new Date().getFullYear();
}

export function hasControlError(
  form: AbstractControl,
  controlName: string,
  errorKey: string,
): boolean {
  const control = form.get(controlName);
  if (!control) {
    return false;
  }

  return control.hasError(errorKey) && (control.dirty || control.touched);
}

// Payload's  helpers
export function toNullableText(value: unknown): string | null {
  if (typeof value !== 'string') {
    return null;
  }

  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

export function toNullableNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

export function toRequiredText(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

export function normalizeDate(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export function getYearFromDob(dobValue: unknown): number | null {
  const date = parseDate(dobValue);

  return date?.getFullYear() ?? null;
}

// Parsers
export function parseDate(value: unknown): Date | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }

  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value;
  }

  if (typeof value !== 'string') {
    return null;
  }

  const parsedDate = new Date(value);
  return Number.isNaN(parsedDate.getTime()) ? null : parsedDate;
}

export function parseYear(value: string): number | null {
  if (!value) {
    return null;
  }

  if (!/^\d{4}$/.test(value)) {
    return null;
  }

  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

// Validators
export function birthDateValidator(maxAgeYears: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const rawValue = control.value;
    if (rawValue === null || rawValue === undefined || rawValue === '') {
      return null;
    }

    const birthDate = parseDate(rawValue);
    if (!birthDate) {
      return { dateInvalid: true };
    }

    const today = normalizeDate(new Date());
    const normalizedBirthDate = normalizeDate(birthDate);
    if (normalizedBirthDate > today) {
      return { birthDateInFuture: true };
    }

    const oldestAllowedDate = new Date(
      today.getFullYear() - maxAgeYears,
      today.getMonth(),
      today.getDate(),
    );

    if (normalizedBirthDate < oldestAllowedDate) {
      return { birthDateTooOld: true };
    }

    return null;
  };
}

export function theatreYearsValidator(minYear: number, maxYear: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const rawValue = String(control.value ?? '').trim();
    if (!rawValue) {
      return null;
    }

    const [fromRaw = '', toRaw = ''] = rawValue.split(/[–-]/);
    const fromValue = fromRaw.trim();
    const toValue = toRaw.trim();

    const fromYear = parseYear(fromValue);
    const toYear = parseYear(toValue);

    if ((fromValue && fromYear === null) || (toValue && toYear === null)) {
      return { yearsFormatInvalid: true };
    }

    if (
      (fromYear !== null && (fromYear < minYear || fromYear > maxYear)) ||
      (toYear !== null && (toYear < minYear || toYear > maxYear))
    ) {
      return { yearsOutOfRange: true };
    }

    if (fromYear !== null && toYear !== null && fromYear > toYear) {
      return { yearsRangeInvalid: true };
    }

    return null;
  };
}

export function dobYearsValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const rawValue = String(control.value ?? '').trim();
    if (!rawValue) {
      return null;
    }

    const birthYear = getYearFromDob(control.root.get('birthDate')?.value);
    if (birthYear === null) {
      return null;
    }

    const [fromRaw = '', toRaw = ''] = rawValue.split(/[–-]/);
    const fromYear = parseYear(fromRaw.trim());
    const toYear = parseYear(toRaw.trim());

    if ((fromYear !== null && fromYear < birthYear) || (toYear !== null && toYear < birthYear)) {
      return { dobRangeInvalid: true };
    }

    return null;
  };
}
