import {
  Component,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import * as Highcharts from 'highcharts';
import { CORES } from '../shared/cores';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-grafico-historico',
  standalone: true,
  templateUrl: './grafico-historico.component.html',
  styleUrls: ['./grafico-historico.component.css'],
})
export class GraficoHistoricoComponent implements OnChanges {
  @Input() eventos: any[] = [];

  constructor(private el: ElementRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    this.renderizarGrafico();
  }

  renderizarGrafico() {
    const momentoAtual = new Date();

    const eventosPassados = this.eventos.filter((e) => {
      return e.startTime <= momentoAtual;
    });

    //ordenado pela data os eventos que já aconteceram
    const ordenado = [...eventosPassados].sort(
      (a, b) => a.startTime - b.startTime,
    );

    //converte o time para dia e horas
    //tirei os segundos
    const categorias = ordenado.map((e: any) => {
      const data = new Date(e.startTime);

      const dataFormatada = data.toLocaleDateString('pt-BR');
      const horaFormatada = data.toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit',
      });

      return `${dataFormatada} ${horaFormatada}`;
    });

    const solicitacoes = ordenado.map((e: any) => {
      const potMaximaDescarga =
        environment.estacao.potenciaMaximaDescarga *
        environment.estacao.quantidadeBaterias;

      if (e.type === 'inject') {
        const potenciaW = (potMaximaDescarga * e.value) / 100;
        const valorPotencia = potenciaW / 1000;

        if (valorPotencia > potMaximaDescarga) {
          return -potMaximaDescarga;
        }
        return -valorPotencia;
      }
      return e.value / 1000;
    });

    Highcharts.charts.forEach((c) => c?.destroy());

    Highcharts.chart(this.el.nativeElement.querySelector('#grafico'), {
      chart: { type: 'line' },

      title: { text: 'Histórico de Reduções' },

      xAxis: {
        categories: categorias,
      },

      yAxis: {
        title: { text: 'Potência (kW)' },
      },

      series: [
        {
          name: 'Valor de Potência',
          type: 'line',
          data: solicitacoes,
          zoneAxis: 'y',
          zones: [
            {
              value: 0,
              color: CORES.INJECAO,
            },
            {
              color: CORES.REDUCAO,
            },
          ],
        },
      ],
    });
  }
}
