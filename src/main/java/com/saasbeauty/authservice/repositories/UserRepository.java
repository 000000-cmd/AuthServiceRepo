package com.saasbeauty.authservice.repositories;

import com.saasbeauty.authservice.dto.internal.ThirdPartyBasicInfoDTO;
import com.saasbeauty.authservice.dto.response.LoginResponseDTO;
import com.saasbeauty.authservice.entities.User;
import com.saasbeauty.authservice.entities.UserRole;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;
import java.util.stream.Collectors;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Override
    <S extends User> S save(S entity);

    @Override
    Optional<User> findById(UUID uuid);

    @Override
    boolean existsById(UUID uuid);

    @Override
    long count();

    @Override
    void deleteById(UUID uuid);

    @Override
    void delete(User entity);

    @Override
    void deleteAllById(Iterable<? extends UUID> uuids);

    @Override
    void deleteAll(Iterable<? extends User> entities);

    @Override
    void deleteAll();

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String mail);

}
