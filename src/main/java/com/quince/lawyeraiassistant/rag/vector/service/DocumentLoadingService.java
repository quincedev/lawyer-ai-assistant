package com.quince.lawyeraiassistant.rag.vector.service;

import com.quince.lawyeraiassistant.rag.vector.sync.LoadedKnowledgeBatch;

public interface DocumentLoadingService {

    LoadedKnowledgeBatch load(String location);
}
