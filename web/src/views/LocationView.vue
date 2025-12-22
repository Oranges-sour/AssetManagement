<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
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
  useDialog,
  useMessage,
} from 'naive-ui'
import { createLocation, deleteLocation, fetchDepartments, fetchLocations, updateLocation } from '@/api'

type DepartmentOption = {
  label: string
  value: number
}

type LocationSpace = {
  id: number
  deptId: number
  deptName: string
  roomNo: string
  area: number
  remark?: string
}

const message = useMessage()
const dialog = useDialog()

const keyword = ref('')
const deptFilter = ref<number | null>(null)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

const isModalOpen = ref(false)
const isEditMode = ref(false)
const formRef = ref<InstanceType<typeof NForm> | null>(null)

const departmentOptions = ref<DepartmentOption[]>([])

const formModel = reactive<LocationSpace>({
  id: 0,
  deptId: 0,
  deptName: '',
  roomNo: '',
  area: 0,
  remark: '',
})

const rules = {
  deptId: { required: true, message: '请选择所属部门', trigger: ['blur', 'change'] },
  roomNo: { required: true, message: '请输入房间号', trigger: ['blur', 'input'] },
  area: { required: true, type: 'number', message: '请输入面积', trigger: ['blur', 'change'] },
}

const list = ref<LocationSpace[]>([])

const columns = computed(() => [
  { title: '所属部门', key: 'deptName' },
  { title: '房间号', key: 'roomNo' },
  { title: '面积(㎡)', key: 'area' },
  { title: '备注', key: 'remark' },
  {
    title: '操作',
    key: 'actions',
    render: (row: LocationSpace) => [
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
  formModel.deptId = 0
  formModel.deptName = ''
  formModel.roomNo = ''
  formModel.area = 0
  formModel.remark = ''
}


const loadDepartments = async () => {
  try {
    const data = await fetchDepartments({ page: 1, size: 1000 })
    departmentOptions.value = data.list.map((item) => ({
      label: item.deptName,
      value: item.id,
    }))
  } catch (error) {
    message.error((error as Error).message || '部门列表加载失败')
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const data = await fetchLocations({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      deptId: deptFilter.value ?? undefined,
    })
    list.value = data.list
    total.value = data.total
  } catch (error) {
    message.error((error as Error).message || '位置空间列表加载失败')
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
  handleSearch()
}

const handleOpenCreate = () => {
  isEditMode.value = false
  resetForm()
  isModalOpen.value = true
}

const handleEdit = (row: LocationSpace) => {
  isEditMode.value = true
  formModel.id = row.id
  formModel.deptId = row.deptId
  formModel.deptName = row.deptName
  formModel.roomNo = row.roomNo
  formModel.area = row.area
  formModel.remark = row.remark ?? ''
  isModalOpen.value = true
}

const handleDelete = (row: LocationSpace) => {
  dialog.warning({
    title: '确认删除',
    content: `确定删除位置空间 ${row.roomNo} 吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteLocation(row.id)
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
      deptId: formModel.deptId,
      roomNo: formModel.roomNo,
      area: formModel.area,
      remark: formModel.remark,
    }
    const action = isEditMode.value
      ? updateLocation(formModel.id, payload)
      : createLocation(payload)
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

onMounted(() => {
  loadDepartments()
  loadList()
})
</script>

<script lang="ts">
export default {
  name: 'LocationView',
}
</script>

<template>
  <n-card title="位置空间管理">
    <n-space vertical size="large">
      <n-space align="center" justify="space-between">
        <n-space align="center">
          <n-select
            v-model:value="deptFilter"
            placeholder="选择部门"
            clearable
            :options="departmentOptions"
            style="width: 200px"
          />
          <n-input v-model:value="keyword" placeholder="输入房间号" clearable style="width: 200px" />
          <n-button type="primary" @click="handleSearch">查询</n-button>
          <n-button @click="handleReset">重置</n-button>
        </n-space>
        <n-button type="primary" @click="handleOpenCreate">新增位置空间</n-button>
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

  <n-modal v-model:show="isModalOpen" preset="card" :title="isEditMode ? '编辑位置空间' : '新增位置空间'">
    <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="left" label-width="90">
      <n-form-item label="所属部门" path="deptId">
        <n-select
          v-model:value="formModel.deptId"
          placeholder="请选择部门"
          :options="departmentOptions"
        />
      </n-form-item>
      <n-form-item label="房间号" path="roomNo">
        <n-input v-model:value="formModel.roomNo" placeholder="如 A-301" />
      </n-form-item>
      <n-form-item label="面积(㎡)" path="area">
        <n-input-number v-model:value="formModel.area" :min="0" :step="1" style="width: 100%" />
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
