"""Head-up rate detection core logic.

Adapted from API/PythonAPI/headup_rate/backend/detection_core.py.
Uses Config values from the unified config.
"""
import base64

import cv2
import numpy as np

from config import Config

# YOLO model – loaded lazily on first use
_model = None


def _get_model():
    global _model
    if _model is None:
        try:
            from ultralytics import YOLO
            _model = YOLO(Config.MODEL_PATH)
        except ImportError:
            raise RuntimeError("ultralytics 未安装，请先运行 pip install ultralytics")
    return _model


def decode_base64_image(raw_data: str) -> np.ndarray:
    """Decode a base64-encoded JPEG/PNG string into an OpenCV BGR image."""
    try:
        img_bytes = base64.b64decode(raw_data)
        img_np = np.frombuffer(img_bytes, dtype=np.uint8)
        img = cv2.imdecode(img_np, cv2.IMREAD_COLOR)
        if img is None:
            raise ValueError("Base64解码失败，图片格式错误")
        return img
    except Exception as e:
        raise ValueError(f"图片解码异常：{e}") from e


def detect_head_up_rate(
    data_type: str,
    raw_data: str | None = None,
    calculated_rate: float | None = None
) -> float:
    """Compute the head-up rate for the given input.

    Args:
        data_type: 'image' or 'video_frame'.
        raw_data: Base64-encoded image (required when calculated_rate is None).
        calculated_rate: Pre-computed rate from the client (0–100), used as-is.

    Returns:
        Head-up rate as a float in [0, 100], rounded to Config.RATE_PRECISION decimals.
    """
    if data_type not in Config.SUPPORTED_DATA_TYPES:
        raise ValueError(f"不支持的数据类型！仅支持：{Config.SUPPORTED_DATA_TYPES}")

    if calculated_rate is not None:
        if not (0 <= calculated_rate <= 100):
            raise ValueError("抬头率必须在 0-100 之间")
        return round(calculated_rate, Config.RATE_PRECISION)

    if not raw_data:
        raise ValueError("raw_data 不能为空（后端计算模式）")

    img = decode_base64_image(raw_data)
    model = _get_model()
    results = model(img, verbose=False)
    keypoints = results[0].keypoints.data

    total = len(keypoints)
    if total == 0:
        return 0.0

    img_height = img.shape[0]
    head_up_count = 0
    for kp in keypoints:
        nose, left_eye, right_eye = kp[0], kp[1], kp[2]
        if nose[2] < 0.5 or left_eye[2] < 0.5 or right_eye[2] < 0.5:
            continue
        if nose[1] < img_height * 0.4:
            head_up_count += 1

    rate = (head_up_count / total) * 100
    return round(rate, Config.RATE_PRECISION)
