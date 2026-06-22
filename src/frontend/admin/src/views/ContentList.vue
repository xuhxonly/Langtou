<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'

type ContentStatus = 'published' | 'pending' | 'rejected' | 'offline'

interface ContentItem {
  id: number
  title: string
  author: string
  category: string
  status: ContentStatus
  viewCount: number
  likeCount: number
  createdAt: string
}

// 状态映射
const statusMap: Record<ContentStatus, { label: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
  published: { label: '已发布', type: 'success' },
  pending: { label: '待审核', type: 'warning' },
  rejected: { label: '已拒绝', type: 'danger' },
  offline: { label: '已下架', type: 'info' }
}

// 内容列表
const contents = ref<ContentItem[]>([])
const loading = ref(false)

const searchKeyword = ref('')
const statusFilter = ref<ContentStatus | ''>('')

const pagination = reactive({
  page: 1,
  pageSize: 10
})

// 模拟数据生成
const generateMockData = (): ContentItem[] => {
  const list: ContentItem[] = []
  const titles = [
    '小红书爆款笔记写作技巧',
    '2024 年最值得买的数码产品',
    '健身餐食谱大全',
    '旅行 Vlog 拍摄指南',
    '职场穿搭 | 秋冬通勤 look',
    '居家收纳｜小户型神器',
    '护肤干货｜敏感肌必看',
    '宝宝辅食 100 天不重样',
    '亲子出游目的地推荐',
    '手冲咖啡入门指南'
  ]
  const categories = ['时尚', '美食', '旅行', '数码', '家居', '母婴', '美妆', '亲子']
  const statuses: ContentStatus[] = ['published', 'pending', 'rejected', 'offline']
  for (let i = 1; i <= 40; i++) {
    list.push({
      id: i,
      title: titles[i % titles.length] + ' · ' + i,
      author: '用户' + (100 + i),
      category: categories[i % categories.length],
      status: statuses[i % statuses.length],
      viewCount: Math.floor(Math.random() * 50000),
      likeCount: Math.floor(Math.random() * 3000),
      createdAt: `2024-0${(i % 9) + 1}-${String((i % 28) + 1).padStart(2, '0')}`
    })
  }
  return list
}

const fetchData = async () => {
  loading.value = true
  await new Promise((r) => setTimeout(r, 300))
  contents.value = generateMockData()
  loading.value = false
}

const filteredContents = computed(() => {
  return contents.value.filter((c) => {
    const matchKeyword =
      !searchKeyword.value ||
      c.title.includes(searchKeyword.value) ||
      c.author.includes(searchKeyword.value)
    const matchStatus = !statusFilter.value || c.status === statusFilter.value
    return matchKeyword && matchStatus
  })
})

const pagedContents = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  return filteredContents.value.slice(start, start + pagination.pageSize)
})

const handleApprove = async (row: ContentItem) => {
  await ElMessageBox.confirm('确定审核通过该内容？', '提示')
  row.status = 'published'
  ElMessage.success('已通过')
}

const handleReject = async (row: ContentItem) => {
  await ElMessageBox.confirm('确定拒绝该内容？', '提示', { type: 'warning' })
  row.status = 'rejected'
  ElMessage.success('已拒绝')
}

const handleOffline = async (row: ContentItem) => {
  await ElMessageBox.confirm('确定下架该内容？', '提示', { type: 'warning' })
  row.status = 'offline'
  ElMessage.success('已下架')
}

onMounted(fetchData)
</script>

<template>
  <div class="content-list">
    <el-card shadow="never" class="toolbar-card">
      <el-form :inline="true">
        <el-form-item label="关键词">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索标题 / 作者"
            clearable
            :prefix-icon="Search"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="statusFilter"
            placeholder="全部状态"
            clearable
            style="width: 150px"
          >
            <el-option label="已发布" value="published" />
            <el-option label="待审核" value="pending" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="已下架" value="offline" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :icon="Refresh" @click="fetchData">刷新</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Plus">新建内容</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="pagedContents"
        v-loading="loading"
        stripe
        border
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="内容标题" min-width="240" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="130" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status].type" effect="dark" round>
              {{ statusMap[row.status].label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="110" align="right">
          <template #default="{ row }">{{ row.viewCount.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="likeCount" label="点赞数" width="100" align="right" />
        <el-table-column prop="createdAt" label="发布时间" min-width="130" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending'"
              size="small"
              type="success"
              plain
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              size="small"
              type="danger"
              plain
              @click="handleReject(row)"
            >
              拒绝
            </el-button>
            <el-button
              v-if="row.status === 'published'"
              size="small"
              type="warning"
              plain
              @click="handleOffline(row)"
            >
              下架
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span class="total-text">共 {{ filteredContents.length }} 条记录</span>
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="filteredContents.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.content-list {
  .toolbar-card {
    margin-bottom: 16px;
    background: var(--lt-card-bg);
    border: 1px solid var(--lt-border-color);

    :deep(.el-form-item) {
      margin-bottom: 0;
    }
  }

  .table-card {
    background: var(--lt-card-bg);
    border: 1px solid var(--lt-border-color);

    :deep(.el-table) {
      background: transparent;
      --el-table-bg-color: transparent;
      --el-table-tr-bg-color: transparent;
      --el-table-header-bg-color: var(--lt-input-bg);
      --el-table-border-color: var(--lt-border-color);
      --el-table-header-text-color: var(--lt-text-secondary);
      --el-table-text-color: var(--lt-text-primary);
    }
  }

  .pagination-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 16px;
  }

  .total-text {
    color: var(--lt-text-muted);
    font-size: 13px;
  }
}
</style>
