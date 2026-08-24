package com.quince.lawyeraiassistant.security.identity.persistence.mybatis;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ApplicationUserMapper {

    List<ApplicationUserRow> findRowsByUsername(
            @Param("username") String username);
}