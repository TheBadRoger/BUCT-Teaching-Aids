"""Lightweight emotion detection engine.

Strategy (lightweight):
  Uses face_recognition facial landmarks to compute geometric features
  (mouth aspect ratio, eye openness, eyebrow position) and maps them to
  emotion categories via heuristic rules.

  If face_recognition is not installed, falls back to a neutral default
  so the rest of the system keeps working.

Future upgrade path:
  Replace _heuristic_classify with a DeepFace / FER model call.
"""
import logging
import math
from typing import Dict, List, Optional, Tuple

import numpy as np

logger = logging.getLogger(__name__)

# Emotion categories aligned with ai_classroom.routes.EMOTION_CATEGORIES
EMOTION_LABELS = [
    'focused', 'happy', 'engaged', 'excited',        # positive
    'neutral', 'calm', 'thinking',                    # neutral
    'confused', 'bored', 'frustrated', 'distracted',  # negative
]


def _euclidean(p1, p2) -> float:
    return math.hypot(p1[0] - p2[0], p1[1] - p2[1])


def _mouth_aspect_ratio(landmarks: np.ndarray) -> float:
    """Compute a simplified mouth aspect ratio from face_recognition landmarks.

    face_recognition returns 68 landmarks.  Mouth points are indices 48-67.
    """
    # Outer mouth corners
    left = landmarks[48]
    right = landmarks[54]
    # Top / bottom inner lip
    top = landmarks[51]
    bottom = landmarks[57]
    horizontal = _euclidean(left, right)
    if horizontal < 1e-6:
        return 0.0
    vertical = _euclidean(top, bottom)
    return vertical / horizontal


def _eye_openness(landmarks: np.ndarray) -> float:
    """Average eye openness ratio (EAR simplified)."""
    def _ear(eye_idx):
        p1, p2, p3, p4, p5, p6 = [landmarks[i] for i in eye_idx]
        vertical = (_euclidean(p2, p6) + _euclidean(p3, p5)) / 2.0
        horizontal = _euclidean(p1, p4)
        if horizontal < 1e-6:
            return 0.0
        return vertical / horizontal
    left = _ear([36, 37, 38, 39, 40, 41])
    right = _ear([42, 43, 44, 45, 46, 47])
    return (left + right) / 2.0


def _eyebrow_position(landmarks: np.ndarray) -> float:
    """Relative eyebrow height vs eye — higher = surprised/raised."""
    brow_left = landmarks[19]
    brow_right = landmarks[24]
    eye_left = landmarks[37]
    eye_right = landmarks[44]
    eye_y = (eye_left[1] + eye_right[1]) / 2.0
    brow_y = (brow_left[1] + brow_right[1]) / 2.0
    face_height = _euclidean(landmarks[8], landmarks[27])  # chin to nose bridge top
    if face_height < 1e-6:
        return 0.0
    return (eye_y - brow_y) / face_height


def _heuristic_classify(mar: float, ear: float, brow: float) -> str:
    """Map geometric features to an emotion label using simple thresholds."""
    # Smiling / happy
    if mar > 0.5:
        if brow > 0.12:
            return 'excited'
        return 'happy'
    # Eyes very open + raised brows → engaged / excited
    if ear > 0.30 and brow > 0.10:
        return 'engaged'
    # Eyes half-closed, neutral mouth → focused or bored
    if ear < 0.20:
        if brow < 0.05:
            return 'bored'
        return 'focused'
    # Raised brows + small mouth → confused / thinking
    if brow > 0.10:
        return 'confused'
    if mar < 0.2 and 0.20 <= ear <= 0.28:
        return 'thinking'
    return 'neutral'


def detect_emotion_from_landmarks(landmarks: np.ndarray) -> Dict:
    """Detect emotion from a 68-point facial landmark array.

    Returns:
        {
            'emotion': str,
            'scores': {emotion: float},   # soft distribution
            'features': {mar, ear, brow}
        }
    """
    mar = _mouth_aspect_ratio(landmarks)
    ear = _eye_openness(landmarks)
    brow = _eyebrow_position(landmarks)
    emotion = _heuristic_classify(mar, ear, brow)

    # Build a simple soft-score distribution around the predicted label
    scores = {label: 0.05 for label in EMOTION_LABELS}
    scores[emotion] = 0.6
    # Give secondary weight to related emotions
    if emotion == 'happy':
        scores['excited'] = 0.2
    elif emotion == 'engaged':
        scores['focused'] = 0.2
    elif emotion == 'bored':
        scores['distracted'] = 0.2
    elif emotion == 'confused':
        scores['frustrated'] = 0.15

    return {
        'emotion': emotion,
        'scores': scores,
        'features': {'mar': round(mar, 3), 'ear': round(ear, 3), 'brow': round(brow, 3)}
    }


def detect_emotion_from_frame(frame) -> Dict:
    """Detect emotion from an OpenCV BGR frame.

    Uses face_recognition if available; otherwise returns 'neutral'.
    """
    try:
        import face_recognition
        import cv2
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        face_landmarks_list = face_recognition.face_landmarks(rgb)
        if not face_landmarks_list:
            return {'emotion': 'neutral', 'scores': {}, 'features': {}}

        # Convert first face's landmarks dict to 68-point array
        lm = face_landmarks_list[0]
        # face_recognition returns named parts; reconstruct ordered array
        ordered_names = [
            'chin', 'left_eyebrow', 'right_eyebrow', 'nose_bridge', 'nose_tip',
            'left_eye', 'right_eye', 'top_lip', 'bottom_lip'
        ]
        points = []
        for name in ordered_names:
            points.extend(lm.get(name, []))
        if len(points) < 68:
            return {'emotion': 'neutral', 'scores': {}, 'features': {}}
        landmarks = np.array(points[:68], dtype=float)
        return detect_emotion_from_landmarks(landmarks)
    except ImportError:
        logger.debug("face_recognition 未安装，情感检测返回 neutral")
        return {'emotion': 'neutral', 'scores': {}, 'features': {}}
    except Exception as e:
        logger.warning("情感检测异常: %s", e)
        return {'emotion': 'neutral', 'scores': {}, 'features': {}}


def classify_emotion_category(emotion: str) -> str:
    """Map a single emotion label to 'positive' / 'neutral' / 'negative'."""
    for category, emotions in {
        'positive': ['focused', 'happy', 'engaged', 'excited'],
        'neutral': ['neutral', 'calm', 'thinking'],
        'negative': ['confused', 'bored', 'frustrated', 'distracted'],
    }.items():
        if emotion in emotions:
            return category
    return 'neutral'

