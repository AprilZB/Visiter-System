<template>
  <div class="portal-container">
    <div class="header-banner">
      <h2>浙江脉通智造科技有限公司</h2>
      <h3>智能化园区访客系统 - 全功能测试导航门户</h3>
      <p class="subtitle">打通 外部访客 + 内部员工 + 物业保安 + 后台管理 四方协同</p>
    </div>

    <!-- 正门固定静态二维码展示区 -->
    <div class="qr-gate-card">
      <div class="gate-header">
        <van-icon name="qr" size="24" color="#1989fa" />
        <span class="gate-title">正门门岗 - 现场盲来固定静态申请二维码</span>
      </div>

      <div class="gate-body">
        <div class="qr-wrapper">
          <qrcode-vue :value="visitorPageUrl" :size="180" level="H" />
          <div class="scan-hint">扫码进入现场盲来 H5 申请</div>
        </div>
        <div class="gate-desc">
          <p><b>使用说明：</b>在园区正门门岗打印张贴此固定二维码。</p>
          <p>外部访客无预约到访时，手机微信/浏览器直接扫描此静态二维码即可填报申请、识别身份证并在线签署保密协议。</p>
          <div style="display: flex; gap: 12px; margin-top: 12px;">
            <el-button type="primary" plain icon="Printer" @click="printQr">打印固定门岗二维码</el-button>
            <el-button type="success" plain icon="Search" @click="openPhoneDialog">凭手机号找回通行码</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 手机号找回弹窗 -->
    <el-dialog v-model="phoneDialogVisible" title="凭手机号找回最新动态通行二维码" width="450px">
      <el-form label-width="100px">
        <el-form-item label="手机号码" required>
          <el-input v-model="queryPhone" placeholder="请输入申请时填报的手机号" />
        </el-form-item>
      </el-form>
      <div v-if="foundResult" style="text-align: center; padding: 16px; background: #f0f9eb; border-radius: 8px; margin-top: 12px;">
        <h4 style="color: #67c23a; margin: 0 0 8px 0;">已查询到近期有效通行码 (访客: {{ foundResult.visitorName }})</h4>
        <qrcode-vue :value="foundResult.passToken" :size="160" level="H" />
        <p style="font-size: 12px; color: #909399; margin-top: 8px;">出示给门岗保安扫码即可放行入园</p>
      </div>
      <template #footer>
        <el-button @click="phoneDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="searching" @click="searchByPhone">提交查询</el-button>
      </template>
    </el-dialog>


    <!-- 四大系统模块测试入口卡片 -->
    <div class="portal-grid">
      <!-- 卡片 1: 外部访客端 -->
      <div class="portal-card blue-card" @click="navTo('/visitor')">
        <div class="card-icon"><van-icon name="user-o" size="36" /></div>
        <div class="card-content">
          <h4>1. 外部访客申请端 (H5)</h4>
          <p>身份证 OCR 上传识别、先选部门后选人（自动过滤屏蔽部门）、来访事由下拉选择、保密协议强制弹窗签署存证、限时动态通行码。</p>
          <div class="path-badge">/visitor</div>
        </div>
      </div>

      <!-- 卡片 2: 内部员工端 -->
      <div class="portal-card green-card" @click="navTo('/host')">
        <div class="card-icon"><van-icon name="apps-o" size="36" /></div>
        <div class="card-content">
          <h4>2. 内部员工微应用 (钉钉端)</h4>
          <p>钉钉环境自动免登、主动生成场景 A 预约邀约链接、现场盲来申请一键卡片【同意/拒绝】审批、查看访客签署状态。</p>
          <div class="path-badge">/host</div>
        </div>
      </div>

      <!-- 卡片 3: 物业保安端 -->
      <div class="portal-card orange-card" @click="navTo('/security')">
        <div class="card-icon"><van-icon name="shield-o" size="36" /></div>
        <div class="card-content">
          <h4>3. 门岗物业保安端 (H5)</h4>
          <p>专属安全 URL 免登、扫描动态通行码、界面强脱敏展示中间 4 位掩码身份证 (如 3301021234****1234)、人证比对一键放行销号。</p>
          <div class="path-badge">/security</div>
        </div>
      </div>

      <!-- 卡片 4: PC 系统管理后台 -->
      <div class="portal-card purple-card" @click="navTo('/admin')">
        <div class="card-icon"><van-icon name="setting-o" size="36" /></div>
        <div class="card-content">
          <h4>4. PC 系统管理后台 (Dashboard)</h4>
          <p>动态修改 OCR 服务器地址、部门屏蔽防骚扰设置、来访事由字典维护、保密协议 (NDA) 版本控制发布、全量访客 SHA-256 审计链追溯。</p>
          <div class="path-badge">/admin (admin / Accupath@0723)</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import QrcodeVue from 'qrcode.vue'
import axios from 'axios'

const router = useRouter()
const visitorPageUrl = ref(window.location.origin + '/visitor')

const phoneDialogVisible = ref(false)
const queryPhone = ref('')
const searching = ref(false)
const foundResult = ref(null)

const openPhoneDialog = () => {
  queryPhone.value = ''
  foundResult.value = null
  phoneDialogVisible.value = true
}

const searchByPhone = async () => {
  if (!queryPhone.value || !queryPhone.value.trim()) {
    ElMessage.warning('请输入手机号码！')
    return
  }
  searching.value = true
  try {
    const res = await axios.get(`/api/visitor/latest-pass-token?phone=${queryPhone.value.trim()}`)
    if (res.data.code === 200 && res.data.data) {
      foundResult.value = res.data.data
      ElMessage.success('成功查询到有效的通行二维码！')
    } else {
      ElMessage.error(res.data.message || '未找到记录')
    }
  } catch (e) {
    ElMessage.error('查询失败')
  } finally {
    searching.value = false
  }
}

const navTo = (path) => {
  router.push(path)
}

const printQr = () => {
  window.print()
}
</script>


<style scoped>
.portal-container {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 24px;
  box-sizing: border-box;
}
.header-banner {
  text-align: center;
  background: linear-gradient(135deg, #001529, #003a70);
  color: #fff;
  padding: 32px 20px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  margin-bottom: 24px;
}
.header-banner h2 { margin: 0 0 8px 0; font-size: 24px; }
.header-banner h3 { margin: 0 0 8px 0; font-size: 18px; color: #409eff; }
.subtitle { margin: 0; font-size: 13px; opacity: 0.8; }

.qr-gate-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.gate-header { display: flex; align-items: center; border-bottom: 1px solid #f0f0f0; padding-bottom: 12px; margin-bottom: 16px; }
.gate-title { font-size: 18px; font-weight: bold; margin-left: 8px; color: #1f2937; }
.gate-body { display: flex; align-items: center; gap: 32px; flex-wrap: wrap; }
.qr-wrapper {
  text-align: center;
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
  border: 2px dashed #409eff;
}
.scan-hint { font-size: 12px; color: #409eff; margin-top: 8px; font-weight: bold; }
.gate-desc { flex: 1; min-width: 280px; font-size: 14px; color: #4b5563; line-height: 1.8; }
.gate-desc p { margin: 0 0 8px 0; }

.portal-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px; }
.portal-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
  position: relative;
  overflow: hidden;
}
.portal-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.12); }
.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.blue-card .card-icon { background: linear-gradient(135deg, #1989fa, #0570db); }
.green-card .card-icon { background: linear-gradient(135deg, #07c160, #049b4c); }
.orange-card .card-icon { background: linear-gradient(135deg, #ff976a, #ed6a0c); }
.purple-card .card-icon { background: linear-gradient(135deg, #7232dd, #521fb1); }

.card-content h4 { margin: 0 0 8px 0; font-size: 16px; color: #111827; }
.card-content p { margin: 0 0 12px 0; font-size: 13px; color: #6b7280; line-height: 1.5; }
.path-badge { display: inline-block; font-size: 11px; background: #f3f4f6; color: #374151; padding: 2px 8px; border-radius: 6px; font-weight: bold; font-family: monospace; }
</style>
