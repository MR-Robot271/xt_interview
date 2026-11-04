package com.xt.interview.controller;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.PageResult;
import com.xt.interview.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getAll() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/page")
    public ResponseEntity<PageResult<AccountDTO>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer roleLevel) {
        return ResponseEntity.ok(accountService.getAccountsPage(page, size, username, status, roleLevel));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @PostMapping
    public ResponseEntity<AccountDTO> create(@RequestBody AccountDTO dto) {
        return ResponseEntity.ok(accountService.createAccount(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDTO> update(@PathVariable Long id, @RequestBody AccountDTO dto) {
        return ResponseEntity.ok(accountService.updateAccount(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok().build();
    }
}
