package com.example.prop.code;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ComGrpCdMapper {

    List<ComGrpCd> findAll(ComGrpCd condition);

    default List<ComGrpCd> findAll() {
        return findAll(null);
    }

    ComGrpCd findById(@Param("grpCd") String grpCd);

    int insert(ComGrpCd comGrpCd);

    int update(ComGrpCd comGrpCd);

    int delete(@Param("grpCd") String grpCd);
}
