<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const activeTab = ref<'login' | 'register'>('login')
const loading = ref(false)
const usernameMinLength = 3
const usernameMaxLength = 64
const nicknameMinLength = 3
const nicknameMaxLength = 64

const form = reactive({
  username: '',
  password: '',
  nickname: ''
})

const nicknameError = computed(() => {
  if (activeTab.value !== 'register' || !form.nickname) {
    return ''
  }
  if (form.nickname.length < nicknameMinLength) {
    return `昵称不能少于 ${nicknameMinLength} 个字符，当前已输入 ${form.nickname.length} 个字符`
  }
  if (form.nickname.length > nicknameMaxLength) {
    return `昵称不能超过 ${nicknameMaxLength} 个字符，当前已输入 ${form.nickname.length} 个字符`
  }
  return ''
})

const usernameError = computed(() => {
  if (!form.username) {
    return ''
  }
  if (form.username.length < usernameMinLength) {
    return `用户名长度必须在 ${usernameMinLength} 到 ${usernameMaxLength} 个字符之间，当前已输入 ${form.username.length} 个字符`
  }
  if (form.username.length > usernameMaxLength) {
    return `用户名长度必须在 ${usernameMinLength} 到 ${usernameMaxLength} 个字符之间，当前已输入 ${form.username.length} 个字符`
  }
  return ''
})

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (usernameError.value) {
    ElMessage.warning(usernameError.value)
    return
  }
  if (nicknameError.value) {
    ElMessage.warning(nicknameError.value)
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
        nickname: form.nickname || undefined
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
          <div class="field-stack">
            <el-input v-model.trim="form.username" :prefix-icon="User" placeholder="用户名" size="large" />
            <p v-if="usernameError" class="field-tip error">{{ usernameError }}</p>
          </div>
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
          <div class="field-stack">
            <el-input v-model.trim="form.nickname" placeholder="昵称" size="large" @keyup.enter="submit" />
            <p v-if="nicknameError" class="field-tip error">{{ nicknameError }}</p>
          </div>
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

.field-stack {
  width: 100%;
}

.field-tip {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.5;
}

.field-tip.error {
  color: #c45656;
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
