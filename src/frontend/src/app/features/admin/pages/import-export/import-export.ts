import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, signal, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { InputFile } from '../../../../shared/ui/input-file/input-file';
import {
  ImportExportService,
  CatalogSnapshot,
} from '../../../../core/services/import-export-service';
import {
  NoticeBanner,
  SubmitNotification,
  NoticeBannerTone,
} from '../../../../shared/ui/notice-banner/notice-banner';

@Component({
  selector: 'app-import-export',
  imports: [InputFile, NoticeBanner],
  templateUrl: './import-export.html',
  styleUrl: './import-export.scss',
})
export class ImportExport {
  private readonly importExportApi = inject(ImportExportService);
  private readonly destroyRef = inject(DestroyRef);

  readonly accept = '.json';
  readonly importInputResetVersion = signal(0);
  readonly pendingImportFile = signal<File | null>(null);
  readonly isImporting = signal(false);
  readonly isExporting = signal(false);

  readonly submitNotification = signal<SubmitNotification | null>(null);

  setSubmitNotification(tone: NoticeBannerTone, message: string): void {
    this.submitNotification.set({ tone, message });
  }

  clearSubmitNotification(): void {
    this.submitNotification.set(null);
  }

  private getNotificationMessage(action: 'import' | 'export', error: unknown): string {
    if (error instanceof Error && error.message) {
      return error.message;
    }

    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        return 'Недостаточно прав для выполнения операции. Войдите как администратор.';
      }

      if (error.status === 0) {
        return 'Не удалось связаться с сервером. Проверьте соединение и попробуйте еще раз.';
      }
    }

    return action === 'import'
      ? 'Не удалось импортировать данные. Попробуйте еще раз.'
      : 'Не удалось экспортировать данные. Попробуйте еще раз.';
  }

  private async buildImportSnapshot(file: File): Promise<CatalogSnapshot> {
    const rawText = await file.text();

    let parsed: unknown;
    try {
      parsed = JSON.parse(rawText);
    } catch {
      throw new Error('Файл не является корректным JSON.');
    }

    if (!parsed || typeof parsed !== 'object') {
      throw new Error('Некорректная структура файла импорта.');
    }

    const snapshot = parsed as Partial<CatalogSnapshot>;

    if (
      snapshot.format !== 'nsql1-catalog' ||
      typeof snapshot.version !== 'number' ||
      !Array.isArray(snapshot.universities) ||
      !Array.isArray(snapshot.actors) ||
      !Array.isArray(snapshot.admins) ||
      !Array.isArray(snapshot.media)
    ) {
      throw new Error('Файл импорта не соответствует формату CatalogSnapshot.');
    }

    return snapshot as CatalogSnapshot;
  }

  addImportFile(files: FileList | null): void {
    if (!files || files.length === 0) {
      this.pendingImportFile.set(null);
      return;
    }

    this.pendingImportFile.set(files[0]);
  }

  async commitImportFile(): Promise<void> {
    const file = this.pendingImportFile();
    if (!file || this.isImporting()) {
      return;
    }

    this.clearSubmitNotification();
    this.isImporting.set(true);

    try {
      const snapshot = await this.buildImportSnapshot(file);

      this.importExportApi
        .importData(snapshot)
        .pipe(
          finalize(() => this.isImporting.set(false)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          next: () => {
            this.pendingImportFile.set(null);
            this.importInputResetVersion.update((version) => version + 1);
            this.setSubmitNotification('success', 'Импорт данных успешно завершен.');
          },
          error: (error) => {
            this.setSubmitNotification('error', this.getNotificationMessage('import', error));
          },
        });
    } catch (error) {
      this.isImporting.set(false);
      this.setSubmitNotification('error', this.getNotificationMessage('import', error));
    }
  }

  saveExportFile(): void {
    if (this.isExporting()) {
      return;
    }

    this.clearSubmitNotification();
    this.isExporting.set(true);

    this.importExportApi
      .exportData()
      .pipe(
        finalize(() => this.isExporting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (snapshot) => {
          const fileContent = JSON.stringify(snapshot, null, 2);
          const blob = new Blob([fileContent], { type: 'application/json;charset=utf-8' });
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          const timestamp = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-');

          link.href = url;
          link.download = `actorsHub-export-${timestamp}.json`;
          link.click();

          URL.revokeObjectURL(url);
          this.setSubmitNotification('success', 'Экспорт успешно завершен. Файл загружается.');
        },
        error: (error) => {
          this.setSubmitNotification('error', this.getNotificationMessage('export', error));
        },
      });
  }
}
