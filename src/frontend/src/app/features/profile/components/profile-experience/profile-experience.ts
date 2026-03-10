import { Component, Input } from '@angular/core';
import { components } from '../../../../shared/api/types';

type TheatrePlayItem = components['schemas']['TheatrePlayItem'];
type FilmPlayItem = components['schemas']['FilmPlayItem'];

@Component({
  selector: 'app-profile-experience',
  imports: [],
  templateUrl: './profile-experience.html',
  styleUrl: './profile-experience.scss',
})
export class ProfileExperience {
  @Input() theatrePlayList: TheatrePlayItem[] = [];
  @Input() filmPlayList: FilmPlayItem[] = [];
}
