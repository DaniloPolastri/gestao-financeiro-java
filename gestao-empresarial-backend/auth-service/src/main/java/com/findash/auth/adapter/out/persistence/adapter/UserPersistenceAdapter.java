package com.findash.auth.adapter.out.persistence.adapter;

import com.findash.auth.adapter.out.persistence.entity.UserEntity;
import com.findash.auth.adapter.out.persistence.repository.JpaUserRepository;
import com.findash.auth.domain.model.User;
import com.findash.auth.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final JpaUserRepository jpaRepo;

    public UserPersistenceAdapter(JpaUserRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepo.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepo.existsByEmail(email);
    }

    private UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setActive(user.isActive());
        return entity;
    }

    private User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getName(), entity.getEmail(),
                        entity.getPassword(), entity.isActive(),
                        entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
