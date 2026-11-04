package com.xt.interview.controller;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.PageResult;
import com.xt.interview.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户分页接口的 Controller 层单元测试：校验参数绑定与返回结构。
 */
@DisplayName("用户管理 Controller 单元测试（分页）")
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        AccountController controller = new AccountController();
        ReflectionTestUtils.setField(controller, "accountService", accountService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("GET /api/accounts/page 绑定全部分页与筛选参数")
    void page_shouldBindAllQueryParameters() throws Exception {
        AccountDTO dto = new AccountDTO();
        dto.setId(1L);
        dto.setUsername("zhangsan");
        dto.setName("张三");
        dto.setStatus(1);
        dto.setRoleLevel(2);

        when(accountService.getAccountsPage(1, 5, "张", 1, 2))
                .thenReturn(new PageResult<>(List.of(dto), 12L, 3, 1, 5));

        mockMvc.perform(get("/api/accounts/page")
                        .param("page", "1")
                        .param("size", "5")
                        .param("username", "张")
                        .param("status", "1")
                        .param("roleLevel", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.list.length()").value(1))
                .andExpect(jsonPath("$.list[0].username").value("zhangsan"))
                .andExpect(jsonPath("$.list[0].name").value("张三"));

        verify(accountService).getAccountsPage(1, 5, "张", 1, 2);
    }

    @Test
    @DisplayName("GET /api/accounts/page 缺省参数时使用 page=0、size=10，且筛选条件为空")
    void page_shouldUseDefaultParameters() throws Exception {
        when(accountService.getAccountsPage(0, 10, null, null, null))
                .thenReturn(new PageResult<>(List.of(), 0L, 0, 0, 10));

        mockMvc.perform(get("/api/accounts/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.list.length()").value(0));

        verify(accountService).getAccountsPage(0, 10, null, null, null);
    }
}
