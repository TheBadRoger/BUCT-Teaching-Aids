"""SQLAlchemy models for the student_analysis module.

Adapted from API/PythonAPI/student_analysis/models/db_models.py.
Uses the shared Base from modules.db so tables are created on startup.
"""
from datetime import datetime

from sqlalchemy import Column, Integer, String, Float, DateTime, DECIMAL

from modules.db import Base


class Student(Base):
    __tablename__ = "student"
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, unique=True, nullable=False)
    name = Column(String(50), nullable=False)
    class_name = Column(String(50), nullable=False)
    data_study_duration = Column(Integer, default=0)
    practice_duration = Column(Integer, default=0)
    practice_count = Column(Integer, default=0)


class Knowledge(Base):
    __tablename__ = "knowledge"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), unique=True, nullable=False)


class KnowledgeMastery(Base):
    __tablename__ = "knowledge_mastery"
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, nullable=False)
    subject_name = Column(String(100), nullable=False)
    rate = Column(DECIMAL(5, 2), nullable=False)
    progress = Column(DECIMAL(5, 2), nullable=False, default=0.0)
    class_name = Column(String(50), nullable=False)

    @property
    def mastery_rate(self):
        return float(self.rate)


class AdminUser(Base):
    __tablename__ = "admin_users"
    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, nullable=False)
    password = Column(String(100), nullable=False)
