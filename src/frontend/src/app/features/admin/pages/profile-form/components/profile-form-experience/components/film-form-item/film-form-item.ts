import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-film-form-item',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './film-form-item.html',
  styleUrl: './film-form-item.scss',
})
export class FilmFormItem {
  @Input() filmForm!: FormGroup;
  @Output() removeFilm = new EventEmitter<void>();
  readonly maxYear = new Date().getFullYear();
  readonly minYear = this.maxYear - 100;
  

  hasYearError(errorKey: string): boolean {
    const control = this.filmForm.get('year');
    if (!control) {
      return false;
    }

    return control.hasError(errorKey) && (control.dirty || control.touched);
  }
}
