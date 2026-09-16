package com.buctta.api.service;

import com.buctta.api.entities.MediaFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * 上传文件的落盘、读取与清理。
 * <p>
 * 只负责"文件系统"这一层，不接触数据库；持久化由 {@link MediaUploadService} 负责。
 */
public interface MediaStorageService {

    /**
     * 校验并落盘一个上传文件。
     *
     * @param file    上传文件
     * @param purpose 上传用途，决定允许的扩展名/大小/类型
     * @return 尚未入库的元数据
     * @throws MediaValidationException 校验不通过
     * @throws IOException              落盘失败
     */
    StoredFile store(MultipartFile file, MediaPurpose purpose) throws IOException;

    /** 按 storageKey 定位磁盘文件 */
    Path resolve(String storageKey);

    boolean exists(String storageKey);

    /** 把指定区间写入输出流，用于 Range 响应 */
    void writeTo(String storageKey, long offset, long length, OutputStream out) throws IOException;

    /** 删除磁盘文件；文件不存在时返回 false，不抛异常 */
    boolean delete(String storageKey);

    /** 删除一组文件，用于业务失败回滚 */
    void deleteQuietly(Iterable<String> storageKeys);

    /** 落盘结果（未入库的元数据） */
    record StoredFile(String storageKey,
                      String originalFilename,
                      String contentType,
                      long size,
                      com.buctta.api.entities.MediaKind kind) {
    }

    /** 便捷方法：把元数据转成实体（未保存） */
    default MediaFile toEntity(StoredFile stored) {
        return new MediaFile(stored.storageKey(), stored.originalFilename(),
                stored.contentType(), stored.size(), stored.kind());
    }
}
