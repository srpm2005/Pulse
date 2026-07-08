package com.pulse.repository;

import com.pulse.entity.PriceAlert;
import com.pulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByUser(User user);
    List<PriceAlert> findByTriggeredFalse();
}
