"""Hand-up detection core logic.

Adapted from API/PythonAPI/face_hand_up/app/core/hand_detect.py.
Uses Config values from the unified config.
"""
import cv2
import imutils
import numpy as np

from config import Config

# Background subtractor – initialised once per process
_bg_subtractor = cv2.createBackgroundSubtractorMOG2(
    history=500, varThreshold=16, detectShadows=False
)


def detect_hand_up(frame, recognized_faces: list):
    """Detect hand-up gestures in *frame* given face positions.

    Returns:
        hand_up_students: list of name_id strings for students raising hands.
        hand_up_locations: list of (x1, y1, x2, y2) bounding boxes.
    """
    hand_up_students: list = []
    hand_up_locations: list = []

    frame_copy = frame.copy()
    h, w = frame_copy.shape[:2]
    detect_top = int(h * Config.DETECT_AREA_TOP)
    detect_bottom = int(h * Config.DETECT_AREA_BOTTOM)

    detect_area = frame_copy[detect_top:detect_bottom, :]
    gray = cv2.cvtColor(detect_area, cv2.COLOR_BGR2GRAY)
    blur = cv2.GaussianBlur(gray, (5, 5), 0)
    fg_mask = _bg_subtractor.apply(blur)

    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5))
    fg_mask = cv2.morphologyEx(fg_mask, cv2.MORPH_CLOSE, kernel)
    fg_mask = cv2.morphologyEx(fg_mask, cv2.MORPH_OPEN, kernel)

    contours = cv2.findContours(fg_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    contours = imutils.grab_contours(contours)

    for cnt in contours:
        if cv2.contourArea(cnt) < Config.CONTOUR_THRESHOLD:
            continue
        x, y, w_cnt, h_cnt = cv2.boundingRect(cnt)
        y += detect_top  # convert back to full-frame coordinates
        for (top, right, bottom, left, name_id) in recognized_faces:
            if (left - 50) < x < (right + 50) and y < top:
                if name_id not in hand_up_students and name_id != "未知人员":
                    hand_up_students.append(name_id)
                hand_up_locations.append((x, y, x + w_cnt, y + h_cnt))
                break

    return hand_up_students, hand_up_locations


def draw_hand_up_box(frame, hand_up_locations: list):
    """Draw boxes around detected raised hands (for debugging)."""
    for (x1, y1, x2, y2) in hand_up_locations:
        cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 0, 255), 2)
        cv2.putText(frame, "Hand Up", (x1, y1 - 10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 255), 2)
    return frame


def calculate_hand_up_rate(recognized_faces: list, hand_up_students: list):
    """Return (hand_up_rate, hand_up_num, valid_num)."""
    valid_num = len([f for f in recognized_faces if f[4] != "未知人员"])
    if valid_num == 0:
        return 0.0, 0, 0
    hand_up_num = len(hand_up_students)
    rate = round(hand_up_num / valid_num, 2)
    return rate, hand_up_num, valid_num
