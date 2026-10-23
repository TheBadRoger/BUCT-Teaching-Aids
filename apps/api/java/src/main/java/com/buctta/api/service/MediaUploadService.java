package com.buctta.api.service;

import com.buctta.api.dto.MediaFileDTO;
import com.buctta.api.entities.MediaFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 上传落库与业务字段回填。
 */
public interface MediaUploadService {

    /**
     * 上传文件并（在需要时）把地址回填到业务字段。
     *
     * @param file    上传文件
     * @param purpose 上传用途；不允许为空，决定校验规则与回填目标
     * @param ownerId 业务主体 ID：COURSE_COVER 传 courseId，ORG_* 传 organizationId；
     *                VIDEO 忽略该参数，可为 null
     * @return 上传结果，成功时业务字段已被更新
     */
    UploadResult upload(MultipartFile file, MediaPurpose purpose, Long ownerId) throws IOException;

    /** 读取文件元数据 */
    MediaFile getMediaFile(Long id);

    /** 删除文件记录与磁盘文件 */
    void delete(Long id);

    /** 上传结果的对外视图 */
    record UploadResult(MediaFileDTO file, String purpose, String targetField) {
    }
}
