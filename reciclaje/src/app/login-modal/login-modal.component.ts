import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login-modal.component.html',
  styleUrl: './login-modal.component.css'
})
export class LoginModalComponent {
  @Input() busy = false;
  @Input() errorMessage = '';
  @Output() submitted = new EventEmitter<string>();
  @Output() closed = new EventEmitter<void>();

  identifier = '';

  submit(): void {
    const identifier = this.identifier.trim();
    if (identifier && !this.busy) {
      this.submitted.emit(identifier);
    }
  }
}
