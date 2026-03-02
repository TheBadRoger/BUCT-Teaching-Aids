import sys
sys.path.append("D:/BUCT-Teaching-Aids-main/BUCT-Teaching-Aids-main/API/python_backend/head_up_detection")  # 你的head_up_detection目录绝对路径
import db_utils
import detection_core

def test_full_flow():
    """测试完整流程：创建表→检测→存储→查询"""
    # 1. 首次运行：创建数据表（仅需执行一次）
    db_utils.create_tables()
    print("数据表创建成功（若已存在则跳过）")

    # 2. 模拟检测数据（前端传入）
    test_data = {
        "student_id": 1001,
        "course_id": "math_101",
        "course_name": "高等数学（上）",
        "data_type": "image",
        "detection_device": "教室摄像头-1",
        "remarks": "上课10分钟检测",
        # 可选：用本地图片测试后端计算，或直接传calculated_rate
        # "calculated_rate": 88.5
    }

    # 3. （可选）用本地图片测试后端检测
    try:
        # 替换为你的本地图片路径（如学生上课的照片）
        local_image_path = "test_student.jpg"
        head_up_rate = detection_core.test_detection_with_local_image(local_image_path)
        print(f"后端检测结果：抬头率={head_up_rate}%")
    except Exception as e:
        # 若没有本地图片，用模拟值测试
        head_up_rate = 85.2
        print(f"使用模拟值测试：抬头率={head_up_rate}%，原因：{str(e)}")

    # 4. 补充抬头率到检测数据
    test_data["head_up_rate"] = head_up_rate

    # 5. 保存到数据库
    db = next(db_utils.get_db())
    saved_result = db_utils.save_detection_result(db, test_data)
    print(f"数据保存成功，记录ID：{saved_result.id}")

    # 6. 查询测试
    query_results = db_utils.query_student_results(db, student_id=1001, course_id="math_101")
    print(f"查询结果：{len(query_results)}条记录")
    for res in query_results:
        print(f"时间：{res.detection_time}，抬头率：{res.head_up_rate}%")


if __name__ == "__main__":
    test_full_flow()