from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from student_analysis.services import student_service
from student_analysis.models.data_models import ClassStudentListResponse
from student_analysis.dependencies import get_db  # 假设依赖中定义了数据库Session获取方法

router = APIRouter(
    prefix="/api/student",
    tags=["学生观测"]
)

@router.get("/class_list", response_model=ClassStudentListResponse)
def get_class_student_list(
    class_name: str = Query(..., description="班级名称，如'高材2304'"),
    db: Session = Depends(get_db)
):
    """获取班级学生列表（支撑学生观测界面）"""
    return student_service.get_class_student_list(db, class_name)