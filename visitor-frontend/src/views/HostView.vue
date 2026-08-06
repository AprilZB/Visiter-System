<template>
  <div class="host-container">
    <van-nav-bar title="钉钉微应用 - 员工访客协同" />

    <!-- 员工免登个人信息 -->
    <div class="user-card">
      <div class="user-avatar">{{ currentUser.name ? currentUser.name[0] : '员' }}</div>
      <div class="user-info">
        <div class="user-name">{{ currentUser.name || '内部员工' }} <van-tag type="primary">已钉钉免登</van-tag></div>
        <div class="user-dept">工号: {{ currentUser.workNo }} | 部门: {{ currentUser.deptName }}</div>
      </div>
    </div>

    <!-- 选项卡切换 -->
    <van-tabs v-model:active="activeTab" sticky>
      <!-- 标签 1: 盲来实时待办审批 -->
      <van-tab title="待办审批">
        <div v-if="pendingList.length === 0" class="empty-box">
          <van-empty description="暂无待处理的现场盲来申请" />
        </div>
        <div v-else class="card-list">
          <div v-for="item in pendingList" :key="item.id" class="approve-card">
            <div class="card-header">
              <span class="visitor-name">{{ item.visitorName }}</span>
              <van-tag type="warning">待审批</van-tag>
            </div>
            <div class="card-body">
              <p><b>访客手机:</b> {{ item.phone }}</p>
              <p><b>身份证:</b> {{ item.idCardMasked }}</p>
              <p><b>来访事由:</b> {{ item.visitPurpose }}</p>
              <p><b>申请时间:</b> {{ formatTime(item.createdAt) }}</p>
            </div>
            <div class="card-actions">
              <van-button size="small" type="danger" plain @click="handleApprove(item.id, false)">拒绝到访</van-button>
              <van-button size="small" type="primary" style="margin-left: 8px;" @click="handleApprove(item.id, true)">同意放行</van-button>
            </div>
          </div>
        </div>
      </van-tab>

      <!-- 标签 2: 主动预约邀约 (场景 A) -->
      <van-tab title="发起预约">
        <div class="form-box">
          <van-form @submit="submitInvite">
            <van-cell-group inset>
              <van-field v-model="inviteForm.visitorName" label="访客姓名" placeholder="来访人员姓名" required :rules="[{ required: true }]" />
              <van-field v-model="inviteForm.phone" label="访客手机号" placeholder="用于接收预约通知" type="tel" required :rules="[{ required: true }]" />
              <van-field v-model="inviteForm.idCard" label="身份证号" placeholder="预填/让访客自行识别" />
              <!-- 预约事由下拉选择 (与访客端保持字典一致) -->
              <van-field
                v-model="inviteForm.visitPurpose"
                is-link
                readonly
                name="purpose"
                label="预约事由"
                placeholder="请选择预约事由"
                required
                :rules="[{ required: true }]"
                @click="showReasonPicker = true"
              />
              <van-popup v-model:show="showReasonPicker" position="bottom">
                <van-picker
                  :columns="reasonColumns"
                  @confirm="onReasonConfirm"
                  @cancel="showReasonPicker = false"
                />
              </van-popup>
            </van-cell-group>


            <div style="margin: 16px;">
              <van-button round block type="primary" native-type="submit" :loading="inviteLoading">
                生成预约邀约链接
              </van-button>
            </div>
          </van-form>
        </div>
      </van-tab>

      <!-- 标签 3: 我约请的访客列表 & NDA 协议签署状态 -->
      <van-tab title="预约记录">
        <div class="card-list">
          <div v-for="item in recordList" :key="item.id" class="record-card">
            <div class="record-header">
              <span>{{ item.visitorName }} ({{ item.phone }})</span>
              <van-tag :type="getStatusTagType(item.status)">{{ getStatusText(item) }}</van-tag>
            </div>
            <div class="record-body">
              <p><b>场景:</b> {{ item.scenario === 'A' ? '员工预预约' : '现场盲扫' }}</p>
              <p><b>保密协议:</b> 
                <span :class="item.ndaSigned === 1 ? 'text-green' : 'text-red'">
                  {{ item.ndaSigned === 1 ? '已签署 (存证完成)' : '未签署 (拦截中)' }}
                </span>
              </p>
              <p><b>到达状态:</b> {{ item.status === 'ENTERED' ? '已通过门岗入园' : '未到访核销' }}</p>
            </div>
          </div>
        </div>
      </van-tab>
    </van-tabs>

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


const activeTab = ref(0)
const currentUser = ref({})
const pendingList = ref([])
const recordList = ref([])
const inviteLoading = ref(false)

const inviteForm = reactive({
  scenario: 'A',
  visitorName: '',
  phone: '',
  idCard: '',
  visitPurpose: '商务交流',
  hostUserId: 1
})

const showReasonPicker = ref(false)
const reasonList = ref([])

const reasonColumns = computed(() => reasonList.value.map(r => ({ text: r.reasonName, value: r.reasonName })))

const loadReasons = async () => {
  try {
    const res = await axios.get('/api/public/visit-reasons')
    if (res.data.code === 200) {
      reasonList.value = res.data.data
      if (reasonList.value.length > 0) {
        inviteForm.visitPurpose = reasonList.value[0].reasonName
      }
    }
  } catch (e) {}
}

const onReasonConfirm = (val) => {
  const selected = val && val.selectedOptions && val.selectedOptions[0] ? val.selectedOptions[0] : null
  if (selected) {
    inviteForm.visitPurpose = selected.text
    showReasonPicker.value = false
  }
}

const autoLogin = async () => {
  try {
    const res = await axios.post('/api/host/login', { authCode: 'MT001' })
    if (res.data.code === 200) {
      currentUser.value = res.data.data
      inviteForm.hostUserId = currentUser.value.id || 1
      loadPendingList()
      loadRecordList()
      loadReasons()
    }
  } catch (e) {
    showFailToast('钉钉免登连接失败')
  }
}


const loadPendingList = async () => {
  try {
    const res = await axios.get(`/api/host/pending?hostUserId=${currentUser.value.id || 1}`)
    if (res.data.code === 200) {
      pendingList.value = res.data.data
    }
  } catch (e) {}
}

const loadRecordList = async () => {
  try {
    const res = await axios.get(`/api/host/records?hostUserId=${currentUser.value.id || 1}`)
    if (res.data.code === 200) {
      recordList.value = res.data.data
    }
  } catch (e) {}
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
  autoLogin()
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
