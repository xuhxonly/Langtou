<script setup lang="ts">
import { ref, onMounted, shallowRef } from 'vue'
import * as echarts from 'echarts'

// 数据统计
const stats = ref({
  users: 28463,
  newUsers: 1287,
  notes: 3562,
  interactions: 156892
})

const chartRef = shallowRef<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// 初始化 ECharts 趋势图
const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['DAU', '新增用户', '笔记发布'],
      textStyle: { color: '#8b95a8' },
      top: 0,
      right: 10
    },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLine: { lineStyle: { color: '#252d3d' } },
      axisLabel: { color: '#8b95a8' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#252d3d' } },
      axisLabel: { color: '#8b95a8' }
    },
    series: [
      {
        name: 'DAU',
        type: 'line',
        smooth: true,
        data: [22000, 24000, 26000, 25000, 27000, 28000, 28463],
        itemStyle: { color: '#3b82f6' },
        areaStyle: { color: 'rgba(59,130,246,0.15)' }
      },
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: [900, 1000, 1100, 1050, 1200, 1250, 1287],
        itemStyle: { color: '#10b981' }
      },
      {
        name: '笔记发布',
        type: 'line',
        smooth: true,
        data: [3200, 3300, 3400, 3350, 3500, 3600, 3562],
        itemStyle: { color: '#f59e0b' }
      }
    ]
  }
  chartInstance.setOption(option)
}

// 窗口 resize 处理
const handleResize = () => chartInstance?.resize()

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})
</script>

<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="(item, idx) in [
        { label: '今日 DAU', value: stats.users, change: '+12.5%', icon: 'User', color: 'var(--lt-brand-color)' },
        { label: '新增用户', value: stats.newUsers, change: '+8.3%', icon: 'UserFilled', color: 'var(--lt-color-green)' },
        { label: '发布笔记数', value: stats.notes, change: '-2.1%', icon: 'Document', color: 'var(--lt-color-yellow)' },
        { label: '互动总数', value: stats.interactions, change: '+15.7%', icon: 'ChatDotRound', color: 'var(--lt-color-purple)' }
      ]" :key="idx">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-header">
            <span class="stat-label">{{ item.label }}</span>
            <el-icon :size="22" :color="item.color"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-value">{{ item.value.toLocaleString() }}</div>
          <div
            class="stat-change"
            :class="item.change.startsWith('+') ? 'up' : 'down'"
          >
            <el-icon><component :is="item.change.startsWith('+') ? 'Top' : 'Bottom'" /></el-icon>
            {{ item.change }} 较昨日
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :md="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <div>
                <span class="card-title">7 天活跃趋势</span>
                <span class="card-subtitle">DAU / 新增用户 / 笔记发布</span>
              </div>
            </div>
          </template>
          <div ref="chartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">热门笔记 TOP 5</span>
          </template>
          <ul class="hot-list">
            <li v-for="(item, idx) in [
              { title: '小红书爆款笔记写作技巧', views: '12.3k' },
              { title: '2024 年最值得买的数码产品', views: '9.8k' },
              { title: '健身餐食谱大全（附教程）', views: '8.5k' },
              { title: '旅行 Vlog 拍摄指南', views: '7.2k' },
              { title: '职场穿搭 | 秋冬通勤 look', views: '6.1k' }
            ]" :key="idx" class="hot-item">
              <div class="hot-rank" :class="'top' + (idx + 1)">{{ idx + 1 }}</div>
              <div class="hot-info">
                <div class="hot-title">{{ item.title }}</div>
                <div class="hot-meta">浏览量 {{ item.views }}</div>
              </div>
            </li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统状态 -->
    <el-card shadow="hover" class="health-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">系统状态</span>
          <el-button size="small" :icon="Refresh">刷新</el-button>
        </div>
      </template>
      <el-row :gutter="12">
        <el-col :xs="12" :sm="6" :md="3" v-for="svc in [
          { name: '用户服务', version: 'v1.2.0', status: '正常', color: 'var(--lt-color-green)' },
          { name: '内容服务', version: 'v1.3.1', status: '正常', color: 'var(--lt-color-green)' },
          { name: '互动服务', version: 'v1.1.2', status: '正常', color: 'var(--lt-color-green)' },
          { name: 'AI 服务', version: 'v0.9.5', status: '加载中', color: 'var(--lt-color-yellow)' }
        ]" :key="svc.name">
          <div class="svc-item">
            <div class="svc-name">{{ svc.name }}</div>
            <div class="svc-version">{{ svc.version }}</div>
            <div class="svc-status" :style="{ color: svc.color }">● {{ svc.status }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.dashboard {
  .stats-row {
    margin-bottom: 20px;
  }

  .stat-card {
    background: var(--lt-card-bg);
    border: 1px solid var(--lt-border-color);

    :deep(.el-card__body) {
      padding: 18px 20px;
    }
  }

  .stat-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  .stat-label {
    font-size: 13px;
    color: var(--lt-text-secondary);
  }

  .stat-value {
    font-size: 26px;
    font-weight: 700;
    color: var(--lt-text-primary);
    margin-bottom: 4px;
  }

  .stat-change {
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 4px;

    &.up { color: var(--lt-color-green); }
    &.down { color: var(--lt-color-red); }
  }

  .chart-row {
    margin-bottom: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .card-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--lt-text-primary);
  }

  .card-subtitle {
    display: block;
    font-size: 12px;
    color: var(--lt-text-muted);
    margin-top: 2px;
  }

  .chart-container {
    height: 280px;
  }

  .hot-list {
    list-style: none;
    padding: 0;
    margin: 0;
  }

  .hot-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 0;
    border-bottom: 1px solid var(--lt-border-color);

    &:last-child { border-bottom: none; }
  }

  .hot-rank {
    width: 24px;
    height: 24px;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
    background: var(--lt-input-bg);
    color: var(--lt-text-muted);
    flex-shrink: 0;

    &.top1 { background: linear-gradient(135deg, #f59e0b, #d97706); color: #fff; }
    &.top2 { background: linear-gradient(135deg, #94a3b8, #64748b); color: #fff; }
    &.top3 { background: linear-gradient(135deg, #c2884e, #a06830); color: #fff; }
  }

  .hot-info {
    flex: 1;
    min-width: 0;
  }

  .hot-title {
    font-size: 13px;
    color: var(--lt-text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .hot-meta {
    font-size: 12px;
    color: var(--lt-text-muted);
    margin-top: 2px;
  }

  .health-card {
    background: var(--lt-card-bg);
    border: 1px solid var(--lt-border-color);
  }

  .svc-item {
    padding: 12px 14px;
    background: var(--lt-input-bg);
    border: 1px solid var(--lt-border-color);
    border-radius: 6px;
  }

  .svc-name {
    font-size: 13px;
    font-weight: 500;
    color: var(--lt-text-primary);
  }

  .svc-version {
    font-size: 11px;
    color: var(--lt-text-muted);
    margin: 2px 0;
  }

  .svc-status {
    font-size: 12px;
  }
}
</style>
