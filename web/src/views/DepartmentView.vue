<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NInput,
  NModal,
  NPagination,
  NSpace,
  useDialog,
  useMessage,
} from 'naive-ui'
import { createDepartment, deleteDepartment, fetchDepartments, updateDepartment } from '@/api'

type Department = {
  id: number
  deptCode: string
  deptName: string
  remark?: string
}

const message = useMessage()
const dialog = useDialog()

const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

const isModalOpen = ref(false)
const isEditMode = ref(false)
const formRef = ref<InstanceType<typeof NForm> | null>(null)

const formModel = reactive<Department>({
  id: 0,
  deptCode: '',
  deptName: '',
  remark: '',
})

const rules = {
  deptCode: { required: true, message: '请输入部门编码', trigger: ['blur', 'input'] },
  deptName: { required: true, message: '请输入部门名称', trigger: ['blur', 'input'] },
}

const list = ref<Department[]>([])

const columns = computed(() => [
  { title: '部门编码', key: 'deptCode' },
  { title: '部门名称', key: 'deptName' },
  { title: '备注', key: 'remark' },
  {
    title: '操作',
    key: 'actions',
    render: (row: Department) => [
      h(
        NButton,
        { size: 'small', tertiary: true, onClick: () => handleEdit(row) },
        { default: () => '编辑' },
      ),
      h(
        NButton,
        {
          size: 'small',
          tertiary: true,
          type: 'error',
          style: 'margin-left: 8px',
          onClick: () => handleDelete(row),
        },
        { default: () => '删除' },
      ),
    ],
  },
])

const resetForm = () => {
  formModel.id = 0
  formModel.deptCode = ''
  formModel.deptName = ''
  formModel.remark = ''
}

const loadList = async () => {
  loading.value = true
  try {
    const data = await fetchDepartments({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
    })
    list.value = data.list
    total.value = data.total
  } catch (error) {
    message.error((error as Error).message || '部门列表加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadList()
}

const handleReset = () => {
  keyword.value = ''
  handleSearch()
}

const handleOpenCreate = () => {
  isEditMode.value = false
  resetForm()
  isModalOpen.value = true
}

const handleEdit = (row: Department) => {
  isEditMode.value = true
  formModel.id = row.id
  formModel.deptCode = row.deptCode
  formModel.deptName = row.deptName
  formModel.remark = row.remark ?? ''
  isModalOpen.value = true
}

const handleDelete = (row: Department) => {
  dialog.warning({
    title: '确认删除',
    content: `确定删除部门 ${row.deptName} 吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteDepartment(row.id)
        message.success('删除成功')
        loadList()
      } catch (error) {
        message.error((error as Error).message || '删除失败')
      }
    },
  })
}

const handleSubmit = () => {
  formRef.value?.validate((errors) => {
    if (errors) {
      return
    }
    const payload = {
      deptCode: formModel.deptCode,
      deptName: formModel.deptName,
      remark: formModel.remark,
    }
    const action = isEditMode.value
      ? updateDepartment(formModel.id, payload)
      : createDepartment(payload)
    action
      .then(() => {
        message.success(isEditMode.value ? '更新成功' : '新增成功')
        isModalOpen.value = false
        if (!isEditMode.value) {
          page.value = 1
        }
        loadList()
      })
      .catch((error) => {
        message.error((error as Error).message || '保存失败')
      })
  })
}

onMounted(loadList)
</script>

<script lang="ts">
export default {
  name: 'DepartmentView',
}
</script>

<template>
  <n-card title="部门管理">
    <n-space vertical size="large">
      <n-space align="center" justify="space-between">
        <n-space align="center">
          <n-input
            v-model:value="keyword"
            placeholder="输入部门编码或名称"
            clearable
            style="width: 220px"
          />
          <n-button type="primary" @click="handleSearch">查询</n-button>
          <n-button @click="handleReset">重置</n-button>
        </n-space>
        <n-button type="primary" @click="handleOpenCreate">新增部门</n-button>
      </n-space>

      <n-data-table :columns="columns" :data="list" :bordered="false" :loading="loading" />

      <n-pagination
        v-model:page="page"
        v-model:page-size="size"
        :item-count="total || list.length"
        show-size-picker
        @update:page="loadList"
        @update:page-size="loadList"
      />
    </n-space>
  </n-card>

  <n-modal v-model:show="isModalOpen" preset="card" :title="isEditMode ? '编辑部门' : '新增部门'">
    <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="left" label-width="80">
      <n-form-item label="部门编码" path="deptCode">
        <n-input v-model:value="formModel.deptCode" placeholder="如 D001" />
      </n-form-item>
      <n-form-item label="部门名称" path="deptName">
        <n-input v-model:value="formModel.deptName" placeholder="如 行政部" />
      </n-form-item>
      <n-form-item label="备注" path="remark">
        <n-input v-model:value="formModel.remark" placeholder="可选" />
      </n-form-item>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button @click="isModalOpen = false">取消</n-button>
        <n-button type="primary" @click="handleSubmit">保存</n-button>
      </n-space>
    </template>
  </n-modal>
</template>
