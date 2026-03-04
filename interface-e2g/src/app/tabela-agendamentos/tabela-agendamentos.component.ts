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
import { VtnDTO } from '../model/VtnDTO';
import { VtnService } from '../services/vtn.service';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-tabela-agendamentos',
  standalone: true,
  imports: [MatTableModule, CommonModule, MatIconModule, MatButtonModule],
  templateUrl: './tabela-agendamentos.component.html',
  styleUrl: './tabela-agendamentos.component.css',
})
export class TabelaAgendamentosComponent implements OnChanges {
  @Input() eventos: VtnDTO[] = [];
  @Output() atualizar = new EventEmitter<void>();

  private vtnService = inject(VtnService);

  displayedColumns: string[] = [
    'tipoEvento',
    'potencia',
    'dataInicial',
    'dataFinal',
    'deletar',
  ];

  eventosFuturos: VtnDTO[] = [];

  ngOnChanges(changes: SimpleChanges) {
    if (changes['eventos']) {
      this.filtrarEventosFuturos();
    }
  }

  obterValorPotencia(evento: any) {
    const potMaximaDescarga =
      environment.estacao.potenciaMaximaDescarga *
      environment.estacao.quantidadeBaterias;

    if (evento.type === 'inject') {
      const valorPotencia = (potMaximaDescarga * evento.value) / 100;
      if (valorPotencia > potMaximaDescarga) {
        return -potMaximaDescarga;
      }
      return -valorPotencia;
    }
    return evento.value;
  }

  filtrarEventosFuturos() {
    const agora = Date.now();

    this.eventosFuturos = this.eventos
      .filter((e) => e.startTime > agora)
      .sort((a, b) => a.startTime - b.startTime);
  }

  deletar(id: string) {
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
