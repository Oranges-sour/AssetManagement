<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import {
  NConfigProvider,
  NLayout,
  NLayoutHeader,
  NLayoutFooter,
  NLayoutSider,
  NLayoutContent,
  NMenu,
  NMessageProvider,
  NDialogProvider,
  NNotificationProvider,
  NModal,
  NButton,
  NSpace,
} from 'naive-ui'
import { checkHealth } from '@/api'

const route = useRoute()
const router = useRouter()

const menuOptions = [
  { label: '部门管理', key: '/departments' },
  { label: '位置空间', key: '/locations' },
  { label: '资产管理', key: '/assets' },
  { label: '领用人管理', key: '/assignees' },
]

const activeKey = computed(() => {
  const matched = menuOptions.find((item) => route.path.startsWith(item.key))
  return matched?.key ?? '/departments'
})

const handleMenuUpdate = (key: string) => {
  router.push(key)
}

const showHealthModal = ref(false)
const healthMessage = ref('网络未连接或后端服务不可用')

const runHealthCheck = async () => {
  try {
    await checkHealth()
  } catch (error) {
    showHealthModal.value = true
  }
}

onMounted(runHealthCheck)
</script>

<template>
  <n-config-provider>
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <n-layout class="app-shell" has-sider>
            <n-layout-sider
              bordered
              collapse-mode="width"
              :width="220"
              :collapsed-width="64"
              class="app-sider"
            >
              <div class="sider-brand">
                <div class="sider-title">资产管理系统</div>
                <div class="sider-subtitle">Asset Console</div>
              </div>
              <n-menu :value="activeKey" :options="menuOptions" @update:value="handleMenuUpdate" />
            </n-layout-sider>
            <n-layout class="app-main">
              <n-layout-header bordered class="app-header">
                <div class="header-title">公司资产管理</div>
                <div class="header-subtitle">部门 / 位置空间 / 资产 / 领用人</div>
              </n-layout-header>
              <n-layout-content class="app-content">
                <RouterView />
              </n-layout-content>
              <n-layout-footer bordered class="app-footer">
                <div class="footer-title">资产管理系统</div>
                <div class="footer-meta">© 2025 课程设计 · 制作人：Orange</div>
              </n-layout-footer>
            </n-layout>
          </n-layout>
          <n-modal v-model:show="showHealthModal" preset="card" title="后端状态异常">
            <div class="health-message">{{ healthMessage }}</div>
            <template #footer>
              <n-space justify="end">
                <n-button @click="showHealthModal = false">知道了</n-button>
              </n-space>
            </template>
          </n-modal>
        </n-notification-provider>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f6f4f0;
}

.app-sider {
  background: #fdfbf7;
  border-right: 1px solid #efe7db;
}

.sider-brand {
  padding: 20px 18px 12px;
  border-bottom: 1px solid #efe7db;
}

.sider-title {
  color: #3b2f24;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.sider-subtitle {
  margin-top: 6px;
  color: #8a7a6a;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.app-header {
  padding: 16px 24px;
  background: #fffaf2;
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-bottom: 1px solid #efe7db;
}

.app-main {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: #3b2f24;
}

.header-subtitle {
  font-size: 12px;
  color: #8a7a6a;
}

.app-content {
  padding: 24px;
  flex: 1;
}

.app-footer {
  padding: 16px 24px;
  background: #fffaf2;
  border-top: 1px solid #efe7db;
  color: #6b5a49;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.footer-title {
  font-size: 14px;
  font-weight: 600;
  color: #3b2f24;
}

.footer-meta {
  font-size: 12px;
  color: #8a7a6a;
}

.health-message {
  color: #5b4636;
}
</style>
