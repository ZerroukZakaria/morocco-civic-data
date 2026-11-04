package io.github.zerroukzakaria.civicdata.election;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionRepository extends JpaRepository<Election, Long> {

    Optional<Election> findBySlug(String slug);

    List<Election> findAllByOrderByElectionDateDesc();
}