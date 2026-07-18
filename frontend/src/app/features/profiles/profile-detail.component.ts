import { Component, OnInit, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ApiService, Profile } from '../../core/api.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'sa-profile-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './profile-detail.component.html',
})
export class ProfileDetailComponent implements OnInit, OnDestroy {
  profile = signal<Profile | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  
  private routeSubscription: Subscription | null = null;

  constructor(
    private readonly api: ApiService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.routeSubscription = this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.loadProfile(id);
      } else {
        this.error.set('ID de perfil no proporcionado');
        this.loading.set(false);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.routeSubscription) {
      this.routeSubscription.unsubscribe();
    }
  }

  loadProfile(id: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.api.getProfileById(id).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar perfil');
        this.loading.set(false);
        console.error('Error loading profile:', err);
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/profiles']);
  }
}
