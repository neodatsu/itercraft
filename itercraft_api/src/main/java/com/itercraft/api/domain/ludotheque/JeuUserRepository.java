package com.itercraft.api.domain.ludotheque;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JeuUserRepository extends JpaRepository<JeuUser, UUID> {

    List<JeuUser> findByUserSub(String userSub);

    Optional<JeuUser> findByUserSubAndJeuId(String userSub, UUID jeuId);

    boolean existsByUserSub(String userSub);

    @Query("SELECT COUNT(ju) FROM JeuUser ju WHERE ju.userSub = :userSub")
    long countByUserSub(String userSub);

    void deleteByUserSubAndJeuId(String userSub, UUID jeuId);

    @Query("SELECT ju FROM JeuUser ju WHERE ju.userSub = :userSub AND ju.note IS NOT NULL")
    List<JeuUser> findRatedByUserSub(String userSub);
}
