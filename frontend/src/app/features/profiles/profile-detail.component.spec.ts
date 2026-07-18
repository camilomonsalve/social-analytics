import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { Observable, of, delay } from 'rxjs';
import { fakeAsync, tick } from '@angular/core/testing';
import { ProfileDetailComponent } from './profile-detail.component';
import { ApiService } from '../../core/api.service';

describe('ProfileDetailComponent', () => {
  let component: ProfileDetailComponent;
  let fixture: ComponentFixture<ProfileDetailComponent>;
  let mockApiService: jasmine.SpyObj<ApiService>;
  let mockActivatedRoute: any;

  beforeEach(async () => {
    mockApiService = jasmine.createSpyObj('ApiService', ['getProfileById']);
    mockActivatedRoute = {
      paramMap: of({
        get: jasmine.createSpy('get').and.returnValue('test-id')
      })
    };

    await TestBed.configureTestingModule({
      imports: [ProfileDetailComponent],
      providers: [
        { provide: ApiService, useValue: mockApiService },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileDetailComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load profile on init with valid id', () => {
    const mockProfile = {
      id: 'test-id',
      nombre: 'Test Profile',
      descripcion: 'Test Description',
      foto: 'https://example.com/test.jpg',
      categoria: 'artistas',
      createdAt: '2024-01-01T00:00:00',
      updatedAt: '2024-01-01T00:00:00'
    };
    mockApiService.getProfileById.and.returnValue(of(mockProfile));

    component.ngOnInit();
    fixture.detectChanges();

    expect(mockApiService.getProfileById).toHaveBeenCalledWith('test-id');
    expect(component.profile()).toEqual(mockProfile);
    expect(component.loading()).toBe(false);
  });

  it('should handle error when id is missing', () => {
    mockActivatedRoute.paramMap = of({
      get: jasmine.createSpy('get').and.returnValue(null)
    });

    component.ngOnInit();
    fixture.detectChanges();

    expect(component.error()).toBe('ID de perfil no proporcionado');
    expect(component.loading()).toBe(false);
  });

  it('should handle loading state', fakeAsync(() => {
    const mockProfile = {
      id: 'test-id',
      nombre: 'Test Profile',
      descripcion: 'Test Description',
      foto: 'https://example.com/test.jpg',
      categoria: 'artistas',
      createdAt: '2024-01-01T00:00:00',
      updatedAt: '2024-01-01T00:00:00'
    };
    mockApiService.getProfileById.and.returnValue(of(mockProfile).pipe(delay(10)));

    fixture.detectChanges();
    component.ngOnInit();

    expect(component.loading()).toBe(true);
    tick(10);
    expect(component.loading()).toBe(false);
  }));

  it('should handle error state', () => {
    mockApiService.getProfileById.and.returnValue(
      new Observable(observer => observer.error('Error'))
    );

    component.ngOnInit();
    fixture.detectChanges();

    expect(component.error()).toBe('Error al cargar perfil');
    expect(component.loading()).toBe(false);
  });
});
