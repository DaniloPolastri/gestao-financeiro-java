import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  CategoryGroupResponse, CreateCategoryGroupRequest, UpdateCategoryGroupRequest,
  CategoryResponse, CreateCategoryRequest, UpdateCategoryRequest,
} from '../models/category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/categories';

  private readonly _groups = signal<CategoryGroupResponse[]>([]);
  readonly groups = this._groups.asReadonly();

  loadGroups(): Observable<CategoryGroupResponse[]> {
    return this.http.get<CategoryGroupResponse[]>(`${this.API_URL}/groups`).pipe(
      tap((groups) => this._groups.set(groups)),
    );
  }

  createGroup(data: CreateCategoryGroupRequest): Observable<CategoryGroupResponse> {
    return this.http.post<CategoryGroupResponse>(`${this.API_URL}/groups`, data);
  }

  updateGroup(id: string, data: UpdateCategoryGroupRequest): Observable<CategoryGroupResponse> {
    return this.http.put<CategoryGroupResponse>(`${this.API_URL}/groups/${id}`, data);
  }

  deleteGroup(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/groups/${id}`);
  }

  createCategory(data: CreateCategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(this.API_URL, data);
  }

  updateCategory(id: string, data: UpdateCategoryRequest): Observable<CategoryResponse> {
    return this.http.put<CategoryResponse>(`${this.API_URL}/${id}`, data);
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
