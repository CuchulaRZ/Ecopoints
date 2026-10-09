import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ReciclajeApiService } from '../core/reciclaje-api.service';
import { StudentDashboardData } from '../core/dashboard.models';

@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './student-dashboard.component.html',
  styleUrl: './student-dashboard.component.css'
})
export class StudentDashboardComponent implements OnInit {
  @Output() logout = new EventEmitter<void>();

  dashboard: StudentDashboardData | null = null;
  loading = true;
  errorMessage = '';

  constructor(private readonly api: ReciclajeApiService) {}

  ngOnInit(): void {
    this.api.getStudentDashboard().subscribe({
      next: dashboard => {
        this.dashboard = dashboard;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudo cargar tu historial de puntos. Inténtalo de nuevo.';
        this.loading = false;
      }
    });
  }
}
