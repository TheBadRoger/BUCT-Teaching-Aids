from fastapi import FastAPI, File, UploadFile, Form, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, JSONResponse
import os
from datetime import datetime
from typing import Optional, Dict, Any
# 从外部database.py导入所有数据库相关依赖
from database import engine, Base, SessionLocal, get_db, PBLRecord
from sqlalchemy.orm import Session

# -------------------------- 本地PBL模板生成逻辑 --------------------------
def generate_pbl_from_template(form_data: Dict[str, Any]) -> str:
    """使用本地专业模板生成PBL内容（适配任意学科）"""
    grade_subject = form_data.get("grade_subject")
    unit = form_data.get("unit")
    textbook_version = form_data.get("textbook_version")
    difficulty = form_data.get("difficulty", "综合型")
    teaching_hours = form_data.get("teachingHours", "8")
    content = form_data.get("content", "")
    requirements = form_data.get("requirements", "")
    
    # 学科适配的核心驱动问题（本地模板）
    subject_driving_questions = {
        "高等数学": f"如何运用{unit}的极限思维解决工程建模中的实际问题？",
        "线性代数": f"如何利用矩阵运算和向量空间理论解决{unit}中的数据分析问题？",
        "程序设计基础": f"如何通过{unit}的编程知识设计并实现一个解决实际问题的小型应用？",
        "数据结构": f"如何选择和优化{unit}中的数据结构以提升算法性能？",
        "计算机组成原理": f"如何理解{unit}中的硬件原理并设计简单的计算机系统模块？",
        "default": f"如何将{unit}的核心知识点应用于解决实际问题？"
    }
    
    driving_question = subject_driving_questions.get(grade_subject, subject_driving_questions["default"])
    
    # 生成专业级PBL方案（纯本地）
    pbl_content = f"""# {grade_subject} - {unit} 专业级PBL设计方案
## 一、项目背景
基于{textbook_version}教材{unit}单元的教学要求，结合以下核心内容设计本PBL项目：
{content[:500]}...

## 二、核心驱动问题
【{difficulty}难度】{driving_question}

## 三、学习目标
### 1. 知识目标
- 掌握{unit}的核心知识点和基本概念
- 理解{grade_subject}相关理论的应用场景和价值
- 熟悉{unit}知识点的推导过程和应用方法

### 2. 能力目标
- {"逻辑推理、数学建模能力" if "数学" in grade_subject else "编程实现、问题解决能力" if "程序" in grade_subject else "自主探究、合作学习能力"}
- 团队协作与沟通表达能力
- 问题分析与解决方案设计能力

### 3. 素养目标
- {"数学抽象、逻辑推理素养" if "数学" in grade_subject else "计算思维、工程实践素养" if "计算机" in grade_subject else "创新思维、实践应用素养"}
- 批判性思维与创新意识
- 学科核心素养与跨学科应用能力

## 四、核心任务设计（总计{teaching_hours}课时）
"""
    
    # 按难度和课时拆解任务
    hours = int(teaching_hours) if teaching_hours.isdigit() else 8
    
    if difficulty == "基础型":
        pbl_content += f"""
### 第1-2课时：知识预习与问题导入
- 活动1：教师讲解{unit}核心知识点，学生完成预习任务
- 活动2：发布核心驱动问题，学生分组（2-3人/组）
- 产出物：预习笔记、分组方案

### 第3-{hours-2}课时：基础探究与实践
- 活动1：小组查阅资料，完成基础练习题
- 活动2：教师指导，解决基础应用问题
- 产出物：基础探究报告、练习题解答

### 第{hours-1}课时：成果整理与准备
- 活动1：小组整理探究成果，准备展示材料
- 活动2：教师点评，提出改进建议
- 产出物：成果初稿、展示PPT框架

### 第{hours}课时：成果展示与评价
- 活动1：小组展示（5分钟/组），其他小组提问
- 活动2：教师总结点评，完成评价表
- 产出物：最终成果报告、评价表
"""
    elif difficulty == "综合型":
        pbl_content += f"""
### 第1课时：项目启动与分组
- 活动1：{unit}知识点回顾与拓展讲解
- 活动2：核心驱动问题解析，学生分组（3-4人/组）
- 产出物：项目计划书、任务分工表

### 第2-{hours-3}课时：深度探究与实践
- 活动1：小组自主探究，解决核心驱动问题
- 活动2：中期汇报，教师指导解决难点问题
- 产出物：探究过程记录、中期成果报告

### 第{hours-2}课时：方案优化与完善
- 活动1：小组优化解决方案，完善成果
- 活动2：跨组交流，互相提出改进建议
- 产出物：优化后的解决方案、成果草稿

### 第{hours-1}课时：成果展示准备
- 活动1：小组准备展示材料，模拟展示
- 活动2：教师指导展示技巧和评价标准
- 产出物：展示PPT、成果报告终稿

### 第{hours}课时：成果展示与综合评价
- 活动1：小组正式展示（8分钟/组）+ 问答环节
- 活动2：学生互评 + 教师综合评价
- 产出物：评价表、反思总结
"""
    else:  # 创新型
        pbl_content += f"""
### 第1课时：项目构思与设计
- 活动1：{unit}知识点创新应用案例分享
- 活动2：学生自主设计PBL子项目，分组（4-5人/组）
- 产出物：项目设计方案、创新点说明

### 第2-{hours-3}课时：创新实践与开发
- 活动1：小组自主开展项目研究与开发
- 活动2：定期小组讨论，教师个性化指导
- 产出物：项目开发文档、中期成果原型

### 第{hours-2}课时：跨学科融合与优化
- 活动1：结合其他学科知识优化项目成果
- 活动2：邀请行业专家/学长点评指导
- 产出物：优化方案、改进记录

### 第{hours-1}课时：成果完善与展示设计
- 活动1：完善项目成果，设计创新展示形式
- 活动2：排练展示内容，准备答辩
- 产出物：最终成果、展示方案

### 第{hours}课时：创新成果展示与答辩
- 活动1：创新成果展示（10分钟/组）+ 答辩
- 活动2：综合评价与创新点表彰
- 产出物：创新成果报告、评价表、反思日志
"""
    
    # 补充完整模板内容
    pbl_content += f"""
## 五、实施流程
### 1. 课前准备
- 教师：准备{unit}相关教学资源、评价表、任务单
- 学生：预习{unit}核心知识点，组建学习小组

### 2. 课中实施
- 问题导入 → 分组探究 → 任务实施 → 中期反馈 → 成果完善 → 展示评价

### 3. 课后拓展
- 学生：完成反思总结，拓展学习相关知识点
- 教师：整理学生成果，优化后续PBL设计

## 六、评价体系
### 1. 过程性评价（60%）
| 评价维度 | 权重 | 评价标准 |
|---------|------|----------|
| 参与度 | 20% | 积极参与小组讨论，完成分配任务 |
| 探究过程 | 25% | 探究方法科学，记录完整，思路清晰 |
| 团队协作 | 15% | 有效沟通，分工合理，互相帮助 |

### 2. 终结性评价（40%）
| 评价维度 | 权重 | 评价标准 |
|---------|------|----------|
| 成果质量 | 25% | 内容完整，方法正确，有应用价值 |
| 展示表达 | 10% | 逻辑清晰，表达流畅，重点突出 |
| 创新点 | 5% | 有独立思考，提出创新性观点或方法 |

## 七、成果展示形式
{"数学建模报告、解题方案展示" if "数学" in grade_subject else "程序作品、项目演示" if "程序" in grade_subject else "研究报告、PPT展示"}

## 八、教学反思
### 1. 预设问题
- 学生可能在{unit}核心知识点应用上存在困难
- 小组协作可能出现分工不均的情况
- 探究时间可能不足，需要合理把控

### 2. 解决方案
- 提供分层指导，设置阶梯式任务
- 制定小组协作评价标准，确保全员参与
- 预留弹性时间，重点难点提前讲解

## 九、额外要求
{requirements if requirements else "无特殊要求"}

---
生成时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
PBL难度级别：{difficulty}
计划课时：{teaching_hours}课时
"""
    
    return pbl_content

# -------------------------- FastAPI应用配置 --------------------------
app = FastAPI(title="PBL生成器", description="项目式学习设计生成系统（纯本地版）")

# 允许跨域请求（保留原有配置）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境请替换为具体域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 确保上传目录存在
UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

# -------------------------- 接口定义 --------------------------
@app.get("/health")
def check_health(db: Session = Depends(get_db)):
    """健康检查接口（新增数据库连接检查）"""
    try:
        # 测试数据库连接
        db.execute("SELECT 1")
        return {
            "status": "后端接口正常运行", 
            "database_status": "连接成功",
            "timestamp": datetime.now().isoformat()
        }
    except Exception as e:
        return {
            "status": "后端接口正常运行",
            "database_status": f"连接失败: {str(e)}",
            "timestamp": datetime.now().isoformat()
        }

@app.get("/", response_class=HTMLResponse)
async def read_root():
    """首页"""
    return """
    <html>
        <head>
            <title>PBL生成器API</title>
        </head>
        <body>
            <h1>PBL项目式学习生成器</h1>
            <p>后端服务已启动！（纯本地模板版，无大模型调用）</p>
            <p>API文档：<a href="/docs">/docs</a></p>
            <p>健康检查：<a href="/health">/health</a></p>
        </body>
    </html>
    """

@app.post("/generate-pbl/")
async def generate_pbl(
    grade_subject: str = Form(...),
    textbook_version: str = Form(...),
    unit: str = Form(...),
    difficulty: str = Form("综合型"),
    teachingHours: str = Form("8"),
    requirements: Optional[str] = Form(None),
    content: Optional[str] = Form(None),
    file: Optional[UploadFile] = File(None),
    preview: bool = Form(False),
    db: Session = Depends(get_db)  # 依赖注入获取数据库会话
):
    """
    生成PBL设计方案（纯本地模板，无大模型调用）
    """
    try:
        # 1. 处理文件上传（保留原有逻辑）
        if file and file.filename:
            file_path = os.path.join(UPLOAD_DIR, file.filename)
            with open(file_path, "wb") as f:
                content_bytes = await file.read()
                f.write(content_bytes)
            
            # 读取文本文件内容
            if file.filename.endswith('.txt'):
                with open(file_path, "r", encoding="utf-8") as f:
                    uploaded_content = f.read()
                    if not content:
                        content = uploaded_content

        # 2. 构建请求数据
        form_data = {
            "grade_subject": grade_subject,
            "textbook_version": textbook_version,
            "unit": unit,
            "difficulty": difficulty,
            "teachingHours": teachingHours,
            "requirements": requirements,
            "content": content
        }

        # 3. 使用本地模板生成PBL内容
        pbl_content = generate_pbl_from_template(form_data)

        # 4. 生成预览内容
        preview_content = f"""# {grade_subject} - {unit} PBL预览
**教材版本：** {textbook_version}
**难度级别：** {difficulty}
**计划课时：** {teachingHours}课时

## 核心框架
- 项目背景：基于{unit}单元核心内容设计
- 驱动问题：适配{difficulty}难度的开放性探究问题
- 课时安排：分{teachingHours}课时完成探究、实践、展示全流程
- 评价体系：过程性评价（60%）+ 终结性评价（40%）

## 自定义需求
{requirements[:100]}...""" if requirements else "无特殊自定义需求"

        # 5. 保存到MySQL数据库（非预览模式）
        record_id = None
        if not preview:
            # 使用依赖注入的db会话（无需手动创建/关闭）
            record = PBLRecord(
                grade_subject=grade_subject,
                textbook_version=textbook_version,
                unit=unit,
                difficulty=difficulty,
                teachingHours=teachingHours,
                requirements=requirements,
                content=content,
                generated_content=pbl_content
            )
            db.add(record)
            db.commit()
            db.refresh(record)
            record_id = record.id

        # 6. 返回结果（保持原有响应格式）
        return JSONResponse(content={
            "success": True,
            "message": "PBL生成成功！（本地模板）",
            "pbl_content": pbl_content,
            "preview_content": preview_content,
            "record_id": record_id,
            "download_link": f"/download-pbl/{grade_subject.replace(' ', '_')}_{record_id if record_id else 'preview'}"
        })

    except Exception as e:
        print(f"生成PBL失败: {str(e)}")
        return JSONResponse(content={
            "success": False,
            "message": f"生成失败：{str(e)}",
            "error_type": type(e).__name__
        }, status_code=500)

# -------------------------- 历史记录接口 --------------------------
@app.get("/history/")
async def query_history(
    subject: Optional[str] = None,
    textbook_version: Optional[str] = None,
    limit: int = 20,
    offset: int = 0,
    db: Session = Depends(get_db)
):
    """查询历史记录"""
    try:
        query = db.query(PBLRecord)
        if subject:
            query = query.filter(PBLRecord.grade_subject.like(f"%{subject}%"))
        if textbook_version:
            query = query.filter(PBLRecord.textbook_version.like(f"%{textbook_version}%"))
        
        total = query.count()
        records = query.order_by(PBLRecord.created_at.desc()).offset(offset).limit(limit).all()
        
        result = []
        for r in records:
            result.append({
                "id": r.id,
                "grade_subject": r.grade_subject,
                "textbook_version": r.textbook_version,
                "unit": r.unit,
                "difficulty": r.difficulty,
                "teachingHours": r.teachingHours,
                "created_at": r.created_at.isoformat()
            })
        
        return JSONResponse(content={
            "success": True,
            "records": result,
            "total": total
        })
    except Exception as e:
        return JSONResponse(content={
            "success": False,
            "message": f"查询失败：{str(e)}"
        }, status_code=500)

@app.get("/history/{record_id}")
async def get_history(record_id: int, db: Session = Depends(get_db)):
    """获取单条历史记录"""
    try:
        record = db.query(PBLRecord).filter(PBLRecord.id == record_id).first()
        if not record:
            return JSONResponse(content={
                "success": False,
                "message": "记录未找到"
            }, status_code=404)
        
        return JSONResponse(content={
            "success": True,
            "record": {
                "id": record.id,
                "grade_subject": record.grade_subject,
                "textbook_version": record.textbook_version,
                "unit": record.unit,
                "difficulty": record.difficulty,
                "teachingHours": record.teachingHours,
                "requirements": record.requirements,
                "content": record.content,
                "generated_content": record.generated_content,
                "created_at": record.created_at.isoformat()
            }
        })
    except Exception as e:
        return JSONResponse(content={
            "success": False,
            "message": f"获取记录失败：{str(e)}"
        }, status_code=500)

@app.delete("/history/{record_id}")
async def delete_history(record_id: int, db: Session = Depends(get_db)):
    """删除单条记录"""
    try:
        record = db.query(PBLRecord).filter(PBLRecord.id == record_id).first()
        if not record:
            return JSONResponse(content={
                "success": False,
                "message": "记录未找到"
            }, status_code=404)
        
        db.delete(record)
        db.commit()
        return JSONResponse(content={
            "success": True,
            "message": "记录已删除"
        })
    except Exception as e:
        return JSONResponse(content={
            "success": False,
            "message": f"删除记录失败：{str(e)}"
        }, status_code=500)

@app.delete("/history/")
async def delete_all_history(confirm: bool = False, db: Session = Depends(get_db)):
    """删除所有记录"""
    if not confirm:
        return JSONResponse(content={
            "success": False,
            "message": "请传入 confirm=true 确认删除"
        }, status_code=400)
    
    try:
        deleted = db.query(PBLRecord).delete()
        db.commit()
        return JSONResponse(content={
            "success": True,
            "deleted": deleted,
            "message": f"成功删除 {deleted} 条记录"
        })
    except Exception as e:
        return JSONResponse(content={
            "success": False,
            "message": f"删除所有记录失败：{str(e)}"
        }, status_code=500)

@app.post("/history/")
async def create_history(
    grade_subject: str = Form(...),
    textbook_version: str = Form(...),
    unit: str = Form(...),
    difficulty: str = Form("综合型"),
    teachingHours: str = Form("8"),
    requirements: Optional[str] = Form(None),
    content: Optional[str] = Form(None),
    generated_content: str = Form(...),
    db: Session = Depends(get_db)
):
    """手动保存记录"""
    try:
        record = PBLRecord(
            grade_subject=grade_subject,
            textbook_version=textbook_version,
            unit=unit,
            difficulty=difficulty,
            teachingHours=teachingHours,
            requirements=requirements,
            content=content,
            generated_content=generated_content
        )
        db.add(record)
        db.commit()
        db.refresh(record)
        
        return JSONResponse(content={
            "success": True,
            "record_id": record.id
        })
    except Exception as e:
        return JSONResponse(content={
            "success": False,
            "message": f"保存记录失败：{str(e)}"
        }, status_code=500)

# -------------------------- 下载接口 --------------------------
@app.get("/download-pbl/{filename}")
async def download_pbl(filename: str):
    """下载PBL文件"""
    return JSONResponse(content={
        "success": True,
        "message": "下载接口已就绪",
        "filename": filename,
        "content_type": "text/markdown"
    })

# -------------------------- 启动配置 --------------------------
if __name__ == "__main__":
    import uvicorn
    # 安装依赖：pip install fastapi uvicorn sqlalchemy pymysql
    uvicorn.run(
        app="main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    )