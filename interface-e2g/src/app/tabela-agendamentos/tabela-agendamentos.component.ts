import {
  Component,
  inject,
  Input,
  OnInit,
  OnChanges,
  SimpleChanges,
  Output,
  EventEmitter,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { EventoDTO } from '../model/EventoDTO';
import { VtnService } from '../services/vtn.service';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { environment } from '../../environments/environment';
import { EventoUnificado } from '../model/EventoUnificado';

@Component({
  selector: 'app-tabela-agendamentos',
  standalone: true,
  imports: [MatTableModule, CommonModule, MatIconModule, MatButtonModule],
  templateUrl: './tabela-agendamentos.component.html',
  styleUrl: './tabela-agendamentos.component.css',
})
export class TabelaAgendamentosComponent implements OnChanges {
  @Input() eventos: EventoUnificado[] = [];
  @Output() atualizar = new EventEmitter<void>();

  private vtnService = inject(VtnService);

  displayedColumns: string[] = [
    'type',
    'value',
    'startTime',
    'endTime',
    'deletar',
  ];

  eventosFuturos: EventoUnificado[] = [];

  ngOnChanges(changes: SimpleChanges) {
    console.log('EVENTOS RECEBIDOS:', this.eventos);

    if (changes['eventos']) {
      this.filtrarEventosFuturos();
    }
  }

  obterValorPotencia(evento: EventoUnificado): number {
    if (evento.type === 'inject') {
      return -evento.value;
    } else {
      return evento.value;
    }
  }

  filtrarEventosFuturos() {
    const agora = Date.now();
    console.log('AGORA:', agora);

    this.eventosFuturos = this.eventos
      .filter((e) => e.startTime > agora)
      .sort((a, b) => a.startTime - b.startTime);
  }

  deletar(id: string) {
    console.log('ID enviado para deletar:', id);
    this.vtnService.deletarEvento(id).subscribe({
      next: (res) => {
        this.atualizar.emit();
      },
      error: (err) => {
        console.error('Erro ao deletar evento:', err);
      },
    });
  }
}
