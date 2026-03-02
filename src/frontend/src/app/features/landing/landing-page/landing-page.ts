import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Footer } from '../../../layout/footer/footer';

@Component({
  standalone: true,
  selector: 'app-landing-page',
  imports: [CommonModule, Footer],
  templateUrl: './landing-page.html',
  styleUrls: ['./landing-page.scss'],
})
export class LandingPage {}
