// FICHIER NON MODIFIABLE
package com.mototrip.repository;

import com.mototrip.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {}

