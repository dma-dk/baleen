import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  providers: [CookieService],
  template: `
    <router-outlet></router-outlet>
  `,
  styles: []
})
export class AppComponent {
  title = 'Baleen Frontend';
  private cookieService = inject(CookieService);

  constructor() {
    // Example: Set a cookie
    this.cookieService.set('user-preference', 'dark-mode', 7); // expires in 7 days

    // Example: Get a cookie
    const preference = this.cookieService.get('user-preference');
    console.log('User preference:', preference);

    // Example: Check if cookie exists
    const cookieExists = this.cookieService.check('user-preference');
    console.log('Cookie exists:', cookieExists);

    // Example: Get all cookies
    const allCookies = this.cookieService.getAll();
    console.log('All cookies:', allCookies);
  }
}