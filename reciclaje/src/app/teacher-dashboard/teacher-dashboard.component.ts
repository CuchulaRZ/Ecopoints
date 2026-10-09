import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ReciclajeApiService } from '../core/reciclaje-api.service';
import { StudentWinner, TeacherStudent } from '../core/dashboard.models';

@Component({
  selector: 'app-teacher-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './teacher-dashboard.component.html',
  styleUrl: './teacher-dashboard.component.css'
})
export class TeacherDashboardComponent implements OnInit {
  @Output() logout = new EventEmitter<void>();

  students: TeacherStudent[] = [];
  modal: 'student' | 'points' | null = null;
  loading = true;
  saving = false;
  errorMessage = '';
  statusMessage = '';
  studentName = '';
  studentSurname = '';
  selectedStudentId: number | null = null;
  points = 10;
  reason = '';
  winner: StudentWinner | null = null;
  showingCurtain = false;
  curtainOpen = false;
  loadingWinner = false;

  constructor(private readonly api: ReciclajeApiService) {}

  ngOnInit(): void {
    this.loadStudents();
  }

  openModal(modal: 'student' | 'points'): void {
    this.errorMessage = '';
    this.statusMessage = '';
    this.modal = modal;
    if (modal === 'points') {
      this.selectedStudentId = this.students[0]?.alumnoId ?? null;
    }
  }

  closeModal(): void {
    if (!this.saving) {
      this.modal = null;
      this.errorMessage = '';
    }
  }

  registerStudent(): void {
    if (!this.studentName.trim() || !this.studentSurname.trim()) {
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.api.registerStudent({
      nombre: this.studentName.trim(),
      apellido: this.studentSurname.trim()
    }).subscribe({
      next: student => {
        this.students = [...this.students, student].sort((first, second) =>
          first.nombreCompleto.localeCompare(second.nombreCompleto));
        this.statusMessage = `Alumno ${student.nombreCompleto} registrado correctamente. Su ID es ${student.alumnoId}.`;
        this.resetStudentForm();
      },
      error: error => {
        switch (error.status) {
          case 0:
            this.errorMessage = 'No se pudo conectar con el servidor. Verifica que el backend esté iniciado.';
            break;
          case 400:
            this.errorMessage = 'El backend rechazó los datos del alumno. Reinicia el backend para aplicar la versión actualizada.';
            break;
          case 401:
          case 403:
            this.errorMessage = 'Tu sesión venció o no tienes permiso. Cierra sesión e ingresa de nuevo.';
            break;
          case 422:
            this.errorMessage = this.getApiErrorDetail(error.error)
              ?? 'El profesor no tiene un salón asignado. Asigna un salón al profesor y vuelve a intentarlo.';
            break;
          case 409:
            this.errorMessage = this.getApiErrorDetail(error.error)
              ?? 'El servidor rechazó la asociación del alumno. Revisa la consola del backend para ver el detalle de Oracle.';
            break;
          default:
            this.errorMessage = `No se pudo registrar al alumno (error ${error.status || 'desconocido'}). Revisa la consola del backend.`;
        }
        this.saving = false;
      }
    });
  }

  private getApiErrorDetail(response: unknown): string | null {
    let payload = response;
    if (typeof response === 'string') {
      try {
        payload = JSON.parse(response) as unknown;
      } catch {
        return response.trim() || null;
      }
    }
    if (!this.isRecord(payload)) {
      return null;
    }
    for (const field of ['detail', 'message', 'reason', 'error']) {
      const value = payload[field];
      if (typeof value === 'string' && value.trim()) {
        return value;
      }
    }
    return null;
  }

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
  }

  assignPoints(): void {
    if (this.selectedStudentId === null || !this.reason.trim() || this.points < 1 || this.points > 10000) {
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.api.awardPoints({
      alumnoId: this.selectedStudentId,
      puntos: this.points,
      motivo: this.reason.trim()
    }).subscribe({
      next: () => {
        const student = this.students.find(item => item.alumnoId === this.selectedStudentId);
        if (student) {
          student.puntosTotal += this.points;
        }
        this.statusMessage = `${this.points} puntos asignados${student ? ` a ${student.nombreCompleto}` : ''}.`;
        this.resetPointsForm();
      },
      error: error => {
        this.errorMessage = error.status === 404
          ? 'El alumno seleccionado ya no pertenece a este colegio.'
          : 'No se pudieron asignar los puntos. Inténtalo de nuevo.';
        this.saving = false;
      }
    });
  }

  showWinner(): void {
    if (this.loadingWinner) {
      return;
    }
    this.loadingWinner = true;
    this.errorMessage = '';
    this.api.getStudentWinnerLast30Days().subscribe({
      next: winner => {
        this.winner = winner;
        this.showingCurtain = true;
        this.curtainOpen = false;
        this.loadingWinner = false;
      },
      error: () => {
        this.errorMessage = 'No se pudo consultar al alumno con más puntos. Inténtalo de nuevo.';
        this.loadingWinner = false;
      }
    });
  }

  openCurtain(): void {
    this.curtainOpen = true;
  }

  closeCurtain(): void {
    this.showingCurtain = false;
    this.curtainOpen = false;
  }

  private loadStudents(): void {
    this.loading = true;
    this.api.getTeacherStudents().subscribe({
      next: students => {
        this.students = students;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudo cargar la lista de alumnos.';
        this.loading = false;
      }
    });
  }

  private resetStudentForm(): void {
    this.studentName = '';
    this.studentSurname = '';
    this.saving = false;
    this.modal = null;
  }

  private resetPointsForm(): void {
    this.points = 10;
    this.reason = '';
    this.saving = false;
    this.modal = null;
  }
}
