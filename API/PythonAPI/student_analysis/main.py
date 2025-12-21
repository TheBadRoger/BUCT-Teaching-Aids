from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from student_analysis.routers import student_router, knowledge_router, report_router
from student_analysis.config.database import engine
from student_analysis.models import db_models

student_analysis = FastAPI(title="学情分析系统API")

# 新增：跨域配置
student_analysis.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 开发环境允许所有前端访问，生产环境替换为具体前端域名
    allow_credentials=True,
    allow_methods=["*"],  # 允许所有HTTP方法（GET/POST/PUT/DELETE等）
    allow_headers=["*"],  # 允许所有请求头
)
# 创建数据库表（首次运行时执行）
db_models.Base.metadata.create_all(bind=engine)



# 注册路由
student_analysis.include_router(student_router.router)
student_analysis.include_router(knowledge_router.router)
student_analysis.include_router(report_router.router)

@student_analysis.get("/")
def root():
    return {"message": "学情分析系统API运行中"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:student_analysis",
        host="0.0.0.0",
        port=9527,
        reload=True
    )