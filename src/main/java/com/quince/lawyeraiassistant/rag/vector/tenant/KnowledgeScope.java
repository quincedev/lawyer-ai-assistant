package com.quince.lawyeraiassistant.rag.vector.tenant;

public enum KnowledgeScope {

    /**
     * 平台公共知识。
     *
     * 例如：
     * 《劳动合同法》
     * 《民法典》
     * 公开司法解释
     */
    SHARED,

    /**
     * 某个 Tenant 私有知识。
     *
     * 例如：
     * 律所内部文件
     * 客户合同
     * 案件材料
     */
    TENANT
}