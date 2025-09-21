import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout.component';
import { HomeComponent } from './home/home.component';
import { SubscribersComponent } from './subscribers/subscribers.component';
import { S124DatasetsComponent } from './s124-datasets/s124-datasets.component';
import { NiordComponent } from './niord/niord.component';
import { LoggingComponent } from './logging/logging.component';
import { DatabaseComponent } from './database/database.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: 'home', component: HomeComponent },
      { path: 'subscribers', component: SubscribersComponent },
      { path: 's124-datasets', component: S124DatasetsComponent },
      { path: 'niord', component: NiordComponent },
      { path: 'logging', component: LoggingComponent },
      { path: 'database', component: DatabaseComponent },
      { path: '', redirectTo: '/home', pathMatch: 'full' }
    ]
  }
];