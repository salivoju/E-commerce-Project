import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { LoginRequest } from '../models/login-request';
import { Observable } from 'rxjs';
import { LoginResponse } from '../models/login-response';

const BASE_URL = "http://localhost:8080/api";

@Injectable({
  providedIn: 'root'
})
export class IntegrationService {

  constructor(private http:HttpClient) { }

  doLogin(request:LoginRequest):Observable<LoginRequest>{
    return this.http.post<LoginResponse>(BASE_URL + "/login",request)}

  dashboard(): Observable<any> {
       return this.http.get<any>(BASE_URL + "/dashboard");
  }
}
