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


def detect_head_up_rate(data_type: str, raw_data: str = None, image_data: np.ndarray = None) -> float:
    """
    计算抬头率（核心检测逻辑）
    :param data_type: 数据类型，支持 "image"（图片）或 "video_frame"（视频帧）
    :param raw_data: 可选，Base64编码的图片/视频帧（用于图片文件）
    :param image_data: 可选，numpy数组格式的图像数据（用于摄像头帧）
    :return: 抬头率（保留两位小数）
    """
    # 1. 验证数据类型
    if data_type not in config.DetectionConfig.SUPPORTED_DATA_TYPES:
        raise ValueError(f"不支持的数据类型！仅支持：{config.DetectionConfig.SUPPORTED_DATA_TYPES}")

    # 2. 处理图像数据（根据输入类型选择不同的解码方式）
    try:
        if data_type == "image" and raw_data:
            # 处理Base64编码的图片（原逻辑保留）
            img_data = base64.b64decode(raw_data)
            img_array = np.frombuffer(img_data, dtype=np.uint8)
            frame = cv2.imdecode(img_array, cv2.IMREAD_COLOR)
        elif data_type == "video_frame" and image_data is not None:
            # 处理摄像头传入的numpy帧（新增逻辑）
            frame = image_data  # 直接使用摄像头的帧数据
        else:
            raise ValueError("数据类型与传入数据不匹配（需提供raw_data或image_data）")
    except Exception as e:
        raise RuntimeError(f"图像数据解码失败：{str(e)}")

    # 3. 用YOLO模型检测人体姿态（原逻辑保留）
    results = model(frame, verbose=False)  # verbose=False关闭模型输出

    # 4. 计算抬头率（优化：用人体关键点判断抬头姿态）
    total_persons = 0
    head_up_persons = 0

    for result in results:
        # 遍历检测到的每个人体（YOLO Pose会返回人体关键点）
        for pose in result.keypoints.data:  # keypoints.data 是关键点坐标数组
            total_persons += 1

            # 关键点索引（YOLO Pose定义）：0=鼻子，1=左眼，2=右眼，3=左耳，4=右耳
            # 提取头部关键点（鼻子、眼睛、耳朵）
            nose = pose[0]  # [x坐标, y坐标, 置信度]
            left_eye = pose[1]
            right_eye = pose[2]

            # 过滤低置信度的关键点（避免误判）
            if nose[2] < 0.5 or left_eye[2] < 0.5 or right_eye[2] < 0.5:
                continue  # 关键点不清晰，跳过

            # 抬头判断：头部关键点y坐标（高度）低于颈部关键点（简化逻辑）
            # 原理：抬头时头部更靠上（y值更小），低头时头部更靠下（y值更大）
            # 若没有颈部关键点，用胸部关键点（5=左肩，6=右肩）近似
            left_shoulder = pose[5]
            right_shoulder = pose[6]
            shoulder_y = (left_shoulder[1] + right_shoulder[1]) / 2  # 肩膀平均y坐标
            head_y = (nose[1] + left_eye[1] + right_eye[1]) / 3  # 头部平均y坐标

            # 头部比肩膀高（y值小），视为抬头（阈值可根据实际调整）
            if head_y < shoulder_y - 10:  # 10是容错值，可根据画面分辨率调整
                head_up_persons += 1

    # 5. 避免除零错误
    if total_persons == 0:
        return 0.0

    # 6. 计算并返回抬头率
    calculated_rate = (head_up_persons / total_persons) * 100
    return round(calculated_rate, config.DetectionConfig.RATE_PRECISION)



# 测试用：模拟前端传入图片（可选）
def test_detection_with_local_image(image_path: str) -> float:
    """用本地图片测试检测功能"""
    # 读取本地图片并编码为Base64
    with open(image_path, "rb") as f:
        raw_data = base64.b64encode(f.read()).decode("utf-8")
    # 调用检测函数
    return detect_head_up_rate(data_type="image", raw_data=raw_data)
def detect_head_up_rate_from_camera(show_window=True, camera_index=0):
    """
    从电脑摄像头实时检测抬头率
    :param show_window: 是否显示检测窗口（True/False）
    :param camera_index: 摄像头索引，0为默认摄像头，1为外接摄像头
    :return: 实时抬头率（每帧计算的平均值）
    """
    # 打开摄像头
    cap = cv2.VideoCapture(camera_index)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    if not cap.isOpened():
        raise ValueError("无法打开摄像头，请检查摄像头是否被占用或驱动正常")

    rate_list = []  # 存储每帧的抬头率，最后返回平均值
    print("摄像头检测中，按 'q' 键退出检测...")

    while True:
        # 读取摄像头帧
        ret, frame = cap.read()
        if not ret:
            print("摄像头读取失败，退出检测")
            break

        # 调用原有检测逻辑，计算当前帧的抬头率（核心修正：参数传递方式）
        try:
            # 直接传递 data_type 和 image_data 两个参数（符合函数要求）
            head_up_rate = detect_head_up_rate(
                data_type="video_frame",  # 明确数据类型为视频帧
                image_data=frame  # 传入摄像头采集的帧（numpy数组）
            )
            rate_list.append(head_up_rate)

            # 显示检测结果（在窗口上标注抬头率）
            if show_window:
                cv2.putText(
                    frame,
                    f"Head Up Rate: {head_up_rate}%",
                    (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    1,
                    (0, 255, 0),
                    2
                )
                cv2.imshow("Head Up Detection (Camera)", frame)
        except Exception as e:
            print(f"单帧检测出错：{str(e)}")
            continue

        # 按 'q' 键退出循环
        if cv2.waitKey(1) & 0xFF == ord('q'):
            print("用户手动退出检测")
            break

    # 释放摄像头和窗口
    cap.release()
    cv2.destroyAllWindows()

    # 计算并返回平均抬头率（避免空列表）
    if not rate_list:
        return 0.0
    avg_rate = round(sum(rate_list) / len(rate_list), config.DetectionConfig.RATE_PRECISION)
    return avg_rate