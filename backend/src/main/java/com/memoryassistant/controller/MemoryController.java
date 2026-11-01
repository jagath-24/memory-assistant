package com.memoryassistant.controller;

import com.memoryassistant.dto.MemoryFactDto;
import com.memoryassistant.service.MemoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public List<MemoryFactDto> getAll() {
        return memoryService.getAllFacts();
    }

    @DeleteMapping("/{factId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long factId) {
        memoryService.deleteFact(factId);
    }
}
