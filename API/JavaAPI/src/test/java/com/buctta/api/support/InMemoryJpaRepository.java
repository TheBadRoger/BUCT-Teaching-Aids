package com.buctta.api.support;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.DeleteSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.UpdateSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.FluentQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 内存版仓储基类：实现 {@link JpaRepository} 与 {@link JpaSpecificationExecutor}
 * 的全部成员，业务用不到的（条件查询、Example 查询、批量 update/delete）统一抛
 * {@link UnsupportedOperationException}，避免被误当成"返回空结果"。
 * <p>
 * 之所以完整实现接口而不是"只提供同名方法"：测试注入走反射写字段，字段类型是仓储接口，
 * 因此 fake 必须真的是该接口的实例。
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public abstract class InMemoryJpaRepository<T, ID>
        implements JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /** 按插入顺序保存，保证 findAll 结果稳定 */
    protected final Map<ID, T> store = new LinkedHashMap<>();

    private final AtomicLong sequence = new AtomicLong(0);

    /** 由子类提供实体主键的读写方式 */
    protected abstract ID getId(T entity);

    protected abstract void setId(T entity, ID id);

    @SuppressWarnings("unchecked")
    protected ID nextId() {
        return (ID) Long.valueOf(sequence.incrementAndGet());
    }

    private UnsupportedOperationException unsupported(String method) {
        return new UnsupportedOperationException("测试内存仓储未实现: " + method);
    }

    /* ==================== 基本 CRUD ==================== */

    @Override
    public <S extends T> S save(S entity) {
        if (getId(entity) == null) {
            setId(entity, nextId());
        }
        store.put(getId(entity), entity);
        return entity;
    }

    @Override
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        for (S entity : entities) {
            result.add(save(entity));
        }
        return result;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(ID id) {
        return store.containsKey(id);
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        List<T> result = new ArrayList<>();
        for (ID id : ids) {
            T entity = store.get(id);
            if (entity != null) {
                result.add(entity);
            }
        }
        return result;
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteById(ID id) {
        store.remove(id);
    }

    @Override
    public void delete(T entity) {
        store.remove(getId(entity));
    }

    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        for (ID id : ids) {
            store.remove(id);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        for (T entity : entities) {
            store.remove(getId(entity));
        }
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    @Override
    public void flush() {
        // 内存实现无需 flush
    }

    @Override
    public <S extends T> S saveAndFlush(S entity) {
        return save(entity);
    }

    @Override
    public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) {
        return saveAll(entities);
    }

    @Override
    public void deleteAllInBatch(Iterable<T> entities) {
        deleteAll(entities);
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<ID> ids) {
        deleteAllById(ids);
    }

    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }

    @Override
    public T getOne(ID id) {
        return findById(id).orElse(null);
    }

    @Override
    public T getById(ID id) {
        return findById(id).orElse(null);
    }

    @Override
    public T getReferenceById(ID id) {
        return findById(id).orElse(null);
    }

    @Override
    public List<T> findAll(Sort sort) {
        return findAll();
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        throw unsupported("findAll(Pageable)");
    }

    /* ==================== QueryByExampleExecutor ==================== */

    @Override
    public <S extends T> Optional<S> findOne(Example<S> example) {
        throw unsupported("findOne(Example)");
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example) {
        throw unsupported("findAll(Example)");
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
        throw unsupported("findAll(Example, Sort)");
    }

    @Override
    public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw unsupported("findAll(Example, Pageable)");
    }

    @Override
    public <S extends T> long count(Example<S> example) {
        throw unsupported("count(Example)");
    }

    @Override
    public <S extends T> boolean exists(Example<S> example) {
        throw unsupported("exists(Example)");
    }

    @Override
    public <S extends T, R> R findBy(Example<S> example,
                                     Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        throw unsupported("findBy(Example, Function)");
    }

    /* ==================== JpaSpecificationExecutor ==================== */

    @Override
    public Optional<T> findOne(Specification<T> spec) {
        throw unsupported("findOne(Specification)");
    }

    @Override
    public List<T> findAll(Specification<T> spec) {
        throw unsupported("findAll(Specification)");
    }

    @Override
    public Page<T> findAll(Specification<T> spec, Pageable pageable) {
        throw unsupported("findAll(Specification, Pageable)");
    }

    @Override
    public Page<T> findAll(Specification<T> spec, Specification<T> countSpec, Pageable pageable) {
        throw unsupported("findAll(Specification, Specification, Pageable)");
    }

    @Override
    public List<T> findAll(Specification<T> spec, Sort sort) {
        throw unsupported("findAll(Specification, Sort)");
    }

    @Override
    public long count(Specification<T> spec) {
        throw unsupported("count(Specification)");
    }

    @Override
    public boolean exists(Specification<T> spec) {
        throw unsupported("exists(Specification)");
    }

    @Override
    public long update(UpdateSpecification<T> spec) {
        throw unsupported("update(UpdateSpecification)");
    }

    @Override
    public long delete(DeleteSpecification<T> spec) {
        throw unsupported("delete(DeleteSpecification)");
    }

    @Override
    public <S extends T, R> R findBy(Specification<T> spec,
                                     Function<? super JpaSpecificationExecutor.SpecificationFluentQuery<S>, R>
                                             queryFunction) {
        throw unsupported("findBy(Specification, Function)");
    }
}
