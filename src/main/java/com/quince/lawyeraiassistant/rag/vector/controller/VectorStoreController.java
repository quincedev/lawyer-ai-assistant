package com.quince.lawyeraiassistant.rag.vector.controller;

import com.quince.lawyeraiassistant.common.response.ApiResponse;
import com.quince.lawyeraiassistant.dto.response.SearchResponse;
import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.rag.vector.service.VectorSearchService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@ConditionalOnRagEnabled
@RequiredArgsConstructor
public class VectorStoreController {

    private final VectorSearchService vectorStoreService;

    @GetMapping("/rag/search")
    public ApiResponse<List<SearchResponse>> search(
            @RequestParam
            @NotBlank(message = "问题不能为空")
            @Size(max = 1000, message = "问题长度不能超过1000个字符")
            String question
            ) {

        return ApiResponse.success(vectorStoreService.search(question)
            .stream()
            .map(document -> new SearchResponse(
                    document.getText(),
                    document.getMetadata()))
            .toList());

    }

}
