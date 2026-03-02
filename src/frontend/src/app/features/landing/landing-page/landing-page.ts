import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Footer } from '../../../layout/footer/footer';
import { Header } from '../../../layout/header/header';

@Component({
  standalone: true,
  selector: 'app-landing-page',
  imports: [CommonModule, Footer, Header],
  templateUrl: './landing-page.html',
  styleUrls: ['./landing-page.scss'],
})
export class LandingPage {}
