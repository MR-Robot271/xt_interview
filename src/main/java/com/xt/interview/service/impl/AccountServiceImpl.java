package com.xt.interview.service.impl;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.PageResult;
import com.xt.interview.entity.Account;
import com.xt.interview.entity.Department;
import com.xt.interview.repository.AccountRepository;
import com.xt.interview.repository.DepartmentRepository;
import com.xt.interview.service.AccountService;
import com.xt.interview.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    private static final String DEFAULT_PASSWORD = "123456";

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public AccountDTO createAccount(AccountDTO dto) {
        if (accountRepository.existsByUsernameAndDeleted(dto.getUsername(), NOT_DELETED)) {
            throw new RuntimeException("用户名已存在");
        }
        validateDepartment(dto.getDepartmentId());
        Account account = new Account();
        account.setUsername(dto.getUsername());
        account.setName(dto.getName());
        account.setGender(dto.getGender());
        account.setPhone(dto.getPhone());
        String password = (dto.getPassword() == null || dto.getPassword().isEmpty())
                ? DEFAULT_PASSWORD : dto.getPassword();
        account.setPassword(MD5Util.encode(password));
        account.setRoleLevel(dto.getRoleLevel());
        account.setStatus(dto.getStatus());
        account.setIsSuper(dto.getIsSuper());
        account.setDepartmentId(dto.getDepartmentId());
        account.setDeleted(NOT_DELETED);
        account = accountRepository.save(account);
        return convertToDTO(account);
    }

    @Override
    public AccountDTO updateAccount(Long id, AccountDTO dto) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (!account.getUsername().equals(dto.getUsername()) &&
            accountRepository.existsByUsernameAndDeleted(dto.getUsername(), NOT_DELETED)) {
            throw new RuntimeException("用户名已存在");
        }

        validateDepartment(dto.getDepartmentId());

        account.setUsername(dto.getUsername());
        account.setName(dto.getName());
        account.setGender(dto.getGender());
        account.setPhone(dto.getPhone());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            account.setPassword(MD5Util.encode(dto.getPassword()));
        }
        account.setRoleLevel(dto.getRoleLevel());
        account.setStatus(dto.getStatus());
        account.setIsSuper(dto.getIsSuper());
        account.setDepartmentId(dto.getDepartmentId());
        account = accountRepository.save(account);
        return convertToDTO(account);
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        account.setDeleted(DELETED);
        accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(Long id) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        return convertToDTO(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        List<Account> accounts = accountRepository.findByDeleted(NOT_DELETED);
        Map<Long, String> departmentNames = loadDepartmentNames(accounts);
        return accounts.stream()
                .map(account -> convertToDTO(account, departmentNames.get(account.getDepartmentId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AccountDTO> getAccountsPage(int page, int size, String username, Integer status, Integer roleLevel) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String keyword = (username == null || username.trim().isEmpty()) ? null : username.trim();

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Account> pageResult = accountRepository.search(keyword, status, roleLevel, pageable);

        Map<Long, String> departmentNames = loadDepartmentNames(pageResult.getContent());
        List<AccountDTO> list = pageResult.getContent().stream()
                .map(account -> convertToDTO(account, departmentNames.get(account.getDepartmentId())))
                .collect(Collectors.toList());

        return new PageResult<>(list,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getNumber(),
                pageResult.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO login(String username, String password) {
        Account account = accountRepository.findByUsernameAndPasswordAndDeleted(
                username, MD5Util.encode(password), NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        if (account.getStatus() != 1) {
            throw new RuntimeException("账号已禁用");
        }
        return convertToDTO(account);
    }

    /**
     * 部门为可选字段：不传（null）表示不设置部门；传了则必须是未删除的部门。
     */
    private void validateDepartment(Long departmentId) {
        if (departmentId == null) {
            return;
        }
        departmentRepository.findByIdAndDeleted(departmentId, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("所属部门不存在"));
    }

    /**
     * 批量取部门名称，避免列表查询出现 N+1。
     */
    private Map<Long, String> loadDepartmentNames(List<Account> accounts) {
        Set<Long> departmentIds = accounts.stream()
                .map(Account::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return departmentRepository.findAllById(departmentIds).stream()
                .filter(department -> !DELETED.equals(department.getDeleted()))
                .collect(Collectors.toMap(Department::getId, Department::getName));
    }

    private AccountDTO convertToDTO(Account account) {
        String departmentName = null;
        if (account.getDepartmentId() != null) {
            departmentName = departmentRepository
                    .findByIdAndDeleted(account.getDepartmentId(), NOT_DELETED)
                    .map(Department::getName)
                    .orElse(null);
        }
        return convertToDTO(account, departmentName);
    }

    private AccountDTO convertToDTO(Account account, String departmentName) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setUsername(account.getUsername());
        dto.setName(account.getName());
        dto.setGender(account.getGender());
        dto.setPhone(account.getPhone());
        dto.setRoleLevel(account.getRoleLevel());
        dto.setStatus(account.getStatus());
        dto.setIsSuper(account.getIsSuper());
        dto.setDepartmentId(account.getDepartmentId());
        dto.setDepartmentName(departmentName);
        dto.setCreateTime(account.getCreateTime());
        dto.setUpdateTime(account.getUpdateTime());
        return dto;
    }
}
