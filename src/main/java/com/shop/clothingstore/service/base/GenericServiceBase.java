package com.shop.clothingstore.service.base;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.clothingstore.entity.base.BaseEntity;
import com.shop.clothingstore.repository.base.BaseRepository;
import com.shop.clothingstore.service.GenericService;

@SuppressWarnings("null")
public abstract class GenericServiceBase<T extends BaseEntity, ID>
        implements GenericService<T, ID> {

    protected final BaseRepository<T, ID> repository;

    protected GenericServiceBase(BaseRepository<T, ID> repository) {
        this.repository = repository;
    }

    @Override
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Optional<T> findById(ID id) {
        return repository.findById(id);
    }

    @Override
    public T save(T entity) {
        return repository.save(entity);
    }

    @Override
    public void delete(ID id) {
        repository.deleteById(id);
    }
}
