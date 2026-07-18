package com.socialanalytics.profile.repository;

import com.socialanalytics.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByNombre(String nombre);

    List<Profile> findByCategoria(String categoria);

    @Query("SELECT DISTINCT p.categoria FROM Profile p")
    List<String> findAllCategories();

    @Query("SELECT p.categoria, COUNT(p) FROM Profile p GROUP BY p.categoria")
    List<Object[]> countByCategory();
}
