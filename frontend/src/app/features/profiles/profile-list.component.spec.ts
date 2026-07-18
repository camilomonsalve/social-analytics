import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, delay } from 'rxjs';
import { fakeAsync, tick } from '@angular/core/testing';
import { ProfileListComponent } from './profile-list.component';
import { ApiService } from '../../core/api.service';

describe('ProfileListComponent', () => {
  let component: ProfileListComponent;
  let fixture: ComponentFixture<ProfileListComponent>;
  let mockApiService: jasmine.SpyObj<ApiService>;

  beforeEach(async () => {
    mockApiService = jasmine.createSpyObj('ApiService', ['getProfiles', 'getProfilesByCategory']);

    await TestBed.configureTestingModule({
      imports: [ProfileListComponent],
      providers: [
        { provide: ApiService, useValue: mockApiService },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load profiles on init', () => {
    const mockProfiles = [
      {
        id: '1',
        nombre: 'Test Profile',
        descripcion: 'Test Description',
        foto: 'https://example.com/test.jpg',
        categoria: 'artistas',
        createdAt: '2024-01-01T00:00:00',
        updatedAt: '2024-01-01T00:00:00'
      }
    ];
    mockApiService.getProfiles.and.returnValue(of(mockProfiles));

    component.ngOnInit();
    fixture.detectChanges();

    expect(mockApiService.getProfiles).toHaveBeenCalled();
    expect(component.profiles()).toEqual(mockProfiles);
    expect(component.loading()).toBe(false);
  });

  it('should handle loading state', fakeAsync(() => {
    mockApiService.getProfiles.and.returnValue(of([]).pipe(delay(10)));

    fixture.detectChanges();
    component.ngOnInit();

    expect(component.loading()).toBe(true);
    tick(10);
    expect(component.loading()).toBe(false);
  }));

  it('should handle error state', () => {
    mockApiService.getProfiles.and.returnValue(
      new Observable(observer => observer.error('Error'))
    );

    component.ngOnInit();
    fixture.detectChanges();

    expect(component.error()).toBe('Error al cargar perfiles');
    expect(component.loading()).toBe(false);
  });

  it('should filter by category', () => {
    const mockProfiles = [
      {
        id: '1',
        nombre: 'Test Artist',
        descripcion: 'Test Description',
        foto: 'https://example.com/test.jpg',
        categoria: 'artistas',
        createdAt: '2024-01-01T00:00:00',
        updatedAt: '2024-01-01T00:00:00'
      }
    ];
    mockApiService.getProfilesByCategory.and.returnValue(of(mockProfiles));

    component.onCategoryChange('artistas');
    fixture.detectChanges();

    expect(mockApiService.getProfilesByCategory).toHaveBeenCalledWith('artistas');
    expect(component.selectedCategory()).toBe('artistas');
  });
});
