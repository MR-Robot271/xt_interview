package com.xt.interview.controller;

import com.xt.interview.dto.DepartmentDTO;
import com.xt.interview.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 部门管理 Controller 层单元测试：校验五条 REST 接口的路径映射、请求绑定与响应结构。
 * 使用 standalone MockMvc，不启动 Spring 容器。
 */
@DisplayName("部门管理 Controller 单元测试")
class DepartmentControllerTest {

    @Mock
    private DepartmentService departmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        DepartmentController controller = new DepartmentController();
        ReflectionTestUtils.setField(controller, "departmentService", departmentService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("GET /api/departments 返回部门列表")
    void getAll_shouldReturnDepartmentList() throws Exception {
        when(departmentService.getAllDepartments()).thenReturn(List.of(
                buildDto(1L, "研发部", "RD", 1),
                buildDto(2L, "运维部", "OPS", 0)));

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("研发部"))
                .andExpect(jsonPath("$[1].code").value("OPS"))
                .andExpect(jsonPath("$[1].status").value(0));

        verify(departmentService).getAllDepartments();
    }

    @Test
    @DisplayName("GET /api/departments/{id} 绑定路径ID并返回部门详情")
    void getById_shouldBindPathIdAndReturnDepartment() throws Exception {
        DepartmentDTO dto = buildDto(5L, "研发部", "RD", 1);
        when(departmentService.getDepartmentById(5L)).thenReturn(dto);

        mockMvc.perform(get("/api/departments/{id}", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("研发部"))
                .andExpect(jsonPath("$.code").value("RD"));

        verify(departmentService).getDepartmentById(5L);
    }

    @Test
    @DisplayName("POST /api/departments 接收JSON请求体并返回创建结果")
    void create_shouldPassRequestBodyAndReturnCreatedDepartment() throws Exception {
        when(departmentService.createDepartment(any(DepartmentDTO.class)))
                .thenReturn(buildDto(9L, "测试部", "TEST", 1));

        String body = "{\"name\":\"测试部\",\"code\":\"TEST\",\"description\":\"接口测试\",\"status\":1}";

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.name").value("测试部"));

        ArgumentCaptor<DepartmentDTO> captor = ArgumentCaptor.forClass(DepartmentDTO.class);
        verify(departmentService).createDepartment(captor.capture());
        assertEquals("测试部", captor.getValue().getName());
        assertEquals("TEST", captor.getValue().getCode());
        assertEquals("接口测试", captor.getValue().getDescription());
        assertEquals(1, captor.getValue().getStatus().intValue());
    }

    @Test
    @DisplayName("PUT /api/departments/{id} 同时绑定路径ID与请求体")
    void update_shouldBindPathIdAndReturnUpdatedDepartment() throws Exception {
        when(departmentService.updateDepartment(eq(7L), any(DepartmentDTO.class)))
                .thenReturn(buildDto(7L, "研发中心", "RD", 0));

        String body = "{\"name\":\"研发中心\",\"code\":\"RD\",\"status\":0}";

        mockMvc.perform(put("/api/departments/{id}", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("研发中心"))
                .andExpect(jsonPath("$.status").value(0));

        ArgumentCaptor<DepartmentDTO> captor = ArgumentCaptor.forClass(DepartmentDTO.class);
        verify(departmentService).updateDepartment(eq(7L), captor.capture());
        assertEquals("研发中心", captor.getValue().getName());
    }

    @Test
    @DisplayName("DELETE /api/departments/{id} 调用软删除并返回200")
    void delete_shouldInvokeServiceAndReturnOk() throws Exception {
        mockMvc.perform(delete("/api/departments/{id}", 3))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(departmentService).deleteDepartment(3L);
    }

    private DepartmentDTO buildDto(Long id, String name, String code, Integer status) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setCode(code);
        dto.setStatus(status);
        if (id != null) {
            dto.setCreateTime(LocalDateTime.of(2026, 11, 4, 10, 0, 0));
            dto.setUpdateTime(LocalDateTime.of(2026, 11, 4, 10, 0, 0));
        }
        return dto;
    }
}
