import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SearchFilters } from '../../../search/components/search-filters/search-filters';
import * as echarts from 'echarts';

@Component({
  selector: 'app-admin-dashboard',
  imports: [RouterLink, SearchFilters, CommonModule, FormsModule],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboard implements OnInit, AfterViewInit {
  @ViewChild('chartContainer') chartContainer!: ElementRef;
  
  private chart: any;

  xAxisOptions = [
    { value: 'age', label: 'Возраст' },
    { value: 'height', label: 'Рост' },
    { value: 'weight', label: 'Вес' },
    { value: 'birthYear', label: 'Год рождения' },
    { value: 'filmYear', label: 'Год фильма' },
    { value: 'playYear', label: 'Год пьесы' }
  ];
  
  groupByOptions = [
    { value: 'title', label: 'Звание' },
    { value: 'gender', label: 'Пол' },
    { value: 'university', label: 'Университет' },
    { value: 'genre', label: 'Жанр' }
  ];

  selectedXAxis = 'age';
  selectedGroupBy = 'title';
  currentFilters: any = {};
  isLoading = false;

  constructor() {}

  ngOnInit() {
    setTimeout(() => {
      this.loadStatistics();
    }, 500);
  }

  ngAfterViewInit() {
    if (this.chartContainer) {
      this.chart = echarts.init(this.chartContainer.nativeElement);
    }
  }

  onFiltersChanged(filters: any) {
    console.log('Filters changed:', filters);
    this.currentFilters = filters;
    this.loadStatistics();
  }

  onChartOptionsChanged() {
    if (this.chart)
    {
      this.chart.clear();
    }
    
    this.loadStatistics();
  }

  loadStatistics() {
    this.isLoading = true;

    const cleanFilters = (filters: any) => {
      const cleaned: any = {};
      for (const [key, value] of Object.entries(filters)) {
        if (value === "" || value === '') {
          cleaned[key] = null;
        } else {
          cleaned[key] = value;
        }
      }
      return cleaned;
    };

    const transformFilters = (filters: any) => {
      const transformed: any = {};
  
      for (const [key, value] of Object.entries(filters)) {
        if (value === null || value === '' || value === undefined) {
          continue;
        }

        switch(key) {
          case 'age_from':
            transformed.ageFrom = Number(value);
            break;
          case 'age_to':
            transformed.ageTo = Number(value);
            break;
          case 'height_from':
            transformed.heightMin = Number(value);
            break;
          case 'height_to':
            transformed.heightMax = Number(value);
            break;
          case 'weight_from':
            transformed.weightMin = Number(value);
            break;
          case 'weight_to':
            transformed.weightMax = Number(value);
            break;
          case 'activity_years_from':
            transformed.activityYearFrom = Number(value);
            break;
          case 'activity_years_to':
            transformed.activityYearTo = Number(value);
            break;
          case 'university_id':
            transformed.universityId = value;
            break;
          case 'theatre':
            transformed.theatre = value;
            break;
          case 'actor_rank':
            if (value === 'honored') transformed.title = 'honored';
            else if (value === 'national') transformed.title = 'national';
            else if (value === 'none') transformed.title = 'none';
            break;
          case 'gender':
            transformed.gender = value;
            break;
          case 'hair_color':
            transformed.hairColor = value;
            break;
          case 'eye_color':
            transformed.eyeColor = value;
            break;
          default:
            if (key.startsWith('genre_') && value === true) {
              let genreName = key.replace('genre_', '');

              const genreMap: { [key: string]: string } = {
                'drama': 'драма',
                'comedy': 'комедия',
                'tragedy': 'трагедия',
                'melodrama': 'мелодрама',
                'tragicomedy': 'трагикомедия',
                'musical': 'мюзикл',
                'opera': 'опера',
                'ballet': 'балет',
                'monodrama': 'монодрама'
              };
              
              const mappedGenre = genreMap[genreName];
              if (mappedGenre) {
                if (!transformed.genres) transformed.genres = [];
                transformed.genres.push(mappedGenre);
              }
            } else if (!key.startsWith('genre_')) {
              transformed[key] = value;
            }
        }
      }
      
      return transformed;
    };

    const cleanedFilters = cleanFilters(this.currentFilters);
    const transformedFilters = transformFilters(cleanedFilters);

    const requestBody: any = {
      filters: transformedFilters,
      xAxis: this.selectedXAxis,
    };

    if (this.selectedGroupBy !== 'none') {
      requestBody.groupBy = this.selectedGroupBy;
    }

    fetch('/v1/actors/stats', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestBody)
    })
    .then(response => {
      if (!response.ok) {
        return response.text().then(text => {
          throw new Error(`Error: ${response.status}. Message: ${text}`);
        });
      }
      return response.json();
    })
    .then(data => {
      console.log('Received data with filters:', data);
      this.renderChart(data);
      this.isLoading = false;
    })
    .catch(error => {
      console.error('Failed to load statistics - DETAILS:', {
        message: error.message,
        cause: error.cause,
        stack: error.stack
      });
      this.isLoading = false;
      this.showErrorChart();
    });
  }

  showErrorChart() {
    if (!this.chart) return;
    
    this.chart.setOption({
      title: {
        text: 'Ошибка загрузки данных',
        left: 'center',
        textStyle: { color: 'red' }
      }
    });
  }

  renderChart(data: any) {
    if (!this.chart) return;
    this.chart.clear();
    
    if (!data || !data.series || data.series.length === 0) {
      this.chart.setOption({
        title: {
          text: 'Нет данных для отображения',
          left: 'center'
        }
      });
      return;
    }

    const xAxisData: any[] = [];
    const seriesMap = new Map();
    
    data.series.forEach((series: any) => {
      seriesMap.set(series.name, series.data);
      series.data.forEach((point: any) => {
        if (!xAxisData.includes(point.x)) {
          xAxisData.push(point.x);
        }
      });
    });
    
    xAxisData.sort((a, b) => {
      if (typeof a === 'number' && typeof b === 'number') return a - b;
      return String(a).localeCompare(String(b));
    });
    
    const series = Array.from(seriesMap.entries()).map(([name, points]) => ({
      name: name,
      type: 'bar',
      data: xAxisData.map((x: any) => {
        const point = points.find((p: any) => p.x === x);
        return point ? point.value : 0;
      })
    }));
    
    this.chart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: any) => {
          if (!params || !params.length) return '';
          let result = `${params[0].axisValue}<br/>`;
          params.forEach((p: any) => {
            result += `${p.marker} ${p.seriesName}: ${p.value} чел.<br/>`;
          });
          return result;
        }
      },
      legend: {
        data: series.map((s: any) => s.name),
        bottom: '0%',
        left: 'center'
      },
      grid: {
        left: '8%',
        right: '5%',
        bottom: '5%',
        top: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        nameLocation: 'middle',
        nameGap: 35,
        data: xAxisData,
        axisLabel: {
          rotate: xAxisData.length > 8 ? 45 : 0,
          interval: 0
        }
      },
      yAxis: {
        type: 'value',
        nameLocation: 'middle',
        nameGap: 45,
        minInterval: 1
      },
      series: series
    });
    
    window.addEventListener('resize', () => this.chart.resize());
  }
  
  getXAxisLabel(): string {
    const option = this.xAxisOptions.find(opt => opt.value === this.selectedXAxis);
    return option ? option.label : 'Значение';
  }
}
