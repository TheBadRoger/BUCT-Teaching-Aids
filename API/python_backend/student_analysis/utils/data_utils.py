from typing import Dict, List

def get_student_base_info(student_id: int) -> Dict:
    """模拟从数据库/Excel读取学生基础信息"""
    # 真实场景：这里会写连接数据库/Excel的代码，现在用假数据
    return {
        "student_name": "张三",
        "class_name": "计算机2201班"
    }

def get_student_knowledge(student_id: int) -> List[Dict]:
    """模拟读取知识掌握情况"""
    return [
        {"knowledge_name": "Python编程", "mastery_rate": 85.5, "level": "良", "weak_points": ["装饰器", "生成器"]},
        {"knowledge_name": "数据库", "mastery_rate": 72.0, "level": "中", "weak_points": ["索引优化", "事务"]},
        {"knowledge_name": "Web开发", "mastery_rate": 92.3, "level": "优", "weak_points": []}
    ]

def get_student_head_up_rate(student_id: int) -> Dict:
    """模拟读取抬头率数据"""
    return {
        "average_rate": 88.2,
        "period_rates": [
            {"period": "第1-2节课", "rate": 90.5},
            {"period": "第3-4节课", "rate": 85.7},
            {"period": "第5-6节课", "rate": 89.1},
            {"period": "晚自习", "rate": 87.3}
        ]
    }

def get_student_extra_metrics(student_id: int) -> Dict:
    """模拟读取补充指标"""
    return {
        "interaction_count": 12,
        "homework_completion_rate": 95.0,
        "error_rate": 12.3
    }
