package com.xt.interview.service.impl;

import com.xt.interview.dto.DepartmentDTO;
import com.xt.interview.entity.Department;
import com.xt.interview.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 部门管理 Service 层单元测试（Repository 使用 Mockito 模拟）。
 */
@DisplayName("部门管理 Service 单元测试")
class DepartmentServiceImplTest {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ==================== 新增部门 ====================

    @Test
    @DisplayName("新增部门：去除首尾空格，保存未删除记录并返回带ID的DTO")
    void createDepartment_shouldTrimInputAndReturnPersistedDto() {
        when(departmentRepository.existsByCodeAndDeleted("RD", NOT_DELETED)).thenReturn(false);
        stubSave();

        DepartmentDTO result = departmentService.createDepartment(
                buildDto("  研发部  ", "  RD  ", "  负责产品研发  ", 1));

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        Department saved = captor.getValue();
        assertEquals("研发部", saved.getName());
        assertEquals("RD", saved.getCode());
        assertEquals("负责产品研发", saved.getDescription());
        assertEquals(1, saved.getStatus().intValue());
        assertEquals(NOT_DELETED, saved.getDeleted());

        assertEquals(100L, result.getId().longValue());
        assertEquals("研发部", result.getName());
        assertEquals("RD", result.getCode());
        assertEquals(1, result.getStatus().intValue());
    }

    @Test
    @DisplayName("新增部门：status 为空时默认启用（1）")
    void createDepartment_shouldDefaultStatusToEnabled() {
        when(departmentRepository.existsByCodeAndDeleted("RD", NOT_DELETED)).thenReturn(false);
        stubSave();

        departmentService.createDepartment(buildDto("研发部", "RD", null, null));

        assertEquals(1, captureSaved().getStatus().intValue());
    }

    @Test
    @DisplayName("新增部门：显式传入 status=0 时必须保持禁用，不能被默认值覆盖")
    void createDepartment_shouldKeepExplicitlyDisabledStatus() {
        when(departmentRepository.existsByCodeAndDeleted("RD", NOT_DELETED)).thenReturn(false);
        stubSave();

        departmentService.createDepartment(buildDto("研发部", "RD", null, 0));

        assertEquals(0, captureSaved().getStatus().intValue());
    }

    @Test
    @DisplayName("新增部门：空白描述统一存为 null")
    void createDepartment_shouldNormalizeBlankDescriptionToNull() {
        when(departmentRepository.existsByCodeAndDeleted("RD", NOT_DELETED)).thenReturn(false);
        stubSave();

        departmentService.createDepartment(buildDto("研发部", "RD", "   ", 1));

        assertNull(captureSaved().getDescription());
    }

    @Test
    @DisplayName("新增部门：名称为 null 或空白时拒绝")
    void createDepartment_shouldRejectBlankName() {
        RuntimeException nullName = assertThrows(RuntimeException.class,
                () -> departmentService.createDepartment(buildDto(null, "RD", null, 1)));
        assertEquals("部门名称不能为空", nullName.getMessage());

        RuntimeException blankName = assertThrows(RuntimeException.class,
                () -> departmentService.createDepartment(buildDto("   ", "RD", null, 1)));
        assertEquals("部门名称不能为空", blankName.getMessage());

        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("新增部门：编码为 null 或空白时拒绝")
    void createDepartment_shouldRejectBlankCode() {
        RuntimeException nullCode = assertThrows(RuntimeException.class,
                () -> departmentService.createDepartment(buildDto("研发部", null, null, 1)));
        assertEquals("部门编码不能为空", nullCode.getMessage());

        RuntimeException blankCode = assertThrows(RuntimeException.class,
                () -> departmentService.createDepartment(buildDto("研发部", "  ", null, 1)));
        assertEquals("部门编码不能为空", blankCode.getMessage());

        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("新增部门：编码已存在时拒绝，且不落库")
    void createDepartment_shouldRejectDuplicateCode() {
        when(departmentRepository.existsByCodeAndDeleted("RD", NOT_DELETED)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> departmentService.createDepartment(buildDto("研发二部", "RD", null, 1)));

        assertEquals("部门编码已存在", ex.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("新增部门：名称超过100字符时拒绝")
    void createDepartment_shouldRejectTooLongName() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> departmentService.createDepartment(buildDto("名".repeat(101), "RD", null, 1)));

        assertEquals("部门名称长度不能超过100个字符", ex.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("新增部门：编码超过50字符时拒绝")
    void createDepartment_shouldRejectTooLongCode() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> departmentService.createDepartment(buildDto("研发部", "C".repeat(51), null, 1)));

        assertEquals("部门编码长度不能超过50个字符", ex.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("新增部门：描述超过500字符时拒绝")
    void createDepartment_shouldRejectTooLongDescription() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> departmentService.createDepartment(buildDto("研发部", "RD", "描".repeat(501), 1)));

        assertEquals("部门描述长度不能超过500个字符", ex.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("新增部门：恰好等于字段上限时允许新增")
    void createDepartment_shouldAcceptValuesAtMaxLength() {
        String name = "名".repeat(100);
        String code = "C".repeat(50);
        String description = "描".repeat(500);
        when(departmentRepository.existsByCodeAndDeleted(code, NOT_DELETED)).thenReturn(false);
        stubSave();

        DepartmentDTO result = departmentService.createDepartment(buildDto(name, code, description, 1));

        assertEquals(name, result.getName());
        assertEquals(code, result.getCode());
        assertEquals(description, result.getDescription());
    }

    @Test
    @DisplayName("新增部门：status 非 0/1 时拒绝")
    void createDepartment_shouldRejectInvalidStatus() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> departmentService.createDepartment(buildDto("研发部", "RD", null, 2)));

        assertEquals("部门状态只能为0（禁用）或1（启用）", ex.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    // ==================== 更新部门 ====================

    @Test
    @DisplayName("更新部门：名称、描述、状态被更新，编码未变化时不做重复校验")
    void updateDepartment_shouldApplyChanges() {
        Department existing = buildExisting(7L, "研发部", "RD", 1, NOT_DELETED);
        when(departmentRepository.findByIdAndDeleted(7L, NOT_DELETED)).thenReturn(Optional.of(existing));
        stubSave();

        DepartmentDTO result = departmentService.updateDepartment(
                7L, buildDto("  研发中心  ", "RD", "新的描述", 0));

        assertEquals("研发中心", result.getName());
        assertEquals("RD", result.getCode());
        assertEquals("新的描述", result.getDescription());
        assertEquals(0, result.getStatus().intValue());
        verify(departmentRepository, never()).existsByCodeAndDeleted(anyString(), anyInt());
    }

    @Test
    @DisplayName("更新部门：编码改为未被占用的新编码时允许")
    void updateDepartment_shouldAllowChangingToUnusedCode() {
        Department existing = buildExisting(7L, "研发部", "RD", 1, NOT_DELETED);
        when(departmentRepository.findByIdAndDeleted(7L, NOT_DELETED)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByCodeAndDeleted("OPS", NOT_DELETED)).thenReturn(false);
        stubSave();

        DepartmentDTO result = departmentService.updateDepartment(
                7L, buildDto("研发部", "OPS", null, null));

        assertEquals("OPS", result.getCode());
        verify(departmentRepository).existsByCodeAndDeleted("OPS", NOT_DELETED);
    }

    @Test
    @DisplayName("更新部门：编码被其他部门占用时拒绝")
    void updateDepartment_shouldRejectCodeUsedByAnotherDepartment() {
        Department existing = buildExisting(7L, "研发部", "RD", 1, NOT_DELETED);
        when(departmentRepository.findByIdAndDeleted(7L, NOT_DELETED)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByCodeAndDeleted("OPS", NOT_DELETED)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> departmentService.updateDepartment(7L, buildDto("研发部", "OPS", null, 1)));

        assertEquals("部门编码已存在", ex.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("更新部门：ID 不存在时拒绝")
    void updateDepartment_shouldRejectUnknownDepartment() {
        when(departmentRepository.findByIdAndDeleted(99L, NOT_DELETED)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> departmentService.updateDepartment(99L, buildDto("研发部", "RD", null, 1)));

        assertEquals("部门不存在", ex.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("更新部门：查询时带 deleted=0，软删除的部门不能被更新")
    void updateDepartment_shouldOnlyLookUpUndeletedDepartment() {
        when(departmentRepository.findByIdAndDeleted(7L, NOT_DELETED)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> departmentService.updateDepartment(7L, buildDto("研发部", "RD", null, 1)));

        verify(departmentRepository).findByIdAndDeleted(7L, NOT_DELETED);
    }

    @Test
    @DisplayName("更新部门：status 为空时保留原有状态")
    void updateDepartment_shouldKeepExistingStatusWhenNull() {
        Department existing = buildExisting(7L, "研发部", "RD", 0, NOT_DELETED);
        when(departmentRepository.findByIdAndDeleted(7L, NOT_DELETED)).thenReturn(Optional.of(existing));
        stubSave();

        DepartmentDTO result = departmentService.updateDepartment(
                7L, buildDto("研发部", "RD", null, null));

        assertEquals(0, result.getStatus().intValue());
    }

    @Test
    @DisplayName("更新部门：名称空白时拒绝")
    void updateDepartment_shouldRejectBlankName() {
        Department existing = buildExisting(7L, "研发部", "RD", 1, NOT_DELETED);
        when(departmentRepository.findByIdAndDeleted(7L, NOT_DELETED)).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> departmentService.updateDepartment(7L, buildDto("   ", "RD", null, 1)));

        assertEquals("部门名称不能为空", ex.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("更新部门：描述超长时拒绝")
    void updateDepartment_shouldRejectTooLongDescription() {
        Department existing = buildExisting(7L, "研发部", "RD", 1, NOT_DELETED);
        when(departmentRepository.findByIdAndDeleted(7L, NOT_DELETED)).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> departmentService.updateDepartment(7L, buildDto("研发部", "RD", "描".repeat(501), 1)));

        assertEquals("部门描述长度不能超过500个字符", ex.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    // ==================== 删除部门（软删除） ====================

    @Test
    @DisplayName("删除部门：置 deleted=1 并保存，不物理删除")
    void deleteDepartment_shouldSoftDelete() {
        Department existing = buildExisting(3L, "运维部", "OPS", 1, NOT_DELETED);
        when(departmentRepository.findByIdAndDeleted(3L, NOT_DELETED)).thenReturn(Optional.of(existing));
        stubSave();

        departmentService.deleteDepartment(3L);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        assertEquals(DELETED, captor.getValue().getDeleted());
        assertEquals(3L, captor.getValue().getId().longValue());
        verify(departmentRepository, never()).delete(any(Department.class));
        verify(departmentRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除部门：ID 不存在时拒绝")
    void deleteDepartment_shouldRejectUnknownDepartment() {
        when(departmentRepository.findByIdAndDeleted(99L, NOT_DELETED)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> departmentService.deleteDepartment(99L));

        assertEquals("部门不存在", ex.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    // ==================== 查询部门 ====================

    @Test
    @DisplayName("根据ID查询：返回部门详情")
    void getDepartmentById_shouldReturnDto() {
        Department existing = buildExisting(1L, "研发部", "RD", 1, NOT_DELETED);
        when(departmentRepository.findByIdAndDeleted(1L, NOT_DELETED)).thenReturn(Optional.of(existing));

        DepartmentDTO result = departmentService.getDepartmentById(1L);

        assertEquals(1L, result.getId().longValue());
        assertEquals("研发部", result.getName());
        assertEquals("RD", result.getCode());
        assertEquals(1, result.getStatus().intValue());
    }

    @Test
    @DisplayName("根据ID查询：ID 不存在时拒绝")
    void getDepartmentById_shouldRejectUnknownDepartment() {
        when(departmentRepository.findByIdAndDeleted(99L, NOT_DELETED)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> departmentService.getDepartmentById(99L));

        assertEquals("部门不存在", ex.getMessage());
    }

    @Test
    @DisplayName("查询全部：只返回未删除的部门")
    void getAllDepartments_shouldReturnOnlyUndeleted() {
        when(departmentRepository.findByDeleted(NOT_DELETED)).thenReturn(List.of(
                buildExisting(1L, "研发部", "RD", 1, NOT_DELETED),
                buildExisting(2L, "运维部", "OPS", 0, NOT_DELETED)));

        List<DepartmentDTO> result = departmentService.getAllDepartments();

        assertEquals(2, result.size());
        assertEquals("研发部", result.get(0).getName());
        assertEquals("运维部", result.get(1).getName());
        verify(departmentRepository).findByDeleted(NOT_DELETED);
    }

    @Test
    @DisplayName("查询全部：没有数据时返回空列表")
    void getAllDepartments_shouldReturnEmptyList() {
        when(departmentRepository.findByDeleted(NOT_DELETED)).thenReturn(Collections.emptyList());

        List<DepartmentDTO> result = departmentService.getAllDepartments();

        assertTrue(result.isEmpty());
    }

    // ==================== 辅助方法 ====================

    private DepartmentDTO buildDto(String name, String code, String description, Integer status) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setName(name);
        dto.setCode(code);
        dto.setDescription(description);
        dto.setStatus(status);
        return dto;
    }

    private Department buildExisting(Long id, String name, String code, Integer status, Integer deleted) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setCode(code);
        department.setStatus(status);
        department.setDeleted(deleted);
        department.setCreateTime(LocalDateTime.now().minusDays(1));
        department.setUpdateTime(LocalDateTime.now().minusDays(1));
        return department;
    }

    private void stubSave() {
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(100L);
            }
            saved.setCreateTime(LocalDateTime.now());
            saved.setUpdateTime(LocalDateTime.now());
            return saved;
        });
    }

    private Department captureSaved() {
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        return captor.getValue();
    }
}
