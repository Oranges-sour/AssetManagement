<script setup lang="ts">
import { computed, h, reactive, ref } from 'vue'
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

type AssetBrief = {
  id: number
  assetNo: string
  assetName: string
  roomNo: string
  status: number
}

type Assignee = {
  id: number
  empNo: string
  name: string
  phone?: string
  remark?: string
}

const message = useMessage()
const dialog = useDialog()

const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

const isModalOpen = ref(false)
const isEditMode = ref(false)
const isAssetsOpen = ref(false)
const formRef = ref<InstanceType<typeof NForm> | null>(null)

const formModel = reactive<Assignee>({
  id: 0,
  empNo: '',
  name: '',
  phone: '',
  remark: '',
})

const rules = {
  empNo: { required: true, message: '请输入工号', trigger: ['blur', 'input'] },
  name: { required: true, message: '请输入姓名', trigger: ['blur', 'input'] },
}

const list = ref<Assignee[]>([
  { id: 1, empNo: 'E1001', name: '张三', phone: '13800000000', remark: '行政' },
  { id: 2, empNo: 'E1002', name: '李四', phone: '13900000000', remark: '研发' },
])

const assetList = ref<AssetBrief[]>([
  { id: 1, assetNo: 'AS0002', assetName: '投影仪', roomNo: 'B-210', status: 1 },
  { id: 2, assetNo: 'AS0005', assetName: '显示器', roomNo: 'B-210', status: 1 },
])

const columns = computed(() => [
  { title: '工号', key: 'empNo' },
  { title: '姓名', key: 'name' },
  { title: '电话', key: 'phone' },
  { title: '备注', key: 'remark' },
  {
    title: '操作',
    key: 'actions',
    render: (row: Assignee) => [
      h(
        NButton,
        { size: 'small', tertiary: true, onClick: () => handleShowAssets(row) },
        { default: () => '名下资产' },
      ),
      h(
        NButton,
        { size: 'small', tertiary: true, style: 'margin-left: 8px', onClick: () => handleEdit(row) },
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

const assetColumns = computed(() => [
  { title: '资产编号', key: 'assetNo' },
  { title: '资产名称', key: 'assetName' },
  { title: '位置', key: 'roomNo' },
  {
    title: '状态',
    key: 'status',
    render: (row: AssetBrief) => (row.status === 1 ? '领用' : '闲置'),
  },
])

const resetForm = () => {
  formModel.id = 0
  formModel.empNo = ''
  formModel.name = ''
  formModel.phone = ''
  formModel.remark = ''
}

const handleSearch = () => {
  page.value = 1
  message.success('已应用筛选条件')
  // TODO: 接入后端分页查询
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

const handleEdit = (row: Assignee) => {
  isEditMode.value = true
  formModel.id = row.id
  formModel.empNo = row.empNo
  formModel.name = row.name
  formModel.phone = row.phone ?? ''
  formModel.remark = row.remark ?? ''
  isModalOpen.value = true
}

const handleDelete = (row: Assignee) => {
  dialog.warning({
    title: '确认删除',
    content: `确定删除领用人 ${row.name} 吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: () => {
      list.value = list.value.filter((item) => item.id !== row.id)
      message.success('删除成功')
    },
  })
}

const handleShowAssets = (row: Assignee) => {
  message.info(`查看 ${row.name} 名下资产`)
  isAssetsOpen.value = true
  // TODO: 接入后端查询名下资产
}

const handleSubmit = () => {
  formRef.value?.validate((errors) => {
    if (errors) {
      return
    }
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
  <n-card title="领用人管理">
    <n-space vertical size="large">
      <n-space align="center" justify="space-between">
        <n-space align="center">
          <n-input
            v-model:value="keyword"
            placeholder="输入工号或姓名"
            clearable
            style="width: 220px"
          />
          <n-button type="primary" @click="handleSearch">查询</n-button>
          <n-button @click="handleReset">重置</n-button>
        </n-space>
        <n-button type="primary" @click="handleOpenCreate">新增领用人</n-button>
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

  <n-modal v-model:show="isModalOpen" preset="card" :title="isEditMode ? '编辑领用人' : '新增领用人'">
    <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="left" label-width="80">
      <n-form-item label="工号" path="empNo">
        <n-input v-model:value="formModel.empNo" placeholder="如 E1001" />
      </n-form-item>
      <n-form-item label="姓名" path="name">
        <n-input v-model:value="formModel.name" placeholder="如 张三" />
      </n-form-item>
      <n-form-item label="电话" path="phone">
        <n-input v-model:value="formModel.phone" placeholder="可选" />
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

  <n-modal v-model:show="isAssetsOpen" preset="card" title="名下资产">
    <n-data-table :columns="assetColumns" :data="assetList" :bordered="false" />
    <template #footer>
      <n-space justify="end">
        <n-button @click="isAssetsOpen = false">关闭</n-button>
      </n-space>
    </template>
  </n-modal>
</template>
