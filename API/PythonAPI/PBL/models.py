from pydantic import BaseModel
from typing import Optional

# 定义PBL表单数据模型
class PBLRequest(BaseModel):
    """PBL生成请求数据模型"""
    year: Optional[str] = None  # 大学年级（如：大一、大二）
    grade_subject: str  # 年级学科或课程名称
    textbook_version: str  # 教材版本
    unit: str  # 层次单元
    requirements: Optional[str] = None  # 其他要求
    content: Optional[str] = None  # 课文或教材内容
    
class PBLResponse(BaseModel):
    """PBL生成响应数据模型"""
    success: bool
    message: str
    pbl_content: Optional[str] = None
    preview_content: Optional[str] = None
    record_id: Optional[int] = None  # 如果保存到数据库，返回记录id

class HistoryRecord(BaseModel):
    id: int
    year: Optional[str]
    grade_subject: str
    textbook_version: str
    unit: str
    requirements: Optional[str] = None
    content: Optional[str] = None
    generated_content: str
    created_at: str

class HistoryListResponse(BaseModel):
    success: bool
    records: list[HistoryRecord] = []
    total: Optional[int] = 0