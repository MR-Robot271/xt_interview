package com.xt.interview.service.impl;

import com.xt.interview.dto.DepartmentDTO;
import com.xt.interview.entity.Department;
import com.xt.interview.repository.DepartmentRepository;
import com.xt.interview.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    private static final int NAME_MAX_LENGTH = 100;
    private static final int CODE_MAX_LENGTH = 50;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public DepartmentDTO createDepartment(DepartmentDTO dto) {
        String name = validateName(dto.getName());
        String code = validateCode(dto.getCode());
        String description = validateDescription(dto.getDescription());
        Integer status = validateStatus(dto.getStatus());

        if (departmentRepository.existsByCodeAndDeleted(code, NOT_DELETED)) {
            throw new RuntimeException("部门编码已存在");
        }

        Department department = new Department();
        department.setName(name);
        department.setCode(code);
        department.setDescription(description);
        department.setStatus(status == null ? 1 : status);
        department.setDeleted(NOT_DELETED);
        department = departmentRepository.save(department);
        return convertToDTO(department);
    }

    @Override
    public DepartmentDTO updateDepartment(Long id, DepartmentDTO dto) {
        Department department = departmentRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("部门不存在"));

        String name = validateName(dto.getName());
        String code = validateCode(dto.getCode());
        String description = validateDescription(dto.getDescription());
        Integer status = validateStatus(dto.getStatus());

        if (!code.equals(department.getCode())
                && departmentRepository.existsByCodeAndDeleted(code, NOT_DELETED)) {
            throw new RuntimeException("部门编码已存在");
        }

        department.setName(name);
        department.setCode(code);
        department.setDescription(description);
        if (status != null) {
            department.setStatus(status);
        }
        department = departmentRepository.save(department);
        return convertToDTO(department);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        department.setDeleted(DELETED);
        departmentRepository.save(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        return convertToDTO(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findByDeleted(NOT_DELETED).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private String validateName(String value) {
        String name = trimToNull(value);
        if (name == null) {
            throw new RuntimeException("部门名称不能为空");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new RuntimeException("部门名称长度不能超过" + NAME_MAX_LENGTH + "个字符");
        }
        return name;
    }

    private String validateCode(String value) {
        String code = trimToNull(value);
        if (code == null) {
            throw new RuntimeException("部门编码不能为空");
        }
        if (code.length() > CODE_MAX_LENGTH) {
            throw new RuntimeException("部门编码长度不能超过" + CODE_MAX_LENGTH + "个字符");
        }
        return code;
    }

    private String validateDescription(String value) {
        String description = trimToNull(value);
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new RuntimeException("部门描述长度不能超过" + DESCRIPTION_MAX_LENGTH + "个字符");
        }
        return description;
    }

    private Integer validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new RuntimeException("部门状态只能为0（禁用）或1（启用）");
        }
        return status;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DepartmentDTO convertToDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setCode(department.getCode());
        dto.setDescription(department.getDescription());
        dto.setStatus(department.getStatus());
        dto.setCreateTime(department.getCreateTime());
        dto.setUpdateTime(department.getUpdateTime());
        return dto;
    }
}
