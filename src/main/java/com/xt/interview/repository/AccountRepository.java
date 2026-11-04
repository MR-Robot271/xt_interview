package com.xt.interview.repository;

import com.xt.interview.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByDeleted(Integer deleted);
    Optional<Account> findByIdAndDeleted(Long id, Integer deleted);
    Optional<Account> findByUsernameAndDeleted(String username, Integer deleted);
    Optional<Account> findByUsernameAndPasswordAndDeleted(String username, String password, Integer deleted);
    boolean existsByUsernameAndDeleted(String username, Integer deleted);

    /**
     * 分页查询：三个条件都是可选的，传 null 表示不参与过滤。
     *
     * @param keyword   用户名模糊匹配关键字（调用方已做 trim，并保证 null 表示不过滤）
     * @param status    状态：0-禁用，1-启用
     * @param roleLevel 权限级别
     */
    @Query("SELECT a FROM Account a WHERE a.deleted = 0 " +
           "AND (:keyword IS NULL OR a.username LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:roleLevel IS NULL OR a.roleLevel = :roleLevel)")
    Page<Account> search(@Param("keyword") String keyword,
                         @Param("status") Integer status,
                         @Param("roleLevel") Integer roleLevel,
                         Pageable pageable);
}
