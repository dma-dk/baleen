import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface S124Dataset {
  id: number;
  mrn: string;
  uuid: string;
  createdAt: string;
  validFrom: string;
  validTo: string;
  dataProductVersion: string;
  geometryWkt: string;
  referencedDatasetIds: number[];
}

export interface S124DatasetDetail extends S124Dataset {
  gml: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class S124DatasetService {
  private apiUrl = '/api/s124-datasets';

  constructor(private http: HttpClient) { }

  getDatasets(page: number = 0, size: number = 20, sortBy: string = 'createdAt', sortDirection: string = 'DESC'): Observable<Page<S124Dataset>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDirection', sortDirection);

    return this.http.get<Page<S124Dataset>>(this.apiUrl, { params });
  }

  getDataset(id: number): Observable<S124Dataset> {
    return this.http.get<S124Dataset>(`${this.apiUrl}/${id}`);
  }

  getDatasetDetails(id: number): Observable<S124DatasetDetail> {
    return this.http.get<S124DatasetDetail>(`${this.apiUrl}/${id}/details`);
  }

  /**
   * The number of datasets SECOM is serving, which comes from the live Niord poll and not from the stored datasets
   * this service otherwise lists - the two are not kept in sync.
   */
  getServedDatasetCount(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/count/served`);
  }

  getNiordStatus(): Observable<{ configured: boolean }> {
    return this.http.get<{ configured: boolean }>(`${this.apiUrl}/niord-status`);
  }

  clearAllDatasets(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/clear`);
  }

  reloadFromNiord(): Observable<{ success: boolean; datasetsLoaded: number; message: string }> {
    return this.http.post<{ success: boolean; datasetsLoaded: number; message: string }>(
      `${this.apiUrl}/reload-from-niord`, 
      {}
    );
  }
}