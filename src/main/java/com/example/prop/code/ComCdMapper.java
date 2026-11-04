package com.example.prop.code;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ComCdMapper {

    List<ComCd> findAll(ComCd condition);

    default List<ComCd> findAll() {
        return findAll(null);
    }

    /** Active codes for one group, ordered by sort_ord. */
    List<ComCd> findByGrpCd(@Param("grpCd") String grpCd);

    ComCd findById(@Param("grpCd") String grpCd, @Param("cd") String cd);

    int insert(ComCd comCd);

    int update(ComCd comCd);

    int delete(@Param("grpCd") String grpCd, @Param("cd") String cd);
}
