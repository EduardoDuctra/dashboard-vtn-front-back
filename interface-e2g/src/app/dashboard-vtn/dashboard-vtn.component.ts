import { ChangeDetectorRef, Component, inject, ViewChild } from '@angular/core';
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
import { EventoDTO } from '../model/EventoDTO';
import { MatTabsModule } from '@angular/material/tabs';
import { TipoEvento } from '../shared/tipoEvento';
import { environment } from '../../environments/environment';
import { EventoUnificado } from '../model/EventoUnificado';

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

  eventos: EventoUnificado[] = [];

  potenciaExibidaDashboard: number | null = null;
  dataFim: Date | null = null;

  boolCardTabelaAgendamentos = false;
  agendar = false;

  statusAtivo = false;
  horasReducao: number | null = null;

  potenciaAtual: number | null = null;

  type = TipoEvento.limit_charge;
  abaSelecionada = 0;

  @ViewChild(GraficoHistoricoComponent)
  grafico!: GraficoHistoricoComponent;

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
      this.type = TipoEvento.inject;
      this.abaSelecionada = 1;
    } else {
      this.type = TipoEvento.limit_charge;
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

    valorFinal = valorDigitado;

    const eventoDTO: EventoDTO = {
      type: this.type,
      value: valorFinal,
      startTime: inicio.getTime(),
      endTime: fim.getTime(),
    };

    console.log('DTO:', eventoDTO);

    this.vtnService.criarEvento(eventoDTO).subscribe({
      next: (res) => {
        console.log('Sucesso ao criar evento:', res);

        this.formReducao.reset();

        this.getEventos();
      },
      error: (err) => {
        if (err.status === 409 || err.status === 400) {
          alert(err.error.message);
        }

        this.getEventos();

        console.log('Erro:', err);
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
        console.log('Eventos recebidos da API:', res);

        this.eventos = res;

        this.verificarEventosAtivos();

        if (this.grafico) {
          this.grafico.atualizarDados(res);
        }
      },
    });
  }

  eventoAtivo: EventoUnificado | null = null;

  verificarEventosAtivos() {
    const agora = Date.now();

    this.eventoAtivo =
      this.eventos.find((e) => {
        const inicio = new Date(e.startTime).getTime();
        const fim = new Date(e.endTime).getTime();
        return inicio <= agora && fim > agora;
      }) || null;

    if (this.eventoAtivo) {
      this.statusAtivo = true;
      this.tipoEventoAtivo = this.eventoAtivo.type;
      this.dataFim = new Date(this.eventoAtivo.endTime);
      this.potenciaAtual = this.eventoAtivo.value;

      this.monitorarStatus();
    } else {
      this.statusAtivo = false;
      this.tipoEventoAtivo = null;
      this.dataFim = null;
      this.potenciaAtual = null;
    }
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

  deletarEventoAtual() {
    if (!this.eventoAtivo?.apiId) {
      return;
    }

    this.vtnService.deletarEvento(this.eventoAtivo.apiId).subscribe({
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
