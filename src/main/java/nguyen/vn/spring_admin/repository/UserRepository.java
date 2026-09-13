package nguyen.vn.spring_admin.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import nguyen.vn.spring_admin.entity.Role;
import nguyen.vn.spring_admin.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    long countByActiveTrue();

    @Query("select u from User u where lower(u.username) like :pattern escape '!' "
            + "or lower(u.fullName) like :pattern escape '!' or lower(u.email) like :pattern escape '!'")
    Page<User> search(@Param("pattern") String pattern, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.role = :role and u.active = true order by u.id")
    List<User> lockActiveAdministrators(@Param("role") Role role);
}
