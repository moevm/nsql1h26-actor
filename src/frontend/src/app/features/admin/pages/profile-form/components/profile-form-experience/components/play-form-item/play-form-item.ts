import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-play-form-item',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './play-form-item.html',
  styleUrl: './play-form-item.scss',
})
export class PlayFormItem {
  @Input() playForm!: FormGroup;
  @Output() removePlay = new EventEmitter<void>();
}
