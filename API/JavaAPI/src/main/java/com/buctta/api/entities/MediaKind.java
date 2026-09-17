package com.buctta.api.entities;

/**
 * 上传文件的种类，用于区分可播放的视频与可直接展示的图片。
 */
public enum MediaKind {
    /** 视频文件（mp4 / webm / mov / mkv） */
    VIDEO,
    /** 图片文件（jpg / png / gif / webp） */
    IMAGE
}
