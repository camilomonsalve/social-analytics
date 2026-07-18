import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { ApiService, Profile } from '../../core/api.service';

@Component({
  selector: 'sa-profile-list',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './profile-list.component.html',
  styles: [`
    .profile-card {
      transition: transform 0.2s;
    }
    .profile-card:hover {
      transform: scale(1.02);
    }
  `]
})
export class ProfileListComponent implements OnInit {
  profiles = signal<Profile[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  selectedCategory = signal<string>('all');

  categories = [
    { value: 'all', label: 'Todos' },
    { value: 'artistas', label: 'Artistas' },
    { value: 'empresas', label: 'Empresas' },
    { value: 'medios', label: 'Medios' },
    { value: 'politica', label: 'Política' },
  ];

  constructor(
    private readonly api: ApiService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.loadProfiles();
  }

  loadProfiles(): void {
    this.loading.set(true);
    this.error.set(null);

    const category = this.selectedCategory();
    const request$ = category === 'all'
      ? this.api.getProfiles()
      : this.api.getProfilesByCategory(category);

    request$.subscribe({
      next: (profiles) => {
        this.profiles.set(profiles);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar perfiles');
        this.loading.set(false);
        console.error('Error loading profiles:', err);
      },
    });
  }

  onCategoryChange(category: string): void {
    this.selectedCategory.set(category);
    this.loadProfiles();
  }

  viewProfile(id: string): void {
    this.router.navigate(['/profiles', id]);
  }
}
