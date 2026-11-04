package com.example.prop.code;

import java.util.List;

public interface ComGrpCdService {

    List<ComGrpCd> findAll();

    List<ComGrpCd> findAll(ComGrpCd condition);

    ComGrpCd findById(String grpCd);

    void save(ComGrpCd comGrpCd);

    void delete(String grpCd);
}
