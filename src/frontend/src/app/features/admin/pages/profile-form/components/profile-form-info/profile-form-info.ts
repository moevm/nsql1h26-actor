import {
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
  signal,
} from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { components } from '../../../../../../shared/api/types';
import { UniversitySearch } from '../../../../../../shared/ui/university-search/university-search';
import { hasControlError, isAccepted } from '../../../../../../shared/utils/formHeplers';
import {
  EditableMedia,
  actorRankOptions,
  eyeColorOptions,
  genderOprions,
  genreOptions,
  hairColorOptions,
} from '../../profile-form.model';

type EducationItem = components['schemas']['EducationItem'];

@Component({
  selector: 'app-profile-form-info',
  imports: [UniversitySearch, ReactiveFormsModule],
  templateUrl: './profile-form-info.html',
  styleUrl: './profile-form-info.scss',
})
export class ProfileFormInfo implements OnChanges, OnDestroy {
  readonly hairColorOptions = hairColorOptions;
  readonly eyeColorOptions = eyeColorOptions;
  readonly actorRankOptions = actorRankOptions;
  readonly genderOprions = genderOprions;
  readonly genreOptions = genreOptions;
  readonly accept = '.png,.jpg,.jpeg,.gif,.webp';

  previewUrl = signal('');
  isFileCorrect = true;
  @ViewChild('profilePicInput') profilePicInput?: ElementRef<HTMLInputElement>;

  @Input() profile_form!: FormGroup;
  @Input() alreadyExistsUni: EducationItem[] | null = null;
  hasControlError = hasControlError;

  private mainPhotoSub?: Subscription;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['profile_form']) {
      this.bindMainPhotoControl();
    }
  }

  toggleGenre(genre: string, checked: boolean): void {
    const current = this.profile_form.get('genres')?.value ?? [];
    this.profile_form
      .get('genres')
      ?.setValue(
        checked ? [...new Set([...current, genre])] : current.filter((g: string) => g !== genre),
      );
  }

  isGenreSelected(genre: string): boolean {
    return (this.profile_form.get('genres')?.value ?? []).includes(genre);
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (!file) {
      this.clearMainPhoto();
      return;
    }

    if (!isAccepted(file, this.accept)) {
      this.clearMainPhoto();
      this.isFileCorrect = false;
      return;
    }

    this.isFileCorrect = true;
    this.mainPhotoControl?.setValue(this.toNewMedia(file));
    this.mainPhotoControl?.markAsTouched();
  }

  removePreview(): void {
    this.clearMainPhoto();
    this.resetNativeInput();
    this.mainPhotoControl?.markAsTouched();
  }

  ngOnDestroy(): void {
    this.mainPhotoSub?.unsubscribe();
    this.revokePreviewUrl();
  }

  onUniversityIdChange(universityIds: string[] | null): void {
    this.profile_form.get('education')?.setValue((universityIds ?? []).map((uniId) => ({ uniId })));
  }

  private get mainPhotoControl() {
    return this.profile_form.get('mainPhoto');
  }

  private bindMainPhotoControl(): void {
    this.mainPhotoSub?.unsubscribe();
    this.syncPreview(this.mainPhotoControl?.value as EditableMedia | null);
    this.mainPhotoSub = this.mainPhotoControl?.valueChanges.subscribe((media) => {
      this.syncPreview(media as EditableMedia | null);
    });
  }

  private clearMainPhoto(): void {
    this.mainPhotoControl?.setValue(null);
  }

  private syncPreview(media: EditableMedia | null): void {
    if (this.previewUrl() === (media?.url ?? '')) {
      return;
    }

    this.revokePreviewUrl();
    this.previewUrl.set(media?.url ?? '');
  }

  private toNewMedia(file: File): EditableMedia {
    return {
      kind: 'new',
      tempId: `${file.name}-${file.lastModified}-${file.size}-${Math.random().toString(36).slice(2, 9)}`,
      file,
      url: URL.createObjectURL(file),
      caption: file.name,
      type: 'photo',
    };
  }

  private revokePreviewUrl(): void {
    const currentPreviewUrl = this.previewUrl();
    if (!currentPreviewUrl) {
      return;
    }

    URL.revokeObjectURL(currentPreviewUrl);
    this.previewUrl.set('');
  }

  private resetNativeInput(): void {
    if (!this.profilePicInput) {
      return;
    }

    this.profilePicInput.nativeElement.value = '';
  }
}
