package com.shop.clothingstore.service.base;

import java.util.List;
import java.util.Optional;

import com.shop.clothingstore.entity.base.BaseEntity;
import com.shop.clothingstore.repository.base.BaseRepository;
import com.shop.clothingstore.service.GenericService;

public abstract class GenericServiceImpl<T extends BaseEntity, ID>
        implements GenericService<T, ID> {

    protected final BaseRepository<T, ID> repository;

    protected GenericServiceImpl(BaseRepository<T, ID> repository) {
        this.repository = repository;
    }

    @Override
    public List<T> findAll() {
        return repository.findAll();
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
