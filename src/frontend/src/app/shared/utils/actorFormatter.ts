import { components } from '../api/types';

type Actor = components['schemas']['Actor'];

export function capitalize(value: string | null | undefined): string {
  if (!value) return '-';

  const normalized = value.trim();
  if (!normalized) return '-';

  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

export function getAgeFromDob(date: string): number {
  const birthDate = new Date(date);
  const today = new Date();

  let age = today.getFullYear() - birthDate.getFullYear();

  const monthDiff = today.getMonth() - birthDate.getMonth();

  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--;
  }

  return age;
}

export function ageWord(date: string): string {
  const age = getAgeFromDob(date);
  const lastDigit = age % 10;
  const lastTwo = age % 100;

  if (lastTwo >= 11 && lastTwo <= 14) return 'лет';
  if (lastDigit === 1) return 'год';
  if (lastDigit >= 2 && lastDigit <= 4) return 'года';
  return age + ' лет';
}

export function fullName(actor: Actor): string {
  const lastName = actor.lastName?.trim() ?? '';
  const firstName = actor.firstName?.trim() ?? '';
  const middleName = actor.middleName?.trim() ?? '';

  return `${lastName} ${firstName}\n${middleName}`;
}

export function formatDate(date: string | undefined | null): string {
  if (!date) return '-';

  const d = new Date(date);

  return `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}.${d.getFullYear()} (${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')})`;
}
