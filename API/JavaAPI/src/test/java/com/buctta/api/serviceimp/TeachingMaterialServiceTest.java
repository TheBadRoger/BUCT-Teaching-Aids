package com.buctta.api.serviceimp;

import com.buctta.api.entities.TeachingMaterial;
import com.buctta.api.support.FakeRepositories;
import com.buctta.api.support.TestInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 教参（教学参考资料）管理。
 */
class TeachingMaterialServiceTest {

    private IMPL_TeachingMaterialService service;
    private FakeRepositories.FakeTeachingMaterialRepository materials;

    private static final Long COURSE_ID = 7L;
    private static final Long TEACHER_ID = 3L;

    @BeforeEach
    void setUp() {
        materials = new FakeRepositories.FakeTeachingMaterialRepository();
        service = new IMPL_TeachingMaterialService();
        TestInjector.injectOrdered(service, materials);
    }

    @Test
    void create_appliesDefaultsAndPersists() {
        TeachingMaterial material = new TeachingMaterial("第一章 讲义", "内容", COURSE_ID, TEACHER_ID);

        TeachingMaterial saved = service.create(material);

        assertEquals("第一章 讲义", saved.getTitle());
        assertEquals(true, saved.getIsPublic(), "未指定时默认为公开");
        assertEquals(0, saved.getViewCount());
        assertEquals(1, materials.count());
    }

    @Test
    void create_explicitPrivateIsKept() {
        TeachingMaterial material = new TeachingMaterial("内部资料", "内容", COURSE_ID, TEACHER_ID);
        material.setIsPublic(false);

        TeachingMaterial saved = service.create(material);

        assertEquals(false, saved.getIsPublic());
    }

    @Test
    void create_ignoresCallerSuppliedId() {
        TeachingMaterial material = new TeachingMaterial("x", "y", COURSE_ID, TEACHER_ID);
        material.setId(999L);

        TeachingMaterial saved = service.create(material);

        assertEquals(1L, saved.getId().longValue(),
                "主键应重新生成，避免调用方传 id 覆盖既有记录");
        assertEquals(1, materials.count());
    }

    @Test
    void create_rejectsUnsafeAttachmentUrl() {
        TeachingMaterial material = new TeachingMaterial("恶意附件", "y", COURSE_ID, TEACHER_ID);
        material.setAttachmentUrl("javascript:alert(1)");

        assertThrows(IllegalArgumentException.class, () -> service.create(material));
        assertEquals(0, materials.count(), "校验失败不应落库");
    }

    @Test
    void create_acceptsSafeAttachmentUrls() {
        for (String url : new String[]{"/api/media/3/content", "https://example.com/a.pdf",
                "http://example.com/b.pdf"}) {
            TeachingMaterial material = new TeachingMaterial("t-" + url, "y", COURSE_ID, TEACHER_ID);
            material.setAttachmentUrl(url);
            assertEquals(url, service.create(material).getAttachmentUrl());
        }
        assertEquals(3, materials.count());
    }

    @Test
    void getById_returnsNullForMissing() {
        assertNull(service.getById(404L));
        assertNull(service.getById(null));
    }

    @Test
    void viewAndCount_incrementsOnlyOnExplicitCall() {
        TeachingMaterial saved = service.create(
                new TeachingMaterial("讲义", "y", COURSE_ID, TEACHER_ID));

        // 普通读取不应累加浏览量
        service.getById(saved.getId());
        service.getById(saved.getId());
        assertEquals(0, service.getById(saved.getId()).getViewCount(),
                "只读查询不应改变浏览量");

        // 显式计数：第一次返回 1，第二次返回 2
        assertEquals(1, service.viewAndCount(saved.getId()).getViewCount());
        assertEquals(2, service.viewAndCount(saved.getId()).getViewCount());
        assertEquals(2, service.getById(saved.getId()).getViewCount(),
                "计数值应已持久化");
    }

    @Test
    void viewAndCount_returnsNullForMissing() {
        assertNull(service.viewAndCount(404L));
    }

    @Test
    void listByTeacher_and_listByCourse_filterCorrectly() {
        service.create(new TeachingMaterial("A", "y", COURSE_ID, TEACHER_ID));
        service.create(new TeachingMaterial("B", "y", COURSE_ID, 99L));
        service.create(new TeachingMaterial("C", "y", 88L, TEACHER_ID));

        Page<TeachingMaterial> byTeacher = service.listByTeacher(TEACHER_ID, PageRequest.of(0, 10));
        assertEquals(2, byTeacher.getTotalElements());

        Page<TeachingMaterial> byCourse = service.listByCourse(COURSE_ID, PageRequest.of(0, 10));
        assertEquals(2, byCourse.getTotalElements());
    }

    @Test
    void listPublic_returnsOnlyPublicOnes() {
        service.create(new TeachingMaterial("公开", "y", COURSE_ID, TEACHER_ID));
        TeachingMaterial hidden = new TeachingMaterial("私有", "y", COURSE_ID, TEACHER_ID);
        hidden.setIsPublic(false);
        service.create(hidden);

        Page<TeachingMaterial> page = service.listPublic(PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("公开", page.getContent().get(0).getTitle());
    }

    @Test
    void listWithNullId_returnsEmptyPageInsteadOfThrowing() {
        assertEquals(0, service.listByTeacher(null, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, service.listByCourse(null, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void update_overwritesOnlyProvidedFields() {
        TeachingMaterial original = service.create(
                new TeachingMaterial("原标题", "原内容", COURSE_ID, TEACHER_ID));
        original.setMaterialType("讲义");
        service.update(original.getId(), original);

        TeachingMaterial patch = new TeachingMaterial();
        patch.setTitle("新标题");

        TeachingMaterial updated = service.update(original.getId(), patch);

        assertEquals("新标题", updated.getTitle());
        assertEquals("原内容", updated.getContent(), "未提供的字段应保持原值");
        assertEquals(COURSE_ID, updated.getCourseId());
        assertEquals("讲义", updated.getMaterialType());
    }

    @Test
    void update_rejectsUnsafeAttachmentUrl() {
        TeachingMaterial original = service.create(
                new TeachingMaterial("标题", "内容", COURSE_ID, TEACHER_ID));
        TeachingMaterial patch = new TeachingMaterial();
        patch.setAttachmentUrl("data:text/html;base64,PHNjcmlwdD4=");

        assertThrows(IllegalArgumentException.class,
                () -> service.update(original.getId(), patch));
        assertNull(service.getById(original.getId()).getAttachmentUrl(), "非法值不应写入");
    }

    @Test
    void update_returnsNullForMissing() {
        assertNull(service.update(404L, new TeachingMaterial()));
    }

    @Test
    void delete_removesOnlyExisting() {
        TeachingMaterial saved = service.create(
                new TeachingMaterial("待删", "y", COURSE_ID, TEACHER_ID));

        assertTrue(service.delete(saved.getId()));
        assertFalse(service.delete(saved.getId()), "重复删除应返回 false");
        assertFalse(service.delete(null));
        assertEquals(0, materials.count());
    }
}
