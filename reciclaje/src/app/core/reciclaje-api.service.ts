import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, switchMap, tap, throwError } from 'rxjs';
import {
  StudentDashboardData,
  StudentWinner,
  TeacherStudent,
  UserProfile
} from './dashboard.models';

@Injectable({ providedIn: 'root' })
export class ReciclajeApiService {
  readonly currentUser = signal<UserProfile | null>(null);

  constructor(private readonly http: HttpClient) {}

  restoreSession(): Observable<UserProfile | null> {
    return this.http.get<UserProfile>('/api/auth/me', { withCredentials: true }).pipe(
      tap(user => this.currentUser.set(user)),
      catchError(error => {
        this.currentUser.set(null);
        return error.status === 401 ? of(null) : throwError(() => error);
      })
    );
  }

  loginById(identificador: string): Observable<UserProfile> {
    return this.http.get<{ token: string }>('/api/auth/csrf', { withCredentials: true }).pipe(
      switchMap(({ token }) => this.http.post<UserProfile>(
        '/api/auth/login-id',
        { identificador },
        { withCredentials: true, headers: { 'X-XSRF-TOKEN': token } }
      )),
      tap(user => this.currentUser.set(user))
    );
  }

  logout(): Observable<void> {
    return this.http.get<{ token: string }>('/api/auth/csrf', { withCredentials: true }).pipe(
      switchMap(({ token }) => this.http.post<void>(
        '/api/auth/logout',
        {},
        { withCredentials: true, headers: { 'X-XSRF-TOKEN': token } }
      )),
      tap(() => this.currentUser.set(null))
    );
  }

  getStudentDashboard(): Observable<StudentDashboardData> {
    return this.http.get<StudentDashboardData>('/api/alumno/puntos', { withCredentials: true });
  }

  getTeacherStudents(): Observable<TeacherStudent[]> {
    return this.http.get<TeacherStudent[]>('/api/docente/alumnos', { withCredentials: true });
  }

  getStudentWinnerLast30Days(): Observable<StudentWinner | null> {
    return this.http.get<StudentWinner | null>(
      '/api/docente/alumnos/ganador-30-dias',
      { withCredentials: true }
    );
  }

  registerStudent(student: { nombre: string; apellido: string }): Observable<TeacherStudent> {
    return this.http.get<{ token: string }>('/api/auth/csrf', { withCredentials: true }).pipe(
      switchMap(({ token }) => this.http.post<TeacherStudent>(
        '/api/docente/alumnos',
        student,
        { withCredentials: true, headers: { 'X-XSRF-TOKEN': token } }
      ))
    );
  }

  awardPoints(award: { alumnoId: number; puntos: number; motivo: string }): Observable<void> {
    return this.http.get<{ token: string }>('/api/auth/csrf', { withCredentials: true }).pipe(
      switchMap(({ token }) => this.http.post<void>(
        '/api/docente/puntos',
        award,
        { withCredentials: true, headers: { 'X-XSRF-TOKEN': token } }
      ))
    );
  }
}
