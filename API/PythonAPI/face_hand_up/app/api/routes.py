from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from datetime import datetime
from app.core.face_recog import load_face_database, recognize_face
from app.core.hand_detect import detect_hand_up, calculate_hand_up_rate
from app.db.crud import create_hand_up_record, get_latest_hand_up_records
from app.db.model import get_db
from config import init_camera
import cv2

# 创建路由实例（相当于Flask的Blueprint）
router = APIRouter(prefix="/api", tags=["人脸识别举手率统计"])

# 初始化摄像头（全局唯一，避免重复打开）
try:
    cap = init_camera()
except Exception as e:
    cap = None
    print(f"摄像头初始化失败：{e}")

# 接口1：重新加载人脸库（新增/删除人脸照片后调用）
@router.get("/reload_face_db", summary="重新加载人脸库")
def reload_face_database():
    from app.core.face_recog import FACE_ENCODINGS_CACHE
    FACE_ENCODINGS_CACHE.clear()  # 清空缓存
    load_face_database()
    return {"code": 200, "msg": "人脸库重新加载成功"}

# 接口2：获取实时举手率（核心接口，返回并记录到数据库）
@router.get("/real_time_hand_up", summary="获取实时举手率（自动记录到数据库）")
def real_time_hand_up(class_name: str = Query("默认班级", description="班级名称"), db: Session = Depends(get_db)):
    if cap is None:
        return {"code": 500, "msg": "摄像头未初始化，请检查摄像头连接", "data": None}
    # 读取一帧画面
    ret, frame = cap.read()
    if not ret:
        return {"code": 500, "msg": "无法读取摄像头画面", "data": None}
    # 1. 人脸识别
    recognized_faces = recognize_face(frame)
    # 2. 举手检测
    hand_up_students, hand_up_locations = detect_hand_up(frame, recognized_faces)
    # 3. 计算举手率
    hand_up_rate, hand_up_num, valid_num = calculate_hand_up_rate(recognized_faces, hand_up_students)
    # 4. 记录到数据库
    if valid_num > 0:  # 有有效学生时才记录
        create_hand_up_record(db, valid_num, hand_up_num, hand_up_rate, class_name)
    # 构造返回数据
    data = {
        "record_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "class_name": class_name,
        "total_student": valid_num,  # 有效学生数
        "hand_up_student": hand_up_num,  # 举手人数
        "hand_up_rate": hand_up_rate,  # 举手率
        "hand_up_students": hand_up_students,  # 举手学生列表
        "recognized_students": [f[4] for f in recognized_faces if f[4] != "未知人员"]  # 已识别学生列表
    }
    return {"code": 200, "msg": "获取实时举手率成功", "data": data}

# 接口3：获取历史举手率记录
@router.get("/history_hand_up", summary="获取历史举手率记录")
def history_hand_up(
    class_name: str = Query("默认班级", description="班级名称"),
    limit: int = Query(10, description="获取最新的N条记录", ge=1, le=100),
    db: Session = Depends(get_db)
):
    records = get_latest_hand_up_records(db, limit=limit, class_name=class_name)
    # 转换为字典（方便前端解析）
    data = [
        {
            "id": r.id,
            "record_time": r.record_time.strftime("%Y-%m-%d %H:%M:%S"),
            "class_name": r.class_name,
            "total_student": r.total_student,
            "hand_up_student": r.hand_up_student,
            "hand_up_rate": r.hand_up_rate
        }
        for r in records
    ]
    return {"code": 200, "msg": "获取历史记录成功", "data": data}

# 接口4：摄像头画面流（可选，用于前端实时查看画面，返回MJPEG流）
@router.get("/video_feed", summary="摄像头实时画面流（带人脸和举手标记）")
async def video_feed():
    from fastapi.responses import StreamingResponse
    from app.core.face_recog import draw_face_box
    from app.core.hand_detect import draw_hand_up_box
    import io
    from PIL import Image

    def generate_frames():
        while True:
            if cap is None:
                break
            ret, frame = cap.read()
            if not ret:
                break
            # 人脸识别+绘制人脸框
            recognized_faces = recognize_face(frame)
            frame = draw_face_box(frame, recognized_faces)
            # 举手检测+绘制举手框
            hand_up_students, hand_up_locations = detect_hand_up(frame, recognized_faces)
            frame = draw_hand_up_box(frame, hand_up_locations)
            # 转换为JPEG格式，生成流
            img = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
            buf = io.BytesIO()
            img.save(buf, format="JPEG")
            buf.seek(0)
            yield (b'--frame\r\n'
                   b'Content-Type: image/jpeg\r\n\r\n' + buf.getvalue() + b'\r\n')
    return StreamingResponse(generate_frames(), media_type="multipart/x-mixed-replace; boundary=frame")