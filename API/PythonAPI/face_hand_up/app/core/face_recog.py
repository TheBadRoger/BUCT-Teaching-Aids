import os
import face_recognition
import cv2
from config import FACE_DB_PATH, TOLERANCE, FACE_ENCODINGS_CACHE

# 加载人脸库：将人脸照片转换为编码，缓存起来（避免重复加载）
def load_face_database():
    if FACE_ENCODINGS_CACHE:  # 已有缓存，直接返回
        return FACE_ENCODINGS_CACHE
    face_encodings = {}  # 键：学生ID_姓名，值：人脸编码
    # 遍历人脸库所有文件
    for filename in os.listdir(FACE_DB_PATH):
        if filename.endswith((".jpg", ".jpeg", ".png")):
            # 提取学生ID和姓名（文件名格式：ID_姓名.jpg）
            name_id = os.path.splitext(filename)[0]
            # 加载图片并生成人脸编码
            img_path = os.path.join(FACE_DB_PATH, filename)
            image = face_recognition.load_image_file(img_path)
            # 生成编码（只取第一张人脸，确保照片中只有一个人）
            encodings = face_recognition.face_encodings(image)
            if encodings:
                face_encodings[name_id] = encodings[0]
                FACE_ENCODINGS_CACHE[name_id] = encodings[0]
            else:
                print(f"警告：{filename} 中未检测到人脸，已跳过")
    if not face_encodings:
        raise Exception("人脸库为空，请在face_database中放入人脸照片")
    print(f"人脸库加载完成，共加载 {len(face_encodings)} 个人脸")
    return face_encodings

# 单帧画面人脸识别：输入OpenCV帧，返回「人脸位置+匹配的学生信息」
def recognize_face(frame):
    # 1. 转换颜色空间：OpenCV是BGR，face_recognition是RGB
    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    # 2. 检测人脸位置（返回[(top, right, bottom, left), ...]）
    face_locations = face_recognition.face_locations(rgb_frame)
    # 3. 生成检测到的人脸编码
    face_encodings_in_frame = face_recognition.face_encodings(rgb_frame, face_locations)
    # 4. 加载人脸库编码
    db_encodings = load_face_database()
    # 5. 匹配人脸：对比编码相似度
    recognized_faces = []  # 每个元素：(top, right, bottom, left, 学生ID_姓名)
    for (top, right, bottom, left), face_enc in zip(face_locations, face_encodings_in_frame):
        # 对比人脸库，返回布尔列表
        matches = face_recognition.compare_faces(list(db_encodings.values()), face_enc, tolerance=TOLERANCE)
        name_id = "未知人员"
        # 如果匹配成功，取第一个匹配的姓名
        if True in matches:
            first_match_idx = matches.index(True)
            name_id = list(db_encodings.keys())[first_match_idx]
        # 添加到识别结果
        recognized_faces.append((top, right, bottom, left, name_id))
    return recognized_faces

# 绘制人脸框和姓名（用于可视化，测试时用）
def draw_face_box(frame, recognized_faces):
    for (top, right, bottom, left, name_id) in recognized_faces:
        # 绘制矩形框（OpenCV的坐标是(x1,y1,x2,y2)）
        cv2.rectangle(frame, (left, top), (right, bottom), (0, 255, 0), 2)
        # 绘制姓名背景
        cv2.rectangle(frame, (left, bottom - 35), (right, bottom), (0, 255, 0), cv2.FILLED)
        # 绘制姓名文字
        cv2.putText(frame, name_id, (left + 6, bottom - 6), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)
    return frame