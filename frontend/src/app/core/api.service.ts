import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface HealthStatus {
  status: string;
}

export interface Profile {
  id: string;
  nombre: string;
  descripcion: string | null;
  foto: string | null;
  categoria: string;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  categoria: string;
  count: number;
}

/**
 * Thin wrapper around the backend REST API.
 * Nothing else in the frontend should build API URLs directly -
 * everything goes through here so the base URL only lives in one place.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = 'http://localhost:8080/api/v1';

  constructor(private readonly http: HttpClient) {}

  getHealth(): Observable<HealthStatus> {
    return this.http.get<HealthStatus>(`${this.baseUrl}/health`);
  }

  getProfiles(): Observable<Profile[]> {
    return this.http.get<Profile[]>(`${this.baseUrl}/profiles`);
  }

  getProfileById(id: string): Observable<Profile> {
    return this.http.get<Profile>(`${this.baseUrl}/profiles/${id}`);
  }

  getProfilesByCategory(categoria: string): Observable<Profile[]> {
    return this.http.get<Profile[]>(`${this.baseUrl}/profiles/by-category/${categoria}`);
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.baseUrl}/profiles/categories`);
  }
}
