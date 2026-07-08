package com.pulse.repository;

import com.pulse.entity.TrackedStock;
import com.pulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackedStockRepository extends JpaRepository<TrackedStock, Long> {
    List<TrackedStock> findByUser(User user);
    Optional<TrackedStock> findByUserAndSymbol(User user, String symbol);
}
