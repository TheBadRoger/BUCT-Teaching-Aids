from fastapi import FastAPI, File, UploadFile, Form
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
import os
from models import PBLRequest, PBLResponse


# 临时测试接口
# 创建FastAPI应用
app = FastAPI(title="PBL生成器", description="项目式学习设计生成系统")
@app.get("/health")
def check_health():
    return {"status": "后端接口正常运行"}

# 允许跨域请求（让前端能访问后端）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 开发时可设为所有来源
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 确保上传目录存在
UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

# 存储PBL模板的字典（实际应用中可以存储在数据库中）
PBL_TEMPLATES = {
    "数学": """# 数学PBL项目设计方案

## 项目名称
生活中的{年级学科}应用探索

## 核心问题
如何运用数学知识解决生活中的实际问题？

## 项目目标
1. 理解并掌握{教材版本}中的核心概念
2. 运用数学思维解决实际问题
3. 培养团队协作能力

## 项目任务
**任务一：问题发现**
- 观察生活中与{单元}相关的现象
- 记录至少3个实际问题

**任务二：方案设计**
- 小组讨论解决方案
- 设计数据收集表格
- 制定实施计划

**任务三：实践探究**
- 收集真实数据
- 应用数学公式计算
- 验证解决方案的有效性

## 评价标准
| 评价维度 | 优秀 | 良好 | 待改进 |
|---------|------|------|--------|
| 知识应用 | 灵活运用多种方法 | 正确应用所学知识 | 需要指导才能应用 |
| 团队协作 | 积极合作，分工明确 | 能完成分配任务 | 参与度不足 |
| 成果展示 | 逻辑清晰，表达生动 | 内容完整，表达清楚 | 内容不完整 |

## 教学建议
1. 提供真实的生活场景案例
2. 引导学生发现数学与生活的联系
3. 鼓励创新思维和多种解决方案
"""
}

@app.get("/", response_class=HTMLResponse)
async def read_root():
    """返回欢迎页面"""
    return """
    <html>
        <head>
            <title>PBL生成器API</title>
        </head>
        <body>
            <h1>PBL项目式学习生成器</h1>
            <p>后端服务已启动！</p >
            <p>请访问 /docs 查看API文档</p >
            <p>前端页面：/index.html</p >
        </body>
    </html>
    """

@app.post("/generate-pbl/")
async def generate_pbl(
    grade_subject: str = Form(...),
    textbook_version: str = Form(...),
    unit: str = Form(...),
    requirements: str = Form(None),
    content: str = Form(None),
    file: UploadFile = File(None)
):
    """
    生成PBL设计方案
    参数说明：
    - grade_subject: 年级学科（如：五年级数学）
    - textbook_version: 教材版本（如：人教版）
    - unit: 单元信息（如：五年级上册第一单元）
    - requirements: 其他要求
    - content: 课文内容
    - file: 上传的课文文件
    """
    
    try:
        # 如果上传了文件，读取文件内容
        if file and file.filename:
            file_path = os.path.join(UPLOAD_DIR, file.filename)
            with open(file_path, "wb") as f:
                content_bytes = await file.read()
                f.write(content_bytes)
            
            # 如果是文本文件，可以读取内容
            if file.filename.endswith('.txt'):
                with open(file_path, "r", encoding="utf-8") as f:
                    uploaded_content = f.read()
                    if not content:  # 如果没有直接输入内容，使用文件内容
                        content = uploaded_content
        
        # 根据学科选择模板
        subject = grade_subject.split(" ")[-1] if " " in grade_subject else grade_subject
        
        # 生成PBL内容
        template = PBL_TEMPLATES.get("数学", PBL_TEMPLATES["数学"])  # 默认使用数学模板
        
        pbl_content = template.format(
            年级学科=grade_subject,
            教材版本=textbook_version,
            单元=unit
        )
        
        # 添加额外要求
        if requirements:
            pbl_content += f"\n\n## 额外要求\n{requirements}"
        
        # 添加课文内容
        if content:
            pbl_content += f"\n\n## 参考课文\n{content[:500]}..."  # 限制长度
        
        # 生成预览内容（简略版）
        preview_content = f"# {grade_subject} PBL预览\n\n**单元：**{unit}\n**版本：**{textbook_version}\n\n{pbl_content[:200]}..."
        
        return JSONResponse(content={
            "success": True,
            "message": "PBL生成成功！",
            "pbl_content": pbl_content,
            "preview_content": preview_content,
            "download_link": f"/download-pbl/{grade_subject.replace(' ', '_')}"
        })
        
    except Exception as e:
        return JSONResponse(content={
            "success": False,
            "message": f"生成失败：{str(e)}"
        }, status_code=500)

@app.get("/download-pbl/{filename}")
async def download_pbl(filename: str):
    """下载生成的PBL文件"""
    # 这里可以返回生成的PBL文件
    content = f"# PBL设计方案 - {filename.replace('_', ' ')}\n\n请在界面中查看完整内容。"
    return HTMLResponse(content=content)

@app.get("/get-templates/")
async def get_templates():
    """获取可用的PBL模板"""
    return {
        "success": True,
        "templates": list(PBL_TEMPLATES.keys()),
        "description": "当前支持的教学科目模板"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
