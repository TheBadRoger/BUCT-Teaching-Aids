package com.buctta.api.serviceimp;

import com.buctta.api.dao.TeachingMaterialRepository;
import com.buctta.api.entities.TeachingMaterial;
import com.buctta.api.service.TeachingMaterialService;
import com.buctta.api.utils.MediaUrls;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class IMPL_TeachingMaterialService implements TeachingMaterialService {

    @Resource
    private TeachingMaterialRepository teachingMaterialRepository;

    @Override
    @Transactional
    public TeachingMaterial create(TeachingMaterial material) {
        if (material.getAttachmentUrl() != null
                && !MediaUrls.isSafe(material.getAttachmentUrl())) {
            throw new IllegalArgumentException(MediaUrls.rejectMessage("attachmentUrl"));
        }
        if (material.getIsPublic() == null) {
            material.setIsPublic(true);
        }
        if (material.getViewCount() == null) {
            material.setViewCount(0);
        }
        // 主键由数据库生成，避免调用方传入 id 造成覆盖更新
        material.setId(null);
        return teachingMaterialRepository.save(material);
    }

    @Override
    public TeachingMaterial getById(Long id) {
        if (id == null) {
            return null;
        }
        return teachingMaterialRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public TeachingMaterial viewAndCount(Long id) {
        TeachingMaterial material = getById(id);
        if (material == null) {
            return null;
        }
        Integer current = material.getViewCount();
        material.setViewCount(current == null ? 1 : current + 1);
        return teachingMaterialRepository.save(material);
    }

    @Override
    public Page<TeachingMaterial> listByTeacher(Long teacherId, Pageable pageable) {
        if (teacherId == null) {
            return Page.empty(pageable);
        }
        return teachingMaterialRepository.findByTeacherId(teacherId, pageable);
    }

    @Override
    public Page<TeachingMaterial> listByCourse(Long courseId, Pageable pageable) {
        if (courseId == null) {
            return Page.empty(pageable);
        }
        return teachingMaterialRepository.findByCourseId(courseId, pageable);
    }

    @Override
    public Page<TeachingMaterial> listPublic(Pageable pageable) {
        return teachingMaterialRepository.findByIsPublicTrue(pageable);
    }

    @Override
    @Transactional
    public TeachingMaterial update(Long id, TeachingMaterial details) {
        TeachingMaterial existing = getById(id);
        if (existing == null) {
            return null;
        }
        if (details.getTitle() != null) {
            existing.setTitle(details.getTitle());
        }
        if (details.getContent() != null) {
            existing.setContent(details.getContent());
        }
        if (details.getCourseId() != null) {
            existing.setCourseId(details.getCourseId());
        }
        if (details.getTeacherId() != null) {
            existing.setTeacherId(details.getTeacherId());
        }
        if (details.getMaterialType() != null) {
            existing.setMaterialType(details.getMaterialType());
        }
        if (details.getAttachmentUrl() != null) {
            if (!MediaUrls.isSafe(details.getAttachmentUrl())) {
                throw new IllegalArgumentException(MediaUrls.rejectMessage("attachmentUrl"));
            }
            existing.setAttachmentUrl(details.getAttachmentUrl());
        }
        if (details.getIsPublic() != null) {
            existing.setIsPublic(details.getIsPublic());
        }
        return teachingMaterialRepository.save(existing);
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        if (id == null || !teachingMaterialRepository.existsById(id)) {
            return false;
        }
        try {
            teachingMaterialRepository.deleteById(id);
            return true;
        }
        catch (RuntimeException e) {
            log.error("删除教参失败 id={}", id, e);
            return false;
        }
    }
}
