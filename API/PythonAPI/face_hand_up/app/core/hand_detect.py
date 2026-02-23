import cv2
import imutils
import numpy as np
from config import DETECT_AREA_TOP, DETECT_AREA_BOTTOM, CONTOUR_THRESHOLD, CAMERA_HEIGHT

# 初始化背景减除器（用于过滤静态背景，只保留动态物体）
bg_subtractor = cv2.createBackgroundSubtractorMOG2(history=500, varThreshold=16, detectShadows=False)

# 单帧画面举手检测：输入OpenCV帧+人脸识别结果，返回「举手学生列表+举手位置」
def detect_hand_up(frame, recognized_faces):
    hand_up_students = []  # 举手的学生ID_姓名列表
    hand_up_locations = [] # 举手的轮廓位置
    frame_copy = frame.copy()
    h, w = frame_copy.shape[:2]
    # 计算举手检测区的上下边界（y轴）
    detect_top = int(h * DETECT_AREA_TOP)
    detect_bottom = int(h * DETECT_AREA_BOTTOM)
    # 1. 预处理：裁剪检测区+灰度+降噪+背景减除
    detect_area = frame_copy[detect_top:detect_bottom, :]  # 裁剪上半区域
    gray = cv2.cvtColor(detect_area, cv2.COLOR_BGR2GRAY)  # 转灰度
    blur = cv2.GaussianBlur(gray, (5, 5), 0)  # 高斯降噪
    fg_mask = bg_subtractor.apply(blur)  # 背景减除，得到前景掩码（白色为动态物体）
    # 2. 形态学操作：消除噪音，强化轮廓
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5))
    fg_mask = cv2.morphologyEx(fg_mask, cv2.MORPH_CLOSE, kernel)
    fg_mask = cv2.morphologyEx(fg_mask, cv2.MORPH_OPEN, kernel)
    # 3. 检测轮廓
    contours = cv2.findContours(fg_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    contours = imutils.grab_contours(contours)
    # 4. 遍历轮廓，判断是否为举手（过滤小轮廓，匹配人脸所属学生）
    for cnt in contours:
        if cv2.contourArea(cnt) < CONTOUR_THRESHOLD:  # 过滤小轮廓（噪音）
            continue
        # 获取轮廓的外接矩形
        x, y, w_cnt, h_cnt = cv2.boundingRect(cnt)
        # 转换为原画面的坐标（因为裁剪了上半区域）
        y += detect_top
        # 遍历人脸识别结果，判断该轮廓属于哪个学生（轮廓在人脸上方/附近）
        for (top, right, bottom, left, name_id) in recognized_faces:
            # 轮廓的x在人脸左右之间，y在人脸上方（举手时手在人脸上方）
            if (left - 50) < x < (right + 50) and y < top:
                if name_id not in hand_up_students and name_id != "未知人员":
                    hand_up_students.append(name_id)
                hand_up_locations.append((x, y, x + w_cnt, y + h_cnt))
                break
    return hand_up_students, hand_up_locations

# 绘制举手框（用于可视化，测试时用）
def draw_hand_up_box(frame, hand_up_locations):
    for (x1, y1, x2, y2) in hand_up_locations:
        cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 0, 255), 2)
        cv2.putText(frame, "Hand Up", (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 255), 2)
    return frame

# 计算举手率：举手人数/已识别的非未知人员数
def calculate_hand_up_rate(recognized_faces, hand_up_students):
    # 已识别的有效学生数（排除未知人员）
    valid_student_num = len([f for f in recognized_faces if f[4] != "未知人员"])
    if valid_student_num == 0:
        return 0.0, 0, 0
    # 举手人数
    hand_up_num = len(hand_up_students)
    # 举手率（保留2位小数）
    hand_up_rate = round(hand_up_num / valid_student_num, 2)
    return hand_up_rate, hand_up_num, valid_student_num