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
import { EventoUnificado } from '../model/EventoUnificado';

@Component({
  selector: 'app-grafico-historico',
  standalone: true,
  templateUrl: './grafico-historico.component.html',
  styleUrls: ['./grafico-historico.component.css'],
})
export class GraficoHistoricoComponent implements OnChanges {
  @Input() eventos: EventoUnificado[] = [];

  constructor(private el: ElementRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    console.log('Atualizou eventos:', this.eventos);

    this.renderizarGrafico();
  }

  chart?: Highcharts.Chart;

  renderizarGrafico() {
    const momentoAtual = new Date();

    const eventosPassados = this.eventos.filter(
      (e) => new Date(e.startTime) <= momentoAtual,
    );

    const ordenado = [...eventosPassados].sort(
      (a, b) =>
        new Date(a.startTime).getTime() - new Date(b.startTime).getTime(),
    );

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
      const valorPotencia = e.value / 1000;

      if (e.type === 'inject') {
        return -valorPotencia;
      }

      return valorPotencia;
    });

    const container = this.el.nativeElement.querySelector('#grafico');

    if (!this.chart) {
      this.chart = Highcharts.chart(container, {
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
              { value: 0, color: CORES.INJECAO },
              { color: CORES.REDUCAO },
            ],
          },
        ],
      });
    } else {
      this.chart.xAxis[0].setCategories(categorias);
      this.chart.series[0].setData(solicitacoes);
    }
  }

  atualizarDados(eventos: EventoUnificado[]) {
    const momentoAtual = new Date();

    const eventosPassados = eventos.filter(
      (e) => new Date(e.startTime) <= momentoAtual,
    );

    const ordenado = [...eventosPassados].sort(
      (a, b) =>
        new Date(a.startTime).getTime() - new Date(b.startTime).getTime(),
    );

    const categorias = ordenado.map((e) => {
      const data = new Date(e.startTime);

      const dataFormatada = data.toLocaleDateString('pt-BR');
      const horaFormatada = data.toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit',
      });

      return `${dataFormatada} ${horaFormatada}`;
    });

    const solicitacoes = ordenado.map((e: any) => {
      const valorPotencia = e.potencialTotal / 1000;

      if (e.type === 'inject') {
        return -valorPotencia;
      }

      return valorPotencia;
    });

    if (!this.chart) return;

    this.chart.xAxis[0].setCategories(categorias);
    this.chart.series[0].setData(solicitacoes);
  }

  ngAfterViewInit() {
    this.chart = Highcharts.chart(
      this.el.nativeElement.querySelector('#grafico'),
      {
        chart: { type: 'line' },

        title: { text: 'Histórico de Reduções' },

        xAxis: { categories: [] },

        yAxis: {
          title: { text: 'Potência (kW)' },
        },

        series: [
          {
            name: 'Fluxo de Potência',
            type: 'line',
            color: '#000000',
            data: [],
            zoneAxis: 'y',
            zones: [
              { value: 0, color: CORES.INJECAO },
              { color: CORES.REDUCAO },
            ],
          },
        ],
      },
    );
  }
}
