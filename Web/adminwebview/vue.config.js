const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  //处理跨域问题
  lintOnSave: false, // 关闭语法检测
  // 开启代理服务器
  devServer: {
    //前端端口
    port: 8080,
    // 代理服务器可以将路由中的指定前缀转发到指定的后端服务器中
    proxy: {
      '/api': {
        target: 'http://localhost:8081/api',//8081是后端开放的端口。如后端端口改变，需要在这里同步。
        ws: true, // 是否启用websockets
        changeOrigin: true,  // 代理时是否更改host
        pathRewrite: {
          '^/api': '' //这里理解成用'/api'代替target里面的地址
        }
      }
    }
  }
})