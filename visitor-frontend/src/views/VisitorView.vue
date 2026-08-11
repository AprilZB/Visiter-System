<template>
  <div class="visitor-container">
    <van-nav-bar title="浙江脉通智造 - 访客入园申请" left-arrow @click-left="goHome" />

    <!-- 凭手机号找回/检索通行码弹窗 -->

    <van-dialog v-model:show="showFindTokenModal" title="凭手机号恢复通行二维码" show-cancel-button confirm-button-text="查询找回" :before-close="onFindTokenConfirm">
      <div style="padding: 20px 16px;">
        <p style="font-size: 13px; color: #646566; margin-bottom: 12px;">防止误关闭或刷新页面，输入您申请时填写的手机号即可快速恢复有效通行码：</p>
        <van-field v-model="searchPhone" label="手机号码" placeholder="请输入申请填写的手机号" type="tel" clearable />
      </div>
    </van-dialog>

    <!-- 顶部状态 Banner -->
    <div class="status-banner" :class="statusClass">
      <van-icon :name="statusIcon" size="24" />
      <div class="status-text">
        <div class="main-status">{{ statusTitle }}</div>
        <div class="sub-status">{{ statusSub }}</div>
      </div>
    </div>

    <!-- 快捷查询入口：员工已预约/已有申请的访客凭手机号查找 -->
    <div style="margin: 12px 16px; background: #e8f4ff; border: 1px solid #abdcff; border-radius: 8px; padding: 12px 16px; display: flex; align-items: center; justify-content: space-between;">
      <div>
        <div style="font-weight: bold; color: #1989fa; font-size: 14px;">已有预约或申请记录？</div>
        <div style="font-size: 12px; color: #646566; margin-top: 2px;">凭手机号查找单据，签保密协议领通行码</div>
      </div>
      <van-button size="small" type="primary" round icon="search" @click="showFindTokenModal = true">
        手机号查询
      </van-button>
    </div>


    <!-- 步骤 1: 填报申请 / OCR 识别 -->
    <div v-if="!currentVisitNo" class="form-card">
      <div class="card-title">① 身份证拍摄自动识别</div>
      <van-uploader :after-read="handleOcrUpload" :max-count="1" class="ocr-uploader">
        <van-button icon="photograph" type="primary" block :loading="ocrLoading" loading-text="身份证 OCR 识别中...">
          上传/拍照身份证自动识别
        </van-button>
      </van-uploader>

      <van-form @submit="submitApply" class="apply-form">
        <van-cell-group inset>
          <van-field v-model="form.visitorName" label="访客姓名" placeholder="身份证识别/手填姓名" required :rules="[{ required: true }]" />
          <van-field v-model="form.idCard" label="身份证号" placeholder="身份证识别/手填身份证号" required :rules="[{ required: true }]" />
          <van-field v-model="form.phone" label="手机号码" placeholder="请输入手机号码" type="tel" required :rules="[{ required: true }]" />
          
          <!-- 前往部门下拉选择 (只展示未屏蔽公开部门) -->
          <van-field
            v-model="selectedDeptName"
            is-link
            readonly
            name="dept"
            label="前往部门"
            placeholder="请选择要到访的部门"
            required
            :rules="[{ required: true }]"
            @click="showDeptPicker = true"
          />
          <van-popup v-model:show="showDeptPicker" position="bottom">
            <van-picker
              :columns="deptColumns"
              @confirm="onDeptConfirm"
              @cancel="showDeptPicker = false"
            />
          </van-popup>

          <!-- 拜访人员下拉选择 (先选部门再选人) -->
          <van-field
            v-model="selectedHostName"
            is-link
            readonly
            name="host"
            label="拜访人员"
            placeholder="请先选择部门再选择受访人员"
            required
            :rules="[{ required: true }]"
            @click="openHostPicker"
          />
          <van-popup v-model:show="showHostPicker" position="bottom">
            <van-picker
              :columns="hostColumns"
              @confirm="onHostConfirm"
              @cancel="showHostPicker = false"
            />
          </van-popup>

          <!-- 到访日期与时间段选择 -->
          <van-field
            v-model="form.visitDate"
            is-link
            readonly
            name="visitDate"
            label="到访日期"
            placeholder="请选择拟到访日期"
            required
            :rules="[{ required: true }]"
            @click="showDatePicker = true"
          />
          <van-calendar v-model:show="showDatePicker" @confirm="onDateConfirm" />

          <van-field
            v-model="timeRangeDisplay"
            is-link
            readonly
            name="timeRange"
            label="到访时间段"
            placeholder="请选择具体的到访时间段"
            required
            :rules="[{ required: true }]"
            @click="showTimePicker = true"
          />
          <van-popup v-model:show="showTimePicker" position="bottom">
            <van-picker
              :columns="timeRangeColumns"
              @confirm="onTimeRangeConfirm"
              @cancel="showTimePicker = false"
            />
          </van-popup>

          <!-- 来访事由下拉选择 -->
          <van-field
            v-model="form.visitPurpose"
            is-link
            readonly
            name="purpose"
            label="来访事由"
            placeholder="请选择来访事由"
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
          <van-button round block type="primary" native-type="submit" :loading="submitLoading">
            提交申请并进入下一步
          </van-button>
          <div style="text-align: center; margin-top: 12px;">
            <van-button size="small" type="primary" text plain @click="showFindTokenModal = true">
              已有已批准申请？输入手机号找回通行二维码
            </van-button>
          </div>
        </div>
      </van-form>


    </div>

    <!-- 步骤 2: 页面解锁与通行二维码展示 -->
    <div v-else class="pass-card">
      <div class="visit-info">
        <van-cell title="访客单号" :value="currentVisitNo" />
        <van-cell title="访客姓名" :value="recordDetail.visitorName" />
        <van-cell title="受访人员" :value="recordDetail.hostName + ' (' + (recordDetail.hostDept||'未设') + ')'" />
        <van-cell title="保密协议" :value="isNdaSigned ? '已签署备案' : '未签署 (强拦截)'" :value-class="isNdaSigned ? 'text-green' : 'text-red'" />
      </div>

      <!-- 待审批提示 -->
      <div v-if="recordDetail.status === 'PENDING_APPROVAL'" class="pending-box">
        <van-loading type="spinner" color="#1989fa">待受访员工审批中...</van-loading>
        <p>系统已向受访员工发送钉钉审批通知，每3秒自动刷新中</p>
      </div>

      <!-- 二维码展现区 (仅在已签署协议且审批通过后展现) -->
      <div v-else-if="isNdaSigned && passToken" class="qr-box">
        <div class="qr-title">限时动态通行二维码</div>
        
        <!-- 极致稀疏、抗屏幕拍摄摩尔纹的大格子二维码 (仅渲染短 Token 串) -->
        <div class="qr-watermark" style="padding: 12px; background: #fff; display: inline-block; border-radius: 12px; box-shadow: 0 4px 16px rgba(0,0,0,0.06);">
          <qrcode-vue :value="shortPassCode" :size="200" level="L" />
          <div class="scan-line"></div>
        </div>

        <!-- 保安免扫强兜底：大字号 8 位短通行码面板 -->
        <div style="margin-top: 12px; background: #fffbe8; border: 1px solid #ffe58f; padding: 10px 16px; border-radius: 8px; text-align: center;">
          <div style="font-size: 12px; color: #d46b08; font-weight: bold;">【门岗放行备用短码】若扫码受阻可报下方短码或手机号</div>
          <div style="font-size: 24px; font-weight: 900; color: #1989fa; letter-spacing: 2px; margin-top: 4px; font-family: monospace;">
            {{ shortPassCode }}
          </div>
        </div>

        <p class="qr-tip" style="margin-top: 8px;">请向门岗保安出示此二维码，或直接出示上方 8 位短码/手机号放行</p>
      </div>


      <!-- 协议拦截按钮 -->
      <div v-else-if="!isNdaSigned" class="nda-blocked-box">
        <van-notice-bar left-icon="warning-o" text="重要提示：进入厂区涉及商业机密，必须线上签署保密协议方可获取通行二维码。" />
        <van-button type="danger" block round class="sign-btn" @click="showNdaModal = true">
          立即签署《公司保密协议》解锁通行证
        </van-button>
      </div>
    </div>

    <!-- 步骤 1.5: 强制保密协议 (NDA) 签署弹窗 + HTML5 Canvas 手写签名画板 -->
    <van-popup v-model:show="showNdaModal" round :close-on-click-overlay="false" position="bottom" :style="{ height: '90%' }" @opened="onNdaModalOpened">
      <div class="nda-modal-container">
        <div class="nda-modal-header">
          <h3>{{ ndaTemplate.title || '浙江脉通智造科技有限公司外来人员保密协议书' }}</h3>
          <span class="nda-version-tag">版本: {{ ndaTemplate.version || 'V1.0.0' }}</span>
        </div>

        <div v-if="ndaTemplate.pdfUrl" class="nda-pdf-view" style="padding: 10px 12px;">
          <iframe :src="getFullPdfUrl(ndaTemplate.pdfUrl)" style="width: 100%; height: 260px; border: 1px dashed #c8c9cc; border-radius: 6px; background: #fff;"></iframe>
          <div style="text-align: right; margin-top: 6px;">
            <a :href="getFullPdfUrl(ndaTemplate.pdfUrl)" target="_blank" style="font-size: 13px; color: #1989fa; text-decoration: underline;">
              📄 查看/下载官方盖章版 PDF 协议原件
            </a>
          </div>
        </div>

        <div v-else class="nda-modal-body" v-html="ndaTemplate.content"></div>


        <!-- 强制电子手写签名区域 -->
        <div class="signature-section">
          <div class="signature-title">
            <span>请在下方空白框内用手指/鼠标手写您的姓名：</span>
            <van-button size="mini" type="default" icon="clear" @click="clearSignature">清空重签</van-button>
          </div>
          <div class="canvas-wrapper">
            <canvas
              ref="signatureCanvas"
              class="signature-canvas"
              @mousedown="startDrawing"
              @mousemove="draw"
              @mouseup="stopDrawing"
              @mouseleave="stopDrawing"
              @touchstart="startDrawingTouch"
              @touchmove="drawTouch"
              @touchend="stopDrawing"
            ></canvas>
            <div v-if="!hasSigned" class="signature-placeholder">手写签名区域 (请在框内手写)</div>
          </div>
        </div>

        <div class="nda-modal-footer">
          <van-checkbox v-model="ndaAgreed" shape="square" icon-size="18px">
            我已完整阅读并同意上述保密协议，确认手写签名为本人真实意愿
          </van-checkbox>

          <div style="margin-top: 12px;">
            <van-button block round type="primary" :disabled="!ndaAgreed || !hasSigned" :loading="signLoading" @click="confirmSignNda">
              确认提交电子手写签名
            </van-button>
          </div>
        </div>
      </div>
    </van-popup>


  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from 'vue'

import { useRoute } from 'vue-router'
import { showToast, showSuccessToast, showFailToast } from 'vant'
import QrcodeVue from 'qrcode.vue'
import axios from 'axios'

const route = useRoute()

const shortPassCode = computed(() => {
  if (!passToken.value) return ''
  // 计算 6 位纯数字放行短码（深度学习 OCR 对 6 位大字纯数字拥有接近 100% 的极致识别率）
  let hash = 0
  for (let i = 0; i < passToken.value.length; i++) {
    hash = (hash * 31 + passToken.value.charCodeAt(i)) % 1000000
  }
  return String(Math.abs(hash)).padStart(6, '9')
})





const getFullPdfUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('/')) {
    return 'http://localhost:8096' + url
  }
  return url
}

const todayStr = new Date().toISOString().split('T')[0]
const onFindTokenConfirm = async (action) => {
  if (action === 'cancel') {
    searchPhone.value = ''
    return true
  }
  if (!searchPhone.value || !searchPhone.value.trim()) {
    showToast('请输入申请时填写的手机号码！')
    return false
  }
  try {
    const res = await axios.get(`/api/public/visitor/by-phone?phone=${searchPhone.value.trim()}`)
    if (res.data.code === 200 && res.data.data) {
      const record = res.data.data
      showSuccessToast('已成功检索到您的到访单据！')
      currentVisitNo.value = record.visitNo
      recordDetail.value = record
      
      form.visitorName = record.visitorName || ''
      form.phone = record.phone || ''
      form.visitDate = record.visitDate || ''
      form.visitStartTime = record.visitStartTime || ''
      form.visitEndTime = record.visitEndTime || ''
      
      isNdaSigned.value = (record.ndaSigned === 1)
      if (isNdaSigned.value) {
        passToken.value = record.passToken
      } else {
        // 自动引导访客拍照/上传身份证并签保密协议
        showNdaModal.value = true
      }

      searchPhone.value = ''
      showFindTokenModal.value = false
      return true
    } else {
      showFailToast(res.data.message || '未查询到记录，请直接在下方申请')
      return false
    }
  } catch (e) {
    showFailToast('查询发生网络错误')
    return false
  }
}

const timeRangeDisplay = ref('09:00 ~ 18:00 (全天段)')


const form = reactive({
  scenario: 'B',
  visitorName: '',
  idCard: '',
  phone: '',
  hostUserId: null,
  visitPurpose: '',
  visitDate: todayStr,
  visitStartTime: '09:00',
  visitEndTime: '18:00'
})

const selectedDeptName = ref('')
const selectedHostName = ref('')

const showFindTokenModal = ref(false)
const searchPhone = ref('')

const showDeptPicker = ref(false)
const showHostPicker = ref(false)
const showReasonPicker = ref(false)
const showDatePicker = ref(false)
const showTimePicker = ref(false)

const timeRangeColumns = [
  { text: '08:30 ~ 11:30 (上午段)', start: '08:30', end: '11:30' },
  { text: '13:30 ~ 17:30 (下午段)', start: '13:30', end: '17:30' },
  { text: '09:00 ~ 18:00 (全天段)', start: '09:00', end: '18:00' },
  { text: '18:00 ~ 21:00 (夜班段)', start: '18:00', end: '21:00' }
]

const onDateConfirm = (date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  form.visitDate = `${y}-${m}-${d}`
  showDatePicker.value = false
}

const onTimeRangeConfirm = (val) => {
  const selected = val && val.selectedOptions && val.selectedOptions[0] ? val.selectedOptions[0] : null
  if (selected) {
    timeRangeDisplay.value = selected.text
    form.visitStartTime = selected.start
    form.visitEndTime = selected.end
    showTimePicker.value = false
  }
}




const deptList = ref([])
const hostList = ref([])
const reasonList = ref([])

const deptColumns = computed(() => deptList.value.map(d => ({ text: d.deptName, value: d.id })))
const hostColumns = computed(() => hostList.value.map(h => ({ text: `${h.name} (${h.workNo})`, value: h.id, raw: h })))
const reasonColumns = computed(() => reasonList.value.map(r => ({ text: r.reasonName, value: r.reasonName })))

const loadDepts = async () => {
  try {
    const res = await axios.get('/api/public/depts')
    if (res.data.code === 200) {
      deptList.value = res.data.data
    }
  } catch (e) {}
}

const loadReasons = async () => {
  try {
    const res = await axios.get('/api/public/visit-reasons')
    if (res.data.code === 200) {
      reasonList.value = res.data.data
      if (reasonList.value.length > 0) {
        form.visitPurpose = reasonList.value[0].reasonName
      }
    }
  } catch (e) {}
}

const onDeptConfirm = async (val) => {
  const selected = val && val.selectedOptions && val.selectedOptions[0] ? val.selectedOptions[0] : null
  if (selected) {
    selectedDeptName.value = selected.text
    showDeptPicker.value = false
    // 清空上次选的人，级联加载该部门的人
    selectedHostName.value = ''
    form.hostUserId = null
    try {
      const res = await axios.get(`/api/public/users-by-dept?deptName=${encodeURIComponent(selected.text)}`)
      if (res.data.code === 200) {
        hostList.value = res.data.data
      }
    } catch (e) {}
  }
}

const showNdaModal = ref(false)
const ndaAgreed = ref(false)
const hasSigned = ref(false)
const signLoading = ref(false)
const ndaTemplate = ref({})
const visitorRecord = ref({})

// Canvas 手写签名逻辑
const signatureCanvas = ref(null)
const isDrawing = ref(false)
let ctx = null

const onNdaModalOpened = () => {
  initCanvas()
}

const initCanvas = () => {
  nextTick(() => {
    if (signatureCanvas.value) {
      const canvas = signatureCanvas.value
      const parent = canvas.parentElement
      canvas.width = parent.clientWidth || 320
      canvas.height = 140
      ctx = canvas.getContext('2d')
      ctx.lineWidth = 3
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      ctx.strokeStyle = '#000000'
    }
  })
}

const clearSignature = () => {
  if (ctx && signatureCanvas.value) {
    ctx.clearRect(0, 0, signatureCanvas.value.width, signatureCanvas.value.height)
    hasSigned.value = false
  }
}

const getCanvasPos = (e) => {
  if (!signatureCanvas.value) return { x: 0, y: 0 }
  const rect = signatureCanvas.value.getBoundingClientRect()
  return {
    x: e.clientX - rect.left,
    y: e.clientY - rect.top
  }
}

const getTouchPos = (e) => {
  if (!signatureCanvas.value || !e.touches || !e.touches[0]) return { x: 0, y: 0 }
  const rect = signatureCanvas.value.getBoundingClientRect()
  const touch = e.touches[0]
  return {
    x: touch.clientX - rect.left,
    y: touch.clientY - rect.top
  }
}

const startDrawing = (e) => {
  isDrawing.value = true
  hasSigned.value = true
  const pos = getCanvasPos(e)
  ctx.beginPath()
  ctx.moveTo(pos.x, pos.y)
}

const draw = (e) => {
  if (!isDrawing.value) return
  const pos = getCanvasPos(e)
  ctx.lineTo(pos.x, pos.y)
  ctx.stroke()
}

const startDrawingTouch = (e) => {
  if (e.cancelable) e.preventDefault()
  isDrawing.value = true
  hasSigned.value = true
  const pos = getTouchPos(e)
  ctx.beginPath()
  ctx.moveTo(pos.x, pos.y)
}

const drawTouch = (e) => {
  if (e.cancelable) e.preventDefault()
  if (!isDrawing.value) return
  const pos = getTouchPos(e)
  ctx.lineTo(pos.x, pos.y)
  ctx.stroke()
}

const stopDrawing = () => {
  isDrawing.value = false
}

const openHostPicker = () => {
  if (!selectedDeptName.value) {
    showToast('请先选择要到访的部门！')
    return
  }
  if (hostList.value.length === 0) {
    showToast('该部门暂无可选接待员工')
    return
  }
  showHostPicker.value = true
}

const onHostConfirm = (val) => {
  const selected = val && val.selectedOptions && val.selectedOptions[0] ? val.selectedOptions[0] : null
  if (selected) {
    selectedHostName.value = selected.text
    form.hostUserId = selected.value
    showHostPicker.value = false
  }
}

const onReasonConfirm = (val) => {
  const selected = val && val.selectedOptions && val.selectedOptions[0] ? val.selectedOptions[0] : null
  if (selected) {
    form.visitPurpose = selected.text
    showReasonPicker.value = false
  }
}


const goHome = () => {
  window.location.href = '/'
}


const ocrLoading = ref(false)
const submitLoading = ref(false)
const currentVisitNo = ref('')
const recordDetail = ref({})
const isNdaSigned = ref(false)
const passToken = ref('')

let timer = null


const statusTitle = computed(() => {
  if (!currentVisitNo.value) return '准备填报进出申请'
  const s = recordDetail.value.status
  if (s === 'PENDING_APPROVAL') return '审批中'
  if (s === 'APPROVED' && !isNdaSigned.value) return '审批已通过 (等待签署协议)'
  if (isNdaSigned.value) return '已准入 (通行凭证已生效)'
  if (s === 'ENTERED') return '已核销入园'
  if (s === 'REJECTED') return '申请已被驳回'
  return '处理中'
})

const statusSub = computed(() => {
  if (!currentVisitNo.value) return '请拍照身份证自动识别并提交来访信息'
  if (!isNdaSigned.value) return '依据合规要求，入园前必须勾选签署保密协议'
  return '入园请主动出示动态二维码配合保安核验'
})

const statusClass = computed(() => {
  if (isNdaSigned.value) return 'bg-success'
  if (recordDetail.value.status === 'PENDING_APPROVAL') return 'bg-warning'
  return 'bg-primary'
})

const statusIcon = computed(() => {
  if (isNdaSigned.value) return 'checked'
  if (recordDetail.value.status === 'PENDING_APPROVAL') return 'clock-o'
  return 'info-o'
})

const handleOcrUpload = async (file) => {
  ocrLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file.file)
    const res = await axios.post('/api/visitor/ocr', formData)
    if (res.data.code === 200 && res.data.data) {
      form.visitorName = res.data.data.name
      form.idCard = res.data.data.idCard
      showSuccessToast('OCR 识别成功！')
    } else {
      showFailToast(res.data.message || '识别失败')
    }
  } catch (err) {
    showFailToast('OCR 服务连通异常，请手填身份证')
  } finally {
    ocrLoading.value = false
  }
}

const submitApply = async () => {
  submitLoading.value = true
  try {
    const res = await axios.post('/api/visitor/apply', form)
    if (res.data.code === 200 && res.data.data) {
      currentVisitNo.value = res.data.data.visitNo
      recordDetail.value = res.data.data
      showSuccessToast('申请提交成功')
      fetchNdaTemplate()
      startPolling()
    } else {
      showFailToast(res.data.message || '提交失败')
    }
  } catch (err) {
    showFailToast('提交异常')
  } finally {
    submitLoading.value = false
  }
}

const fetchNdaTemplate = async () => {
  try {
    const res = await axios.get('/api/visitor/nda-template')
    if (res.data.code === 200 && res.data.data) {
      ndaTemplate.value = res.data.data
    }
  } catch (e) {}
}

const checkStatus = async () => {
  if (!currentVisitNo.value) return
  try {
    const res = await axios.get(`/api/visitor/detail?visitNo=${currentVisitNo.value}`)
    if (res.data.code === 200 && res.data.data) {
      recordDetail.value = res.data.data.record
      isNdaSigned.value = res.data.data.ndaSigned

      if (isNdaSigned.value && !passToken.value) {
        fetchPassToken()
      } else if (recordDetail.value.status === 'APPROVED' && !isNdaSigned.value) {
        showNdaModal.value = true
      }
    }
  } catch (e) {}
}

const confirmSignNda = async () => {
  if (!hasSigned.value || !signatureCanvas.value) {
    showToast('请先在上方空白框上手写您的姓名！')
    return
  }
  signLoading.value = true
  try {
    const signatureBase64 = signatureCanvas.value.toDataURL('image/png')
    const res = await axios.post('/api/visitor/sign-nda', {
      visitNo: currentVisitNo.value,
      ndaVersion: ndaTemplate.value.version || 'V1.0.0',
      clientIp: '127.0.0.1',
      deviceFingerprint: navigator.userAgent,
      signatureBase64: signatureBase64
    })


    if (res.data.code === 200) {
      showSuccessToast('保密协议手写签名成功！')
      showNdaModal.value = false
      isNdaSigned.value = true
      fetchPassToken()
    } else {
      showFailToast(res.data.message || '签名提交失败')
    }
  } catch (e) {
    showFailToast('签名提交网络异常')
  } finally {
    signLoading.value = false
  }
}

const fetchPassToken = async () => {
  try {
    const res = await axios.get(`/api/visitor/pass-token?visitNo=${currentVisitNo.value}`)
    if (res.data.code === 200 && res.data.data) {
      passToken.value = res.data.data.passToken
    }
  } catch (e) {}
}

const startPolling = () => {
  checkStatus()
  timer = setInterval(checkStatus, 3000)
}

const checkVisitorTokenUrl = async () => {
  let token = (route && route.query && route.query.visitorToken) ? route.query.visitorToken : null
  if (!token) {
    token = new URLSearchParams(window.location.search).get('visitorToken')
  }

  if (token) {
    try {
      showToast({ type: 'loading', message: '正在加载到访邀请单...', duration: 0 })
      const res = await axios.get(`/api/public/visitor/info-by-token?visitorToken=${encodeURIComponent(token)}`)
      showToast().clear()
      if (res.data.code === 200 && res.data.data) {
        const data = res.data.data
        form.visitorName = data.visitorName || ''
        form.phone = data.phone || ''
        form.visitDate = data.visitDate || ''
        form.visitStartTime = data.visitStartTime || ''
        form.visitEndTime = data.visitEndTime || ''
        visitorRecord.value = data
        
        showSuccessToast('已成功载入邮件邀请信息！')
        if (data.status === 'APPROVED' && data.ndaSigned !== 1) {
          showNdaModal.value = true
        }
      }
    } catch (e) {
      showToast().clear()
    }
  }
}

onMounted(() => {
  fetchNdaTemplate()
  loadDepts()
  loadReasons()
  checkVisitorTokenUrl()
})



onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.visitor-container {
  min-height: 100vh;
  background-color: #f7f8fa;
}
.status-banner {
  display: flex;
  align-items: center;
  padding: 16px;
  color: #fff;
}
.bg-primary { background: linear-gradient(135deg, #1989fa, #0570db); }
.bg-warning { background: linear-gradient(135deg, #ff976a, #ed6a0c); }
.bg-success { background: linear-gradient(135deg, #07c160, #049b4c); }

.status-text { margin-left: 12px; }
.main-status { font-size: 18px; font-weight: bold; }
.sub-status { font-size: 12px; opacity: 0.9; margin-top: 2px; }

.form-card, .pass-card {
  margin: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}
.card-title { font-size: 16px; font-weight: bold; margin-bottom: 12px; color: #323233; }
.ocr-uploader { margin-bottom: 16px; display: block; }

.pending-box { text-align: center; padding: 24px; color: #646566; }
.qr-box { text-align: center; padding: 16px; }
.qr-title { font-weight: bold; margin-bottom: 16px; font-size: 16px; color: #07c160; }
.qr-watermark {
  display: inline-block;
  padding: 16px;
  background: #f0f9eb;
  border-radius: 16px;
  border: 2px dashed #07c160;
  position: relative;
}
.qr-tip { font-size: 12px; color: #969799; margin-top: 12px; }

.nda-blocked-box { margin-top: 16px; text-align: center; }
.sign-btn { margin-top: 16px; font-weight: bold; }

.nda-modal-container {
  padding: 16px;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
}
.nda-modal-header {
  border-bottom: 1px solid #ebedf0;
  padding-bottom: 8px;
  margin-bottom: 12px;
}
.nda-modal-header h3 { margin: 0 0 4px 0; font-size: 16px; color: #323233; }
.nda-version-tag { font-size: 11px; color: #1989fa; background: #e8f4ff; padding: 2px 6px; border-radius: 4px; }

.nda-modal-body {
  flex: 1;
  overflow-y: auto;
  font-size: 13px;
  color: #646566;
  line-height: 1.6;
  background: #fafafa;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.signature-section {
  background: #fff;
  border: 1px solid #ebedf0;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 12px;
}
.signature-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  font-weight: bold;
  color: #323233;
  margin-bottom: 8px;
}
.canvas-wrapper {
  position: relative;
  background: #fff;
  border: 2px dashed #1989fa;
  border-radius: 6px;
  height: 140px;
  overflow: hidden;
}
.signature-canvas {
  width: 100%;
  height: 140px;
  display: block;
  cursor: crosshair;
  touch-action: none;
}
.signature-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 13px;
  color: #c8c9cc;
  pointer-events: none !important;
  user-select: none;
}


.nda-modal-footer { border-top: 1px solid #f2f3f5; padding-top: 10px; }

.text-green { color: #07c160; font-weight: bold; }
.text-red { color: #ee0a24; font-weight: bold; }

/* PC 适配：让只读下拉选择框鼠标展现 Pointer 并且点选极其顺畅 */
.van-field {
  cursor: pointer !important;
}
.van-field__control {
  cursor: pointer !important;
}
.van-cell--clickable {
  cursor: pointer !important;
}
</style>

