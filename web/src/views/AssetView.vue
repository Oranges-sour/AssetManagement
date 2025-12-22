<script setup lang="ts">
import { computed, h, reactive, ref } from 'vue'
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

type SelectOption = {
  label: string
  value: number | string
}

type Asset = {
  id: number
  assetNo: string
  assetName: string
  value: number
  deptId: number
  deptName: string
  locationId: number
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

const isModalOpen = ref(false)
const isEditMode = ref(false)
const formRef = ref<InstanceType<typeof NForm> | null>(null)

const deptOptions = ref<SelectOption[]>([
  { label: '行政部', value: 1 },
  { label: '研发部', value: 2 },
])

const locationOptions = ref<SelectOption[]>([
  { label: 'A-301', value: 11 },
  { label: 'B-210', value: 12 },
])

const assigneeOptions = ref<SelectOption[]>([
  { label: '张三', value: 21 },
  { label: '李四', value: 22 },
])

const statusOptions: SelectOption[] = [
  { label: '闲置', value: 0 },
  { label: '领用', value: 1 },
]

const formModel = reactive<Asset>({
  id: 0,
  assetNo: '',
  assetName: '',
  value: 0,
  deptId: 0,
  deptName: '',
  locationId: 0,
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
  deptId: { required: true, message: '请选择部门', trigger: ['blur', 'change'] },
  locationId: { required: true, message: '请选择位置空间', trigger: ['blur', 'change'] },
}

const list = ref<Asset[]>([
  {
    id: 1,
    assetNo: 'AS0001',
    assetName: '笔记本电脑',
    value: 8000,
    deptId: 1,
    deptName: '行政部',
    locationId: 11,
    roomNo: 'A-301',
    assigneeId: null,
    assigneeName: null,
    status: 0,
    remark: '轻薄本',
  },
  {
    id: 2,
    assetNo: 'AS0002',
    assetName: '投影仪',
    value: 12000,
    deptId: 2,
    deptName: '研发部',
    locationId: 12,
    roomNo: 'B-210',
    assigneeId: 21,
    assigneeName: '张三',
    status: 1,
    remark: '会议室',
  },
])

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
  formModel.deptId = 0
  formModel.deptName = ''
  formModel.locationId = 0
  formModel.roomNo = ''
  formModel.assigneeId = null
  formModel.assigneeName = null
  formModel.status = 0
  formModel.remark = ''
}

const syncDeptName = (deptId: number) => {
  const option = deptOptions.value.find((item) => item.value === deptId)
  formModel.deptName = option?.label ?? ''
}

const syncRoomName = (locationId: number) => {
  const option = locationOptions.value.find((item) => item.value === locationId)
  formModel.roomNo = option?.label ?? ''
}

const handleSearch = () => {
  page.value = 1
  message.success('已应用筛选条件')
  // TODO: 接入后端分页查询
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
  isModalOpen.value = true
}

const handleDelete = (row: Asset) => {
  dialog.warning({
    title: '确认删除',
    content: `确定删除资产 ${row.assetName} 吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: () => {
      list.value = list.value.filter((item) => item.id !== row.id)
      message.success('删除成功')
    },
  })
}

const handleAssignToggle = (row: Asset) => {
  if (row.status === 1) {
    row.status = 0
    row.assigneeId = null
    row.assigneeName = null
    message.success('已归还资产')
  } else {
    const defaultAssignee = assigneeOptions.value[0]
    row.status = 1
    row.assigneeId = Number(defaultAssignee.value)
    row.assigneeName = defaultAssignee.label
    message.success('已领用资产')
  }
}

const handleSubmit = () => {
  formRef.value?.validate((errors) => {
    if (errors) {
      return
    }
    syncDeptName(formModel.deptId)
    syncRoomName(formModel.locationId)
    if (isEditMode.value) {
      const index = list.value.findIndex((item) => item.id === formModel.id)
      if (index !== -1) {
        list.value[index] = { ...formModel }
      }
      message.success('更新成功')
    } else {
      const nextId = Math.max(0, ...list.value.map((item) => item.id)) + 1
      list.value.unshift({ ...formModel, id: nextId })
      message.success('新增成功')
    }
    isModalOpen.value = false
  })
}
</script>

<template>
  <n-card title="资产管理">
    <n-space vertical size="large">
      <n-space align="center" justify="space-between">
        <n-space align="center" wrap>
          <n-input
            v-model:value="keyword"
            placeholder="资产编号/名称"
            clearable
            style="width: 180px"
          />
          <n-select
            v-model:value="deptFilter"
            placeholder="部门"
            clearable
            :options="deptOptions"
            style="width: 160px"
          />
          <n-select
            v-model:value="locationFilter"
            placeholder="位置空间"
            clearable
            :options="locationOptions"
            style="width: 160px"
          />
          <n-select
            v-model:value="assigneeFilter"
            placeholder="领用人"
            clearable
            :options="assigneeOptions"
            style="width: 160px"
          />
          <n-select
            v-model:value="statusFilter"
            placeholder="状态"
            clearable
            :options="statusOptions"
            style="width: 120px"
          />
          <n-button type="primary" @click="handleSearch">查询</n-button>
          <n-button @click="handleReset">重置</n-button>
        </n-space>
        <n-button type="primary" @click="handleOpenCreate">新增资产</n-button>
      </n-space>

      <n-data-table :columns="columns" :data="list" :bordered="false" />

      <n-pagination
        v-model:page="page"
        v-model:page-size="size"
        :item-count="total || list.length"
        show-size-picker
      />
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
        <n-select
          v-model:value="formModel.locationId"
          placeholder="请选择位置空间"
          :options="locationOptions"
        />
      </n-form-item>
      <n-form-item label="领用人" path="assigneeId">
        <n-select
          v-model:value="formModel.assigneeId"
          placeholder="闲置可不选"
          clearable
          :options="assigneeOptions"
        />
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
