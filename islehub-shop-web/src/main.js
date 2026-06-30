import { createApp } from 'vue'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElCascader } from 'element-plus/es/components/cascader/index.mjs'
import { ElCheckbox } from 'element-plus/es/components/checkbox/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElDropdown, ElDropdownItem, ElDropdownMenu } from 'element-plus/es/components/dropdown/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs'
import { ElPagination } from 'element-plus/es/components/pagination/index.mjs'
import { ElSkeleton, ElSkeletonItem } from 'element-plus/es/components/skeleton/index.mjs'
import { ElSwitch } from 'element-plus/es/components/switch/index.mjs'
import { ElTag } from 'element-plus/es/components/tag/index.mjs'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/cascader/style/css'
import 'element-plus/es/components/checkbox/style/css'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/dropdown/style/css'
import 'element-plus/es/components/form/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/input-number/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/pagination/style/css'
import 'element-plus/es/components/skeleton/style/css'
import 'element-plus/es/components/skeleton-item/style/css'
import 'element-plus/es/components/switch/style/css'
import 'element-plus/es/components/tag/style/css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
;[
  ElButton,
  ElCascader,
  ElCheckbox,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElPagination,
  ElSkeleton,
  ElSkeletonItem,
  ElSwitch,
  ElTag
].forEach(component => app.use(component))
app.use(router)
app.mount('#app')
