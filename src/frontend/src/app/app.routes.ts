import { Routes } from '@angular/router';
import { LandingPage } from './features/landing/pages/landing-page/landing-page';
import { MainLayout } from './layout/main-layout/main-layout';
import { SearchPage } from './features/search/pages/search-page/search-page';
import {
  AUTH_HEADER_STATE,
  LANDING_HEADER_STATE,
  SEARCH_HEADER_STATE,
  PROFILE_HEADER_STATE,
  ADMIN_DASHBOARD_STATE,
} from './layout/header/header-state';
import { AdminLoginPage } from './features/auth/pages/admin-login-page/admin-login-page';
import { ProfilePage } from './features/profile/pages/profile-page/profile-page';
import { AdminDashboard } from './features/admin/pages/admin-dashboard/admin-dashboard';
import { adminGuard } from './core/guards/admin-guard';
import { ImportExport } from './features/admin/pages/import-export/import-export';

import { actorResolver } from './core/resolvers/actor-resolver';
import { UniversitiesPage } from './features/admin/pages/universities-page/universities-page';

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
  {
    path: 'admin',
    component: MainLayout,
    children: [
      { path: '', component: AdminDashboard, data: { header: ADMIN_DASHBOARD_STATE } },
      { path: 'import-export', component: ImportExport, data: { header: ADMIN_DASHBOARD_STATE } },
      { path: 'universities', component: UniversitiesPage, data: { header: ADMIN_DASHBOARD_STATE } },
    ],
    canActivate: [adminGuard],
  },
  { path: '**', redirectTo: '' },
];
