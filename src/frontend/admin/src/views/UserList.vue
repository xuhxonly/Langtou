<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'

interface UserItem {
  id: number
  nickname: string
  phone: string
  status: 'normal' | 'banned'
  noteCount: number
  followerCount: number
  createdAt: string
}

// 用户列表数据
const users = ref<UserItem[]>([])
const loading = ref(false)

// 搜索与筛选
const searchKeyword = ref('')
const statusFilter = ref('')

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 50
})

// 模拟生成数据
const generateMockData = (): UserItem[] => {
  const list: UserItem[] = []
  const surnames = ['张', '李', '王', '刘', '陈', '杨', '赵', '黄', '周', '吴']
  for (let i = 1; i <= 50; i++) {
    const name = surnames[i % surnames.length] + '用户' + i
    list.push({
      id: i,
      nickname: name,
      phone: '138****' + String(1000 + i).slice(-4),
      status: i % 7 === 0 ? 'banned' : 'normal',
      noteCount: Math.floor(Math.random() * 200),
      followerCount: Math.floor(Math.random() * 10000),
      createdAt: `2024-0${(i % 9) + 1}-${String((i % 28) + 1).padStart(2, '0')}`
    })
  }
  return list
}

// 加载数据
const fetchData = async () => {
  loading.value = true
  // 模拟请求
  await new Promise((r) => setTimeout(r, 300))
  users.value = generateMockData()
  pagination.total = users.value.length
  loading.value = false
}

// 过滤后的数据
const filteredUsers = computed(() => {
  return users.value.filter((u) => {
    const matchKeyword =
      !searchKeyword.value ||
      u.nickname.includes(searchKeyword.value) ||
      u.phone.includes(searchKeyword.value)
    const matchStatus = !statusFilter.value || u.status === statusFilter.value
    return matchKeyword && matchStatus
  })
})

// 当前页数据
const pagedUsers = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  return filteredUsers.value.slice(start, start + pagination.pageSize)
})

// 分页变化
const handlePageChange = (page: number) => {
  pagination.page = page
}

// 封禁用户
const handleBan = async (row: UserItem) => {
  await ElMessageBox.confirm(`确定要封禁用户「${row.nickname}」吗？`, '提示', {
    type: 'warning'
  })
  row.status = 'banned'
  ElMessage.success('操作成功')
}

// 解封用户
const handleUnban = async (row: UserItem) => {
  await ElMessageBox.confirm(`确定要解封用户「${row.nickname}」吗？`, '提示')
  row.status = 'normal'
  ElMessage.success('操作成功')
}

onMounted(fetchData)
</script>

<template>
  <div class="user-list">
    <!-- 搜索工具栏 -->
    <el-card shadow="never" class="toolbar-card">
      <el-form :inline="true" :model="{ inline: true }">
        <el-form-item label="关键词">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索昵称 / 手机号"
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
            style="width: 140px"
          >
            <el-option label="正常" value="normal" />
            <el-option label="已封禁" value="banned" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :icon="Refresh"
            @click="fetchData"
          >
            刷新
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="pagedUsers"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="nickname" label="用户昵称" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'normal' ? 'success' : 'danger'"
              effect="dark"
              round
            >
              {{ row.status === 'normal' ? '正常' : '已封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="noteCount" label="笔记数" width="100" align="right" />
        <el-table-column prop="followerCount" label="粉丝数" width="110" align="right" />
        <el-table-column prop="createdAt" label="注册时间" min-width="130" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'normal'"
              size="small"
              type="danger"
              plain
              @click="handleBan(row)"
            >
              封禁
            </el-button>
            <el-button
              v-else
              size="small"
              type="success"
              plain
              @click="handleUnban(row)"
            >
              解封
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-bar">
        <span class="total-text">共 {{ filteredUsers.length }} 条记录</span>
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="filteredUsers.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.user-list {
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
