<template>
  <div class="host-container">
    <van-nav-bar title="浙江脉通智造 - 内部员工访客预预约" />

    <!-- 场景 A 批量预约面板 (去登录，面向员工快捷发送邮件) -->
    <div class="form-box" style="padding: 12px 8px;">
      <van-notice-bar left-icon="info-o" text="录入多名来访人员后提交，系统将自动生成唯一 Token 邀请函发送至各访客邮箱。" />

      <van-form @submit="submitBatchInvite" style="margin-top: 12px;">
        <van-cell-group inset title="到访单位与事由信息">
          <van-field v-model="batchForm.company" label="来访单位" placeholder="如：上海某某医疗科技有限公司" required :rules="[{ required: true }]" />
          
          <van-field
            v-model="batchForm.visitDate"
            is-link
            readonly
            label="到访日期"
            placeholder="请选择拟到访日期"
            required
            :rules="[{ required: true }]"
            @click="showDatePicker = true"
          />
          <van-calendar v-model:show="showDatePicker" @confirm="onDateConfirm" />

          <van-field
            v-model="batchTimeRangeDisplay"
            is-link
            readonly
            label="到访时间段"
            placeholder="选择到访时间段"
            required
            :rules="[{ required: true }]"
            @click="showTimePicker = true"
          />
          <van-popup v-model:show="showTimePicker" position="bottom">
            <van-picker :columns="timeRangeColumns" @confirm="onTimeRangeConfirm" @cancel="showTimePicker = false" />
          </van-popup>

          <van-field
            v-model="batchForm.visitPurpose"
            is-link
            readonly
            label="来访事由"
            placeholder="请选择来访事由"
            required
            :rules="[{ required: true }]"
            @click="showReasonPicker = true"
          />
          <van-popup v-model:show="showReasonPicker" position="bottom">
            <van-picker :columns="reasonColumns" @confirm="onReasonConfirm" @cancel="showReasonPicker = false" />
          </van-popup>
        </van-cell-group>

        <!-- 动态添加多名来访人员 -->
        <van-cell-group inset title="来访人员名单 (支持添加多人)" style="margin-top: 16px;">
          <div v-for="(v, index) in batchForm.visitors" :key="index" style="padding: 12px 16px; border-bottom: 1px dashed #ebedf0; position: relative;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
              <span style="font-weight: bold; color: #1989fa;">人员 #{{ index + 1 }}</span>
              <van-button v-if="batchForm.visitors.length > 1" size="mini" type="danger" plain @click="removeVisitor(index)">
                删除
              </van-button>
            </div>
            <van-field v-model="v.visitorName" label="姓名" placeholder="来访人员真实姓名" required :rules="[{ required: true }]" />
            <van-field v-model="v.phone" label="手机号" placeholder="访客手机号码" type="tel" required :rules="[{ required: true }]" />
            <van-field v-model="v.email" label="电子邮箱" placeholder="接收专属邀请函的 Email" type="email" required :rules="[{ required: true }]" />
          </div>

          <div style="padding: 12px 16px;">
            <van-button size="small" icon="plus" type="primary" plain block @click="addVisitor">
              + 添加来访人员
            </van-button>
          </div>
        </van-cell-group>

        <div style="margin: 20px 16px;">
          <van-button round block type="primary" native-type="submit" :loading="submitLoading" size="large">
            发送邀请函到各访客邮箱
          </van-button>
        </div>
      </van-form>
    </div>


    <!-- 专属加密 Token 钉钉免密一键审批弹窗 -->
    <van-popup v-model:show="showTokenApprovalModal" round position="center" :style="{ width: '90%', padding: '20px' }">
      <div style="text-align: center;">
        <h3 style="margin: 0 0 8px 0; color: #323233;">【钉钉卡片快捷审批专区】</h3>
        <p style="font-size: 13px; color: #969799; margin-bottom: 16px;">基于唯一加密凭证安全校验 · 无需再次登录</p>
      </div>

      <div v-if="tokenRecord" class="token-approve-card" style="background: #f7f8fa; padding: 16px; border-radius: 8px; font-size: 14px;">
        <p style="margin: 6px 0;"><b>访客姓名：</b>{{ tokenRecord.visitorName }}</p>
        <p style="margin: 6px 0;"><b>联系电话：</b>{{ tokenRecord.phone }}</p>
        <p style="margin: 6px 0;"><b>身份证件：</b>{{ tokenRecord.idCardMasked }}</p>
        <p style="margin: 6px 0;"><b>来访事由：</b>{{ tokenRecord.visitPurpose }}</p>
        <p style="margin: 6px 0;"><b>受访部门：</b>{{ tokenRecord.hostDept }}</p>
        <p style="margin: 6px 0; color: #1989fa;"><b>拟到访时间段：</b>{{ tokenRecord.visitDate }} {{ tokenRecord.visitStartTime }} ~ {{ tokenRecord.visitEndTime }}</p>
        <p style="margin: 6px 0;"><b>当前状态：</b>

          <van-tag :type="tokenRecord.status === 'PENDING_APPROVAL' ? 'warning' : 'primary'">
            {{ tokenRecord.status === 'PENDING_APPROVAL' ? '待审批' : tokenRecord.status }}
          </van-tag>
        </p>
      </div>

      <div v-if="tokenRecord && tokenRecord.status === 'PENDING_APPROVAL'" style="margin-top: 20px; display: flex; gap: 12px;">
        <van-button block round type="danger" plain :loading="tokenApproving" @click="handleTokenApprove(false)">
          拒绝到访
        </van-button>
        <van-button block round type="primary" :loading="tokenApproving" @click="handleTokenApprove(true)">
          同意放行
        </van-button>
      </div>
      <div v-else style="margin-top: 16px;">
        <van-button block round type="default" @click="showTokenApprovalModal = false">关闭窗口</van-button>
      </div>
    </van-popup>
  </div>

</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { showToast, showSuccessToast, showFailToast } from 'vant'

import axios from 'axios'


const todayStr = new Date().toISOString().split('T')[0]
const submitLoading = ref(false)

const batchTimeRangeDisplay = ref('09:00 ~ 18:00 (全天段)')
const showDatePicker = ref(false)
const showTimePicker = ref(false)
const showReasonPicker = ref(false)
const reasonList = ref([])

const timeRangeColumns = [
  { text: '08:30 ~ 11:30 (上午段)', start: '08:30', end: '11:30' },
  { text: '13:30 ~ 17:30 (下午段)', start: '13:30', end: '17:30' },
  { text: '09:00 ~ 18:00 (全天段)', start: '09:00', end: '18:00' },
  { text: '18:00 ~ 21:00 (夜班段)', start: '18:00', end: '21:00' }
]

const reasonColumns = computed(() => reasonList.value.map(r => ({ text: r.reasonName, value: r.reasonName })))

const batchForm = reactive({
  company: '',
  visitDate: todayStr,
  visitStartTime: '09:00',
  visitEndTime: '18:00',
  visitPurpose: '商务洽谈',
  hostUserId: 1,
  visitors: [
    { visitorName: '', phone: '', email: '' }
  ]
})

const addVisitor = () => {
  batchForm.visitors.push({ visitorName: '', phone: '', email: '' })
}

const removeVisitor = (index) => {
  if (batchForm.visitors.length > 1) {
    batchForm.visitors.splice(index, 1)
  }
}

const onDateConfirm = (date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  batchForm.visitDate = `${y}-${m}-${d}`
  showDatePicker.value = false
}

const onTimeRangeConfirm = (val) => {
  const selected = val && val.selectedOptions && val.selectedOptions[0] ? val.selectedOptions[0] : null
  if (selected) {
    batchTimeRangeDisplay.value = selected.text
    batchForm.visitStartTime = selected.start
    batchForm.visitEndTime = selected.end
    showTimePicker.value = false
  }
}

const onReasonConfirm = (val) => {
  const selected = val && val.selectedOptions && val.selectedOptions[0] ? val.selectedOptions[0] : null
  if (selected) {
    batchForm.visitPurpose = selected.text
    showReasonPicker.value = false
  }
}

const loadReasons = async () => {
  try {
    const res = await axios.get('/api/public/visit-reasons')
    if (res.data.code === 200) {
      reasonList.value = res.data.data
      if (reasonList.value.length > 0) {
        batchForm.visitPurpose = reasonList.value[0].reasonName
      }
    }
  } catch (e) {}
}

const submitBatchInvite = async () => {
  submitLoading.value = true
  try {
    const res = await axios.post('/api/host/batch-invite', batchForm)
    if (res.data.code === 200) {
      showSuccessToast('已成功派发到访邀请函邮件！')
      batchForm.company = ''
      batchForm.visitors = [{ visitorName: '', phone: '', email: '' }]
    } else {
      showFailToast(res.data.message || '发送失败')
    }
  } catch (e) {
    showFailToast('提交发生网络错误')
  } finally {
    submitLoading.value = false
  }
}


const handleApprove = async (recordId, agree) => {
  try {
    const res = await axios.post('/api/host/approve', {
      recordId: recordId,
      agree: agree,
      approverName: currentUser.value.name
    })
    if (res.data.code === 200) {
      showSuccessToast(res.data.message)
      loadPendingList()
      loadRecordList()
    } else {
      showFailToast(res.data.message)
    }
  } catch (e) {
    showFailToast('操作失败')
  }
}

const submitInvite = async () => {
  inviteLoading.value = true
  try {
    const res = await axios.post('/api/host/invite', inviteForm)
    if (res.data.code === 200) {
      showSuccessToast('预约邀约生成成功！')
      activeTab.value = 2
      loadRecordList()
    } else {
      showFailToast(res.data.message)
    }
  } catch (e) {
    showFailToast('提交网络失败')
  } finally {
    inviteLoading.value = false
  }
}

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

const getStatusTagType = (s) => {
  if (s === 'APPROVED' || s === 'NDA_SIGNED') return 'primary'
  if (s === 'ENTERED') return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'warning'
}

const getStatusText = (item) => {
  if (item.status === 'ENTERED') return '已放行入园'
  if (item.status === 'REJECTED') return '已驳回'
  if (item.ndaSigned === 1) return '协议已签/待入园'
  if (item.status === 'APPROVED') return '审批通过/待签协议'
  return '待审批'
}

const route = useRoute()
const showTokenApprovalModal = ref(false)
const tokenRecord = ref(null)
const tokenApproving = ref(false)

const checkApproveTokenUrl = async () => {
  let token = (route && route.query && route.query.approveToken) ? route.query.approveToken : null
  if (!token) {
    token = new URLSearchParams(window.location.search).get('approveToken')
  }
  if (!token && window.location.href.includes('approveToken=')) {
    const parts = window.location.href.split('approveToken=')
    if (parts.length > 1) {
      token = parts[1].split('&')[0].split('#')[0]
    }
  }

  if (token) {
    try {
      showToast({ type: 'loading', message: '正在验证极速审批凭证...', duration: 0 })
      const res = await axios.get(`/api/public/host/apply-info?approveToken=${encodeURIComponent(token)}`)
      showToast().clear()
      if (res.data.code === 200 && res.data.data) {
        tokenRecord.value = res.data.data
        showTokenApprovalModal.value = true
        showSuccessToast('加密凭证校验通过！')
      } else {
        showFailToast(res.data.message || '审批链接无效或已被处理')
      }
    } catch (e) {
      showToast().clear()
      showFailToast('网络连接失败，请检查服务地址')
    }
  }
}


const handleTokenApprove = async (approved) => {
  if (!tokenRecord.value || !tokenRecord.value.approveToken) return
  tokenApproving.value = true
  try {
    const res = await axios.post('/api/public/host/approve-by-token', {
      approveToken: tokenRecord.value.approveToken,
      approved: approved
    })
    if (res.data.code === 200) {
      showSuccessToast(approved ? '已同意放行！系统已告知访客' : '已拒绝该笔到访申请')
      tokenRecord.value.status = approved ? 'APPROVED' : 'REJECTED'
      setTimeout(() => {
        showTokenApprovalModal.value = false
      }, 1500)
    } else {
      showFailToast(res.data.message || '处理失败')
    }
  } catch (e) {
    showFailToast('操作发生网络错误')
  } finally {
    tokenApproving.value = false
  }
}

onMounted(() => {
  loadReasons()
  checkApproveTokenUrl()
})


</script>

<style scoped>
.host-container { min-height: 100vh; background: #f7f8fa; }
.user-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #ebedf0;
}
.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #1989fa;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
}
.user-info { margin-left: 12px; }
.user-name { font-size: 16px; font-weight: bold; }
.user-dept { font-size: 12px; color: #969799; margin-top: 4px; }

.empty-box { padding: 40px 0; }
.card-list, .form-box { padding: 12px; }

.approve-card, .record-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.card-header, .record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #f2f3f5;
  font-weight: bold;
}
.card-body, .record-body {
  font-size: 13px;
  color: #646566;
  margin-top: 8px;
}
.card-body p, .record-body p { margin: 4px 0; }
.card-actions { display: flex; justify-content: flex-end; margin-top: 12px; }

.text-green { color: #07c160; font-weight: bold; }
.text-red { color: #ee0a24; font-weight: bold; }
</style>
