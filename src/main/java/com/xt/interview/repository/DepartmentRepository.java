package com.xt.interview.repository;

import com.xt.interview.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByDeleted(Integer deleted);
    Optional<Department> findByIdAndDeleted(Long id, Integer deleted);
    Optional<Department> findByCodeAndDeleted(String code, Integer deleted);
    boolean existsByCodeAndDeleted(String code, Integer deleted);
}
