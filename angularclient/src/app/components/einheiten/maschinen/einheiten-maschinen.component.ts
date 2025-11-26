import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RcuService } from '../../../services/rcu.service';
import { Rcu } from '../../../model/rcu';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-einheiten-maschinen',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './einheiten-maschinen.component.html'
})
export class EinheitenMaschinenComponent {

   // RCUs
    rcus: Rcu[] = [];

    // Status
    loading = false;
    errorMsg = '';

    constructor(
      private rcuService: RcuService,
      private router: Router
    ) {
      this.loadData();
    }

    loadData(): void {
      this.loading = true;


      this.rcuService.getAllRcus().subscribe({
        next: (data: Rcu[]) => {
          this.rcus = data;
          this.loading = false;
        },
        error: (err: any) => {
          this.errorMsg = err.message || 'Fehler beim Laden der RCUs';
          this.loading = false;
        }
      });

    }


    deleteRcu(id: number): void {
        Swal.fire({
          text: `Möchten Sie wirklich diese Maschine löschen?`,
          icon: 'question',
          showCancelButton: true,
          confirmButtonText: 'Ja',
          cancelButtonText: 'Nein',
          color: '#002B49',
          buttonsStyling: false,
          customClass: {
            actions: 'space-x-4 justify-center',
            confirmButton: 'text-[#002B49] font-semibold px-4 py-2 rounded-lg hover:text-blue-800 transition',
            cancelButton: 'text-[#002B49] font-semibold px-4 py-2 rounded-lg hover:text-blue-800 transition'
          }
        }).then(result => {
          if (result.isConfirmed) {
            this.rcuService.deleteRcu(id).subscribe({
              next: () => this.loadData(),
              error: () => this.errorMsg = 'Fehler beim Löschen der RCU.'
            });
          }
        });
      }

    getMachineImage(machineName: string): { src: string; height: string } {
      if (!machineName) return { src: 'maschine.png', height: 'h-28' };

      const name = machineName.toLowerCase();

      if (name.includes('bagger')) {
        return { src: 'bagger.png', height: 'h-28' };
      } else if (name.includes('kuka')) {
        return { src: 'kuka.png', height: 'h-28' };
      } else if (name.includes('walze')) {
        return { src: 'walze.png', height: 'h-28' };
      } else {
        return { src: 'maschine.png', height: 'h-28' };
      }
    }

    getStatusColor(status?: string): string {
      switch ((status || '').toLowerCase()) {
        case 'idle': return 'text-green-600';
        case 'offline': return 'text-gray-600';
        case 'operational': return 'text-orange-700';
        case 'remote mode': return 'text-[#800080]';
        case 'remote mode requested': return 'text-[#CD853F]';
        default: return 'text-gray-500';
      }
    }

    handleClick(r: Rcu) {
      const machine = this.getMachineImage(r.name);
      const img = machine.src;
      const h = machine.height;
      const color = this.getStatusColor(r.status);

      Swal.fire({
        title: r.name,
        html: `
          <div style="border-bottom: 1px solid #d1d5db; margin: 10px 0 20px 0;"></div>
          <div style="height: 200px;" class="flex items-start gap-10 text-left w-full">

            <!-- Image -->
            <div class="h-full flex items-center">
              <img src="${img}" class="rounded-md h-32 px-2 min-w-[130px]" />
            </div>

            <!-- Information -->
            <div class="h-full flex flex-col justify-center min-w-[170px] pt-3">
              <div class="grid grid-cols-2 gap-y-1 gap-x-2">
                <span class="font-semibold">Cloud-ID:</span> <span>${r.id}</span>
                <span class="font-semibold">RCU-ID:</span> <span>${r.rcuId}</span>
                <span class="font-semibold">Standort:</span> <span>${r.location}</span>
              </div>
            </div>

            <!-- Status -->
            <div class="h-full flex flex-col justify-start min-w-[160px] pt-3">
              <p class="flex items-left gap-2">
                <span style="font-size:50px; font-weight: 900;" class="leading-none pt-14">→</span>
                <span class="leading-none pt-20 font-semibold ${color}">${(r.status + "").toUpperCase()}</span>
              </p>
            </div>

          </div>
          <div style="height: 100px;" class="flex items-center justify-center w-full">

            ${ (r.status == "inactive" || r.status == "idle") ? `
                <button id="StartRemoteMode"
                  style="outline: none; box-shadow: none;"
                  class="px-4 py-2 text-[#800080] font-semibold text-lg rounded-lg hover:text-[#4D2D61] transition">
                  FERNSTEUERUNG STARTEN
                </button>
              ` : ''
            }

          </div>

        `,
        showConfirmButton: false,
        showCancelButton: true,
        cancelButtonText: 'Schließen',
        width: 600,
        color: '#002B49',
        buttonsStyling: false,

        customClass: {
          popup: 'rounded-2xl',
          container: '',
          htmlContainer: 'p-10',
          title: 'pt-14',
          cancelButton: 'text-[#002B49] font-semibold px-4 py-2 rounded-lg hover:text-blue-800 transition'
        },

        didRender: () => {
          const btn = document.getElementById("StartRemoteMode");

          if (btn) {
            btn.addEventListener("click", () => {
              this.startRemoteMode(r.rcuId);  // <--- Deine Funktion aufrufen
            });
          }
        }
      });
    }

    startRemoteMode(rcuId: string) {
      this.rcuService.startRemoteMode(rcuId).subscribe({
          next: () => this.loadData(),
          error: () => this.errorMsg = 'Fehler beim Remote Start.'
        });

      }








}
