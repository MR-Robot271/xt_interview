package com.xt.interview.service;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.PageResult;

import java.util.List;

public interface AccountService {

    AccountDTO createAccount(AccountDTO dto);

    AccountDTO updateAccount(Long id, AccountDTO dto);

    void deleteAccount(Long id);

    AccountDTO getAccountById(Long id);

    List<AccountDTO> getAllAccounts();

    /**
     * 分页 + 条件查询。
     *
     * @param page      页码，从 0 开始
     * @param size      每页条数
     * @param username  用户名模糊匹配，可为空
     * @param status    状态筛选，可为空
     * @param roleLevel 权限级别筛选，可为空
     */
    PageResult<AccountDTO> getAccountsPage(int page, int size, String username, Integer status, Integer roleLevel);

    AccountDTO login(String username, String password);
}
