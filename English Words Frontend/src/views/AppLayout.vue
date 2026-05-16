<script setup lang="ts">
import {
  Collection,
  DataAnalysis,
  Finished,
  House,
  Notebook,
  Reading,
  RefreshRight,
  SwitchButton
} from '@element-plus/icons-vue'
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const navItems = [
  { path: '/dashboard', label: '首页', icon: House },
  { path: '/books', label: '词库', icon: Collection },
  { path: '/words', label: '单词', icon: Notebook },
  { path: '/quiz', label: '抽查', icon: Reading },
  { path: '/review', label: '复习', icon: RefreshRight },
  { path: '/progress', label: '进度', icon: DataAnalysis },
  { path: '/history', label: '历史', icon: Finished }
]

onMounted(() => {
  auth.fetchMe().catch(() => undefined)
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">EW</div>
        <div>
          <div class="brand-name">English Words</div>
          <div class="brand-caption">Vocabulary System</div>
        </div>
      </div>

      <nav class="nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: route.path === item.path }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <div class="topbar-title">网页背单词系统</div>
        <div class="user-box">
          <span>{{ auth.user?.nickname || auth.user?.username || '学习者' }}</span>
          <el-button :icon="SwitchButton" circle @click="logout" />
        </div>
      </header>

      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  min-height: 100vh;
  background: #f4f6f8;
}

.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 22px 16px;
  background: #16202a;
  color: #ffffff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 8px 24px;
}

.brand-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 8px;
  background: #2f8f83;
  font-weight: 800;
}

.brand-name {
  font-size: 17px;
  font-weight: 800;
}

.brand-caption {
  margin-top: 3px;
  color: #a8b3bd;
  font-size: 12px;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 0 12px;
  border-radius: 8px;
  color: #d8dee5;
  text-decoration: none;
  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.nav-item:hover,
.nav-item.active {
  background: #243342;
  color: #ffffff;
}

.main-area {
  min-width: 0;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  padding: 0 28px;
  border-bottom: 1px solid #e5e7eb;
  background: rgb(255 255 255 / 92%);
  backdrop-filter: blur(10px);
}

.topbar-title {
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #374151;
}

.content {
  padding: 28px;
}

@media (max-width: 820px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
    height: auto;
    padding: 14px;
  }

  .brand {
    padding-bottom: 14px;
  }

  .nav {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .nav-item {
    justify-content: center;
  }

  .topbar {
    padding: 0 16px;
  }

  .content {
    padding: 18px;
  }
}
</style>
