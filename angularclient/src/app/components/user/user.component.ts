import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { SmartphoneService } from '../../services/smartphone.service';
import { UserService } from '../../services/user.service';
import { Smartphone } from '../../model/smartphone';
import { User } from '../../model/user';


@Component({
  selector: 'app-user',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user.component.html'
})
export class UserComponent {
  // Formularfelder Registrierung
  UserName = '';
  Email = '';
  SecretHash = '';

  // Request Hash
  getHash: boolean = false;
  selectedUser: User | null = null;
  lastHash: string | null = null;

  // Liste
  users: User[] = [];
  loading = false;
  errorMsg = '';

  constructor(private userService: UserService, private router: Router) {
    this.loadList();
  }

  loadList(): void {
    this.loading = true;
    this.userService.getAllUsers().subscribe({
      next: data => { this.users = data; this.loading = false; },
      error: err => { this.errorMsg = err.message || 'Fehler beim Laden'; this.loading = false; }
    });
  }

  register(): void {
    if (!this.UserName || !this.SecretHash ) {
      alert('Bitte User Name und Hash eingeben');
      return;
    }
    const newUser: User = { username: this.UserName, email: this.Email, secretHash: this.SecretHash };
    this.userService.registerUser(newUser).subscribe({
      next: _ => { this.clearRegForm(); this.loadList(); alert('Registrierung erfolgreich'); },
      error: err => { this.errorMsg = err.error?.message || 'Registrierung fehlgeschlagen'; }
    });

  }

  requestHash(): void {
      if (!this.selectedUser) {
        alert('Bitte ein User auswählen.');
        return;
      }

      this.lastHash = this.selectedUser.secretHash;
      this.getHash = true;

      this.clearHashForm();
    }



  clearRegForm(): void {
    this.UserName = ''; this.Email = ''; this.SecretHash = '';
  }

  clearHashForm(): void {
    this.selectedUser = null;
  }








}
