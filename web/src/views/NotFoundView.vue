<script setup lang="ts">
import { NResult, NButton } from 'naive-ui'
import { useRouter } from 'vue-router'

const router = useRouter()

const handleBack = () => {
  router.push('/departments')
}
</script>

<template>
  <n-result status="404" title="页面不存在" description="请检查访问地址是否正确">
    <template #footer>
      <n-button type="primary" @click="handleBack">返回部门管理</n-button>
    </template>
  </n-result>
</template>
