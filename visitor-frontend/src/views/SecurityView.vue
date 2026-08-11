<template>
  <div class="security-container">
    <van-nav-bar title="园区物业门岗 - 扫码核验放行">
      <template #right>
        <van-button size="small" type="primary" plain icon="qr" @click="showGateQrModal = true">
          正门申请码
        </van-button>
      </template>
    </van-nav-bar>

    <!-- 正门盲来固定静态申请二维码弹窗 -->
    <van-popup v-model:show="showGateQrModal" round position="center" :style="{ width: '85%', padding: '20px', textAlign: 'center' }">
      <h3 style="margin: 0 0 8px 0; color: #323233;">正门门岗 - 现场盲来申请码</h3>
      <p style="font-size: 12px; color: #969799; margin-bottom: 16px;">外部无预约访客请扫描下方二维码填报申请</p>
      <div style="padding: 16px; background: #f9fafb; border-radius: 12px; display: inline-block; border: 2px dashed #1989fa;">
        <qrcode-vue :value="visitorPageUrl" :size="200" level="H" />
      </div>
      <div style="margin-top: 16px;">
        <van-button type="primary" block round @click="showGateQrModal = false">关闭</van-button>
      </div>
    </van-popup>

    <!-- 扫码 / 凭证输入框 -->
    <div class="scan-card">
      <div class="scan-title">门岗通行二维码核验</div>
      
      <!-- 隐藏的原生拍照/相册识别 input (完美兼容 HTTP IP 环境) -->
      <input
        ref="fileInputRef"
        type="file"
        accept="image/*"
        capture="environment"
        style="display: none;"
        @change="handleFileScan"
      />

      <div style="margin-bottom: 12px; display: flex; gap: 8px;">
        <van-button block round type="primary" icon="photograph" @click="triggerPhotoScan">
          📷 拍照/上传二维码识别
        </van-button>
        <van-button block round type="primary" plain icon="scan" @click="startCameraScanner">
          🎥 实时镜头扫码
        </van-button>
      </div>

      <van-field
        v-model="passTokenInput"
        center
        clearable
        placeholder="输入 8位短码 / 访客手机号 / 扫码核验"
        label="核验凭证"
        @keyup.enter="handleScan"
      >
        <template #button>
          <van-button size="small" type="primary" :loading="scanning" @click="handleScan">查询/核验放行</van-button>
        </template>
      </van-field>
      <div style="font-size: 11px; color: #969799; margin-top: 6px; text-align: right;">
        * 支持：① 拍照扫码；② 输入访客手机号；③ 报出 8 位短码；④ 红外扫码枪
      </div>

    </div>


    <!-- 手机摄像头扫码弹窗 -->
    <van-popup v-model:show="showCameraModal" round position="center" :style="{ width: '90%', padding: '16px', textAlign: 'center' }" @closed="stopCameraScanner">
      <h3 style="margin: 0 0 8px 0; color: #323233;">对准访客手机二维码进行扫描</h3>
      <p style="font-size: 12px; color: #969799; margin-bottom: 12px;">请确保光线充足并将二维码放入框内</p>
      
      <div id="qr-reader" style="width: 100%; border-radius: 8px; overflow: hidden; background: #000;"></div>

      <div style="margin-top: 16px;">
        <van-button block round type="default" @click="showCameraModal = false">关闭摄像头</van-button>
      </div>
    </van-popup>


    <!-- 核验结果展示区 (6 大语义化多色调卡片系统) -->
    <div v-if="scanResult" class="result-card" :class="'theme-border-' + (scanResult.resultTheme || 'gray')">
      <!-- 动态 Header Banner -->
      <div class="result-header" :class="'theme-bg-' + (scanResult.resultTheme || 'gray')">
        <van-icon :name="getThemeIcon(scanResult.resultTheme)" size="36" />
        <div style="margin-left: 12px;">
          <div class="result-title">{{ scanResult.resultTitle || (scanResult.canPass ? '准予放行' : '禁止放行') }}</div>
          <div style="font-size: 12px; opacity: 0.9; margin-top: 2px;">
            <span v-if="scanResult.visitType === 'MULTI'">🗓️ 多日通行卡 ({{ scanResult.visitStartDate }} ~ {{ scanResult.visitEndDate }})</span>
            <span v-else>🎫 单次到访凭证</span>
          </div>
        </div>
      </div>

      <div class="result-body">
        <van-notice-bar v-if="scanResult.warningMessage" :scrollable="false" wrapable :class="'theme-notice-' + (scanResult.resultTheme || 'gray')" :text="scanResult.warningMessage" />

        <div class="info-group">
          <div class="info-row">
            <span class="label">到访单号:</span>
            <span class="value font-mono">{{ scanResult.visitNo || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="label">访客姓名:</span>
            <span class="value font-bold" style="font-size: 16px;">{{ scanResult.visitorName }}</span>
          </div>
          <!-- 强脱敏显示掩码身份证 -->
          <div class="info-row">
            <span class="label">身份证号:</span>
            <span class="value font-mono mask-id">{{ scanResult.idCardMasked }}</span>
          </div>
          <div class="info-row">
            <span class="label">联系电话:</span>
            <span class="value">{{ scanResult.phone }}</span>
          </div>
          <div class="info-row">
            <span class="label">受访人员:</span>
            <span class="value">{{ scanResult.hostName }} ({{ scanResult.hostDept }})</span>
          </div>
          <div class="info-row">
            <span class="label">到访目的:</span>
            <span class="value">{{ scanResult.visitPurpose }}</span>
          </div>
          <div class="info-row">
            <span class="label">保密协议:</span>
            <span class="value" :class="scanResult.ndaSigned ? 'text-green' : 'text-red'">
              {{ scanResult.ndaSigned ? '已签署存证' : '未签署 (禁止放行)' }}
            </span>
          </div>
          <div v-if="scanResult.visitType === 'MULTI'" class="info-row" style="background: #e6f7ff; padding: 6px 10px; border-radius: 6px;">
            <span class="label" style="color: #1890ff; font-weight: bold;">通行统计:</span>
            <span class="value" style="color: #096dd9; font-weight: bold;">在有效期内，今日累计打卡 {{ scanResult.todayEntryCount }} 次</span>
          </div>
        </div>

        <div class="security-tip">
          <van-icon name="info-o" /> 请仔细核对来访人员物理身份证姓名与上面脱敏 4 位数据，一致后点击对应放行按钮。
        </div>

        <!-- 智能动态操作按钮组 -->
        <div class="action-box" style="margin-top: 16px;">
          <!-- 1. 准予放行 (单次通行) -->
          <van-button v-if="scanResult.resultCode === 'PASS'" type="primary" block round size="large" color="#07c160" :loading="confirmLoading" @click="confirmEntry">
            ✅ 人证一致，一键确认放行与销号 (单次作废)
          </van-button>

          <!-- 2. 准予放行 (多日通行) -->
          <van-button v-else-if="scanResult.resultCode === 'PASS_MULTI'" type="primary" block round size="large" color="#00b578" :loading="confirmLoading" @click="confirmEntry">
            🟩 人证一致，确认本次放行打卡 (多日凭证保持有效)
          </van-button>

          <!-- 3. 信息不存在 -->
          <van-button v-else-if="scanResult.resultCode === 'NOT_FOUND'" type="primary" block round size="large" color="#1890ff" @click="showGateQrModal = true">
            📱 弹出现场盲来二维码，引导访客扫码填报
          </van-button>

          <!-- 4. 未签署 NDA 协议 -->
          <van-button v-else-if="scanResult.resultCode === 'NO_NDA'" type="danger" block round size="large">
            🛑 提示访客在手机端完成保密协议签署
          </van-button>

          <!-- 5. 待受访人审批 -->
          <van-button v-else-if="scanResult.resultCode === 'PENDING_APPROVAL'" type="warning" block round size="large">
            ⏰ 提示受访员工 ({{ scanResult.hostName }}) 完成到访审批
          </van-button>

          <!-- 6. 通行码已被使用 / 已过期 / 已拒绝 -->
          <van-button v-else disabled block round size="large">
            ⛔ {{ scanResult.resultTitle }} (不可放行)
          </van-button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { showToast, showSuccessToast, showFailToast } from 'vant'
import QrcodeVue from 'qrcode.vue'
import axios from 'axios'
import { Html5Qrcode } from 'html5-qrcode'
import jsQR from 'jsqr'

const route = useRoute()

const showGateQrModal = ref(false)
const showCameraModal = ref(false)
const visitorPageUrl = ref(window.location.origin + '/visitor')

const passTokenInput = ref('')
const fileInputRef = ref(null)

const scanning = ref(false)
const confirmLoading = ref(false)
const scanResult = ref(null)

onMounted(() => {
  const queryToken = route.query.verifyToken || route.query.token
  if (queryToken) {
    passTokenInput.value = String(queryToken).trim()
    showToast('已由原生相机扫码介入，正在自动校验凭证...')
    handleScan()
  }
})


let html5QrcodeScanner = null

const triggerPhotoScan = () => {
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

// 高精度抗摩尔纹图片二维码解码预处理器
const decodeQrFromImageFile = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        // Canvas 降采样至 800px（消除电脑屏幕拍摄的高频摩尔纹噪点）
        const maxDim = 800
        let width = img.width
        let height = img.height

        if (width > maxDim || height > maxDim) {
          if (width > height) {
            height = Math.round((height * maxDim) / width)
            width = maxDim
          } else {
            width = Math.round((width * maxDim) / height)
            height = maxDim
          }
        }

        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.imageSmoothingEnabled = true
        ctx.imageSmoothingQuality = 'high'
        ctx.drawImage(img, 0, 0, width, height)

        // 尝试 1: 直接使用 jsQR 解析
        let imageData = ctx.getImageData(0, 0, width, height)
        let code = jsQR(imageData.data, imageData.width, imageData.height, {
          inversionAttempts: "dontInvert"
        })

        if (code && code.data) {
          return resolve(code.data)
        }

        // 尝试 2: 色彩反转与全模式解析
        code = jsQR(imageData.data, imageData.width, imageData.height, {
          inversionAttempts: "attemptBoth"
        })

        if (code && code.data) {
          return resolve(code.data)
        }

        // 尝试 3: 二值化对比度强化处理
        const data = imageData.data
        for (let i = 0; i < data.length; i += 4) {
          const avg = (data[i] + data[i + 1] + data[i + 2]) / 3
          const val = avg > 120 ? 255 : 0
          data[i] = val
          data[i + 1] = val
          data[i + 2] = val
        }
        ctx.putImageData(imageData, 0, 0)
        const imageDataEnhanced = ctx.getImageData(0, 0, width, height)
        code = jsQR(imageDataEnhanced.data, imageDataEnhanced.width, imageDataEnhanced.height, {
          inversionAttempts: "attemptBoth"
        })

        if (code && code.data) {
          return resolve(code.data)
        }

        reject(new Error("未能识别出二维码"))
      }
      img.onerror = () => reject(new Error("图片加载失败"))
      img.src = e.target.result
    }
    reader.onerror = () => reject(new Error("文件读取失败"))
    reader.readAsDataURL(file)
  })
}

const handleFileScan = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  showToast({ message: '正在智能识别照片二维码...', duration: 2000 })
  
  try {
    // 优先调用后端专业级 ZXing 多尺度切片与自适应二值化 CV 解算引擎
    const formData = new FormData()
    formData.append('file', file)
    
    const res = await axios.post('/api/security/scan-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    if (res.data.code === 200 && res.data.data) {
      scanResult.value = res.data.data
      showSuccessToast('照片二维码识别并核验成功！')
      event.target.value = ''
      return
    } else if (res.data.message) {
      showFailToast(res.data.message)
      event.target.value = ''
      return
    }
  } catch (e) {
    // 降级尝试前端计算
  }

  // 客户端平滑降采样兜底算法
  try {
    const decodedText = await decodeQrFromImageFile(file)
    if (decodedText) {
      let token = decodedText
      if (decodedText.includes('token=')) {
        const match = decodedText.match(/token=([^&]+)/)
        if (match && match[1]) token = match[1]
      }
      passTokenInput.value = token
      showSuccessToast('二维码识别成功！')
      handleScan()
      event.target.value = ''
      return
    }
  } catch (e) {}

  showFailToast('照片中未定位到有效二维码，建议贴近二维码拍摄，或直接报下方的 8 位短码/手机号放行')
  event.target.value = ''
}




const startCameraScanner = async () => {
  showCameraModal.value = true
  await nextTick()
  try {
    if (!html5QrcodeScanner) {
      html5QrcodeScanner = new Html5Qrcode("qr-reader")
    }
    
    await html5QrcodeScanner.start(
      { facingMode: "environment" },
      { fps: 10, qrbox: { width: 250, height: 250 } },
      (decodedText) => {
        let token = decodedText
        if (decodedText && decodedText.includes('token=')) {
          const match = decodedText.match(/token=([^&]+)/)
          if (match && match[1]) token = match[1]
        }
        passTokenInput.value = token
        showSuccessToast('二维码读取成功！')
        stopCameraScanner()
        showCameraModal.value = false
        handleScan()
      },
      () => {}
    )
  } catch (e) {
    const isHttps = window.location.protocol === 'https:' || window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    if (!isHttps) {
      showFailToast({
        message: '受 iOS Safari 苹果安全规范限制，HTTP 协议下网页无法开启实时摄像头。请点左侧【拍照/上传识别】或使用【手机自带相机直扫】！',
        duration: 4500
      })
    } else {
      showFailToast('启动摄像头失败，请在设置中允许浏览器使用摄像头！')
    }
    showCameraModal.value = false
  }

}

const stopCameraScanner = () => {
  if (html5QrcodeScanner && html5QrcodeScanner.isScanning) {
    html5QrcodeScanner.stop().catch(err => console.error(err))
  }
}


const handleScan = async () => {
  if (!passTokenInput.value || !passTokenInput.value.trim()) {
    showFailToast('请输入通行 Token')
    return
  }
  scanning.value = true
  try {
    const res = await axios.get(`/api/security/scan?token=${encodeURIComponent(passTokenInput.value.trim())}`)
    if (res.data.code === 200) {
      scanResult.value = res.data.data
      showSuccessToast('核验请求已完成')
    } else {
      showFailToast(res.data.message || '核验失败')
      scanResult.value = null
    }
  } catch (e) {
    showFailToast('扫描核验网络异常')
  } finally {
    scanning.value = false
  }
}

const securityKey = ref(localStorage.getItem('SECURITY_AUTH_KEY') || '123456')

const getThemeIcon = (theme) => {
  switch (theme) {
    case 'green': return 'checked'
    case 'teal': return 'passed'
    case 'darkgray': return 'clock-o'
    case 'gray': return 'question-o'
    case 'blue': return 'info-o'
    case 'orange': return 'underway-o'
    case 'purple': return 'close'
    case 'red': return 'clear'
    default: return 'info-o'
  }
}

const confirmEntry = async () => {
  if (!scanResult.value || !scanResult.value.visitNo) return
  confirmLoading.value = true
  try {
    const res = await axios.post('/api/security/confirm-entry', {
      visitNo: scanResult.value.visitNo,
      securityName: '门岗保安(一号岗)',
      securityKey: securityKey.value
    })
    if (res.data.code === 200) {
      if (scanResult.value.visitType === 'MULTI') {
        showSuccessToast('打卡放行成功！多日凭证保持有效')
      } else {
        showSuccessToast('放行核销成功！单次凭证已作废')
      }
      scanResult.value = null
      passTokenInput.value = ''
    } else {
      showFailToast(res.data.message || '确认放行失败')
    }
  } catch (e) {
    showFailToast('放行网络失败')
  } finally {
    confirmLoading.value = false
  }
}


</script>

<style scoped>
.security-container { min-height: 100vh; background: #f7f8fa; }
.scan-card { margin: 16px; padding: 16px; background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.scan-title { font-size: 16px; font-weight: bold; margin-bottom: 12px; color: #323233; }

.result-card { margin: 16px; background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.result-header { padding: 16px 20px; display: flex; align-items: center; color: #fff; }
.result-title { font-size: 20px; font-weight: bold; }
.result-body { padding: 16px; }
.info-group { margin-top: 16px; background: #f7f8fa; padding: 12px; border-radius: 8px; }
.info-row { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 14px; }
.label { color: #969799; }
.value { color: #323233; }
.font-bold { font-weight: bold; font-size: 16px; }
.mask-id { letter-spacing: 1px; color: #1989fa; font-weight: bold; }

.security-tip { font-size: 12px; color: #ed6a0c; margin-top: 12px; background: #fffbe8; padding: 8px; border-radius: 4px; }
.action-box { margin-top: 20px; }

.text-green { color: #07c160; font-weight: bold; }
.text-red { color: #ee0a24; font-weight: bold; }

/* 多色调语义化 UI 卡片样式系 */
.theme-bg-green { background: linear-gradient(135deg, #07c160, #00b578); color: #fff; }
.theme-border-green { border-left: 6px solid #07c160; }
.theme-notice-green { background: #e8f8f0; color: #07c160; }

.theme-bg-teal { background: linear-gradient(135deg, #00b578, #10b981); color: #fff; }
.theme-border-teal { border-left: 6px solid #00b578; }
.theme-notice-teal { background: #e6f7f3; color: #00b578; }

.theme-bg-darkgray { background: linear-gradient(135deg, #595959, #434343); color: #fff; }
.theme-border-darkgray { border-left: 6px solid #434343; }
.theme-notice-darkgray { background: #f0f0f0; color: #434343; }

.theme-bg-gray { background: linear-gradient(135deg, #8c8c8c, #595959); color: #fff; }
.theme-border-gray { border-left: 6px solid #8c8c8c; }
.theme-notice-gray { background: #f5f5f5; color: #595959; }

.theme-bg-blue { background: linear-gradient(135deg, #1890ff, #096dd9); color: #fff; }
.theme-border-blue { border-left: 6px solid #1890ff; }
.theme-notice-blue { background: #e6f7ff; color: #096dd9; }

.theme-bg-orange { background: linear-gradient(135deg, #fa8c16, #d46b08); color: #fff; }
.theme-border-orange { border-left: 6px solid #fa8c16; }
.theme-notice-orange { background: #fff7e6; color: #d46b08; }

.theme-bg-purple { background: linear-gradient(135deg, #722ed1, #531dab); color: #fff; }
.theme-border-purple { border-left: 6px solid #722ed1; }
.theme-notice-purple { background: #f9f0ff; color: #531dab; }

.theme-bg-red { background: linear-gradient(135deg, #f5222d, #cf1322); color: #fff; }
.theme-border-red { border-left: 6px solid #f5222d; }
.theme-notice-red { background: #fff1f0; color: #cf1322; }
</style>

