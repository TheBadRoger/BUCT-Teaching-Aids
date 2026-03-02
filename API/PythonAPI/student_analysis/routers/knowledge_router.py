from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from student_analysis.services import knowledge_service
from student_analysis.models.data_models import ClassKnowledgeListResponse
from student_analysis.dependencies import get_db

router = APIRouter(
    prefix="/api/knowledge",
    tags=["知识点观测"]
)

@router.get("/class_list", response_model=ClassKnowledgeListResponse)
def get_class_knowledge_list(
    class_name: str = Query(..., description="班级名称，如'高材2304'"),
    db: Session = Depends(get_db)
):
    """获取班级知识点列表（支撑知识点观测界面）"""
    return knowledge_service.get_class_knowledge_list(db, class_name)