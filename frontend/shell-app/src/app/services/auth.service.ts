import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, catchError, throwError } from 'rxjs';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/api/v1';
  private tokenSubject = new BehaviorSubject<string | null>(null);
  private userSubject = new BehaviorSubject<any>(null);

  public token$ = this.tokenSubject.asObservable();
  public user$ = this.userSubject.asObservable();

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    if (isPlatformBrowser(this.platformId)) {
      const token = localStorage.getItem('token');
      if (token) {
        this.tokenSubject.next(token);
      }
    }
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    console.log('Attempting login to:', `${this.API_URL}/auth/login`);
    console.log('With credentials:', credentials);

    return this.http.post<AuthResponse>(`${this.API_URL}/auth/login`, credentials, { headers })
      .pipe(
        tap(response => {
          if (isPlatformBrowser(this.platformId)) {
            localStorage.setItem('token', response.accessToken);
          }
          this.tokenSubject.next(response.accessToken);
          console.log('Login successful:', response);
        }),
        catchError(error => {
          console.error('Login error:', error);
          if (error.error instanceof ProgressEvent) {
            console.error('Possible network/CORS issue or server didn’t respond.');
          }
          return throwError(() => error);
        })
      );
  }

  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('token');
    }
    this.tokenSubject.next(null);
    this.userSubject.next(null);
  }

  isAuthenticated(): boolean {
    return !!this.tokenSubject.value;
  }

  getToken(): string | null {
    return this.tokenSubject.value;
  }
}
