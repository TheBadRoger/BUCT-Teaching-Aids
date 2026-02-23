from fastapi import FastAPI
from app.api.routes import router as api_router
import uvicorn

# 创建FastAPI实例
app = FastAPI(
    title="人脸识别举手率统计系统",
    description="基于Python+FastAPI+OpenCV的人脸识别举手率统计后端",
    version="1.0.0"
)

# 注册路由
app.include_router(api_router)

# 根路径测试
@app.get("/", summary="项目根路径")
def root():
    return {"code": 200, "msg": "人脸识别举手率统计系统后端运行中", "docs_url": "/docs"}

# 主函数：启动服务
if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",  # 允许所有IP访问（局域网/外网）
        port=9527,       # 服务端口
        reload=True,     # 开发模式：代码修改自动重启
        reload_dirs = ["."]  # 【新增】指定重载目录为项目根，解决静态资源加载路径问题
    )