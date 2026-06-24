"""Attention & engagement analysis engine.

Lightweight strategy:
  Combines multiple cheap signals into attention_level and engagement_level:
    - Head-up rate (from YOLO pose, already implemented in headup_rate module)
    - Face detection count (face_recognition)
    - Hand-up detection (face_hand_up module)
    - Emotion valence (emotion_detector)
    - Interaction frequency (SocketIO events count)

  Each signal contributes a weighted score; the final levels are 0-100.

Future upgrade:
  Add gaze-tracking model and body-pose engagement estimation.
"""
import logging
import time
from collections import deque
from typing import Dict, List, Optional

import numpy as np

from config import Config

logger = logging.getLogger(__name__)

# Weights for each signal (sum = 1.0)
WEIGHTS = {
    'head_up': 0.30,
    'face_detected': 0.15,
    'emotion_valence': 0.25,
    'hand_up': 0.10,
    'interaction': 0.20,
}

# Emotion → valence score (0-100)
EMOTION_VALENCE = {
    'focused': 90, 'happy': 95, 'engaged': 88, 'excited': 85,
    'neutral': 60, 'calm': 65, 'thinking': 70,
    'confused': 35, 'bored': 25, 'frustrated': 20, 'distracted': 15,
}


class AttentionAnalyzer:
    """Stateful per-student attention/engagement analyzer.

    Keeps a rolling window of recent signals to smooth out noise.
    """

    def __init__(self, window_size: int = None):
        self.window_size = window_size or Config.ATTENTION_WINDOW_SIZE
        # student_id -> deque of signal dicts
        self._history: Dict[str, deque] = {}
        # student_id -> interaction event timestamps
        self._interactions: Dict[str, deque] = {}

    def record_interaction(self, student_id: str, timestamp: Optional[float] = None):
        """Record that a student performed an interaction (response, feedback, etc.)."""
        ts = timestamp or time.time()
        if student_id not in self._interactions:
            self._interactions[student_id] = deque(maxlen=50)
        self._interactions[student_id].append(ts)

    def _interaction_score(self, student_id: str) -> float:
        """Score based on interaction frequency in the configured time window."""
        now = time.time()
        events = self._interactions.get(student_id, deque())
        recent = [t for t in events if now - t < Config.INTERACTION_WINDOW_SECONDS]
        # 5+ interactions → 100, linear scale
        return min(100.0, len(recent) / 5.0 * 100.0)

    def analyze(
        self,
        student_id: str,
        head_up_rate: float = 50.0,
        face_detected: bool = True,
        emotion: str = 'neutral',
        hand_up: bool = False,
    ) -> Dict:
        """Compute attention_level and engagement_level for a student.

        Args:
            student_id: Unique student identifier.
            head_up_rate: 0-100, from pose estimation.
            face_detected: Whether a face was detected for this student.
            emotion: Emotion label from emotion_detector.
            hand_up: Whether the student is raising their hand.

        Returns:
            {
                'attention_level': float,
                'engagement_level': float,
                'detailed_metrics': {gaze_focus, posture_engagement, interaction_frequency, facial_engagement},
                'emotion': str,
            }
        """
        signals = {
            'head_up': head_up_rate,
            'face_detected': 100.0 if face_detected else 0.0,
            'emotion_valence': EMOTION_VALENCE.get(emotion, 50.0),
            'hand_up': 100.0 if hand_up else 0.0,
            'interaction': self._interaction_score(student_id),
        }

        # Store in rolling history
        if student_id not in self._history:
            self._history[student_id] = deque(maxlen=self.window_size)
        self._history[student_id].append(signals)

        # Average over the rolling window for stability
        history = self._history[student_id]
        avg_signals = {}
        for key in signals:
            vals = [h[key] for h in history]
            avg_signals[key] = float(np.mean(vals))

        attention = sum(avg_signals[k] * WEIGHTS[k] for k in WEIGHTS)
        # Engagement emphasises interaction + emotion + hand_up
        engagement_weights = {
            'head_up': 0.20,
            'face_detected': 0.10,
            'emotion_valence': 0.30,
            'hand_up': 0.20,
            'interaction': 0.20,
        }
        engagement = sum(avg_signals[k] * engagement_weights[k] for k in engagement_weights)

        detailed = {
            'gaze_focus': round(avg_signals['head_up'], 1),
            'posture_engagement': round(avg_signals['face_detected'], 1),
            'interaction_frequency': round(avg_signals['interaction'], 1),
            'facial_engagement': round(avg_signals['emotion_valence'], 1),
        }

        return {
            'attention_level': round(attention, 1),
            'engagement_level': round(engagement, 1),
            'detailed_metrics': detailed,
            'emotion': emotion,
        }

    def get_history(self, student_id: str) -> List[Dict]:
        """Return the raw signal history for a student."""
        return list(self._history.get(student_id, deque()))


# Module-level singleton
analyzer = AttentionAnalyzer()
