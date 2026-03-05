import { Routes } from '@angular/router';
import { LandingPage } from './features/landing/pages/landing-page';
import { MainLayout } from './layout/main-layout/main-layout';
import { SearchPage } from './features/search/pages/search-page/search-page';

export const routes: Routes = [
  {
    path: '',
    component: MainLayout,
    children: [
      { path: '', component: LandingPage },
      { path: 'search', component: SearchPage },
    ],
  },
  { path: '**', redirectTo: '' },
];
