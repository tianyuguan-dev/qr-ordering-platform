package com.qrordering.table.controller;

import com.qrordering.table.dto.request.CreateTableInfoRequest;
import com.qrordering.table.dto.request.UpdateTableInfoRequest;
import com.qrordering.table.dto.response.TableInfoResponse;
import com.qrordering.table.service.TableInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants/{restaurantId}/tables")
@RequiredArgsConstructor
@Tag(name = "Tables", description = "APIs for managing dining tables. restaurantId=me (restaurant admin) or id (platform admin).")
public class TableInfoController {

    private final TableInfoService tableInfoService;

    @GetMapping
    @Operation(summary = "List tables")
    public ResponseEntity<List<TableInfoResponse>> list(@PathVariable String restaurantId) {
        return ResponseEntity.ok(tableInfoService.listForRestaurant(restaurantId));
    }

    @PostMapping
    @Operation(summary = "Create table")
    public ResponseEntity<TableInfoResponse> create(@PathVariable String restaurantId,
                                                     @Valid @RequestBody CreateTableInfoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableInfoService.create(restaurantId, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update table")
    public ResponseEntity<TableInfoResponse> update(@PathVariable String restaurantId,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody UpdateTableInfoRequest request) {
        return ResponseEntity.ok(tableInfoService.update(restaurantId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete table")
    public ResponseEntity<Void> delete(@PathVariable String restaurantId, @PathVariable Long id) {
        tableInfoService.delete(restaurantId, id);
        return ResponseEntity.noContent().build();
    }
}
