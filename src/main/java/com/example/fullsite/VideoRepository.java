package com.example.fullsite;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
//User repository setup
public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByUserId(Long userId);
}
