import { Routes } from '@angular/router';
import { LandingPage } from './features/landing/landing-page/landing-page';
import { MainLayout } from './layout/main-layout/main-layout';

export const routes: Routes = [
  {
    path: '',
    component: MainLayout,
    children: [
      {
        path: '',
        component: LandingPage,
      }
    ]
  },
];
