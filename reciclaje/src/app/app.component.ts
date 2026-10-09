import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { LoginModalComponent } from './login-modal/login-modal.component';
import { ReciclajeApiService } from './core/reciclaje-api.service';
import { StudentDashboardComponent } from './student-dashboard/student-dashboard.component';
import { TeacherDashboardComponent } from './teacher-dashboard/teacher-dashboard.component';

interface Material {
  name: string;
  icon: string;
  color: string;
  description: string;
  recycle: string;
}

interface ChallengeItem {
  icon: string;
  label: string;
  correct: boolean;
  options: Array<{ icon: string; label: string; correct: boolean }>;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    LoginModalComponent,
    StudentDashboardComponent,
    TeacherDashboardComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'Recicla con corazón';
  activeSection = 'inicio';
  loginModalOpen = false;
  loginBusy = false;
  loginError = '';
  connectionError = '';
  selectedMaterial: Material | null = null;
  quizAnswered = false;
  quizCorrect = false;
  challengeStep = 0;
  challengeComplete = false;
  challengeFeedback = '';
  challengeItems: ChallengeItem[] = [
    {
      icon: '🥤',
      label: 'Bebida vacía',
      correct: true,
      options: [
        { icon: '🥤', label: 'Bebida vacía', correct: true },
        { icon: '🍌', label: 'Banana', correct: false },
        { icon: '📦', label: 'Cajón de cartón', correct: false }
      ]
    },
    {
      icon: '📦',
      label: 'Cajón de cartón',
      correct: true,
      options: [
        { icon: '📦', label: 'Cajón de cartón', correct: true },
        { icon: '🥫', label: 'Lata de metal', correct: false },
        { icon: '🍷', label: 'Vaso roto', correct: false }
      ]
    },
    {
      icon: '🍌',
      label: 'Banana',
      correct: true,
      options: [
        { icon: '🍌', label: 'Banana', correct: false },
        { icon: '🌱', label: 'Compost', correct: true },
        { icon: '🥤', label: 'Botella vacía', correct: false }
      ]
    }
  ];

  constructor(public readonly session: ReciclajeApiService) {}

  ngOnInit(): void {
    this.session.restoreSession().subscribe({
      error: () => {
        this.connectionError = 'No se pudo conectar con el servidor. Verifica que el backend esté iniciado.';
      }
    });
  }

  login(identifier: string): void {
    this.loginBusy = true;
    this.loginError = '';
    this.session.loginById(identifier).subscribe({
      next: () => {
        this.loginBusy = false;
        this.loginModalOpen = false;
        this.connectionError = '';
      },
      error: (error: HttpErrorResponse) => {
        this.loginBusy = false;
        this.loginError = error.status === 401 || error.status === 404
          ? 'No encontramos una cuenta activa con ese ID.'
          : 'No se pudo iniciar sesión. Inténtalo de nuevo en un momento.';
      }
    });
  }

  logout(): void {
    this.session.logout().subscribe({
      error: () => {
        this.connectionError = 'No se pudo cerrar la sesión en el servidor. Inténtalo de nuevo.';
      }
    });
  }

  get currentChallengeOptions() {
    return this.challengeItems[this.challengeStep].options;
  }

  materials: Material[] = [
    {
      name: 'Papel y cartón',
      icon: '📄',
      color: '#ffbf3f',
      description: 'Revistas, cajas y cartulinas pueden convertirse en nuevos objetos.',
      recycle: 'Reciclar'
    },
    {
      name: 'Plástico',
      icon: '🥤',
      color: '#4bc7c9',
      description: 'Botellas y envases claros deben ir a los recipientes de plástico.',
      recycle: 'Reciclar'
    },
    {
      name: 'Metal',
      icon: '🥫',
      color: '#7d8cff',
      description: 'Lata, aluminio y metal pueden reciclarse y hacer nuevos productos.',
      recycle: 'Reciclar'
    },
    {
      name: 'Vidrio',
      icon: '🍷',
      color: '#62d98b',
      description: 'Botellas y jarrones de vidrio pueden volver a la vida.',
      recycle: 'Reciclar'
    },
    {
      name: 'Organicos',
      icon: '🍌',
      color: '#f6d94d',
      description: 'Comida, café y restos de jardín ayudan a crear compost.',
      recycle: 'Compost'
    }
  ];

  selectMaterial(material: Material): void {
    this.selectedMaterial = material;
    document.getElementById('material-detail')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  answerQuiz(answer: boolean): void {
    this.quizAnswered = true;
    this.quizCorrect = answer;
  }

  chooseChallenge(item: { icon: string; label: string; correct: boolean }): void {
    if (item.correct) {
      this.challengeFeedback = '¡Correcto! Esto se recicla en el contenedor adecuado.';
      if (this.challengeStep < this.challengeItems.length - 1) {
        this.challengeStep++;
      } else {
        this.challengeComplete = true;
      }
    } else {
      this.challengeFeedback = 'No exactamente. Piensa en el material y en su contenedor.';
    }
  }

  restartChallenge(): void {
    this.challengeStep = 0;
    this.challengeComplete = false;
    this.challengeFeedback = '';
  }

  scrollTo(section: string): void {
    this.activeSection = section;
    document.getElementById(section)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
