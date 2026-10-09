export type UserRole = 'ALUMNO' | 'PROFESOR' | 'INSTITUCION' | 'ADMIN';

export interface UserProfile {
  id: number;
  identificador: string;
  rol: UserRole;
  alumnoId: number | null;
  colegioId: number | null;
  nombreMostrar: string;
}

export interface PointEntry {
  id: number;
  puntos: number;
  motivo: string;
  fecha: string;
}

export interface StudentDashboardData {
  alumnoId: number;
  nombreCompleto: string;
  puntosTotal: number;
  movimientos: PointEntry[];
}

export interface TeacherStudent {
  alumnoId: number;
  nombreCompleto: string;
  puntosTotal: number;
}

export interface StudentWinner {
  alumnoId: number;
  nombreCompleto: string;
  puntosTotal: number;
}
