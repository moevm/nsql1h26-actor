import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormArray, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { PlayFormItem } from '../play-form-item/play-form-item';

@Component({
  selector: 'app-theatre-form-item',
  standalone: true,
  imports: [PlayFormItem, ReactiveFormsModule],
  templateUrl: './theatre-form-item.html',
  styleUrl: './theatre-form-item.scss',
})
export class TheatreFormItem {
  @Input() theatreForm!: FormGroup;
  @Output() addPlay = new EventEmitter<void>();
  @Output() removePlay = new EventEmitter<number>();
  @Output() removeTheatre = new EventEmitter<void>();
  readonly maxYear = new Date().getFullYear();
  readonly minYear = this.maxYear - 100;

  get plays(): FormArray<FormGroup> {
    return this.theatreForm.get('plays') as FormArray<FormGroup>;
  }

  get yearFrom(): string {
    const [yearFrom] = this.getYearsParts();
    return yearFrom;
  }

  get yearTo(): string {
    const [, yearTo] = this.getYearsParts();

    if (yearTo == '') {
      return String(this.maxYear);
    }

    return yearTo;
  }

  onYearFromInput(yearFrom: string): void {
    const [, yearTo] = this.getYearsParts();
    this.updateYears(yearFrom, yearTo);
  }

  onYearToInput(yearTo: string): void {
    const [yearFrom] = this.getYearsParts();
    this.updateYears(yearFrom, yearTo);
  }

  onYearsBlur(): void {
    this.theatreForm.get('years')?.markAsTouched();
  }

  hasYearsError(errorKey: string): boolean {
    const control = this.theatreForm.get('years');
    if (!control) {
      return false;
    }

    return control.hasError(errorKey) && (control.dirty || control.touched);
  }

  private getYearsParts(): [string, string] {
    const years = String(this.theatreForm.get('years')?.value ?? '');
    const parts = years.split(/[–-]/);

    return [parts[0]?.trim() || '', parts[1]?.trim() || ''];
  }

  private updateYears(yearFrom: string, yearTo: string): void {
    const start = yearFrom.trim();
    const end = yearTo.trim();
    const years = start || end ? `${start}-${end}` : '';
    this.theatreForm.get('years')?.setValue(years);
  }
}
