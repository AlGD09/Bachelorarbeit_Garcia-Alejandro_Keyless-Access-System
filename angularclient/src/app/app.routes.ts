import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { SmartphoneComponent } from './components/smartphone/smartphone.component';
import { SmartphoneAssignComponent } from './components/smartphone/smartphone-assign.component';
import { RcuComponent } from './components/rcu/rcu.component';
import { UserComponent } from './components/user/user.component';
import { RcuAssignComponent } from './components/rcu/rcu-assign.component';
import { HomeComponent } from './components/home/home.component';
import { EinheitenComponent } from './components/einheiten/einheiten.component';
import { ZuweisungenComponent } from './components/zuweisungen/zuweisungen.component';


export const routes: Routes = [
  { path: 'home', component: HomeComponent },
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'user', component: UserComponent },
  { path: 'smartphone', component: SmartphoneComponent },
  { path: 'smartphone/assign', component: SmartphoneAssignComponent },
  { path: 'maschine', component: RcuComponent},
  { path: 'maschine/assign', component: RcuAssignComponent},
  { path: 'einheiten', component: EinheitenComponent},
  { path: 'zuweisungen', component: ZuweisungenComponent}
];

// @NgModule({
//   imports: [RouterModule.forRoot(routes)],
//   exports: [RouterModule]
// })
export class AppRoutingModule { }
