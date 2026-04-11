import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  Output,
  ViewChild,
} from '@angular/core';

@Component({
  selector: 'app-input-file',
  imports: [],
  templateUrl: './input-file.html',
  styleUrl: './input-file.scss',
})
export class InputFile {
  @Input() placeholder = 'Выберите из файлов';
  @Input() accept?: string;
  @Input() multiple = false;
  @Input() disabled = false;
  @Input()
  set resetVersion(_: number) {
    this.clearSelection();
  }

  @Output() fileChange = new EventEmitter<FileList | null>();

  @ViewChild('nativeInput') nativeInput?: ElementRef<HTMLInputElement>;

  readonly inputId = `input-file-${Math.random().toString(36).slice(2, 9)}`;
  fileName = '';
  isPickerOpen = false;
  isFileCorrect = true;

  onOpenPicker(): void {
    if (this.disabled) {
      return;
    }

    this.isPickerOpen = true;
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    this.isPickerOpen = false;

    if (!files || files.length === 0) {
      this.clearSelection();
      this.fileChange.emit(null);
      return;
    }

    for (const file of files) {
      if (!this.isAccepted(file)) {
        this.clearSelection();
        this.fileChange.emit(null);
        this.isFileCorrect = false;
        return;
      }
    }

    this.isFileCorrect = true;
    this.fileName = files.length === 1 ? files[0].name : `${files.length} файлов`;
    this.fileChange.emit(files);
  }

  private clearSelection(): void {
    this.isPickerOpen = false;
    this.fileName = '';
    if (this.nativeInput) {
      this.nativeInput.nativeElement.value = '';
    }
  }

  private isAccepted(file: File): boolean {
    if (!this.accept) return true;

    const fileType = (file.type || '').toLowerCase();
    const fileExt = file.name.toLowerCase().split('.').pop() ?? '';

    return this.accept
      .split(',')
      .map((s) => s.trim().toLowerCase())
      .some((token) => {
        if (!token) return false;

        if (token.startsWith('.')) {
          return fileExt === token.slice(1);
        }

        if (token.endsWith('/*')) {
          const group = token.slice(0, -1);
          return fileType.startsWith(group);
        }

        return fileType === token;
      });
  }

  @HostListener('window:focus')
  onWindowFocus(): void {
    if (this.isPickerOpen) {
      this.isPickerOpen = false;
    }
  }
}
