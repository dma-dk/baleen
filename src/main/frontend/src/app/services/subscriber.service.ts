import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Subscriber {
  id: string;
  dataProductType: string;
  productVersion: string;
  containerType: string;
  unlocode: string;
  wkt: string;
  subscriptionStart: string;
  subscriptionEnd: string;
  nodeMrn: string;
}

@Injectable({
  providedIn: 'root'
})
export class SubscriberService {
  private apiUrl = '/api/subscribers';

  constructor(private http: HttpClient) {}

  getAllSubscribers(): Observable<Subscriber[]> {
    return this.http.get<Subscriber[]>(this.apiUrl);
  }

  clearAllSubscribers(): Observable<void> {
    return this.http.delete<void>(this.apiUrl);
  }
}