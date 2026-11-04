package com.xt.interview.dto;

import java.util.List;

/**
 * 通用分页返回结构。
 */
public class PageResult<T> {

    private List<T> list;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    public PageResult() {}

    public PageResult(List<T> list, long totalElements, int totalPages, int currentPage, int pageSize) {
        this.list = list;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
