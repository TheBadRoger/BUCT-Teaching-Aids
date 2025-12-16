from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from student_analysis.services import report_service
from student_analysis.models.data_models import ReportResponse
from student_analysis.dependencies import get_db

router = APIRouter(prefix="/api/report", tags=["学情报告页"])

@router.get("/student", response_model=ReportResponse)
def get_student_report(student_id: int, db: Session = Depends(get_db)):
    try:
        return report_service.get_report_data(db, student_id)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))