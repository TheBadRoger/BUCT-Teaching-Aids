package com.buctta.api.service;

import com.buctta.api.dto.MediaFileDTO;
import com.buctta.api.entities.CourseVideo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 课程多视频能力：一门课程挂多个视频（课程 → 视频 一对多）。
 */
public interface CourseVideoService {

    /**
     * 上传视频并挂到课程下。
     *
     * @param courseId        课程 ID
     * @param title           视频标题，为空时使用原始文件名
     * @param description     视频简介，可为空
     * @param sortOrder       课程内顺序，为空时自动追加到末尾
     * @param durationSeconds 时长（秒），可为空
     * @param video           视频文件
     * @param cover           封面图，可为空
     * @param withCover       是否上传了封面
     */
    CourseVideoDetail uploadVideo(Long courseId, String title, String description,
                                  Integer sortOrder, Integer durationSeconds,
                                  MultipartFile video, MultipartFile cover,
                                  boolean withCover) throws IOException;

    /** 课程视频目录，按 sortOrder 升序 */
    List<CourseVideoDetail> listByCourse(Long courseId);

    /** 单个视频详情 */
    CourseVideoDetail getDetail(Long videoId);

    /** 更新视频元信息（标题/简介/顺序/时长/封面替换） */
    CourseVideoDetail updateVideo(Long videoId, String title, String description,
                                  Integer sortOrder, Integer durationSeconds,
                                  MultipartFile cover, boolean replaceCover) throws IOException;

    /** 删除视频及其文件（封面一并清理） */
    void deleteVideo(Long videoId);

    /**
     * 视频详情：实体 + 播放地址 + 封面地址。
     * 前端拿到 {@link #videoUrl()} 即可交给 {@code <video src>}，服务端支持 Range 拖动播放。
     */
    record CourseVideoDetail(Long id,
                             Long courseId,
                             String title,
                             String description,
                             Integer sortOrder,
                             Integer durationSeconds,
                             String videoUrl,
                             String coverUrl,
                             Long videoFileId,
                             Long coverFileId,
                             Long videoSize,
                             String videoContentType,
                             boolean playableInBrowser) {

        public static CourseVideoDetail of(CourseVideo video, String videoUrl, String coverUrl,
                                           MediaFileDTO videoFile, MediaFileDTO coverFile) {
            Long videoSize = videoFile == null ? null : videoFile.size();
            String contentType = videoFile == null ? null : videoFile.contentType();
            return new CourseVideoDetail(video.getId(),
                    video.getCourseId(),
                    video.getTitle(),
                    video.getDescription(),
                    video.getSortOrder(),
                    video.getDurationSeconds(),
                    videoUrl,
                    coverUrl,
                    video.getVideoFileId(),
                    video.getCoverFileId(),
                    videoSize,
                    contentType,
                    contentType != null
                            && !com.buctta.api.utils.FileTypeSniffer
                            .isLikelyNotPlayableInBrowser(contentType));
        }
    }
}
