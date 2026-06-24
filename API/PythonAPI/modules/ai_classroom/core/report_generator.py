"""Classroom interaction report generator.

Aggregates per-student metrics, activity records, and emotion distributions
into a structured classroom report that teachers can use to adjust
teaching strategies.

Lightweight: uses pandas + numpy aggregation only.
Future: add LLM-based narrative summary generation.
"""
import logging
from datetime import datetime
from typing import Dict, List, Optional

import numpy as np
import pandas as pd

from modules.ai_classroom.core.emotion_detector import classify_emotion_category

logger = logging.getLogger(__name__)


def generate_classroom_report(classroom: Dict) -> Dict:
    """Generate a comprehensive classroom interaction report.

    Args:
        classroom: The classroom dict from ai_classroom.routes.classrooms.

    Returns:
        Structured report dict with:
          - report_meta
          - overall_summary
          - emotion_distribution
          - attention_engagement
          - student_rankings
          - activity_summary
          - teaching_suggestions
    """
    students: List[Dict] = classroom.get('students', [])
    activities: List[Dict] = classroom.get('activities', [])
    now = datetime.now().isoformat()

    if not students:
        return {
            'report_meta': {
                'classroom_id': classroom.get('id'),
                'generated_at': now,
                'total_students': 0,
            },
            'message': '课堂中暂无学生数据',
        }

    # ── Build DataFrame for aggregation ──────────────────────────────
    df = pd.DataFrame([
        {
            'student_id': s.get('id'),
            'name': s.get('name'),
            'attention_level': s.get('attention_level', 0),
            'engagement_level': s.get('engagement_level', 0),
            'emotion': s.get('emotion', 'neutral'),
        }
        for s in students
    ])

    # ── Emotion distribution ─────────────────────────────────────────
    df['emotion_category'] = df['emotion'].apply(classify_emotion_category)
    emotion_dist = df['emotion_category'].value_counts().to_dict()
    emotion_detail = df['emotion'].value_counts().to_dict()

    # ── Attention / engagement stats ─────────────────────────────────
    avg_att = round(float(df['attention_level'].mean()), 1)
    avg_eng = round(float(df['engagement_level'].mean()), 1)
    att_var = round(float(df['attention_level'].var()), 2)

    # ── Student rankings ─────────────────────────────────────────────
    df['composite_score'] = (
        df['attention_level'] * 0.5 + df['engagement_level'] * 0.5
    )
    ranked = df.sort_values('composite_score', ascending=False)

    top_students = ranked.head(5)[['student_id', 'name', 'attention_level', 'engagement_level', 'composite_score']].to_dict('records')
    # Students who need attention
    needs_help = ranked.tail(5)[['student_id', 'name', 'attention_level', 'engagement_level', 'composite_score']].to_dict('records')

    # ── Activity summary ─────────────────────────────────────────────
    activity_summary = []
    for i, act in enumerate(activities):
        responses = act.get('responses', [])
        activity_summary.append({
            'activity_id': i,
            'type': act.get('type'),
            'question': act.get('question', ''),
            'response_count': len(responses),
            'participation_rate': round(len(responses) / len(students) * 100, 1) if students else 0,
        })

    # ── Teaching suggestions (rule-based) ────────────────────────────
    suggestions = _generate_suggestions(avg_att, avg_eng, emotion_dist, activity_summary, len(students))

    return {
        'report_meta': {
            'classroom_id': classroom.get('id'),
            'teacher': classroom.get('teacher'),
            'subject': classroom.get('subject'),
            'generated_at': now,
            'total_students': len(students),
        },
        'overall_summary': {
            'avg_attention': avg_att,
            'avg_engagement': avg_eng,
            'attention_variance': att_var,
            'active_students': int((df['attention_level'] > 50).sum()),
            'highly_engaged': int((df['engagement_level'] > 70).sum()),
        },
        'emotion_distribution': {
            'categories': emotion_dist,
            'detail': emotion_detail,
        },
        'attention_engagement': {
            'average_attention': avg_att,
            'average_engagement': avg_eng,
            'attention_variance': att_var,
            'high_attention_count': int((df['attention_level'] > 80).sum()),
            'low_attention_count': int((df['attention_level'] < 50).sum()),
        },
        'student_rankings': {
            'top_students': top_students,
            'needs_help': needs_help,
        },
        'activity_summary': activity_summary,
        'teaching_suggestions': suggestions,
    }


def _generate_suggestions(
    avg_att: float,
    avg_eng: float,
    emotion_dist: Dict,
    activities: List[Dict],
    total_students: int,
) -> List[str]:
    """Generate rule-based teaching strategy suggestions."""
    suggestions = []

    if avg_att < 50:
        suggestions.append("整体注意力偏低，建议插入互动环节或调整教学节奏以重新吸引学生注意")
    elif avg_att < 70:
        suggestions.append("注意力水平中等，可考虑增加提问或小组讨论提升专注度")
    else:
        suggestions.append("学生注意力水平良好，可适当推进教学进度")

    if avg_eng < 50:
        suggestions.append("参与度不足，建议增加课堂互动活动（抢答、分组讨论等）")
    elif avg_eng < 70:
        suggestions.append("参与度尚可，可通过实时测评进一步激发学生参与")

    neg_count = emotion_dist.get('negative', 0)
    if total_students > 0 and neg_count / total_students > 0.3:
        suggestions.append("负面情绪占比较高，建议关注学生理解情况，适当放慢节奏或补充讲解")

    if activities:
        avg_participation = np.mean([a['participation_rate'] for a in activities])
        if avg_participation < 50:
            suggestions.append("课堂活动参与率偏低，建议优化问题设计或采用随机点名方式")
        else:
            suggestions.append("课堂活动参与率良好，可继续保持当前互动模式")

    if not suggestions:
        suggestions.append("课堂整体表现良好，建议保持当前教学策略")

    return suggestions


def generate_student_report(student: Dict, history_df: Optional[pd.DataFrame] = None) -> Dict:
    """Generate an individual student report.

    Args:
        student: The student dict from ai_classroom.routes.students.
        history_df: Optional DataFrame from StudentDataManager.get_student_history.
    """
    now = datetime.now().isoformat()
    att = student.get('attention_level', 0)
    eng = student.get('engagement_level', 0)
    emotion = student.get('emotion', 'neutral')

    # Determine learning state
    if att >= 80 and eng >= 80:
        state = 'excellent'
    elif att >= 60 and eng >= 60:
        state = 'good'
    elif att >= 40:
        state = 'moderate'
    else:
        state = 'needs_help'

    suggestions = []
    if att < 50:
        suggestions.append("该学生注意力较低，建议课堂上多加关注和互动")
    if eng < 50:
        suggestions.append("参与度不足，可安排其参与小组讨论或回答问题")
    dm = student.get('detailed_metrics', {})
    if dm.get('gaze_focus', 100) < 50:
        suggestions.append("视线专注度偏低，建议提醒学生注意听讲")
    if dm.get('posture_engagement', 100) < 50:
        suggestions.append("坐姿参与度需改善，建议关注学生课堂姿态")
    if not suggestions:
        suggestions.append("该学生课堂表现良好，建议继续保持")

    report: Dict = {
        'student_info': {'name': student.get('name'), 'id': student.get('id')},
        'generated_at': now,
        'current_state': {
            'attention_level': att,
            'engagement_level': eng,
            'emotion': emotion,
            'emotion_category': classify_emotion_category(emotion),
            'learning_state': state,
        },
        'detailed_metrics': dm,
        'suggestions': suggestions,
    }

    if history_df is not None and not history_df.empty:
        report['history_summary'] = {
            'data_points': len(history_df),
            'avg_attention': round(float(history_df['attention_level'].mean()), 1),
            'avg_engagement': round(float(history_df['engagement_level'].mean()), 1),
            'emotion_distribution': history_df['emotion'].value_counts().to_dict(),
        }

    return report

