from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

# --------------------------
# 学生观测界面相关模型
# --------------------------
class StudentListItem(BaseModel):
    """学生列表项（对应学生观测界面的一行数据）"""
    student_name: str
    student_id: int
    learning_progress: float  # 学习进度（百分比）
    mastery_rate: float       # 知识点掌握度（百分比）
    data_study_duration: int  # 资料学习时长（分钟）
    practice_duration: int    # 练习时长（分钟）
    practice_count: int       # 练习次数

class ClassStudentListResponse(BaseModel):
    """班级学生列表响应（支撑学生观测界面）"""
    class_name: str
    student_list: List[StudentListItem]

# --------------------------
# 知识点观测界面相关模型
# --------------------------
class KnowledgeListItem(BaseModel):
    """知识点列表项（对应知识点观测界面的一行数据）"""
    knowledge_name: str
    avg_progress: float       # 平均学习进度（百分比）
    avg_mastery: float        # 平均掌握度（百分比）
    learned_count: int        # 已学人数
    total_count: int          # 应学人数（班级总人数）

class ClassKnowledgeListResponse(BaseModel):
    """班级知识点列表响应（支撑知识点观测界面）"""
    class_name: str
    knowledge_list: List[KnowledgeListItem]

# --------------------------
# 保留原学情报告模型（如果已有）
# --------------------------
class ReportMasteryItem(BaseModel):
    knowledge_name: str
    mastery_rate: float
    level: str
    weak_points: List[str]

class ReportResponse(BaseModel):
    student_name: str
    student_id: int
    class_name: str
    report_time: str
    knowledge_mastery: List[ReportMasteryItem]