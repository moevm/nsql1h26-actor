import { Routes } from '@angular/router';
import { LandingPage } from './features/landing/pages/landing-page';
import { MainLayout } from './layout/main-layout/main-layout';
import { SearchPage } from './features/search/pages/search-page/search-page';
import {
  AUTH_HEADER_STATE,
  LANDING_HEADER_STATE,
  SEARCH_HEADER_STATE,
  PROFILE_HEADER_STATE
} from './layout/header/header-state';
import { AdminLoginPage } from './features/auth/admin-login-page/admin-login-page';
import { ProfilePage } from './features/profile/page/profile-page/profile-page';
import { actorResolver } from './core/resolvers/actor-resolver';

export const routes: Routes = [
  {
    path: '',
    component: MainLayout,
    children: [
      { path: '', component: LandingPage, data: { header: LANDING_HEADER_STATE } },
      { path: 'search', component: SearchPage, data: { header: SEARCH_HEADER_STATE } },
      { path: 'auth', component: AdminLoginPage, data: { header: AUTH_HEADER_STATE } },
      {
        path: 'profile/:id',
        component: ProfilePage,
        resolve: { actor: actorResolver },
        data: { header: PROFILE_HEADER_STATE },
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
