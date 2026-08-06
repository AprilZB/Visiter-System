<template>
  <div class="admin-container">
    <!-- 顶部固定系统 Header -->
    <el-header class="admin-header">
      <div class="logo-title">
        <el-icon><Monitor /></el-icon>
        <span>脉通多方协同智能化访客系统 - 管理后台</span>
      </div>
      <div class="header-right">
        <div v-if="isLoggedIn" class="user-badge">
          系统管理员 (Admin) <span class="expire-timer">| 无操作自动退出会话: {{ formattedRemainingTime }}</span>
        </div>

        <el-button v-else type="primary" size="small" @click="loginVisible = true">管理员登录</el-button>
      </div>

    </el-header>

    <el-main class="admin-main">
      <el-tabs type="border-card" class="admin-tabs">
        <!-- 标签 1: OCR 服务配置管理 -->
        <el-tab-pane label="OCR 服务器配置">
          <el-card shadow="never" class="box-card">
            <template #header>
              <div class="card-header">
                <span>身份证 OCR 识别 API 配置</span>
              </div>
            </template>
            <el-form :model="ocrForm" label-width="140px">
              <el-form-item label="当前 OCR API 地址">
                <el-input v-model="ocrForm.ocrServiceUrl" placeholder="如 http://10.11.100.238:8081/ocr" style="width: 500px;" />
              </el-form-item>
              <el-form-item label="服务状态说明">
                <el-tag type="success">推荐：内网 PaddleOCR 服务 (10.11.100.238:8081)</el-tag>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="savingOcr" @click="saveOcrConfig">保存配置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-tab-pane>

        <!-- 标签 2: 部门屏蔽与防骚扰设置 (多级组织架构树 + 屏蔽开关) -->
        <el-tab-pane label="部门屏蔽防骚扰配置">
          <el-card shadow="never" class="box-card">
            <template #header>
              <div class="card-header">
                <span>多级防骚扰部门树（开启屏蔽的部门及其员工不会在访客端展示供盲选）</span>
                <el-button type="primary" size="small" icon="Refresh" @click="loadOrgTree">同步刷新架构树</el-button>
              </div>
            </template>
            <div class="shield-tree-wrapper">
              <el-tree
                :data="orgTreeData"
                node-key="fullPath"
                :props="{ label: 'label', children: 'children' }"
                default-expand-all
                :expand-on-click-node="false"
              >
                <template #default="{ node, data }">
                  <div class="custom-shield-node">
                    <span class="shield-node-label">
                      <el-icon v-if="data.children && data.children.length > 0" style="color: #e6a23c; margin-right: 6px;"><Folder /></el-icon>
                      <el-icon v-else style="color: #409eff; margin-right: 6px;"><User /></el-icon>
                      <span style="font-weight: 500; color: #303133;">{{ node.label }}</span>
                      <el-tag v-if="data.count !== undefined" size="small" type="info" style="margin-left: 8px;">{{ data.count }}在职</el-tag>
                    </span>
                    <div class="shield-switch-area">
                      <el-switch
                        v-model="data.isShielded"
                        :active-value="1"
                        :inactive-value="0"
                        active-text="已屏蔽"
                        inactive-text="公开可选"
                        @change="(val) => handleTreeDeptShield(data, val)"
                      />
                    </div>
                  </div>
                </template>
              </el-tree>
            </div>
          </el-card>
        </el-tab-pane>


        <!-- 标签 3: 来访事由字典选项维护 -->
        <el-tab-pane label="来访事由字典配置">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>来访事由选项字典列表（禁用的事由不会在访客注册端下拉框展示）</span>
                <el-button type="primary" size="small" icon="Plus" @click="openAddReasonDialog">新增事由选项</el-button>
              </div>
            </template>
            <el-table :data="adminReasonList" border style="width: 100%">
              <el-table-column prop="id" label="ID" width="80" align="center" />
              <el-table-column prop="reasonName" label="事由名称" min-width="180" />
              <el-table-column prop="sortOrder" label="排序权重" width="120" align="center" />
              <el-table-column label="状态 (启用/禁用)" width="180" align="center">
                <template #default="scope">
                  <el-switch
                    v-model="scope.row.isActive"
                    :active-value="1"
                    :inactive-value="0"
                    active-text="使用中"
                    inactive-text="已禁用"
                    @change="(val) => toggleReasonStatus(scope.row, val)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" align="center">
                <template #default="scope">
                  <el-button type="primary" size="small" icon="Edit" @click="openEditReasonDialog(scope.row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>


        <!-- 标签 4: 保密协议 (NDA) 版本发布 (支持PDF上传+单一生效版本) -->
        <el-tab-pane label="保密协议 (NDA) 版本发布">
          <div class="nda-admin-layout">
            <el-card class="nda-list-card" shadow="never">
              <template #header>
                <div class="card-header">
                  <span>历史与当前保密协议版本（系统保证：当前生效版本有且仅有一个）</span>
                  <el-button type="primary" size="small" icon="Upload" @click="openPublishDialog">发布新版本保密协议</el-button>
                </div>
              </template>
              <el-table :data="ndaTemplates" border style="width: 100%">
                <el-table-column prop="version" label="版本号" width="110" align="center">
                  <template #default="scope">
                    <el-tag type="primary" effect="plain" style="font-weight: bold;">{{ scope.row.version }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="title" label="协议标题" min-width="220" />
                <el-table-column label="PDF保密协议文档" min-width="190" align="center">
                  <template #default="scope">
                    <el-button
                      v-if="scope.row.pdfUrl"
                      type="primary"
                      size="small"
                      icon="Document"
                      @click="previewPdfModal(scope.row.pdfUrl)"
                    >
                      弹窗在线预览 PDF
                    </el-button>
                    <span v-else style="color: #909399; font-size: 13px;">无 PDF (纯文本版)</span>
                  </template>
                </el-table-column>

                <el-table-column label="生效状态" width="140" align="center">
                  <template #default="scope">
                    <el-tag v-if="scope.row.isActive === 1" type="success" effect="dark">
                      <el-icon><Check /></el-icon> 当前生效中
                    </el-tag>
                    <el-tag v-else type="info" effect="plain">历史版本</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="createdBy" label="发布人" width="120" align="center" />
                <el-table-column prop="createdAt" label="发布时间" width="170" align="center" />
                <el-table-column label="版本切换" width="140" align="center">
                  <template #default="scope">
                    <el-button
                      v-if="scope.row.isActive !== 1"
                      type="warning"
                      size="small"
                      icon="Switch"
                      @click="handleActivateNda(scope.row)"
                    >
                      设为唯一生效
                    </el-button>
                    <span v-else style="color: #67c23a; font-size: 12px; font-weight: bold;">[正在使用]</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
        </el-tab-pane>


        <!-- 标签 5: 全量访客通行与保密协议数字审计链 -->
        <el-tab-pane label="访客通行与 NDA 审计追溯">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>全量访客备案与数字证据链存证记录</span>
                <el-button type="success" size="small" @click="loadVisitors">刷新数据</el-button>
              </div>
            </template>
            <el-table :data="visitorList" border style="width: 100%" stripe>
              <el-table-column prop="visitNo" label="访客单号" width="180" />
              <el-table-column prop="visitorName" label="访客姓名" width="100" />
              <el-table-column prop="idCardDecrypted" label="身份证号 (管理明文)" width="190" />
              <el-table-column prop="phone" label="手机号" width="130" />
              <el-table-column prop="hostName" label="受访人" width="100" />
              <el-table-column prop="visitPurpose" label="来访事由" width="120" />
              <el-table-column label="保密协议" width="110">
                <template #default="scope">
                  <el-tag :type="scope.row.ndaSigned ? 'success' : 'danger'">
                    {{ scope.row.ndaSigned ? '已完成签署' : '未签署拦截' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="手写电子签名" width="150" align="center">
                <template #default="scope">
                  <div v-if="scope.row.signatureBase64" style="display: flex; align-items: center; justify-content: center; gap: 4px;">
                    <el-image
                      :src="scope.row.signatureBase64"
                      :preview-src-list="[scope.row.signatureBase64]"
                      fit="contain"
                      style="width: 100px; height: 40px; border: 1px solid #dcdfe6; border-radius: 4px; background: #fafafa; cursor: pointer;"
                      preview-teleported
                    />
                  </div>
                  <el-tag v-else type="info" size="small">未采集签名</el-tag>
                </template>
              </el-table-column>

              <el-table-column prop="ndaVersion" label="协议版本" width="90" />
              <el-table-column prop="clientIp" label="签署 IP" width="130" />
              <el-table-column prop="hashChain" label="SHA-256 存证哈希链" show-overflow-tooltip />
              <el-table-column prop="status" label="流程状态" width="120" />
            </el-table>
          </el-card>
        </el-tab-pane>

        <!-- 标签 6: 组织架构与全员档案 (APHR 系统集成) -->
        <el-tab-pane label="组织架构与全员档案">
          <div class="org-layout">
            <!-- 左侧: 多级树状组织架构 (默认收缩，不展开) -->
            <el-card shadow="never" class="org-tree-card">
              <template #header>
                <div class="tree-header">
                  <span class="tree-title"><el-icon><FolderOpened /></el-icon> 多级组织架构树</span>
                  <el-button type="primary" link size="small" @click="loadOrgTree">刷新树</el-button>
                </div>
              </template>
              <el-input
                v-model="filterTreeText"
                placeholder="搜索部门..."
                size="small"
                clearable
                style="margin-bottom: 12px;"
              />
              <el-tree
                ref="deptTreeRef"
                :data="orgTreeData"
                node-key="id"
                :props="{ label: 'label', children: 'children' }"
                :default-expanded-keys="[0]"
                :highlight-current="true"
                :filter-node-method="filterNode"
                @node-click="handleNodeClick"
              >
                <template #default="{ node, data }">
                  <div class="custom-tree-node">
                    <span class="dept-label-text">
                      <el-icon v-if="data.children && data.children.length > 0" style="margin-right: 4px; color: #e6a23c;"><Folder /></el-icon>
                      <el-icon v-else style="margin-right: 4px; color: #409eff;"><User /></el-icon>
                      {{ node.label }}
                    </span>
                    <el-badge v-if="data.count !== undefined" :value="data.count > 99 ? '99+' : data.count" type="info" class="tree-badge" />
                  </div>
                </template>
              </el-tree>
            </el-card>

            <!-- 右侧: 员工档案列表 (默认10行 + 搜素栏吸顶 + 一眼看到底部分页器) -->
            <el-card shadow="never" class="org-user-card">
              <template #header>
                <div class="user-card-header">
                  <span class="header-title">
                    {{ selectedDeptName ? selectedDeptName + ' - 员工档案' : '全员档案列表' }}
                    <span class="total-tag">(共 {{ pagination.total }} 条档案)</span>
                  </span>
                  <div class="action-buttons">
                    <el-button type="primary" icon="Refresh" size="small" :loading="syncing" @click="handleTriggerSync">
                      手动同步 APHR 数据
                    </el-button>
                  </div>
                </div>
              </template>

              <!-- 上方多条件检索栏 (滚动时固定吸顶) -->
              <el-form :inline="true" :model="orgFilter" size="small" class="search-form">
                <el-form-item label="域账号">
                  <el-input v-model="orgFilter.adAccount" placeholder="例如 zhang.san" clearable @keyup.enter="handleSearch" style="width: 130px;" />
                </el-form-item>
                <el-form-item label="姓名">
                  <el-input v-model="orgFilter.name" placeholder="请输入姓名" clearable @keyup.enter="handleSearch" style="width: 120px;" />
                </el-form-item>
                <el-form-item label="部门">
                  <el-input v-model="orgFilter.deptName" placeholder="部门名称" clearable @keyup.enter="handleSearch" style="width: 130px;" />
                </el-form-item>
                <el-form-item label="状态">
                  <el-select v-model="orgFilter.status" placeholder="入离职状态" clearable style="width: 110px;" @change="handleSearch">
                    <el-option label="全部" value="" />
                    <el-option label="在职" value="在职" />
                    <el-option label="试用期" value="试用期" />
                    <el-option label="离职" value="离职" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
                  <el-button icon="RefreshLeft" @click="resetOrgSearch">重置</el-button>
                </el-form-item>
              </el-form>

              <!-- 表格与底部分页器弹性区域 -->
              <div class="table-pagination-container">
                <el-table :data="orgUserList" border stripe style="width: 100%; flex: 1;" v-loading="loadingUsers" size="default">
                  <el-table-column prop="workNo" label="工号" width="110" />
                  <el-table-column prop="adAccount" label="域账号" width="140">
                    <template #default="scope">
                      <span class="ad-account-tag">{{ scope.row.adAccount || '-' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="name" label="姓名" width="110">
                    <template #default="scope">
                      <span style="font-weight: bold; color: #303133;">{{ scope.row.name }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="gender" label="性别" width="70" />
                  <el-table-column prop="age" label="年龄" width="70">
                    <template #default="scope">
                      <span>{{ scope.row.age ? scope.row.age + '岁' : '-' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="deptName" label="所属部门/层级" show-overflow-tooltip />
                  <el-table-column prop="managerName" label="直属上级" width="120" />
                  <el-table-column prop="status" label="入离职状态" width="110">
                    <template #default="scope">
                      <el-tag :type="scope.row.status === '在职' ? 'success' : (scope.row.status === '试用期' ? 'warning' : 'info')" effect="light">
                        {{ scope.row.status || '在职' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="syncedAt" label="最近同步时间" width="170" />
                </el-table>

                <!-- 底部分页器 (默认 10 行，一眼直接可见) -->
                <div class="pagination-wrapper">
                  <el-pagination
                    v-model:current-page="pagination.currentPage"
                    v-model:page-size="pagination.pageSize"
                    :page-sizes="[10, 20, 50, 100, 200]"
                    layout="total, sizes, prev, pager, next, jumper"
                    :total="pagination.total"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                    background
                  />
                </div>
              </div>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-main>

    <!-- 登录对话框 -->
    <!-- 登录对话框 (强拦截防透传) -->
    <el-dialog
      v-model="loginVisible"
      title="系统管理员身份验证 (未登录)"
      width="400px"
      :close-on-click-overlay="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <el-form :model="loginForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="请输入管理员账号 (admin)" @keyup.enter="handleAdminLogin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入管理员密码" show-password @keyup.enter="handleAdminLogin" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" style="width: 100%;" :loading="loggingIn" @click="handleAdminLogin">验证并登录管理后台</el-button>
      </template>
    </el-dialog>


    <!-- 发布新版本弹窗 (包含PDF上传+建立新版本号) -->
    <el-dialog v-model="publishDialogVisible" title="发布新版本保密协议 (支持上传盖章 PDF 官方协议)" width="650px">
      <el-form :model="newNdaForm" label-width="120px">
        <el-form-item label="版本号" required>
          <el-input v-model="newNdaForm.version" placeholder="例如 V1.1.0 或 V2.0.0" />
        </el-form-item>
        <el-form-item label="协议标题" required>
          <el-input v-model="newNdaForm.title" placeholder="如：浙江脉通智造科技有限公司外来人员保密协议书" />
        </el-form-item>
        <el-form-item label="上传 PDF 协议">
          <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
            <el-upload
              action="/api/admin/nda/upload-pdf"
              :headers="uploadHeaders"
              accept=".pdf"
              :show-file-list="false"
              :on-success="handlePdfUploadSuccess"
              :on-error="handlePdfUploadError"
            >
              <el-button type="primary" icon="Upload">选择并上传 PDF 文件</el-button>
            </el-upload>
            <el-button v-if="newNdaForm.pdfUrl" type="success" plain icon="View" @click="previewPdfModal(newNdaForm.pdfUrl)">
              实时预览已上传 PDF
            </el-button>
          </div>
          <div v-if="newNdaForm.pdfUrl" style="margin-top: 8px; color: #67c23a; font-size: 13px; word-break: break-all;">
            <el-icon><Check /></el-icon> 已成功挂载 PDF 文件：{{ newNdaForm.pdfUrl }}
          </div>
        </el-form-item>
        <el-form-item label="协议文本说明">
          <el-input v-model="newNdaForm.content" type="textarea" :rows="4" placeholder="例如：本版本为2026最新盖章加密版协议，访客到访前需完整阅读并进行手写签名确认。" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="publishNewNda">立即发布并设为唯一生效</el-button>
      </template>
    </el-dialog>


    <!-- PDF 在线弹窗预览 -->
    <el-dialog
      v-model="pdfPreviewVisible"
      title="保密协议 (NDA) 官方盖章 PDF 在线预览"
      width="85%"
      top="4vh"
      destroy-on-close
      append-to-body
    >
      <div style="height: 76vh; width: 100%; background: #525659; border-radius: 6px; overflow: hidden; position: relative;">
        <object
          v-if="currentPreviewPdfUrl"
          :data="currentPreviewPdfUrl"
          type="application/pdf"
          width="100%"
          height="100%"
        >
          <iframe
            :src="currentPreviewPdfUrl"
            style="width: 100%; height: 100%; border: none;"
          ></iframe>
        </object>
      </div>
      <template #footer>
        <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
          <a :href="currentPreviewPdfUrl" target="_blank" style="color: #409eff; font-size: 13px; text-decoration: underline;">
            🔗 若当前浏览器被系统插件拦截渲染，点此在独立标签页打开 PDF 官方源文件
          </a>
          <el-button type="primary" @click="pdfPreviewVisible = false">关闭在线预览</el-button>
        </div>
      </template>
    </el-dialog>




    <!-- 新增事由弹窗 -->
    <el-dialog v-model="addReasonDialogVisible" title="新增来访事由字典选项" width="450px">
      <el-form :model="newReasonForm" label-width="100px">
        <el-form-item label="事由名称" required>
          <el-input v-model="newReasonForm.reasonName" placeholder="如：项目验收、学术交流" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addReasonDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addingReason" @click="saveNewReason">保存添加</el-button>
      </template>
    </el-dialog>

    <!-- 编辑事由弹窗 -->
    <el-dialog v-model="editReasonDialogVisible" title="编辑来访事由字典选项" width="450px">
      <el-form :model="editReasonForm" label-width="100px">
        <el-form-item label="事由名称" required>
          <el-input v-model="editReasonForm.reasonName" placeholder="如：商务洽谈" />
        </el-form-item>
        <el-form-item label="排序权重">
          <el-input-number v-model="editReasonForm.sortOrder" :min="1" :max="999" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-radio-group v-model="editReasonForm.isActive">
            <el-radio :value="1">使用中 (公开)</el-radio>
            <el-radio :value="0">已禁用 (隐去)</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editReasonDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingEditReason" @click="saveEditReason">保存修改</el-button>
      </template>
    </el-dialog>
  </div>

</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'

import { ElMessage, ElMessageBox } from 'element-plus'

import axios from 'axios'

const loginVisible = ref(false)
const loggingIn = ref(false)
const loginForm = reactive({
  username: 'admin',
  password: ''
})

const adminDeptList = ref([])
const adminReasonList = ref([])
const addReasonDialogVisible = ref(false)
const addingReason = ref(false)
const newReasonForm = reactive({ reasonName: '' })

const loadAdminDepts = async () => {
  try {
    const res = await axios.get('/api/admin/depts')
    if (res.data.code === 200) {
      adminDeptList.value = res.data.data
    }
  } catch (e) {}
}

const handleTreeDeptShield = async (deptNode, isShielded) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先完成管理员登录后再操作！')
    loginVisible.value = true
    return
  }
  const targetDeptName = deptNode.fullPath || deptNode.deptName || deptNode.label
  try {
    const token = sessionStorage.getItem('adminToken') || ''
    const res = await axios.post('/api/admin/dept/toggle-shield', 
      { deptName: targetDeptName, isShielded },
      { headers: { Authorization: token } }
    )
    if (res.data.code === 200) {
      ElMessage.success(`部门 [${deptNode.label}] 屏蔽防骚扰状态已更新为：${isShielded === 1 ? '已屏蔽' : '公开可选'}`)
    } else {
      ElMessage.error(res.data.message || '更新失败')
    }
  } catch (e) {
    ElMessage.error('更新部门屏蔽状态失败')
  }
}

const toggleDeptShield = async (deptId, isShielded) => {

  try {
    const res = await axios.post('/api/admin/dept/toggle-shield', { deptId, isShielded })
    if (res.data.code === 200) {
      ElMessage.success(res.data.message)
    } else {
      ElMessage.error(res.data.message)
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const loadAdminReasons = async () => {
  try {
    const res = await axios.get('/api/admin/reasons')
    if (res.data.code === 200) {
      adminReasonList.value = res.data.data
    }
  } catch (e) {}
}

const openAddReasonDialog = () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先完成管理员登录后再添加事由选项！')
    loginVisible.value = true
    return
  }
  newReasonForm.reasonName = ''
  addReasonDialogVisible.value = true
}

const saveNewReason = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先完成管理员登录后再添加事由选项！')
    loginVisible.value = true
    return
  }
  if (!newReasonForm.reasonName || !newReasonForm.reasonName.trim()) {
    ElMessage.warning('请输入事由名称')
    return
  }
  addingReason.value = true
  try {
    const token = sessionStorage.getItem('adminToken') || ''
    const res = await axios.post('/api/admin/reason/add', 
      { reasonName: newReasonForm.reasonName.trim() },
      { headers: { Authorization: token } }
    )
    if (res.data.code === 200) {
      ElMessage.success('来访事由添加成功！')
      addReasonDialogVisible.value = false
      loadAdminReasons()
    } else {
      ElMessage.error(res.data.message || '添加失败')
    }
  } catch (e) {
    ElMessage.error('网络异常')
  } finally {
    addingReason.value = false
  }
}

const editReasonDialogVisible = ref(false)
const savingEditReason = ref(false)
const editReasonForm = reactive({
  id: null,
  reasonName: '',
  sortOrder: 1,
  isActive: 1
})

const openEditReasonDialog = (row) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先完成管理员登录后再编辑事由！')
    loginVisible.value = true
    return
  }
  editReasonForm.id = row.id
  editReasonForm.reasonName = row.reasonName
  editReasonForm.sortOrder = row.sortOrder || 1
  editReasonForm.isActive = row.isActive !== undefined ? row.isActive : 1
  editReasonDialogVisible.value = true
}

const saveEditReason = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先完成管理员登录后再保存修改！')
    loginVisible.value = true
    return
  }
  if (!editReasonForm.reasonName || !editReasonForm.reasonName.trim()) {
    ElMessage.warning('请输入有效的事由名称')
    return
  }
  savingEditReason.value = true
  try {
    const token = sessionStorage.getItem('adminToken') || ''
    const res = await axios.post('/api/admin/reason/update', editReasonForm, { headers: { Authorization: token } })
    if (res.data.code === 200) {
      ElMessage.success('来访事由更新成功！')
      editReasonDialogVisible.value = false
      loadAdminReasons()
    } else {
      ElMessage.error(res.data.message || '修改失败')
    }
  } catch (e) {
    ElMessage.error('修改保存失败')
  } finally {
    savingEditReason.value = false
  }
}

const toggleReasonStatus = async (row, isActiveVal) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先完成管理员登录后再切换事由状态！')
    loginVisible.value = true
    row.isActive = isActiveVal === 1 ? 0 : 1
    return
  }
  try {
    const token = sessionStorage.getItem('adminToken') || ''
    const res = await axios.post('/api/admin/reason/toggle-status', 
      { id: row.id, isActive: isActiveVal },
      { headers: { Authorization: token } }
    )
    if (res.data.code === 200) {
      ElMessage.success(`事由 [${row.reasonName}] 已更新为：${isActiveVal === 1 ? '使用中' : '已禁用'}`)
    } else {
      ElMessage.error(res.data.message || '更新状态失败')
      row.isActive = isActiveVal === 1 ? 0 : 1
    }
  } catch (e) {
    ElMessage.error('切换状态失败')
    row.isActive = isActiveVal === 1 ? 0 : 1
  }
}



const isLoggedIn = ref(false)
const remainingSeconds = ref(300) // 连续5分钟无操作超时 = 300秒
let timer = null
let lastActiveTimestamp = Date.now()

const formattedRemainingTime = computed(() => {
  const m = Math.floor(remainingSeconds.value / 60).toString().padStart(2, '0')
  const s = (remainingSeconds.value % 60).toString().padStart(2, '0')
  return `${m}:${s}`
})

// 用户在页面发生任何键盘/鼠标/点击/滚动操作时，滑动防呆重置 5 分钟倒计时
const resetIdleTimer = () => {
  if (isLoggedIn.value) {
    lastActiveTimestamp = Date.now()
    sessionStorage.setItem('adminLastActive', lastActiveTimestamp.toString())
  }
}

const startSessionTimer = (durationSeconds = 300) => {
  if (timer) clearInterval(timer)
  isLoggedIn.value = true
  lastActiveTimestamp = Date.now()
  sessionStorage.setItem('adminLastActive', lastActiveTimestamp.toString())

  timer = setInterval(() => {
    const idleSeconds = Math.floor((Date.now() - lastActiveTimestamp) / 1000)
    const diff = 300 - idleSeconds
    if (diff > 0) {
      remainingSeconds.value = diff
    } else {
      remainingSeconds.value = 0
      clearInterval(timer)
      timer = null
      isLoggedIn.value = false
      sessionStorage.removeItem('adminLastActive')
      sessionStorage.removeItem('adminToken')
      ElMessage.warning('检测到您已连续 5 分钟未在页面进行任何操作，系统已自动安全退出登录！')
      loginVisible.value = true
    }
  }, 1000)
}

const checkExistingSession = () => {
  const storedLastActive = sessionStorage.getItem('adminLastActive')
  if (storedLastActive) {
    const lastActive = parseInt(storedLastActive, 10)
    const idleSeconds = Math.floor((Date.now() - lastActive) / 1000)
    if (idleSeconds < 300) {
      startSessionTimer(300 - idleSeconds)
    } else {
      sessionStorage.removeItem('adminLastActive')
      sessionStorage.removeItem('adminToken')
    }
  }
}


const handleAdminLogin = async () => {
  loggingIn.value = true
  try {
    const res = await axios.post('/api/admin/login', loginForm)
    if (res.data.code === 200) {
      ElMessage.success('管理员登录成功！登录有效期 5 分钟。')
      loginVisible.value = false
      if (res.data.data && res.data.data.token) {
        sessionStorage.setItem('adminToken', res.data.data.token)
      }
      const expiresIn = res.data.data.expiresIn || 300
      startSessionTimer(expiresIn)
      refreshAllData()
    } else {
      ElMessage.error(res.data.message || '用户名或密码错误')
    }
  } catch (e) {
    ElMessage.error('网络或账号密码验证错误')
  } finally {
    loggingIn.value = false
  }
}



const ocrForm = reactive({ ocrServiceUrl: '' })
const savingOcr = ref(false)

const ndaTemplates = ref([])
const publishDialogVisible = ref(false)
const publishing = ref(false)
const newNdaForm = reactive({
  version: 'V1.2.0',
  title: '浙江脉通智造科技有限公司外来人员保密协议书 (PDF加印官方重置版)',
  content: '已随版本号附带官方盖章 PDF 保密协议附件。',
  pdfUrl: '',
  createdBy: 'Admin'

})

const uploadHeaders = computed(() => {
  return {
    Authorization: sessionStorage.getItem('adminToken') || ''
  }
})

const pdfPreviewVisible = ref(false)
const currentPreviewPdfUrl = ref('')

const getFullPdfUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('/')) {
    return 'http://localhost:8096' + url
  }
  return url
}

const previewPdfModal = (url) => {
  if (!url) {
    ElMessage.warning('暂无可预览的 PDF 文件')
    return
  }
  currentPreviewPdfUrl.value = getFullPdfUrl(url)
  pdfPreviewVisible.value = true
  ElMessage.info('正在载入 PDF 官方协议...')
}



const handlePdfUploadSuccess = (response) => {
  if (response && response.code === 200) {
    newNdaForm.pdfUrl = response.data.fileUrl
    ElMessage.success(`PDF 文件 [${response.data.originalFilename}] 上传成功！`)
  } else {
    ElMessage.error(response.message || 'PDF 上传失败')
  }
}

const handlePdfUploadError = () => {
  ElMessage.error('PDF 上传发生错误，请检查服务器连接或重新登录！')
}

const handleActivateNda = async (row) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先完成管理员身份验证后再切换生效版本！')
    loginVisible.value = true
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要将版本号【${row.version}】（${row.title}）切换为当前【唯一生效版本】吗？系统将自动停用其他所有历史版本。`,
      '版本生效切换确认',
      {
        confirmButtonText: '确定切换并生效',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    const token = sessionStorage.getItem('adminToken') || ''
    const res = await axios.post(`/api/admin/nda/activate/${row.id}`, {}, { headers: { Authorization: token } })
    if (res.data.code === 200) {
      ElMessage.success(res.data.data || res.data.message || '生效版本已切换成功！')
      loadNdaTemplates()
    } else {
      ElMessage.error(res.data.message || '切换失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      const errMsg = e.response?.data?.message || e.message || '网络连接或权限验证发生异常'
      ElMessage.error(errMsg)
      if (e.response?.status === 401) {
        isLoggedIn.value = false
        loginVisible.value = true
      }
    }
  }
}



const visitorList = ref([])

const loadOcrConfig = async () => {
  try {
    const res = await axios.get('/api/admin/config/ocr')
    if (res.data.code === 200) {
      ocrForm.ocrServiceUrl = res.data.data.ocrServiceUrl
    }
  } catch (e) {}
}

const saveOcrConfig = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先完成管理员登录后再保存配置！')
    loginVisible.value = true
    return
  }
  savingOcr.value = true
  try {
    const token = sessionStorage.getItem('adminToken') || ''
    const res = await axios.post('/api/admin/config/ocr', 
      { ocrServiceUrl: ocrForm.ocrServiceUrl },
      { headers: { Authorization: token } }
    )
    if (res.data.code === 200) {
      ElMessage.success('OCR 服务器地址更新成功！')
    } else {
      ElMessage.error(res.data.message || '更新失败')
    }
  } catch (e) {
    ElMessage.error('更新失败：未授权或网络异常')
  } finally {
    savingOcr.value = false
  }
}


const loadNdaTemplates = async () => {
  try {
    const res = await axios.get('/api/admin/nda/templates')
    if (res.data.code === 200) {
      ndaTemplates.value = res.data.data
    }
  } catch (e) {}
}

const openPublishDialog = () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先完成管理员登录后再发布新协议！')
    loginVisible.value = true
    return
  }
  publishDialogVisible.value = true
}

const publishNewNda = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先完成管理员登录后再发布新协议！')
    loginVisible.value = true
    return
  }
  publishing.value = true
  try {
    const token = sessionStorage.getItem('adminToken') || ''
    const res = await axios.post('/api/admin/nda/publish', newNdaForm, { headers: { Authorization: token } })
    if (res.data.code === 200) {
      ElMessage.success('新版本保密协议发布成功！历史到访将要求重新签署新版本。')
      publishDialogVisible.value = false
      loadNdaTemplates()
    } else {
      ElMessage.error(res.data.message || '发布失败')
    }
  } catch (e) {
    ElMessage.error('发布网络错误')
  } finally {
    publishing.value = false
  }
}

const loadVisitors = async () => {

  try {
    const res = await axios.get('/api/admin/visitors')
    if (res.data.code === 200) {
      visitorList.value = res.data.data
    }
  } catch (e) {}
}

// 组织架构与全员档案核心数据与分页逻辑
const filterTreeText = ref('')
const deptTreeRef = ref(null)
const orgTreeData = ref([])
const orgUserList = ref([])
const selectedDeptName = ref('')
const syncing = ref(false)
const loadingUsers = ref(false)

const pagination = reactive({
  currentPage: 1,
  pageSize: 10, // 默认 10 行
  total: 0
})

const orgFilter = reactive({
  adAccount: '',
  name: '',
  deptName: '',
  status: ''
})

const filterNode = (value, data) => {
  if (!value) return true
  return data.label && data.label.includes(value)
}

watch(filterTreeText, (val) => {
  if (deptTreeRef.value) {
    deptTreeRef.value.filter(val)
  }
})

const loadOrgTree = async () => {
  try {
    const res = await axios.get('/api/admin/org/tree')
    if (res.data.code === 200 && res.data.data) {
      orgTreeData.value = res.data.data
    }
  } catch (e) {
    console.error('加载组织架构树失败', e)
  }
}

const loadOrgUsers = async () => {
  loadingUsers.value = true
  try {
    const params = {
      page: pagination.currentPage,
      pageSize: pagination.pageSize,
      adAccount: orgFilter.adAccount ? orgFilter.adAccount.trim() : undefined,
      name: orgFilter.name ? orgFilter.name.trim() : undefined,
      deptName: orgFilter.deptName ? orgFilter.deptName.trim() : undefined,
      status: orgFilter.status ? orgFilter.status.trim() : undefined
    }
    const res = await axios.get('/api/admin/org/users', { params })
    if (res.data.code === 200 && res.data.data) {
      const dataObj = res.data.data
      if (dataObj.list) {
        orgUserList.value = dataObj.list
        pagination.total = dataObj.total || 0
      } else if (Array.isArray(dataObj)) {
        orgUserList.value = dataObj
        pagination.total = dataObj.length
      }
    }
  } catch (e) {
    ElMessage.error('获取全员档案列表失败')
  } finally {
    loadingUsers.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadOrgUsers()
}

const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.currentPage = 1
  loadOrgUsers()
}

const handleCurrentChange = (val) => {
  pagination.currentPage = val
  loadOrgUsers()
}

const handleNodeClick = (nodeData) => {
  const targetPath = nodeData.fullPath || nodeData.deptName || ''
  selectedDeptName.value = targetPath
  orgFilter.deptName = targetPath
  handleSearch()
}


const resetOrgSearch = () => {
  orgFilter.adAccount = ''
  orgFilter.name = ''
  orgFilter.deptName = ''
  orgFilter.status = ''
  selectedDeptName.value = ''
  pagination.currentPage = 1
  loadOrgUsers()
}

const handleTriggerSync = async () => {
  syncing.value = true
  try {
    const res = await axios.post('/api/admin/org/sync')
    if (res.data.code === 200) {
      ElMessage.success(res.data.message || '手动同步 APHR 数据成功！')
      loadOrgTree()
      loadOrgUsers()
    } else {
      ElMessage.error(res.data.message || '同步失败')
    }
  } catch (e) {
    ElMessage.error('触发同步出现网络异常')
  } finally {
    syncing.value = false
  }
}

const refreshAllData = () => {
  loadOcrConfig()
  loadAdminDepts()
  loadAdminReasons()
  loadNdaTemplates()
  loadVisitors()
  loadOrgTree()
  loadOrgUsers()
}

const userEvents = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart']

onMounted(() => {
  checkExistingSession()
  refreshAllData()
  userEvents.forEach(evt => window.addEventListener(evt, resetIdleTimer, { passive: true }))
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  userEvents.forEach(evt => window.removeEventListener(evt, resetIdleTimer))
})



</script>

<style scoped>
.admin-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  overflow: hidden;
}

.admin-header {
  height: 56px;
  background: #001529;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.logo-title {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.logo-title .el-icon {
  margin-right: 8px;
  font-size: 22px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-badge {
  font-size: 13px;
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
  padding: 4px 14px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.expire-timer {
  color: #e6a23c;
  font-family: monospace;
  font-weight: bold;
}


.admin-main {
  padding: 16px 24px;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.admin-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}

:deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

:deep(.el-tab-pane) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

/* 组织架构分栏布局 */
.org-layout {
  display: flex;
  gap: 16px;
  align-items: stretch;
  height: 100%;
  overflow: hidden;
}

.org-tree-card {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

:deep(.org-tree-card .el-card__body) {
  flex: 1;
  overflow-y: auto;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.tree-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #303133;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;
}

.dept-label-text {
  display: flex;
  align-items: center;
  font-size: 13px;
}

.tree-badge {
  margin-left: 8px;
}

.org-user-card {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.org-user-card .el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: auto;
  position: relative;
}

.user-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.total-tag {
  font-size: 13px;
  font-weight: normal;
  color: #909399;
  margin-left: 8px;
}

/* 多条件检索栏吸顶固定 */
.search-form {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #ffffff;
  padding: 10px 16px 2px 16px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  margin-bottom: 12px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
  flex-shrink: 0;
}

.table-pagination-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.ad-account-tag {
  color: #409eff;
  font-family: monospace;
  font-weight: 500;
}

.pagination-wrapper {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
  padding-bottom: 4px;
}

/* 多级防骚扰屏蔽树样式 */
.shield-tree-wrapper {
  max-height: 60vh;
  overflow-y: auto;
  padding: 8px 12px;
}

.custom-shield-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 4px 12px;
  border-bottom: 1px dashed #f0f2f5;
}

.shield-node-label {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.shield-switch-area {
  margin-left: auto;
}
</style>

