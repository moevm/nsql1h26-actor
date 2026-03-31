import { Component, DestroyRef, EventEmitter, inject, signal, Input, Output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormArray,
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';
import { hasControlError, toRequiredText } from '../../../../../../shared/utils/formHeplers';
import {
  UniversityCreateRequest,
  UniversityService,
} from '../../../../../../core/services/university-service';
import { SubmitNotification } from '../../../../../../shared/ui/notice-banner/notice-banner';
import { components } from '../../../../../../shared/api/types';

type UniversitySearchItem = components['schemas']['UniversitySearchItem'];

@Component({
  selector: 'app-university-form',
  imports: [ReactiveFormsModule],
  templateUrl: './university-form.html',
  styleUrl: './university-form.scss',
})
export class UniversityForm {
  private readonly uni_id = signal<string | null>(null);
  @Input()
  set universityData(value: UniversitySearchItem | null) {
    if (value) {
      this.fillForm(value);
      this.isEditing.set(true);
      this.uni_id.set(value.id ?? null);
      return;
    }

    this.oldNames.clear();
    this.university_form.reset({
      name: '',
      shortName: '',
      oldNames: [],
    });
  }
  @Output() updated = new EventEmitter<void>();
  @Output() notify = new EventEmitter<SubmitNotification | null>();

  private readonly fb = inject(FormBuilder);
  private readonly universityApi = inject(UniversityService);
  private readonly destroyRef = inject(DestroyRef);

  hasControlError = hasControlError;
  readonly isEditing = signal(false);
  readonly isSubmitting = signal(false);

  university_form = this.fb.group({
    name: ['', Validators.required],
    shortName: ['', Validators.required],
    oldNames: this.fb.array<FormControl<string | null>>([]),
  });

  get oldNames(): FormArray<FormControl<string | null>> {
    return this.university_form.get('oldNames') as FormArray<FormControl<string | null>>;
  }

  addOldName(): void {
    this.oldNames.push(this.fb.control('', Validators.required));
  }

  removeOldName(index: number): void {
    this.oldNames.removeAt(index);
  }

  private fillForm(universityData: UniversitySearchItem): void {
    this.university_form.patchValue({
      name: universityData.name ?? '',
      shortName: universityData.shortName ?? '',
    });

    this.university_form.setControl(
      'oldNames',
      this.fb.array(
        (universityData.oldNames ?? []).map((name) => this.fb.control(name, Validators.required)),
      ),
    );
  }

  private buildCreatePayload(): UniversityCreateRequest {
    const raw = this.university_form.getRawValue();

    const oldNames = raw.oldNames
      .map((item) => toRequiredText(item))
      .filter((item) => item.length > 0);

    return {
      name: toRequiredText(raw.name),
      shortName: toRequiredText(raw.shortName),
      oldNames,
    };
  }

  private resetFormState(): void {
    this.oldNames.clear();
    this.university_form.reset({
      name: '',
      shortName: '',
      oldNames: [],
    });
    this.isEditing.set(false);
    this.uni_id.set(null);
  }

  deleteUniversity(): void {
    const universityId = this.uni_id();

    if (!this.isEditing() || !universityId || this.isSubmitting()) {
      return;
    }

    this.notify.emit(null);
    this.isSubmitting.set(true);

    this.universityApi
      .deleteUniversity(universityId)
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.resetFormState();
          this.updated.emit();

          this.notify.emit({
            tone: 'success',
            message: 'Университет успешно удален.',
          });
        },
        error: (error) => {
          this.notify.emit({
            tone: 'error',
            message: 'Ошибка при удалении университета. Пожалуйста, попробуйте снова.',
          });
        },
      });
  }

  onSubmit(): void {
    if (this.university_form.invalid || this.isSubmitting()) {
      this.university_form.markAllAsTouched();
      return;
    }

    const payload = this.buildCreatePayload();

    this.notify.emit(null);
    this.isSubmitting.set(true);

    if (this.isEditing()) {
      this.universityApi
        .updateUniversity(this.uni_id()!, payload)
        .pipe(
          finalize(() => this.isSubmitting.set(false)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          next: (uniResponse) => {
            if (uniResponse.status !== 'ok' || !uniResponse.id) {
              const errorCode = uniResponse.errorCode?.trim();
              this.notify.emit({
                tone: 'error',
                message: errorCode
                  ? `Ошибка при обновлении университета. Код: ${errorCode}.`
                  : 'Ошибка при обновлении университета.',
              });
              return;
            }

            this.resetFormState();
            this.updated.emit();

            this.notify.emit({
              tone: 'success',
              message: 'Университет успешно обновлен.',
            });
          },
          error: (error) => {
            this.notify.emit({
              tone: 'error',
              message: 'Ошибка при обновлении университета. Пожалуйста, попробуйте снова.',
            });
          },
        });
    } else {
      this.universityApi
        .createUniversity(payload)
        .pipe(
          finalize(() => this.isSubmitting.set(false)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          next: (uniResponse) => {
            if (uniResponse.status !== 'ok' || !uniResponse.id) {
              const errorCode = uniResponse.errorCode?.trim();
              this.notify.emit({
                tone: 'error',
                message: errorCode
                  ? `Ошибка при создании университета. Код: ${errorCode}.`
                  : 'Ошибка при создании университета.',
              });
              return;
            }

            this.resetFormState();
            this.notify.emit({
              tone: 'success',
              message: 'Университет успешно создан.',
            });
          },
          error: (error) => {
            this.notify.emit({
              tone: 'error',
              message: 'Ошибка при создании университета. Пожалуйста, попробуйте снова.',
            });
          },
        });
    }
  }
}
