<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const activeTab = ref<'login' | 'register'>('login')
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  nickname: ''
})

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    if (activeTab.value === 'login') {
      await auth.login({ username: form.username, password: form.password })
    } else {
      await auth.register({
        username: form.username,
        password: form.password,
        nickname: form.nickname
      })
    }
    ElMessage.success(activeTab.value === 'login' ? '登录成功' : '注册成功')
    router.push((route.query.redirect as string) || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-visual">
      <div class="system-name">English Words</div>
      <h1>把词库、抽查和熟练度放到一条清楚的学习线上</h1>
      <p>管理自己的词库，按到期进度复习，用随机抽查把记忆拉回到真实使用里。</p>
      <div class="visual-grid">
        <div class="visual-tile">
          <strong>SM-2</strong>
          <span>间隔重复</span>
        </div>
        <div class="visual-tile">
          <strong>CSV</strong>
          <span>批量导入</span>
        </div>
        <div class="visual-tile">
          <strong>JWT</strong>
          <span>独立账户</span>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>

      <el-form class="login-form" @submit.prevent>
        <el-form-item>
          <el-input v-model.trim="form.username" :prefix-icon="User" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            placeholder="密码"
            show-password
            size="large"
            type="password"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-form-item v-if="activeTab === 'register'">
          <el-input v-model.trim="form.nickname" placeholder="昵称" size="large" @keyup.enter="submit" />
        </el-form-item>
        <el-button class="submit-button" size="large" type="primary" :loading="loading" @click="submit">
          {{ activeTab === 'login' ? '登录' : '创建账户' }}
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 420px);
  min-height: 100vh;
  background:
    linear-gradient(135deg, rgb(22 32 42 / 96%), rgb(31 65 63 / 94%)),
    url("https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=1800&q=80");
  background-size: cover;
  background-position: center;
}

.login-visual {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 56px;
  color: #ffffff;
}

.system-name {
  margin-bottom: 24px;
  font-weight: 800;
  letter-spacing: 0;
  color: #9be0d1;
}

.login-visual h1 {
  max-width: 720px;
  margin: 0;
  font-size: 48px;
  line-height: 1.12;
  letter-spacing: 0;
}

.login-visual p {
  max-width: 620px;
  margin: 18px 0 0;
  color: #d8e4e2;
  font-size: 17px;
  line-height: 1.8;
}

.visual-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 140px));
  gap: 12px;
  margin-top: 42px;
}

.visual-tile {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 16px;
  border: 1px solid rgb(255 255 255 / 18%);
  border-radius: 8px;
  background: rgb(255 255 255 / 9%);
}

.visual-tile strong {
  font-size: 20px;
}

.visual-tile span {
  color: #d8e4e2;
}

.login-panel {
  align-self: center;
  margin-right: 56px;
  padding: 28px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 24px 70px rgb(0 0 0 / 26%);
}

.login-form {
  padding-top: 12px;
}

.submit-button {
  width: 100%;
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-visual {
    min-height: 44vh;
    padding: 32px 20px;
  }

  .login-visual h1 {
    font-size: 34px;
  }

  .visual-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .login-panel {
    width: calc(100% - 40px);
    max-width: 460px;
    margin: 0 auto 28px;
  }
}
</style>
