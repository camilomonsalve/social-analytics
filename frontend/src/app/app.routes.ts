import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ProfileListComponent } from './features/profiles/profile-list.component';
import { ProfileDetailComponent } from './features/profiles/profile-detail.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'profiles', component: ProfileListComponent },
  { path: 'profiles/:id', component: ProfileDetailComponent },
];
