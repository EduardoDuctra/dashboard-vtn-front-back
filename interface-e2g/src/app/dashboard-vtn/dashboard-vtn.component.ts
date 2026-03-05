import { Component, inject } from '@angular/core';
import { Breakpoints, BreakpointObserver } from '@angular/cdk/layout';
import { map } from 'rxjs/operators';
import { AsyncPipe, CommonModule } from '@angular/common';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import {
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { GraficoHistoricoComponent } from '../grafico-historico/grafico-historico.component';
import { TabelaAgendamentosComponent } from '../tabela-agendamentos/tabela-agendamentos.component';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ModalAgendamentoComponent } from '../modal-agendamento/modal-agendamento.component';
import { VtnService } from '../services/vtn.service';
import { VtnDTO } from '../model/VtnDTO';
import { MatTabsModule } from '@angular/material/tabs';
import { TipoEvento } from '../shared/tipoEvento';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-dashboard-vtn',
  standalone: true,
  templateUrl: './dashboard-vtn.component.html',
  styleUrl: './dashboard-vtn.component.css',
  imports: [
    AsyncPipe,
    CommonModule,
    MatGridListModule,
    MatMenuModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    GraficoHistoricoComponent,
    TabelaAgendamentosComponent,
    MatDatepickerModule,
    MatNativeDateModule,
    MatDialogModule,
    MatTabsModule,
  ],
})
export class DashboardVtnComponent {
  private breakpointObserver = inject(BreakpointObserver);
  private dialog = inject(MatDialog);

  ambienteDesenvolvimento = !environment.production;

  private vtnService = inject(VtnService);

  eventos: VtnDTO[] = [];

  potenciaExibidaDashboard: number | null = null;
  dataFim: Date | null = null;

  boolCardTabelaAgendamentos = false;
  agendar = false;

  statusAtivo = false;
  horasReducao: number | null = null;

  potenciaMaximaInversor = environment.estacao.potenciaMaximaInversor;
  potenciaAtual: number | null = null;

  type = TipoEvento.LIMIT_CHARGE;
  abaSelecionada = 0;

  capacidadeBateria = environment.estacao.capacidadeBateria;
  quantidadeBaterias = environment.estacao.quantidadeBaterias;
  potenciaMaximaDescarga = environment.estacao.potenciaMaximaDescarga;

  tipoEventoAtivo: String | null = null;

  private fb = inject(FormBuilder);

  formReducao = this.fb.group({
    potencia: ['', Validators.required],
    tempo: ['', Validators.required],
  });

  ngOnInit() {
    this.getEventos();
  }

  abrirTabelaAgendamentos() {
    this.boolCardTabelaAgendamentos = !this.boolCardTabelaAgendamentos;
  }

  abrirModalAgendar() {
    const dialogRef = this.dialog.open(ModalAgendamentoComponent, {
      width: '500px',
      maxWidth: '100vw',
    });

    dialogRef.afterClosed().subscribe((dados) => {
      if (dados) {
        this.enviarFormulario(dados);
      }
    });
  }

  abrirAgendar() {
    this.agendar = !this.agendar;
  }

  onTabChange(index: number) {
    if (index === 1) {
      this.type = TipoEvento.INJECT;
      this.abaSelecionada = 1;
    } else {
      this.type = TipoEvento.LIMIT_CHARGE;
      this.abaSelecionada = 0;
    }
  }

  enviarFormulario(dados?: any) {
    if (!this.formReducao.valid) {
      return;
    }

    const tempo = Number(this.formReducao.value.tempo);

    let inicio: Date;

    //se tem data e hora -> veio do modal (agendamento)
    if (dados?.data && dados?.hora) {
      inicio = new Date(dados.data);

      const [h, m] = dados.hora.split(':');
      inicio.setHours(Number(h));
      inicio.setMinutes(Number(m));
      inicio.setSeconds(0);
      inicio.setMilliseconds(0);
    } else {
      inicio = new Date();
    }

    const fim = new Date(inicio.getTime() + tempo * 60 * 60 * 1000);

    const valorDigitado = Number(this.formReducao.value.potencia);

    let valorFinal: number;

    if (this.type === TipoEvento.INJECT) {
      valorFinal = this.calculoPotenciaDisponivel(valorDigitado);
    } else {
      valorFinal = this.potenciaExibida(valorDigitado);
    }

    const vtnDTO: VtnDTO = {
      type: this.type,
      value: valorFinal,
      startTime: inicio.getTime(),
      endTime: fim.getTime(),
    };

    console.log('DTO:', vtnDTO);

    this.vtnService.criarEvento(vtnDTO).subscribe({
      next: (res) => {
        console.log('Evento criado com sucesso:', res);
        this.formReducao.reset();
        this.getEventos();
      },
      error: (err) => {
        if (err.status === 409) {
          alert('Já existe um evento agendado nesse intervalo.');
        } else {
          alert('Erro ao criar evento.');
        }
      },
    });

    //so marca true se for agora
    if (inicio <= new Date()) {
      this.statusAtivo = true;
      this.dataFim = fim;
      this.monitorarStatus();
    }
  }

  getEventos() {
    this.vtnService.buscarEventos().subscribe({
      next: (res) => {
        this.eventos = res;
        this.verificarEventosAtivos();
        console.log('Eventos:', res);
      },
      error: (err) => {
        console.error('Erro ao carregar eventos:', err);
      },
    });
  }

  eventoAtivo: VtnDTO | null = null;

  verificarEventosAtivos() {
    const agora = Date.now();

    this.eventoAtivo =
      this.eventos.find((e) => e.startTime <= agora && e.endTime > agora) ||
      null;

    if (this.eventoAtivo) {
      this.statusAtivo = true;
      this.tipoEventoAtivo = this.eventoAtivo.type;
      this.dataFim = new Date(this.eventoAtivo.endTime);

      if (this.eventoAtivo.type === TipoEvento.INJECT) {
        this.potenciaAtual = this.calcularPotenciaReal(this.eventoAtivo.value);
      } else {
        this.potenciaAtual = this.potenciaExibida(this.eventoAtivo.value);
      }
      this.monitorarStatus();
    } else {
      this.statusAtivo = false;
      this.tipoEventoAtivo = null;
      this.dataFim = null;
      this.potenciaAtual = null;
    }
  }
  calcularPotenciaReal(porcentagem: number) {
    const potenciaMaximaEstacao =
      this.potenciaMaximaDescarga * this.quantidadeBaterias;

    return (porcentagem / 100) * potenciaMaximaEstacao;
  }
  potenciaExibida(potenciaSolicitada: number): number {
    if (potenciaSolicitada > this.potenciaMaximaInversor) {
      return this.potenciaMaximaInversor;
    }
    return potenciaSolicitada;
  }

  monitorarStatus() {
    if (!this.dataFim) {
      return;
    }

    const tempoRestante = this.dataFim.getTime() - new Date().getTime();

    if (tempoRestante <= 0) {
      this.statusAtivo = false;
      this.dataFim = null;
      localStorage.removeItem('dataFim');
      localStorage.removeItem('statusAtivo');
      return;
    }

    setTimeout(() => {
      this.statusAtivo = false;
      this.dataFim = null;
      localStorage.removeItem('dataFim');
      localStorage.removeItem('statusAtivo');
    }, tempoRestante);
  }

  calculoPotenciaDisponivel(potencia: number) {
    const potenciaMaximaEstacao =
      this.potenciaMaximaDescarga * this.quantidadeBaterias;

    let porcentagemPotencia = (potencia * 100) / potenciaMaximaEstacao;

    if (porcentagemPotencia > 100) {
      porcentagemPotencia = 100;
    }

    console.log('Porcentagem de potência disponível:', porcentagemPotencia);

    return porcentagemPotencia;
  }

  deletarEventoAtual() {
    if (!this.eventoAtivo?.id) {
      return;
    }

    this.vtnService.deletarEvento(this.eventoAtivo.id).subscribe({
      next: () => {
        this.getEventos();
      },
    });
  }

  cards = this.breakpointObserver.observe(Breakpoints.Handset).pipe(
    map(({ matches }) => {
      if (matches) {
        return [
          {
            type: 'potencia',

            cols: 1,
            rows: 1,
          },
          {
            type: 'historico',
            cols: 2,
            rows: 3,
          },

          { type: 'acao', cols: 1, rows: 1 },
          { type: 'status', cols: 1, rows: 1 },
        ];
      }

      return [
        { type: 'potencia', cols: 1, rows: 1 },
        { type: 'historico', cols: 2, rows: 3 },

        { type: 'acao', cols: 1, rows: 1 },
        { type: 'status', cols: 1, rows: 1 },
      ];
    }),
  );
}
