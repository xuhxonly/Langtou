// assets/charts.js
(function() {
  var style = getComputedStyle(document.documentElement);
  var accent = style.getPropertyValue('--accent').trim();
  var accent2 = style.getPropertyValue('--accent2').trim();
  var ink = style.getPropertyValue('--ink').trim();
  var muted = style.getPropertyValue('--muted').trim();
  var rule = style.getPropertyValue('--rule').trim();
  var bg2 = style.getPropertyValue('--bg2').trim();

  // --- Radar Chart: Langtou vs Xiaohongshu ---
  var chartRadar = echarts.init(document.getElementById('chart-radar'), null, { renderer: 'svg' });

  var indicators = [
    { name: '内容发布', max: 100 },
    { name: '搜索能力', max: 100 },
    { name: '推荐系统', max: 100 },
    { name: '社交功能', max: 100 },
    { name: '消息通知', max: 100 },
    { name: '电商直播', max: 100 },
    { name: 'AI能力', max: 100 },
    { name: '内容安全', max: 100 },
    { name: '性能体验', max: 100 },
    { name: '架构完善度', max: 100 }
  ];

  var xhsData = [95, 95, 95, 90, 90, 95, 90, 95, 95, 95];
  var langtouData = [25, 5, 40, 5, 20, 0, 0, 0, 30, 60];

  chartRadar.setOption({
    legend: {
      data: ['小红书', '榔头(Langtou)'],
      bottom: 0,
      textStyle: { color: muted, fontSize: 13 }
    },
    tooltip: {
      trigger: 'item',
      appendToBody: true
    },
    radar: {
      indicator: indicators,
      shape: 'polygon',
      radius: '65%',
      axisName: {
        color: ink,
        fontSize: 12
      },
      splitArea: {
        areaStyle: {
          color: [bg2, '#ffffff']
        }
      },
      axisLine: {
        lineStyle: { color: rule }
      },
      splitLine: {
        lineStyle: { color: rule }
      }
    },
    series: [{
      type: 'radar',
      animation: false,
      data: [
        {
          value: xhsData,
          name: '小红书',
          lineStyle: { color: accent, width: 2 },
          areaStyle: { color: accent + '30' },
          itemStyle: { color: accent },
          symbol: 'circle',
          symbolSize: 6
        },
        {
          value: langtouData,
          name: '榔头(Langtou)',
          lineStyle: { color: accent2, width: 2 },
          areaStyle: { color: accent2 + '30' },
          itemStyle: { color: accent2 },
          symbol: 'circle',
          symbolSize: 6
        }
      ]
    }]
  });

  window.addEventListener('resize', function() { chartRadar.resize(); });
})();
