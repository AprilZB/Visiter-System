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
        placeholder="对准扫码枪扫描 / 拍照 / 粘贴凭证"
        label="通行凭证"
        @keyup.enter="handleScan"
      >
        <template #button>
          <van-button size="small" type="primary" :loading="scanning" @click="handleScan">核验放行</van-button>
        </template>
      </van-field>
      <div style="font-size: 11px; color: #969799; margin-top: 6px; text-align: right;">
        * 支持拍照扫码、红外扫码枪对准扫描或手机实时镜头扫码
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


    <!-- 核验结果展示区 (脱敏合规) -->
    <div v-if="scanResult" class="result-card" :class="scanResult.canPass ? 'border-success' : 'border-danger'">
      <div class="result-header" :class="scanResult.canPass ? 'bg-success' : 'bg-danger'">
        <van-icon :name="scanResult.canPass ? 'checked' : 'clear'" size="32" />
        <div class="result-title">{{ scanResult.canPass ? '【准予放行】' : '【禁止放行】' }}</div>
      </div>

      <div class="result-body">
        <van-notice-bar v-if="scanResult.warningMessage" :type="scanResult.canPass ? 'success' : 'danger'" :text="scanResult.warningMessage" />

        <div class="info-group">
          <div class="info-row">
            <span class="label">访客姓名:</span>
            <span class="value font-bold">{{ scanResult.visitorName }}</span>
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
            <span class="label">保密协议:</span>
            <span class="value" :class="scanResult.ndaSigned ? 'text-green' : 'text-red'">
              {{ scanResult.ndaSigned ? '已签署存证' : '未签署 (不可放行)' }}
            </span>
          </div>
        </div>

        <div class="security-tip">
          <van-icon name="info-o" /> 请仔细核对来访人员物理身份证姓名与上面脱敏 4 位数据，一致后点击一键放行。
        </div>

        <div v-if="scanResult.canPass" class="action-box">
          <van-button type="success" block round size="large" :loading="confirmLoading" @click="confirmEntry">
            核对一致，一键确认放行与销号
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { showToast, showSuccessToast, showFailToast } from 'vant'
import QrcodeVue from 'qrcode.vue'
import axios from 'axios'
import { Html5Qrcode } from 'html5-qrcode'

const showGateQrModal = ref(false)
const showCameraModal = ref(false)
const visitorPageUrl = ref(window.location.origin + '/visitor')

const passTokenInput = ref('')
const fileInputRef = ref(null)

const scanning = ref(false)
const confirmLoading = ref(false)
const scanResult = ref(null)

let html5QrcodeScanner = null

const triggerPhotoScan = () => {
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

const handleFileScan = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  showToast({ message: '正在解析二维码...', duration: 1000 })
  try {
    const html5Qrcode = new Html5Qrcode("qr-reader")
    const decodedText = await html5Qrcode.scanFile(file, true)
    
    let token = decodedText
    if (decodedText && decodedText.includes('token=')) {
      const match = decodedText.match(/token=([^&]+)/)
      if (match && match[1]) token = match[1]
    }
    passTokenInput.value = token
    showSuccessToast('二维码识别成功！')
    handleScan()
  } catch (e) {
    showFailToast('无法识别该图片中的二维码，请拍摄清晰原图')
  } finally {
    event.target.value = ''
  }
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
    showFailToast('启动摄像头失败，请允许浏览器使用摄像头！')
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

const confirmEntry = async () => {
  if (!scanResult.value || !scanResult.value.visitNo) return
  confirmLoading.value = true
  try {
    const res = await axios.post('/api/security/confirm-entry', {
      visitNo: scanResult.value.visitNo,
      securityName: '门岗保安(一号岗)'
    })
    if (res.data.code === 200) {
      showSuccessToast('放行核销成功！动态通行码已作废')
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

.result-card { margin: 16px; background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.08); border: 2px solid transparent; }
.border-success { border-color: #07c160; }
.border-danger { border-color: #ee0a24; }

.result-header { padding: 20px; color: #fff; display: flex; align-items: center; justify-content: center; }
.bg-success { background: linear-gradient(135deg, #07c160, #049b4c); }
.bg-danger { background: linear-gradient(135deg, #ee0a24, #c00000); }
.result-title { font-size: 20px; font-weight: bold; margin-left: 8px; }

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
</style>
