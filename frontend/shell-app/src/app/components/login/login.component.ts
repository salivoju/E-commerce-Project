import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  error = '';
  success = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      username: ['Sukesh@example.com', [Validators.required, Validators.email]],
      password: ['password123', [Validators.required, Validators.minLength(6)]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.loading = true;
      this.error = '';
      this.success = '';

      console.log('Attempting login with:', this.loginForm.value);

      this.authService.login(this.loginForm.value).subscribe({
        next: (response) => {
          console.log('Login successful:', response);
          this.success = 'Login successful! Token received: ' + response.accessToken.substring(0, 20) + '...';
          this.loading = false;
          // You can navigate to dashboard here later
          // this.router.navigate(['/dashboard']);
        },
        error: (error) => {
          console.error('Login failed:', error);
          if (error.status === 0) {
            this.error = 'Cannot connect to server. Please check if backend is running.';
          } else if (error.status === 401) {
            this.error = 'Invalid credentials. Please check your email and password.';
          } else {
            this.error = `Login failed: ${error.message || 'Unknown error'}`;
          }
          this.loading = false;
        }
      });
    } else {
      this.error = 'Please fill in all required fields correctly.';
    }
  }

  // Getter methods for form validation
  get username() { return this.loginForm.get('username'); }
  get password() { return this.loginForm.get('password'); }
}
