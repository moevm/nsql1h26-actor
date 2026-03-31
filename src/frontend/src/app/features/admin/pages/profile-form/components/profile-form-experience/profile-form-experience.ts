import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormArray, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { FilmFormItem } from './components/film-form-item/film-form-item';
import { TheatreFormItem } from './components/theatre-form-item/theatre-form-item';

@Component({
  selector: 'app-profile-form-experience',
  standalone: true,
  imports: [FilmFormItem, TheatreFormItem, ReactiveFormsModule],
  templateUrl: './profile-form-experience.html',
  styleUrl: './profile-form-experience.scss',
})
export class ProfileFormExperience {
  @Input() films!: FormArray<FormGroup>;
  @Input() theatres!: FormArray<FormGroup>;

  @Output() addFilm = new EventEmitter<void>();
  @Output() removeFilm = new EventEmitter<number>();
  @Output() addTheatre = new EventEmitter<void>();
  @Output() removeTheatre = new EventEmitter<number>();
  @Output() addPlay = new EventEmitter<number>();
  @Output() removePlay = new EventEmitter<{ theatreIndex: number; playIndex: number }>();
}
