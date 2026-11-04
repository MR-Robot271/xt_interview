package com.xt.interview.service.impl;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.PageResult;
import com.xt.interview.entity.Account;
import com.xt.interview.entity.Department;
import com.xt.interview.repository.AccountRepository;
import com.xt.interview.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户-部门关联的 Service 层单元测试。
 */
@DisplayName("用户管理 Service 单元测试（部门关联）")
class AccountServiceImplTest {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ==================== 新增用户 ====================

    @Test
    @DisplayName("新增用户：保存所属部门ID，并在DTO中返回部门名称")
    void createAccount_shouldSaveDepartmentAndReturnDepartmentName() {
        when(departmentRepository.findByIdAndDeleted(1L, NOT_DELETED))
                .thenReturn(Optional.of(department(1L, "研发部", NOT_DELETED)));
        stubAccountSave(100L);

        AccountDTO result = accountService.createAccount(accountDto("zhangsan", 1L));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getDepartmentId().longValue());

        assertEquals(1L, result.getDepartmentId().longValue());
        assertEquals("研发部", result.getDepartmentName());
    }

    @Test
    @DisplayName("新增用户：不选部门时 department_id 为 null，且不查部门")
    void createAccount_shouldAllowNullDepartment() {
        stubAccountSave(101L);

        AccountDTO result = accountService.createAccount(accountDto("lisi", null));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertNull(captor.getValue().getDepartmentId());
        assertNull(result.getDepartmentId());
        assertNull(result.getDepartmentName());
        verify(departmentRepository, never()).findByIdAndDeleted(anyLong(), anyInt());
    }

    @Test
    @DisplayName("新增用户：部门不存在时拒绝，且不落库")
    void createAccount_shouldRejectUnknownDepartment() {
        when(departmentRepository.findByIdAndDeleted(99L, NOT_DELETED)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.createAccount(accountDto("wangwu", 99L)));

        assertEquals("所属部门不存在", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    // ==================== 更新用户 ====================

    @Test
    @DisplayName("更新用户：可以改到另一个部门")
    void updateAccount_shouldChangeDepartment() {
        Account existing = account(7L, "zhangsan", 1L);
        when(accountRepository.findByIdAndDeleted(7L, NOT_DELETED)).thenReturn(Optional.of(existing));
        when(departmentRepository.findByIdAndDeleted(2L, NOT_DELETED))
                .thenReturn(Optional.of(department(2L, "运维部", NOT_DELETED)));
        stubAccountSave(7L);

        AccountDTO result = accountService.updateAccount(7L, accountDto("zhangsan", 2L));

        assertEquals(2L, result.getDepartmentId().longValue());
        assertEquals("运维部", result.getDepartmentName());
        assertEquals(2L, existing.getDepartmentId().longValue());
    }

    @Test
    @DisplayName("更新用户：传 null 可以清空所属部门")
    void updateAccount_shouldClearDepartmentWhenNull() {
        Account existing = account(8L, "lisi", 1L);
        when(accountRepository.findByIdAndDeleted(8L, NOT_DELETED)).thenReturn(Optional.of(existing));
        stubAccountSave(8L);

        AccountDTO result = accountService.updateAccount(8L, accountDto("lisi", null));

        assertNull(existing.getDepartmentId());
        assertNull(result.getDepartmentId());
        assertNull(result.getDepartmentName());
        verify(departmentRepository, never()).findByIdAndDeleted(anyLong(), anyInt());
    }

    @Test
    @DisplayName("更新用户：部门不存在时拒绝，且不落库")
    void updateAccount_shouldRejectUnknownDepartment() {
        Account existing = account(9L, "wangwu", 1L);
        when(accountRepository.findByIdAndDeleted(9L, NOT_DELETED)).thenReturn(Optional.of(existing));
        when(departmentRepository.findByIdAndDeleted(99L, NOT_DELETED)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.updateAccount(9L, accountDto("wangwu", 99L)));

        assertEquals("所属部门不存在", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    // ==================== 查询用户 ====================

    @Test
    @DisplayName("根据ID查询：返回部门名称")
    void getAccountById_shouldReturnDepartmentName() {
        when(accountRepository.findByIdAndDeleted(1L, NOT_DELETED))
                .thenReturn(Optional.of(account(1L, "zhangsan", 1L)));
        when(departmentRepository.findByIdAndDeleted(1L, NOT_DELETED))
                .thenReturn(Optional.of(department(1L, "研发部", NOT_DELETED)));

        AccountDTO result = accountService.getAccountById(1L);

        assertEquals("研发部", result.getDepartmentName());
        assertEquals(1L, result.getDepartmentId().longValue());
    }

    @Test
    @DisplayName("查询列表：批量解析部门名称，不产生 N+1 查询")
    void getAllAccounts_shouldResolveDepartmentNamesInBatch() {
        when(accountRepository.findByDeleted(NOT_DELETED)).thenReturn(List.of(
                account(1L, "zhangsan", 1L),
                account(2L, "lisi", 2L),
                account(3L, "wangwu", null)));
        when(departmentRepository.findAllById(any())).thenReturn(List.of(
                department(1L, "研发部", NOT_DELETED),
                department(2L, "运维部", NOT_DELETED)));

        List<AccountDTO> result = accountService.getAllAccounts();

        assertEquals(3, result.size());
        assertEquals("研发部", result.get(0).getDepartmentName());
        assertEquals("运维部", result.get(1).getDepartmentName());
        assertNull(result.get(2).getDepartmentId());
        assertNull(result.get(2).getDepartmentName());

        verify(departmentRepository, times(1)).findAllById(any());
        verify(departmentRepository, never()).findByIdAndDeleted(anyLong(), anyInt());
    }

    @Test
    @DisplayName("查询列表：不展示已软删除部门的名称")
    void getAllAccounts_shouldIgnoreSoftDeletedDepartment() {
        when(accountRepository.findByDeleted(NOT_DELETED))
                .thenReturn(List.of(account(1L, "zhangsan", 3L)));
        when(departmentRepository.findAllById(any()))
                .thenReturn(List.of(department(3L, "已删除部门", DELETED)));

        List<AccountDTO> result = accountService.getAllAccounts();

        assertEquals(1, result.size());
        assertNull(result.get(0).getDepartmentName());
    }

    // ==================== 分页 + 条件查询 ====================

    @Test
    @DisplayName("分页查询：返回分页结构，并按当前页批量回填部门名称")
    void getAccountsPage_shouldReturnPageResultWithDepartmentNames() {
        Pageable pageable = PageRequest.of(1, 5);
        Page<Account> page = new PageImpl<>(
                List.of(account(2L, "lisi", 2L), account(1L, "zhangsan", 1L)), pageable, 12);
        when(accountRepository.search(eq("li"), eq(1), eq(2), any(Pageable.class))).thenReturn(page);
        when(departmentRepository.findAllById(any())).thenReturn(List.of(
                department(1L, "研发部", NOT_DELETED),
                department(2L, "运维部", NOT_DELETED)));

        PageResult<AccountDTO> result = accountService.getAccountsPage(1, 5, "  li  ", 1, 2);

        assertEquals(12L, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(1, result.getCurrentPage());
        assertEquals(5, result.getPageSize());
        assertEquals(2, result.getList().size());
        assertEquals("运维部", result.getList().get(0).getDepartmentName());
        assertEquals("研发部", result.getList().get(1).getDepartmentName());
        verify(accountRepository).search(eq("li"), eq(1), eq(2), any(Pageable.class));
    }

    @Test
    @DisplayName("分页查询：页码负数归零、size 过大按上限截断，并按 id 倒序")
    void getAccountsPage_shouldClampPagingParameters() {
        when(accountRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        PageResult<AccountDTO> result = accountService.getAccountsPage(-5, 1000, null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(accountRepository).search(isNull(), isNull(), isNull(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(100, captor.getValue().getPageSize());
        Sort.Order order = captor.getValue().getSort().getOrderFor("id");
        assertEquals(Sort.Direction.DESC, order.getDirection());
        assertEquals(0L, result.getTotalElements());
    }

    @Test
    @DisplayName("分页查询：size 非法时用默认值，空白用户名不参与过滤")
    void getAccountsPage_shouldUseDefaultSizeAndIgnoreBlankUsername() {
        when(accountRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        accountService.getAccountsPage(0, 0, "   ", null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(accountRepository).search(isNull(), isNull(), isNull(), captor.capture());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("分页查询：结果为空时不查部门表")
    void getAccountsPage_shouldSkipDepartmentLookupWhenEmpty() {
        when(accountRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PageResult<AccountDTO> result = accountService.getAccountsPage(0, 10, "nobody", null, null);

        assertEquals(0, result.getList().size());
        verify(departmentRepository, never()).findAllById(any());
    }

    // ==================== 辅助方法 ====================

    private AccountDTO accountDto(String username, Long departmentId) {
        AccountDTO dto = new AccountDTO();
        dto.setUsername(username);
        dto.setName("测试用户");
        dto.setGender("男");
        dto.setRoleLevel(1);
        dto.setStatus(1);
        dto.setIsSuper(0);
        dto.setDepartmentId(departmentId);
        return dto;
    }

    private Account account(Long id, String username, Long departmentId) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setName("测试用户");
        account.setGender("男");
        account.setRoleLevel(1);
        account.setStatus(1);
        account.setIsSuper(0);
        account.setDepartmentId(departmentId);
        account.setDeleted(NOT_DELETED);
        return account;
    }

    private Department department(Long id, String name, Integer deleted) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setCode("CODE" + id);
        department.setStatus(1);
        department.setDeleted(deleted);
        return department;
    }

    private void stubAccountSave(Long id) {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(id);
            }
            return saved;
        });
    }
}
