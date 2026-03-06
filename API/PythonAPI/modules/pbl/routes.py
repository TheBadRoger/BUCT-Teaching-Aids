"""PBL (Project-Based Learning) blueprint."""
import os

from flask import Blueprint, request, jsonify

from config import Config

pbl_bp = Blueprint('pbl', __name__, url_prefix='/api/pbl')

# PBL模板库 – 默认数学模板（可根据需要扩充）
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


@pbl_bp.route('/', methods=['GET'])
def pbl_index():
    """PBL模块欢迎页"""
    return jsonify({
        "module": "PBL生成器",
        "description": "项目式学习设计生成系统",
        "docs": "/api/pbl/templates"
    })


@pbl_bp.route('/health', methods=['GET'])
def pbl_health():
    return jsonify({"status": "PBL模块正常运行"})


@pbl_bp.route('/generate', methods=['POST'])
def generate_pbl():
    """
    生成PBL设计方案。

    表单字段：
    - grade_subject: 年级学科（如：五年级数学）
    - textbook_version: 教材版本（如：人教版）
    - unit: 单元信息（如：五年级上册第一单元）
    - requirements: 其他要求（可选）
    - content: 课文内容（可选）
    - file: 上传的课文文件（可选 .txt）
    """
    try:
        grade_subject = request.form.get('grade_subject', '')
        textbook_version = request.form.get('textbook_version', '')
        unit = request.form.get('unit', '')
        requirements = request.form.get('requirements')
        content = request.form.get('content')

        # 处理上传文件
        uploaded_file = request.files.get('file')
        if uploaded_file and uploaded_file.filename:
            os.makedirs(Config.UPLOAD_DIR, exist_ok=True)
            file_path = os.path.join(Config.UPLOAD_DIR, uploaded_file.filename)
            uploaded_file.save(file_path)
            if uploaded_file.filename.endswith('.txt') and not content:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()

        # 使用默认数学模板（可根据学科扩充）
        template = PBL_TEMPLATES.get("数学", PBL_TEMPLATES["数学"])
        pbl_content = template.format(
            年级学科=grade_subject,
            教材版本=textbook_version,
            单元=unit
        )

        if requirements:
            pbl_content += f"\n\n## 额外要求\n{requirements}"
        if content:
            pbl_content += f"\n\n## 参考课文\n{content[:500]}..."

        preview_content = (
            f"# {grade_subject} PBL预览\n\n"
            f"**单元：**{unit}\n**版本：**{textbook_version}\n\n"
            f"{pbl_content[:200]}..."
        )
        safe_name = grade_subject.replace(' ', '_')

        return jsonify({
            "success": True,
            "message": "PBL生成成功！",
            "pbl_content": pbl_content,
            "preview_content": preview_content,
            "download_link": f"/api/pbl/download/{safe_name}"
        })

    except Exception as e:
        return jsonify({"success": False, "message": f"生成失败：{e}"}), 500


@pbl_bp.route('/download/<filename>', methods=['GET'])
def download_pbl(filename):
    """下载生成的PBL文件（占位接口）"""
    return jsonify({
        "filename": filename.replace('_', ' '),
        "message": "请在界面中查看完整内容。"
    })


@pbl_bp.route('/templates', methods=['GET'])
def get_templates():
    """获取可用的PBL模板列表"""
    return jsonify({
        "success": True,
        "templates": list(PBL_TEMPLATES.keys()),
        "description": "当前支持的教学科目模板"
    })
