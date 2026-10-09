import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AppComponent } from './app.component';
import { ReciclajeApiService } from './core/reciclaje-api.service';
import { UserProfile } from './core/dashboard.models';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [{
        provide: ReciclajeApiService,
        useValue: {
          currentUser: signal<UserProfile | null>(null),
          restoreSession: () => of(null),
          loginById: () => of({
            id: 1,
            identificador: 'ALUMNO-1',
            rol: 'ALUMNO',
            alumnoId: 1,
            colegioId: null,
            nombreMostrar: 'Ana'
          }),
          logout: () => of(void 0)
        }
      }]
    }).compileComponents();
  });

  it('should create the recycling learning experience', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Recicla bien');
    expect(fixture.nativeElement.textContent).toContain('¿Qué podemos reciclar?');
    expect(fixture.nativeElement.textContent).toContain('Mini prueba');
  });

  it('should show details for a selected material', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    fixture.componentInstance.selectMaterial(fixture.componentInstance.materials[0]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Tu material elegido');
    expect(fixture.nativeElement.textContent).toContain('Papel y cartón');
  });

  it('should open the ID login modal from the header button', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('.header-cta') as HTMLButtonElement;
    button.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ingresa con tu ID');
  });
});
