import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { SmartphoneService } from '../../services/smartphone.service';
import { User } from '../../model/user';


@Component({
  selector: 'app-smartphone-assign',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './smartphone-assign.component.html'
})
export class SmartphoneAssignComponent implements OnInit {
 users: User[] = [];
 selectedUserIds: number[] = [];
 smartphoneId!: number;
 smartphoneName = '';
 message = '';

 constructor(
     private route: ActivatedRoute,
     private router: Router,
     private userService: UserService,
     private smartphoneService: SmartphoneService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.smartphoneId = +params['id'];
      this.smartphoneName = params['name'];
    });

    this.userService.getAllUsers().subscribe({
      next: data => this.users = data,
      error: () => this.message = 'Fehler beim Laden der Smartphones.'
    });
  }

  assign(): void {
      if (!this.selectedUserIds || this.selectedUserIds.length === 0) {
        this.message = 'Bitte mindestens ein User auswählen.';
        return;
      }

      this.smartphoneService.assignUsers(this.smartphoneId, this.selectedUserIds).subscribe({
        next: _ => {
          this.message = 'Users erfolgreich zugewiesen!';
          setTimeout(() => this.router.navigate(['/home']), 1);
        },
        error: () => this.message = 'Zuweisung fehlgeschlagen.'
      });
    }



}
