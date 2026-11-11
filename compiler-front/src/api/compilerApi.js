import axios from 'axios'

// 创建axios实例
const request = axios.create({
  baseURL: 'http://localhost:8080/compiler', // 请确保与后端 Controller 的 @RequestMapping 对应
  timeout: 10000,
  headers: {
    'Content-Type': 'text/plain;charset=utf-8'
  }
})

// 响应拦截器：统一提取后端 Result 对象中的数据
request.interceptors.response.use(
  response => {
    // 对应后端的 Result 实体类
    return response.data
  },
  error => {
    return Promise.reject({
      success: false,
      message: '网络请求异常：' + (error.message || '请检查后端服务是否启动')
    })
  }
)

// 全流程编译接口
export const fullCompile = (sourceCode) => {
  return request.post('/full-compile', sourceCode)
}

// 单独词法分析
export const lexAnalyze = (sourceCode) => {
  return request.post('/lex', sourceCode)
}

// 单独语法分析
export const syntaxAnalyze = (sourceCode) => {
  return request.post('/syntax', sourceCode)
}

// 单独语义分析 (预留)
export const semanticAnalyze = (sourceCode) => {
  return request.post('/semantic', sourceCode)
}

// 中间代码生成
export const irAnalyze = (sourceCode) => {
  return request.post('/ir', sourceCode)
}

// 中间代码优化
export const optimizeAnalyze = (sourceCode) => {
  return request.post('/optimize', sourceCode)
}
// 中间代码优化
export const codeGenAnalyze = (sourceCode) => {
  return request.post('/codeGen', sourceCode)
}