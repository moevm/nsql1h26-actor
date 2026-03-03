import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-profile-card',
  imports: [],
  templateUrl: './profile-card.html',
  styleUrl: './profile-card.scss',
})
export class ProfileCard {
  @Input() fullName?: string;
  @Input() age?: string;
  @Input() education?: string;
  @Input() imageSrc?: string;
  @Input() imageAlt?: string;

  private readonly defaults = {
    fullName: 'Анастасия Сергеевна\nВоронцова',
    age: '24 года',
    education: 'Российский институт театрального искусства\nГИТИС',
    imageSrc: '/img/profilepic.jpg',
  };

  get resolvedFullName(): string {
    return this.fullName?.trim() || this.defaults.fullName;
  }

  get resolvedAge(): string {
    return this.age?.trim() || this.defaults.age;
  }

  get resolvedEducation(): string {
    return this.education?.trim() || this.defaults.education;
  }

  get resolvedImageSrc(): string {
    return this.imageSrc?.trim() || this.defaults.imageSrc;
  }

  get resolvedImageAlt(): string {
    const fallbackAlt = this.resolvedFullName.replace(/\n/g, ' ');
    return this.imageAlt?.trim() || fallbackAlt;
  }
}
