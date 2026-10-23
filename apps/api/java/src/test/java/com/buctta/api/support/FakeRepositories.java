package com.buctta.api.support;

import com.buctta.api.dao.CourseReposit;
import com.buctta.api.dao.CourseVideoRepository;
import com.buctta.api.dao.MediaFileRepository;
import com.buctta.api.dao.OrganizationRepository;
import com.buctta.api.dao.StudentCourseReposit;
import com.buctta.api.dao.StudentReposit;
import com.buctta.api.entities.Course;
import com.buctta.api.entities.CourseVideo;
import com.buctta.api.entities.MediaFile;
import com.buctta.api.entities.Organization;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.StudentCourse;
import com.buctta.api.entities.StudentCourseId;
import com.buctta.api.entities.StudentVideoProgress;
import com.buctta.api.entities.Teacher;
import com.buctta.api.entities.TeachingMaterial;
import com.buctta.api.entities.User;
import com.buctta.api.entities.UserFollow;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 测试用内存仓储集合：替代真实数据库，保留与真实仓储一致的方法名与签名。
 */
public final class FakeRepositories {

    private FakeRepositories() {
    }

    /* ==================== 媒体 ==================== */

    public static class FakeMediaFileRepository
            extends InMemoryJpaRepository<MediaFile, Long> implements MediaFileRepository {

        @Override
        protected Long getId(MediaFile entity) {
            return entity.getId();
        }

        @Override
        protected void setId(MediaFile entity, Long id) {
            entity.setId(id);
        }

        @Override
        public List<MediaFile> findByIdIn(Collection<Long> ids) {
            List<MediaFile> result = new ArrayList<>();
            for (Long id : ids) {
                MediaFile media = store.get(id);
                if (media != null) {
                    result.add(media);
                }
            }
            return result;
        }
    }

    /* ==================== 课程 ==================== */

    /**
     * 课程仓储。
     * <p>
     * 注意：{@code Course.id} 是<b>基本类型</b> {@code long}，未持久化时是 0 而不是 null，
     * 因此不能用基类"id == null 即新增"的规则，否则每次 save 都会重新分配 ID，
     * 导致所有课程都变成同一个 id。这里显式把 0 视为未分配。
     */
    public static class FakeCourseReposit
            extends InMemoryJpaRepository<Course, Long> implements CourseReposit {

        private final java.util.concurrent.atomic.AtomicLong courseSequence =
                new java.util.concurrent.atomic.AtomicLong(0);

        @Override
        protected Long getId(Course entity) {
            return entity.getId();
        }

        @Override
        protected void setId(Course entity, Long id) {
            entity.setId(id);
        }

        @Override
        public <S extends Course> S save(S entity) {
            if (entity.getId() == 0L) {
                entity.setId(courseSequence.incrementAndGet());
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Course findCourseByCourseNumber(String courseNumber) {
            return store.values().stream()
                    .filter(c -> c.getCourseNumber() != null
                            && c.getCourseNumber().equals(courseNumber))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Optional<Course> findCourseById(Long id) {
            return findById(id);
        }

        @Override
        public void deleteAllByIdIn(List<Long> ids) {
            for (Long id : ids) {
                store.remove(id);
            }
        }
    }

    /* ==================== 机构 ==================== */

    public static class FakeOrganizationRepository
            extends InMemoryJpaRepository<Organization, Long> implements OrganizationRepository {

        @Override
        protected Long getId(Organization entity) {
            return entity.getId();
        }

        @Override
        protected void setId(Organization entity, Long id) {
            entity.setId(id);
        }

        @Override
        public Optional<Organization> findByName(String name) {
            return store.values().stream()
                    .filter(o -> o.getName() != null && o.getName().equals(name))
                    .findFirst();
        }

        @Override
        public boolean existsByNameAndIdNot(String name, Long id) {
            return store.values().stream()
                    .anyMatch(o -> o.getName() != null && o.getName().equals(name)
                            && !o.getId().equals(id));
        }
    }

    /* ==================== 课程视频 ==================== */

    public static class FakeCourseVideoRepository
            extends InMemoryJpaRepository<CourseVideo, Long> implements CourseVideoRepository {

        @Override
        protected Long getId(CourseVideo entity) {
            return entity.getId();
        }

        @Override
        protected void setId(CourseVideo entity, Long id) {
            entity.setId(id);
        }

        @Override
        public List<CourseVideo> findByCourseIdOrderBySortOrderAscIdAsc(Long courseId) {
            List<CourseVideo> result = new ArrayList<>();
            for (CourseVideo video : store.values()) {
                if (courseId.equals(video.getCourseId())) {
                    result.add(video);
                }
            }
            result.sort(Comparator
                    .comparing(CourseVideo::getSortOrder,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(CourseVideo::getId));
            return result;
        }

        @Override
        public long countByCourseId(Long courseId) {
            return store.values().stream()
                    .filter(v -> courseId.equals(v.getCourseId()))
                    .count();
        }

        @Override
        public void deleteByCourseId(Long courseId) {
            store.values().removeIf(v -> courseId.equals(v.getCourseId()));
        }

        @Override
        public void deleteByVideoFileId(Long videoFileId) {
            store.values().removeIf(v -> videoFileId.equals(v.getVideoFileId()));
        }
    }

    /* ==================== 学生 ==================== */

    public static class FakeStudentRepository
            extends InMemoryJpaRepository<Student, Long> implements StudentReposit {

        @Override
        protected Long getId(Student entity) {
            return entity.getId();
        }

        @Override
        protected void setId(Student entity, Long id) {
            entity.setId(id);
        }

        @Override
        public Optional<Student> findByStudentNumber(String studentNumber) {
            return store.values().stream()
                    .filter(s -> s.getStudentNumber() != null
                            && s.getStudentNumber().equals(studentNumber))
                    .findFirst();
        }

        @Override
        public List<Student> findByNameContaining(String name) {
            return store.values().stream()
                    .filter(s -> s.getName() != null && s.getName().contains(name))
                    .toList();
        }

        @Override
        public List<Student> findByClassName(String className) {
            return store.values().stream()
                    .filter(s -> className.equals(s.getClassName()))
                    .toList();
        }

        @Override
        public List<Student> findByGender(String gender) {
            return store.values().stream()
                    .filter(s -> gender.equals(s.getGender()))
                    .toList();
        }

        @Override
        public boolean existsByStudentNumber(String studentNumber) {
            return findByStudentNumber(studentNumber).isPresent();
        }

        @Override
        public void deleteAllByIdIn(List<Long> ids) {
            for (Long id : ids) {
                store.remove(id);
            }
        }
    }

    /* ==================== 用户 ==================== */

    public static class FakeUserRepository
            extends InMemoryJpaRepository<User, Long>
            implements com.buctta.api.dao.UserReposit {

        @Override
        protected Long getId(User entity) {
            return entity.getId();
        }

        @Override
        protected void setId(User entity, Long id) {
            entity.setId(id);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return store.values().stream()
                    .filter(u -> u.getUsername() != null && u.getUsername().equals(username))
                    .findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return store.values().stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().equals(email))
                    .findFirst();
        }

        @Override
        public Optional<User> findByTelephone(String telephone) {
            return store.values().stream()
                    .filter(u -> u.getTelephone() != null && u.getTelephone().equals(telephone))
                    .findFirst();
        }

        @Override
        public boolean existsByUsername(String username) {
            return findByUsername(username).isPresent();
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public boolean existsByTelephone(String telephone) {
            return findByTelephone(telephone).isPresent();
        }

        @Override
        public Optional<User> findByTeacherId(Long teacherId) {
            return store.values().stream()
                    .filter(u -> u.getTeacher() != null
                            && u.getTeacher().getId().equals(teacherId))
                    .findFirst();
        }

        @Override
        public Optional<User> findByStudentId(Long studentId) {
            return store.values().stream()
                    .filter(u -> u.getStudent() != null
                            && u.getStudent().getId().equals(studentId))
                    .findFirst();
        }
    }

    /* ==================== 关注关系 ==================== */

    public static class FakeUserFollowRepository
            extends InMemoryJpaRepository<UserFollow, Long>
            implements com.buctta.api.dao.UserFollowRepository {

        @Override
        protected Long getId(UserFollow entity) {
            return entity.getId();
        }

        @Override
        protected void setId(UserFollow entity, Long id) {
            entity.setId(id);
        }

        @Override
        public Optional<UserFollow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId) {
            return store.values().stream()
                    .filter(f -> f.getFollower().getId().equals(followerId)
                            && f.getFollowee().getId().equals(followeeId))
                    .findFirst();
        }

        @Override
        public boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId) {
            return findByFollowerIdAndFolloweeId(followerId, followeeId).isPresent();
        }

        @Override
        public void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId) {
            store.values().removeIf(f -> f.getFollower().getId().equals(followerId)
                    && f.getFollowee().getId().equals(followeeId));
        }

        @Override
        public Page<UserFollow> findByFollowerId(Long followerId, Pageable pageable) {
            List<UserFollow> list = store.values().stream()
                    .filter(f -> f.getFollower().getId().equals(followerId))
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        }

        @Override
        public Page<UserFollow> findByFolloweeId(Long followeeId, Pageable pageable) {
            List<UserFollow> list = store.values().stream()
                    .filter(f -> f.getFollowee().getId().equals(followeeId))
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        }

        @Override
        public long countByFollowerId(Long followerId) {
            return store.values().stream()
                    .filter(f -> f.getFollower().getId().equals(followerId))
                    .count();
        }

        @Override
        public long countByFolloweeId(Long followeeId) {
            return store.values().stream()
                    .filter(f -> f.getFollowee().getId().equals(followeeId))
                    .count();
        }

        @Override
        public List<Long> findFolloweeIds(Long followerId) {
            return store.values().stream()
                    .filter(f -> f.getFollower().getId().equals(followerId))
                    .map(f -> f.getFollowee().getId())
                    .toList();
        }
    }

    /* ==================== 教参 ==================== */

    public static class FakeTeachingMaterialRepository
            extends InMemoryJpaRepository<TeachingMaterial, Long>
            implements com.buctta.api.dao.TeachingMaterialRepository {

        @Override
        protected Long getId(TeachingMaterial entity) {
            return entity.getId();
        }

        @Override
        protected void setId(TeachingMaterial entity, Long id) {
            entity.setId(id);
        }

        @Override
        public Page<TeachingMaterial> findByTeacherId(Long teacherId, Pageable pageable) {
            List<TeachingMaterial> list = store.values().stream()
                    .filter(m -> teacherId.equals(m.getTeacherId()))
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        }

        @Override
        public Page<TeachingMaterial> findByCourseId(Long courseId, Pageable pageable) {
            List<TeachingMaterial> list = store.values().stream()
                    .filter(m -> courseId.equals(m.getCourseId()))
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        }

        @Override
        public Page<TeachingMaterial> findByIsPublicTrue(Pageable pageable) {
            List<TeachingMaterial> list = store.values().stream()
                    .filter(m -> Boolean.TRUE.equals(m.getIsPublic()))
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        }

        @Override
        public List<TeachingMaterial> findByCourseIdAndIsPublicTrue(Long courseId) {
            return store.values().stream()
                    .filter(m -> courseId.equals(m.getCourseId())
                            && Boolean.TRUE.equals(m.getIsPublic()))
                    .toList();
        }

        @Override
        public void deleteByCourseId(Long courseId) {
            store.values().removeIf(m -> courseId.equals(m.getCourseId()));
        }
    }

    /* ==================== 逐视频学习记录 ==================== */

    public static class FakeStudentVideoProgressRepository
            extends InMemoryJpaRepository<StudentVideoProgress, Long>
            implements com.buctta.api.dao.StudentVideoProgressRepository {

        @Override
        protected Long getId(StudentVideoProgress entity) {
            return entity.getId();
        }

        @Override
        protected void setId(StudentVideoProgress entity, Long id) {
            entity.setId(id);
        }

        @Override
        public Optional<StudentVideoProgress> findByStudentIdAndVideoId(Long studentId, Long videoId) {
            return store.values().stream()
                    .filter(p -> studentId.equals(p.getStudentId()) && videoId.equals(p.getVideoId()))
                    .findFirst();
        }

        @Override
        public List<StudentVideoProgress> findByStudentIdAndCourseId(Long studentId, Long courseId) {
            return store.values().stream()
                    .filter(p -> studentId.equals(p.getStudentId()) && courseId.equals(p.getCourseId()))
                    .toList();
        }

        @Override
        public List<StudentVideoProgress> findByStudentId(Long studentId) {
            return store.values().stream()
                    .filter(p -> studentId.equals(p.getStudentId()))
                    .toList();
        }

        @Override
        public long countByStudentIdAndVideoIdIn(Long studentId, List<Long> videoIds) {
            return store.values().stream()
                    .filter(p -> studentId.equals(p.getStudentId())
                            && videoIds.contains(p.getVideoId()))
                    .count();
        }

        @Override
        public long countByStudentIdAndCompletedTrue(Long studentId) {
            return store.values().stream()
                    .filter(p -> studentId.equals(p.getStudentId())
                            && Boolean.TRUE.equals(p.getCompleted()))
                    .count();
        }

        @Override
        public void deleteByVideoId(Long videoId) {
            store.values().removeIf(p -> videoId.equals(p.getVideoId()));
        }

        @Override
        public void deleteByCourseId(Long courseId) {
            store.values().removeIf(p -> courseId.equals(p.getCourseId()));
        }
    }

    /* ==================== 教师 ==================== */

    /**
     * 只实现申请审核用到的查询；其余方法保持基类/显式抛错，
     * 避免把"未实现"静默当成"查不到"。
     */
    public static class FakeTeacherRepository
            extends InMemoryJpaRepository<Teacher, Long>
            implements com.buctta.api.dao.TeacherReposit {

        @Override
        protected Long getId(Teacher entity) {
            return entity.getId();
        }

        @Override
        protected void setId(Teacher entity, Long id) {
            entity.setId(id);
        }

        @Override
        public Teacher findTeacherListById(long id) {
            return store.get(id);
        }

        @Override
        public Teacher findTeacherListByName(String name) {
            return store.values().stream()
                    .filter(t -> t.getName() != null && t.getName().equals(name))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public void deleteAllByIdIn(List<Long> ids) {
            for (Long id : ids) {
                store.remove(id);
            }
        }
    }

    /* ==================== 选课 / 播放进度 ==================== */

    /**
     * 复合主键（studentId, courseId）的内存实现，因此不复用 {@link InMemoryJpaRepository}。
     */
    public static class FakeStudentCourseRepository implements StudentCourseReposit {

        /** studentId → courseId → 选课记录 */
        private final Map<Long, Map<Long, StudentCourse>> store = new LinkedHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <S extends StudentCourse> S save(S entity) {
            store.computeIfAbsent(entity.getStudent().getId(), k -> new LinkedHashMap<>())
                    .put(entity.getCourse().getId(), (StudentCourse) entity);
            return entity;
        }

        @Override
        public <S extends StudentCourse> List<S> saveAll(Iterable<S> entities) {
            List<S> result = new ArrayList<>();
            for (S entity : entities) {
                result.add(save(entity));
            }
            return result;
        }

        @Override
        public Optional<StudentCourse> findById(StudentCourseId id) {
            return findByStudentIdAndCourseId(id.getStudentId(), id.getCourseId());
        }

        @Override
        public boolean existsById(StudentCourseId id) {
            return findById(id).isPresent();
        }

        @Override
        public void deleteById(StudentCourseId id) {
            Map<Long, StudentCourse> byCourse = store.get(id.getStudentId());
            if (byCourse != null) {
                byCourse.remove(id.getCourseId());
            }
        }

        @Override
        public void delete(StudentCourse entity) {
            deleteById(entity.getId());
        }

        @Override
        public List<StudentCourse> findAllById(Iterable<StudentCourseId> ids) {
            List<StudentCourse> result = new ArrayList<>();
            for (StudentCourseId id : ids) {
                findById(id).ifPresent(result::add);
            }
            return result;
        }

        @Override
        public List<StudentCourse> findByStudentId(Long studentId) {
            Map<Long, StudentCourse> byCourse = store.get(studentId);
            return byCourse == null ? List.of() : new ArrayList<>(byCourse.values());
        }

        @Override
        public Page<StudentCourse> findByStudentId(Long studentId, Pageable pageable) {
            throw new UnsupportedOperationException("测试未实现分页查询");
        }

        @Override
        public Page<StudentCourse> findByStudentIdAndIsViewed(Long studentId, Boolean isViewed,
                                                              Pageable pageable) {
            throw new UnsupportedOperationException("测试未实现分页查询");
        }

        @Override
        public Optional<StudentCourse> findByStudentIdAndCourseId(Long studentId, Long courseId) {
            Map<Long, StudentCourse> byCourse = store.get(studentId);
            return byCourse == null ? Optional.empty() : Optional.ofNullable(byCourse.get(courseId));
        }

        @Override
        public boolean existsByStudentIdAndCourseId(Long studentId, Long courseId) {
            return findByStudentIdAndCourseId(studentId, courseId).isPresent();
        }

        @Override
        public List<StudentCourse> findAll() {
            List<StudentCourse> result = new ArrayList<>();
            store.values().forEach(m -> result.addAll(m.values()));
            return result;
        }

        @Override
        public long count() {
            return store.values().stream().mapToLong(Map::size).sum();
        }

        @Override
        public Page<StudentCourse> findAll(Specification<StudentCourse> spec, Pageable pageable) {
            throw new UnsupportedOperationException("测试未实现条件分页查询");
        }

        /* ---------- 以下成员测试用不到，显式抛错以免被误当作"返回空结果" ---------- */

        @Override
        public void deleteAll() {
            store.clear();
        }

        @Override
        public void deleteAll(Iterable<? extends StudentCourse> entities) {
            for (StudentCourse entity : entities) {
                delete(entity);
            }
        }

        @Override
        public void deleteAllById(Iterable<? extends StudentCourseId> ids) {
            for (StudentCourseId id : ids) {
                deleteById(id);
            }
        }

        @Override
        public void flush() {
            // 内存实现无需 flush
        }

        @Override
        public <S extends StudentCourse> S saveAndFlush(S entity) {
            return save(entity);
        }

        @Override
        public <S extends StudentCourse> List<S> saveAllAndFlush(Iterable<S> entities) {
            return saveAll(entities);
        }

        @Override
        public void deleteAllInBatch(Iterable<StudentCourse> entities) {
            deleteAll(entities);
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<StudentCourseId> ids) {
            deleteAllById(ids);
        }

        @Override
        public void deleteAllInBatch() {
            deleteAll();
        }

        @Override
        public StudentCourse getOne(StudentCourseId id) {
            return findById(id).orElse(null);
        }

        @Override
        public StudentCourse getById(StudentCourseId id) {
            return findById(id).orElse(null);
        }

        @Override
        public StudentCourse getReferenceById(StudentCourseId id) {
            return findById(id).orElse(null);
        }

        @Override
        public List<StudentCourse> findAll(org.springframework.data.domain.Sort sort) {
            return findAll();
        }

        @Override
        public Page<StudentCourse> findAll(Pageable pageable) {
            throw new UnsupportedOperationException("测试未实现分页查询");
        }

        /* ---------- QueryByExampleExecutor ---------- */

        @Override
        public <S extends StudentCourse> Optional<S> findOne(Example<S> example) {
            throw new UnsupportedOperationException("测试未实现 Example 查询");
        }

        @Override
        public <S extends StudentCourse> List<S> findAll(Example<S> example) {
            throw new UnsupportedOperationException("测试未实现 Example 查询");
        }

        @Override
        public <S extends StudentCourse> List<S> findAll(Example<S> example,
                                                         org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException("测试未实现 Example 查询");
        }

        @Override
        public <S extends StudentCourse> Page<S> findAll(Example<S> example, Pageable pageable) {
            throw new UnsupportedOperationException("测试未实现 Example 查询");
        }

        @Override
        public <S extends StudentCourse> long count(Example<S> example) {
            throw new UnsupportedOperationException("测试未实现 Example 查询");
        }

        @Override
        public <S extends StudentCourse> boolean exists(Example<S> example) {
            throw new UnsupportedOperationException("测试未实现 Example 查询");
        }

        @Override
        public <S extends StudentCourse, R> R findBy(Example<S> example,
                                                     java.util.function.Function<
                                                             org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R>
                                                             queryFunction) {
            throw new UnsupportedOperationException("测试未实现 Example 查询");
        }

        /* ---------- JpaSpecificationExecutor ---------- */

        @Override
        public Optional<StudentCourse> findOne(Specification<StudentCourse> spec) {
            throw new UnsupportedOperationException("测试未实现条件查询");
        }

        @Override
        public List<StudentCourse> findAll(Specification<StudentCourse> spec) {
            throw new UnsupportedOperationException("测试未实现条件查询");
        }

        @Override
        public Page<StudentCourse> findAll(Specification<StudentCourse> spec,
                                           Specification<StudentCourse> countSpec, Pageable pageable) {
            throw new UnsupportedOperationException("测试未实现条件查询");
        }

        @Override
        public List<StudentCourse> findAll(Specification<StudentCourse> spec,
                                           org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException("测试未实现条件查询");
        }

        @Override
        public long count(Specification<StudentCourse> spec) {
            throw new UnsupportedOperationException("测试未实现条件查询");
        }

        @Override
        public boolean exists(Specification<StudentCourse> spec) {
            throw new UnsupportedOperationException("测试未实现条件查询");
        }

        @Override
        public long update(org.springframework.data.jpa.domain.UpdateSpecification<StudentCourse> spec) {
            throw new UnsupportedOperationException("测试未实现批量更新");
        }

        @Override
        public long delete(org.springframework.data.jpa.domain.DeleteSpecification<StudentCourse> spec) {
            throw new UnsupportedOperationException("测试未实现批量删除");
        }

        @Override
        public <S extends StudentCourse, R> R findBy(Specification<StudentCourse> spec,
                                                     java.util.function.Function<? super org.springframework.data.jpa.repository.JpaSpecificationExecutor.SpecificationFluentQuery<S>, R>
                                                             queryFunction) {
            throw new UnsupportedOperationException("测试未实现条件查询");
        }
    }
}
