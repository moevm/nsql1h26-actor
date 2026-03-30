import { Component, signal } from '@angular/core';
import { UniversityForm } from './components/university-form/university-form';
import { UniversitySearch } from '../../../../shared/ui/university-search/university-search';

import { components } from '../../../../shared/api/types';
import {
  NoticeBanner,
  NoticeBannerTone,
  SubmitNotification,
} from '../../../../shared/ui/notice-banner/notice-banner';

type UniversitySearchItem = components['schemas']['UniversitySearchItem'];

@Component({
  selector: 'app-universities-page',
  imports: [UniversityForm, UniversitySearch, NoticeBanner],
  templateUrl: './universities-page.html',
  styleUrl: './universities-page.scss',
})
export class UniversitiesPage {
  readonly universityForEdit = signal<UniversitySearchItem | null>(null);
  readonly universitySearchResetVersion = signal(0);

  readonly submitNotification = signal<SubmitNotification | null>(null);

  setSubmitNotification(tone: NoticeBannerTone, message: string): void {
    this.submitNotification.set({ tone, message });
  }

  clearSubmitNotification(): void {
    this.submitNotification.set(null);
  }

  onUniversityDataChange(universities: UniversitySearchItem[] | null): void {
    this.universityForEdit.set(universities?.[0] ?? null);
  }

  onUniversityUpdated(): void {
    this.universityForEdit.set(null);
    this.universitySearchResetVersion.update((value) => value + 1);
  }
}
