"""Lightweight speech recognition engine.

Strategy:
  Uses the `SpeechRecognition` library which wraps Google's free
  speech-to-text API.  Audio can be provided as:
    - A file path (wav, aiff, flac)
    - Raw base64-encoded audio bytes

  Also provides a simple keyword-spotting helper to detect
  classroom-relevant keywords (e.g. "问题", "回答", "不懂") from
  transcribed text, which feeds into the engagement analysis.

Future upgrade:
  Swap the recognizer backend to OpenAI Whisper (local) for
  offline, higher-accuracy recognition.
"""
import base64
import logging
import os
import tempfile
from typing import Dict, List

logger = logging.getLogger(__name__)

# Classroom-relevant keywords for engagement signals
ENGAGEMENT_KEYWORDS = {
    'question': ['问题', '请问', '为什么', '怎么', '什么', '如何', '?', '？'],
    'answer': ['回答', '答案', '我认为', '我觉得', '应该是', '因为'],
    'confusion': ['不懂', '不会', '不明白', '没听懂', '什么意思'],
    'agreement': ['对', '是的', '没错', '同意', '好的'],
}


def _get_recognizer():
    """Lazily import and return a SpeechRecognition Recognizer."""
    try:
        import speech_recognition as sr
        return sr.Recognizer()
    except ImportError:
        raise RuntimeError(
            "speech_recognition 未安装，请运行 pip install SpeechRecognition"
        )


def transcribe_audio_file(file_path: str, language: str = 'zh-CN') -> Dict:
    """Transcribe an audio file to text.

    Args:
        file_path: Path to a .wav / .aiff / .flac file.
        language: BCP-47 language code (default zh-CN).

    Returns:
        {
            'text': str,
            'success': bool,
            'error': Optional[str]
        }
    """
    try:
        import speech_recognition as sr
        recognizer = _get_recognizer()
        with sr.AudioFile(file_path) as source:
            audio = recognizer.record(source)
        text = recognizer.recognize_google(audio, language=language)
        return {'text': text, 'success': True, 'error': None}
    except sr.UnknownValueError:
        return {'text': '', 'success': False, 'error': '无法识别语音内容'}
    except sr.RequestError as e:
        return {'text': '', 'success': False, 'error': f'语音识别服务请求失败: {e}'}
    except Exception as e:
        logger.exception("语音识别异常")
        return {'text': '', 'success': False, 'error': str(e)}


def transcribe_base64_audio(b64_data: str, language: str = 'zh-CN') -> Dict:
    """Transcribe base64-encoded audio bytes.

    The audio must be in a format SpeechRecognition can read (wav/aiff/flac).
    """
    try:
        audio_bytes = base64.b64decode(b64_data)
    except Exception as e:
        return {'text': '', 'success': False, 'error': f'base64解码失败: {e}'}

    # Write to a temp file — SpeechRecognition needs a file path
    suffix = '.wav'
    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
    try:
        tmp.write(audio_bytes)
        tmp.close()
        return transcribe_audio_file(tmp.name, language=language)
    finally:
        try:
            os.unlink(tmp.name)
        except OSError:
            pass


def spot_keywords(text: str) -> Dict[str, List[str]]:
    """Detect classroom engagement keywords in transcribed text.

    Returns:
        {category: [matched_keyword, ...]} for each category that had matches.
    """
    result = {}
    if not text:
        return result
    for category, keywords in ENGAGEMENT_KEYWORDS.items():
        matched = [kw for kw in keywords if kw in text]
        if matched:
            result[category] = matched
    return result


def analyze_speech(b64_data: str, language: str = 'zh-CN') -> Dict:
    """Full pipeline: transcribe + keyword spotting.

    Returns:
        {
            'text': str,
            'success': bool,
            'error': Optional[str],
            'keywords': {category: [keywords]},
            'engagement_score': float  # 0-100 based on keyword richness
        }
    """
    result = transcribe_base64_audio(b64_data, language=language)
    if not result['success']:
        return {**result, 'keywords': {}, 'engagement_score': 0.0}

    keywords = spot_keywords(result['text'])
    # Simple engagement score: more diverse keyword categories → higher
    score = min(100.0, len(keywords) * 25.0 + len(result['text']) / 2.0)
    return {
        **result,
        'keywords': keywords,
        'engagement_score': round(score, 1),
    }
