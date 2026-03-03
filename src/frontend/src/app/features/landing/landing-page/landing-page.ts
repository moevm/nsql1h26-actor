import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProfileCard } from '../../../shared/ui/profile-card/profile-card';

@Component({
  standalone: true,
  selector: 'app-landing-page',
  imports: [CommonModule, ProfileCard],
  templateUrl: './landing-page.html',
  styleUrls: ['./landing-page.scss'],
})
export class LandingPage {
  readonly cardSlots = Array.from({ length: 8 });
}
