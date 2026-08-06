import { createRouter, createWebHistory } from 'vue-router'
import PortalView from '../views/PortalView.vue'
import VisitorView from '../views/VisitorView.vue'
import HostView from '../views/HostView.vue'
import SecurityView from '../views/SecurityView.vue'
import AdminView from '../views/AdminView.vue'


const routes = [
  {
    path: '/',
    name: 'Portal',
    component: PortalView,
    meta: { title: '访客系统全功能导航门户' }
  },
  {
    path: '/visitor',
    name: 'Visitor',
    component: VisitorView,
    meta: { title: '访客在线申请与通行证' }
  },

  {
    path: '/host',
    name: 'Host',
    component: HostView,
    meta: { title: '内部员工微应用与审批' }
  },
  {
    path: '/security',
    name: 'Security',
    component: SecurityView,
    meta: { title: '门岗物业保安核验放行' }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: AdminView,
    meta: { title: '系统管理后台与保密审计' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title + ' - 浙江脉通智造'
  }
  // 全局拦截：如果在任何页面链接（包括根路径、Portal等）中带有 approveToken 参数，且当前非 Host 路由，自动重定向至 Host 审批页
  const token = to.query.approveToken || new URLSearchParams(window.location.search).get('approveToken')
  if (token && to.name !== 'Host') {
    return next({ name: 'Host', query: { approveToken: token } })
  }
  next()
})


export default router
