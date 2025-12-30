from pydantic import BaseModel
from typing import Optional

# 定义PBL表单数据模型
class PBLRequest(BaseModel):
    """PBL生成请求数据模型"""
    grade_subject: str  # 年级学科
    textbook_version: str  # 教材版本
    unit: str  # 层次单元
    requirements: Optional[str] = None  # 其他要求
    content: Optional[str] = None  # 课文内容
    
class PBLResponse(BaseModel):
    """PBL生成响应数据模型"""
    success: bool
    message: str
    pbl_content: Optional[str] = None
    preview_content: Optional[str] = None