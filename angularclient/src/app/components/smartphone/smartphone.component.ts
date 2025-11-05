import { Component } from '@angular/core';
import { SmartphoneService } from '../../services/smartphone.service';
import { Smartphone } from '../../model/smartphone';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { User } from '../../model/user';

@Component({
  selector: 'app-smartphone',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './smartphone.component.html',
  styleUrls: ['./smartphone.component.scss']
})
export class SmartphoneComponent {
  // Formularfelder Registrierung
  regDeviceId = '';
  regName = '';

  // Formularfelder Auth
  authToken: string | null = null;
  selectedSmartphone: Smartphone | null = null;

  // Liste
  phones: Smartphone[] = [];
  loading = false;
  errorMsg = '';

  constructor(
    private smartphoneService: SmartphoneService,
    private router: Router
    ) {
    this.loadList();
  }

  loadList(): void {
    this.loading = true;
    this.smartphoneService.getAll().subscribe({
      next: data => {
        this.phones = data;

        this.phones.forEach(phone => {
          if (phone.id) {
            this.smartphoneService.getAssignedUsers(phone.id).subscribe({
              next: (users: User[]) => {
                phone.users = users ?? [];
              },
              error: () => {
                phone.users = []; // Kein Problem, wenn keine Zuordnung existiert
              }
            });
          }
        });

        this.loading = false;
      },
      error: err => { this.errorMsg = err.message || 'Fehler beim Laden'; this.loading = false; }
    });
  }

  register(): void {
    const body: Smartphone = {
      deviceId: this.regDeviceId.trim(),
      name: this.regName.trim(),
    };
    if (!body.deviceId || !body.name) { alert('Bitte alle Felder ausfüllen'); return; }

    this.smartphoneService.registerSmartphone(body).subscribe({
      next: smart => {
        this.clearRegForm(); this.loadList(); alert('Registrierung erfolgreich');
        this.router.navigate(['/smartphone/assign'], { queryParams: { id: smart.id, name: smart.name } });
      },
      error: err => { this.errorMsg = err.error?.message || 'Registrierung fehlgeschlagen'; }
    });
  }

  requestToken(): void {
    if (!this.selectedSmartphone) {
            alert('Bitte ein Smartphone auswählen.');
            return;
    }

    const { deviceId } = this.selectedSmartphone;
    if (!this.selectedSmartphone.users || this.selectedSmartphone.users.length === 0) {
        alert('Kein Benutzer für dieses Smartphone gefunden.');
        return;
    }

    const user = this.selectedSmartphone.users[0];
    const { username, secretHash } = user;


    this.smartphoneService.requestToken(deviceId, username, secretHash).subscribe({
      next: res => { this.authToken = res.auth_token; alert('Token erhalten'); this.clearAuthForm(); },
      error: _ => { this.authToken = null; alert('Auth fehlgeschlagen'); }
    });
  }


  clearRegForm(): void {
    this.regDeviceId = ''; this.regName = '';
  }

  clearAuthForm(): void {
    this.selectedSmartphone = null;
  }



}

