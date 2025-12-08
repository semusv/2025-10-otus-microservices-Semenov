package ru.vvsem.service_user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vvsem.service_user.model.User;

public interface UserRepository extends JpaRepository<User, Long> {}
