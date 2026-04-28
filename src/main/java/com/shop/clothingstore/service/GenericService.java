package com.shop.clothingstore.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GenericService<T, ID> {

    List<T> findAll();

    Page<T> findAll(Pageable pageable);

    Optional<T> findById(ID id);

    T save(T entity);

    void delete(ID id);
}
