import cv2
import numpy as np
import base64
import random
from ultralytics import YOLO
import config

# 加载检测模型（首次运行会自动下载YOLO预训练模型，无需手动配置）
model = YOLO(config.DetectionConfig.MODEL_PATH)


def decode_base64_image(raw_data: str) -> np.ndarray:
    """解码Base64编码的图片为OpenCV格式"""
    try:
        img_data = base64.b64decode(raw_data)
        img_np = np.frombuffer(img_data, dtype=np.uint8)
        img = cv2.imdecode(img_np, cv2.IMREAD_COLOR)
        if img is None:
            raise ValueError("Base64解码失败，图片格式错误")
        return img
    except Exception as e:
        raise Exception(f"图片解码异常：{str(e)}")


def detect_head_up_rate(data_type: str, raw_data: str = None, calculated_rate: float = None) -> float:
    """
    核心：计算抬头率
    :param data_type: 数据类型（image/video_frame）
    :param raw_data: 原始数据（Base64编码的图片）
    :param calculated_rate: 前端已计算的抬头率（可选）
    :return: 0-100的抬头率数值
    """
    # 1. 验证参数
    if data_type not in config.DetectionConfig.SUPPORTED_DATA_TYPES:
        raise ValueError(f"不支持的数据类型！仅支持：{config.DetectionConfig.SUPPORTED_DATA_TYPES}")

    # 2. 优先使用前端已计算的结果
    if calculated_rate is not None:
        if not (0 <= calculated_rate <= 100):
            raise ValueError("抬头率必须在0-100之间")
        return round(calculated_rate, config.DetectionConfig.RATE_PRECISION)

    # 3. 后端计算（基于YOLO姿态检测）
    if not raw_data:
        raise ValueError("原始数据raw_data不能为空（后端计算模式）")

    # 4. 解码图片
    img = decode_base64_image(raw_data)

    # 5. YOLO检测人体姿态关键点
    results = model(img, verbose=False)  # verbose=False关闭模型日志
    keypoints = results[0].keypoints.data  # 人体关键点（shape: [人数, 17, 3]，17个关键点）

    total_persons = len(keypoints)
    if total_persons == 0:
        return 0.0  # 无检测到人员，抬头率为0

    # 6. 根据头部姿态判断是否抬头（核心逻辑）
    head_up_count = 0
    for kp in keypoints:
        # 关键点索引：0=鼻子，1=左眼，2=右眼，3=左耳，4=右耳（头部关键点）
        nose = kp[0]  # [x, y, 置信度]
        left_eye = kp[1]
        right_eye = kp[2]

        # 过滤置信度低的关键点（置信度<0.5视为无效）
        if nose[2] < 0.5 or left_eye[2] < 0.5 or right_eye[2] < 0.5:
            continue

        # 简化逻辑：头部关键点（鼻子、眼睛）在画面上半部分 → 视为抬头
        # 可根据实际需求优化（如通过头部倾斜角度判断）
        img_height = img.shape[0]
        if nose[1] < img_height * 0.4:  # 鼻子在画面上40%区域
            head_up_count += 1

    # 7. 计算抬头率
    head_up_rate = (head_up_count / total_persons) * 100
    return round(head_up_rate, config.DetectionConfig.RATE_PRECISION)


# 测试用：模拟前端传入图片（可选）
def test_detection_with_local_image(image_path: str) -> float:
    """用本地图片测试检测功能"""
    # 读取本地图片并编码为Base64
    with open(image_path, "rb") as f:
        raw_data = base64.b64encode(f.read()).decode("utf-8")
    # 调用检测函数
    return detect_head_up_rate(data_type="image", raw_data=raw_data)