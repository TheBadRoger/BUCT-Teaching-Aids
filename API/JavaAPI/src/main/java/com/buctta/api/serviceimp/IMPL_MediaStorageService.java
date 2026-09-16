package com.buctta.api.serviceimp;

import com.buctta.api.config.MediaProperties;
import com.buctta.api.entities.MediaKind;
import com.buctta.api.service.MediaPurpose;
import com.buctta.api.service.MediaStorageService;
import com.buctta.api.service.MediaValidationException;
import com.buctta.api.utils.BusinessStatus;
import com.buctta.api.utils.FileTypeSniffer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 媒体文件落盘实现。
 * <p>
 * 安全要点：
 * <ol>
 *     <li>存储名由服务端生成（日期目录 + UUID + 白名单扩展名），完全丢弃用户提供的路径，
 *         从根上避免路径穿越；</li>
 *     <li>类型判定以文件头魔术字节为准，扩展名只用于白名单与 ftyp 家族内部消歧，
 *         因此伪造扩展名无法绕过；</li>
 *     <li>读取时再次校验 storageKey 不含 {@code ..}、不是绝对路径，且解析结果必须落在
 *         存储根目录之内。</li>
 * </ol>
 */
@Slf4j
@Service
public class IMPL_MediaStorageService implements MediaStorageService {

    /** 图片体积小，校验时可整体进内存；视频只读取文件头做嗅探 */
    private static final long IN_MEMORY_LIMIT = 64L * 1024 * 1024;

    private static final DateTimeFormatter MONTH_DIR =
            DateTimeFormatter.ofPattern("yyyy/MM", Locale.ROOT);

    @Resource
    private MediaProperties mediaProperties;

    /** 嗅探文件头后仍位于流开头的可重复读取流 */
    private record SniffResult(InputStream stream, byte[] header, String contentType) {
    }

    @Override
    public StoredFile store(MultipartFile file, MediaPurpose purpose) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new MediaValidationException(BusinessStatus.RESOURCE_NOT_FOUND, "上传文件为空");
        }

        String original = sanitizeFilename(file.getOriginalFilename());
        String extension = FileTypeSniffer.extensionOf(original);
        if (extension.isEmpty()) {
            throw new MediaValidationException(BusinessStatus.PARAM_FORMAT_ERROR,
                    "文件名缺少扩展名，无法识别类型: " + original);
        }

        long size = file.getSize();
        long maxSize = purpose.isImage()
                ? mediaProperties.getUpload().getMaxImageSize()
                : mediaProperties.getUpload().getMaxVideoSize();
        if (size > maxSize) {
            throw new MediaValidationException(BusinessStatus.UPLOAD_FILE_TOO_LARGE,
                    "文件大小 " + size + " 字节，超出上限 " + maxSize + " 字节");
        }

        SniffResult sniffed = sniff(file);
        String contentType = sniffed.contentType();
        MediaKind kind = FileTypeSniffer.kindOf(contentType);
        if (kind == null) {
            closeQuietly(sniffed.stream());
            throw new MediaValidationException(BusinessStatus.UPLOAD_CONTENT_TYPE_NOT_SUPPORTED,
                    "无法识别的文件类型（文件头不是已知的图片/视频格式）: " + original);
        }
        if (kind != purpose.requiredKind()) {
            closeQuietly(sniffed.stream());
            throw new MediaValidationException(BusinessStatus.UPLOAD_CONTENT_TYPE_NOT_SUPPORTED,
                    "用途 " + purpose.name() + " 只接受 " + purpose.requiredKind()
                            + "，实际为 " + kind);
        }
        if (!isExtensionAllowed(extension, purpose)) {
            closeQuietly(sniffed.stream());
            throw new MediaValidationException(BusinessStatus.UPLOAD_CONTENT_TYPE_NOT_SUPPORTED,
                    "不支持的扩展名 ." + extension + "（" + purpose.fieldName() + "）");
        }

        String storageKey = buildStorageKey(extension);
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream out = Files.newOutputStream(target,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                sniffed.stream().transferTo(out);
            }
        }
        finally {
            closeQuietly(sniffed.stream());
        }

        return new StoredFile(storageKey, original, contentType, size, kind);
    }

    @Override
    public Path resolve(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new MediaValidationException(BusinessStatus.PARAM_MISSING, "storageKey 不能为空");
        }
        // 归一化后再判断，避免 a/../../b 之类的写法
        Path normalized = Paths.get(storageKey).normalize();
        if (normalized.isAbsolute() || normalized.startsWith("..")) {
            throw new MediaValidationException(BusinessStatus.PARAM_FORMAT_ERROR,
                    "非法的 storageKey: " + storageKey);
        }
        Path mediaRoot = mediaRoot();
        Path resolved = mediaRoot.resolve(normalized).normalize();
        if (!resolved.startsWith(mediaRoot)) {
            throw new MediaValidationException(BusinessStatus.PARAM_FORMAT_ERROR,
                    "非法的 storageKey: " + storageKey);
        }
        return resolved;
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.isRegularFile(resolve(storageKey));
    }

    @Override
    public void writeTo(String storageKey, long offset, long length, OutputStream out)
            throws IOException {
        Path path = resolve(storageKey);
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            raf.seek(offset);
            byte[] buffer = new byte[Math.max(1024, mediaProperties.getStreamBufferSize())];
            long remaining = length;
            while (remaining > 0) {
                int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read < 0) {
                    break;
                }
                out.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }

    @Override
    public boolean delete(String storageKey) {
        try {
            return Files.deleteIfExists(resolve(storageKey));
        }
        catch (IOException e) {
            log.warn("删除媒体文件失败 storageKey={}", storageKey, e);
            return false;
        }
    }

    @Override
    public void deleteQuietly(Iterable<String> storageKeys) {
        for (String key : storageKeys) {
            delete(key);
        }
    }

    /* ------------------------------------------------------------ */

    /** 读取文件头做类型嗅探，并把流重置到开头，避免二次读取 */
    private SniffResult sniff(MultipartFile file) throws IOException {
        int sniffLength = Math.max(8, mediaProperties.getUpload().getSniffLength());

        if (file.getSize() >= 0 && file.getSize() <= IN_MEMORY_LIMIT) {
            byte[] all = file.getBytes();
            int take = Math.min(all.length, sniffLength);
            byte[] header = new byte[take];
            System.arraycopy(all, 0, header, 0, take);
            String extension = FileTypeSniffer.extensionOf(sanitizeFilename(file.getOriginalFilename()));
            String contentType = FileTypeSniffer.resolveContentType(header, take, extension);
            return new SniffResult(new java.io.ByteArrayInputStream(all), header, contentType);
        }

        InputStream raw = new BufferedInputStream(file.getInputStream());
        byte[] header = new byte[sniffLength];
        int read = 0;
        while (read < sniffLength) {
            int n = raw.read(header, read, sniffLength - read);
            if (n < 0) {
                break;
            }
            read += n;
        }
        String extension = FileTypeSniffer.extensionOf(sanitizeFilename(file.getOriginalFilename()));
        String contentType = FileTypeSniffer.resolveContentType(header, read, extension);
        return new SniffResult(new PushbackStream(raw, header, read), header, contentType);
    }

    private boolean isExtensionAllowed(String extension, MediaPurpose purpose) {
        if (purpose.isImage()) {
            return mediaProperties.getUpload().getAllowedImageExtensions().contains(extension);
        }
        return mediaProperties.getUpload().getAllowedVideoExtensions().contains(extension);
    }

    private String buildStorageKey(String extension) {
        String month = LocalDate.now().format(MONTH_DIR);
        String name = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        return month + "/" + name;
    }

    private Path mediaRoot() {
        Path root = Paths.get(mediaProperties.getRoot()).toAbsolutePath().normalize();
        return root.resolve(mediaProperties.getStorage()).toAbsolutePath().normalize();
    }

    /** 去掉任何目录成分，只保留最后的文件名 */
    private String sanitizeFilename(String original) {
        if (!StringUtils.hasText(original)) {
            return "unnamed";
        }
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        return name.isEmpty() ? "unnamed" : name;
    }

    private void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            }
            catch (IOException ignored) {
                // 关闭失败不影响主流程
            }
        }
    }

    /**
     * 把已读出的文件头塞回流前，使后续 {@code transferTo} 能拿到完整内容。
     */
    private static final class PushbackStream extends InputStream {

        private final InputStream delegate;
        private final byte[] header;
        private int position;

        PushbackStream(InputStream delegate, byte[] header, int headerLength) {
            this.delegate = delegate;
            this.header = new byte[headerLength];
            System.arraycopy(header, 0, this.header, 0, headerLength);
        }

        @Override
        public int read() throws IOException {
            if (position < header.length) {
                return header[position++] & 0xFF;
            }
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            if (position < header.length) {
                int available = Math.min(len, header.length - position);
                System.arraycopy(header, position, b, off, available);
                position += available;
                return available;
            }
            return delegate.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    /** 供测试与诊断使用：列出当前已落盘的文件 */
    public List<Path> listStored() throws IOException {
        Path root = mediaRoot();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<Path> result = new ArrayList<>();
        try (var walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile).forEach(result::add);
        }
        return result;
    }
}
