<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NModal,
  NPagination,
  NSelect,
  NSpace,
  NTag,
  useDialog,
  useMessage,
} from 'naive-ui'
import {
  assignAsset,
  createAsset,
  deleteAsset,
  fetchAssignees,
  fetchAssets,
  fetchDepartments,
  fetchLocations,
  returnAsset,
  updateAsset,
} from '@/api'

type SelectOption = {
  label: string
  value: number | string
}

type Asset = {
  id: number
  assetNo: string
  assetName: string
  value: number
  deptId: number | null
  deptName: string
  locationId: number | null
  roomNo: string
  assigneeId: number | null
  assigneeName: string | null
  status: number
  remark?: string
}

const message = useMessage()
const dialog = useDialog()

const keyword = ref('')
const deptFilter = ref<number | null>(null)
const locationFilter = ref<number | null>(null)
const assigneeFilter = ref<number | null>(null)
const statusFilter = ref<number | null>(null)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

const isModalOpen = ref(false)
const isEditMode = ref(false)
const formRef = ref<InstanceType<typeof NForm> | null>(null)

const deptOptions = ref<SelectOption[]>([])
const locationOptions = ref<SelectOption[]>([])
const formLocationOptions = ref<SelectOption[]>([])
const assigneeOptions = ref<SelectOption[]>([])

const statusOptions: SelectOption[] = [
  { label: '闲置', value: 0 },
  { label: '领用', value: 1 },
]

const formModel = reactive<Asset>({
  id: 0,
  assetNo: '',
  assetName: '',
  value: 0,
  deptId: null,
  deptName: '',
  locationId: null,
  roomNo: '',
  assigneeId: null,
  assigneeName: null,
  status: 0,
  remark: '',
})

const rules = {
  assetNo: { required: true, message: '请输入资产编号', trigger: ['blur', 'input'] },
  assetName: { required: true, message: '请输入资产名称', trigger: ['blur', 'input'] },
  value: { required: true, type: 'number', message: '请输入资产价值', trigger: ['blur', 'change'] },
  deptId: { required: true, type: 'number', message: '请选择部门', trigger: ['blur', 'change'] },
  locationId: { required: true, type: 'number', message: '请选择位置空间', trigger: ['blur', 'change'] },
}

const list = ref<Asset[]>([])

const assignModalOpen = ref(false)
const assignTarget = ref<Asset | null>(null)
const selectedAssignee = ref<number | null>(null)

const statusLabel = (value: number) => (value === 1 ? '领用' : '闲置')

const columns = computed(() => [
  { title: '资产编号', key: 'assetNo' },
  { title: '资产名称', key: 'assetName' },
  { title: '价值(元)', key: 'value' },
  { title: '部门', key: 'deptName' },
  { title: '位置', key: 'roomNo' },
  { title: '领用人', key: 'assigneeName' },
  {
    title: '状态',
    key: 'status',
    render: (row: Asset) =>
      h(
        NTag,
        { type: row.status === 1 ? 'success' : 'warning', bordered: false },
        { default: () => statusLabel(row.status) },
      ),
  },
  { title: '备注', key: 'remark' },
  {
    title: '操作',
    key: 'actions',
    render: (row: Asset) => [
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
          type: 'info',
          style: 'margin-left: 8px',
          onClick: () => handleAssignToggle(row),
        },
        { default: () => (row.status === 1 ? '归还' : '领用') },
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
  formModel.assetNo = ''
  formModel.assetName = ''
  formModel.value = 0
  formModel.deptId = null
  formModel.deptName = ''
  formModel.locationId = null
  formModel.roomNo = ''
  formModel.assigneeId = null
  formModel.assigneeName = null
  formModel.status = 0
  formModel.remark = ''
}

const loadDepartments = async () => {
  try {
    const data = await fetchDepartments({ page: 1, size: 1000 })
    deptOptions.value = data.list.map((item) => ({
      label: item.deptName,
      value: item.id,
    }))
  } catch (error) {
    message.error((error as Error).message || '部门列表加载失败')
  }
}

const loadAssignees = async () => {
  try {
    const data = await fetchAssignees({ page: 1, size: 1000 })
    assigneeOptions.value = data.list.map((item) => ({
      label: item.name,
      value: item.id,
    }))
  } catch (error) {
    message.error((error as Error).message || '领用人列表加载失败')
  }
}

const loadLocations = async (deptId?: number, target: 'filter' | 'form' = 'filter') => {
  try {
    const data = await fetchLocations({ page: 1, size: 1000, deptId })
    const options = data.list.map((item) => ({
      label: item.roomNo,
      value: item.id,
    }))
    if (target === 'form') {
      formLocationOptions.value = options
    } else {
      locationOptions.value = options
    }
  } catch (error) {
    message.error((error as Error).message || '位置空间列表加载失败')
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const data = await fetchAssets({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      deptId: deptFilter.value ?? undefined,
      locationId: locationFilter.value ?? undefined,
      assigneeId: assigneeFilter.value ?? undefined,
      status: statusFilter.value ?? undefined,
    })
    list.value = data.list
    total.value = data.total
  } catch (error) {
    message.error((error as Error).message || '资产列表加载失败')
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
  deptFilter.value = null
  locationFilter.value = null
  assigneeFilter.value = null
  statusFilter.value = null
  handleSearch()
}

const handleOpenCreate = () => {
  isEditMode.value = false
  resetForm()
  loadLocations(undefined, 'form')
  isModalOpen.value = true
}

const handleEdit = (row: Asset) => {
  isEditMode.value = true
  formModel.id = row.id
  formModel.assetNo = row.assetNo
  formModel.assetName = row.assetName
  formModel.value = row.value
  formModel.deptId = row.deptId
  formModel.deptName = row.deptName
  formModel.locationId = row.locationId
  formModel.roomNo = row.roomNo
  formModel.assigneeId = row.assigneeId
  formModel.assigneeName = row.assigneeName
  formModel.status = row.status
  formModel.remark = row.remark ?? ''
  if (row.deptId) {
    loadLocations(row.deptId, 'form')
  } else {
    loadLocations(undefined, 'form')
  }
  isModalOpen.value = true
}

const handleDelete = (row: Asset) => {
  dialog.warning({
    title: '确认删除',
    content: `确定删除资产 ${row.assetName} 吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteAsset(row.id)
        message.success('删除成功')
        loadList()
      } catch (error) {
        message.error((error as Error).message || '删除失败')
      }
    },
  })
}

const handleAssignToggle = (row: Asset) => {
  if (row.status === 1) {
    dialog.warning({
      title: '确认归还',
      content: `确定归还资产 ${row.assetName} 吗？`,
      positiveText: '归还',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          await returnAsset(row.id)
          message.success('已归还资产')
          loadList()
        } catch (error) {
          message.error((error as Error).message || '归还失败')
        }
      },
    })
  } else {
    assignTarget.value = row
    selectedAssignee.value = null
    assignModalOpen.value = true
  }
}

const handleAssignSubmit = async () => {
  if (!assignTarget.value || !selectedAssignee.value) {
    message.error('请选择领用人')
    return
  }
  try {
    await assignAsset(assignTarget.value.id, selectedAssignee.value)
    message.success('领用成功')
    assignModalOpen.value = false
    loadList()
  } catch (error) {
    message.error((error as Error).message || '领用失败')
  }
}

const handleSubmit = () => {
  formRef.value?.validate((errors) => {
    if (errors) {
      return
    }
    const payload = {
      assetNo: formModel.assetNo,
      assetName: formModel.assetName,
      value: formModel.value,
      locationId: formModel.locationId,
      assigneeId: formModel.assigneeId,
      status: formModel.assigneeId ? 1 : 0,
      remark: formModel.remark,
    }
    const action = isEditMode.value
      ? updateAsset(formModel.id, payload)
      : createAsset(payload)
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

watch(deptFilter, (value) => {
  locationFilter.value = null
  loadLocations(value ?? undefined, 'filter')
})

watch(
  () => formModel.deptId,
  (value) => {
    formModel.locationId = null
    if (value) {
      loadLocations(value, 'form')
    } else {
      loadLocations(undefined, 'form')
    }
  },
)

onMounted(() => {
  loadDepartments()
  loadAssignees()
  loadLocations(undefined, 'filter')
  loadLocations(undefined, 'form')
  loadList()
})
</script>

<script lang="ts">
export default {
  name: 'AssetView',
}
</script>

<template>
  <n-card title="资产管理">
    <n-space vertical size="large">
      <n-space align="center" justify="space-between">
        <n-space align="center" wrap>
          <n-input v-model:value="keyword" placeholder="资产编号/名称" clearable style="width: 180px" />
          <n-select v-model:value="deptFilter" placeholder="部门" clearable :options="deptOptions" style="width: 160px" />
          <n-select v-model:value="locationFilter" placeholder="位置空间" clearable :options="locationOptions"
            style="width: 160px" />
          <n-select v-model:value="assigneeFilter" placeholder="领用人" clearable :options="assigneeOptions"
            style="width: 160px" />
          <n-select v-model:value="statusFilter" placeholder="状态" clearable :options="statusOptions"
            style="width: 120px" />
          <n-button type="primary" @click="handleSearch">查询</n-button>
          <n-button @click="handleReset">重置</n-button>
        </n-space>
        <n-button type="primary" @click="handleOpenCreate">新增资产</n-button>
      </n-space>

      <n-data-table :columns="columns" :data="list" :bordered="false" :loading="loading" />

      <n-pagination v-model:page="page" v-model:page-size="size" :item-count="total || list.length" show-size-picker
        @update:page="loadList" @update:page-size="loadList" />
    </n-space>
  </n-card>

  <n-modal v-model:show="isModalOpen" preset="card" :title="isEditMode ? '编辑资产' : '新增资产'">
    <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="left" label-width="90">
      <n-form-item label="资产编号" path="assetNo">
        <n-input v-model:value="formModel.assetNo" placeholder="如 AS0001" />
      </n-form-item>
      <n-form-item label="资产名称" path="assetName">
        <n-input v-model:value="formModel.assetName" placeholder="如 笔记本电脑" />
      </n-form-item>
      <n-form-item label="价值(元)" path="value">
        <n-input-number v-model:value="formModel.value" :min="0" :step="100" style="width: 100%" />
      </n-form-item>
      <n-form-item label="部门" path="deptId">
        <n-select v-model:value="formModel.deptId" placeholder="请选择部门" :options="deptOptions" />
      </n-form-item>
      <n-form-item label="位置空间" path="locationId">
        <n-select v-model:value="formModel.locationId" placeholder="请选择位置空间" :options="formLocationOptions" />
      </n-form-item>
      <n-form-item label="领用人" path="assigneeId">
        <n-select v-model:value="formModel.assigneeId" placeholder="闲置可不选" clearable :options="assigneeOptions" />
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

  <n-modal v-model:show="assignModalOpen" preset="card" title="资产领用">
    <n-form label-placement="left" label-width="80">
      <n-form-item label="领用人">
        <n-select v-model:value="selectedAssignee" placeholder="请选择领用人" :options="assigneeOptions" />
      </n-form-item>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button @click="assignModalOpen = false">取消</n-button>
        <n-button type="primary" @click="handleAssignSubmit">确认领用</n-button>
      </n-space>
    </template>
  </n-modal>
</template>
