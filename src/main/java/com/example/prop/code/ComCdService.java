package com.example.prop.code;

import java.util.List;

public interface ComCdService {

    List<ComCd> findAll(ComCd condition);

    /** Active codes for the given group, ordered by sort_ord. Cached by grpCd. */
    List<ComCd> findByGrpCd(String grpCd);

    ComCd findById(String grpCd, String cd);

    void save(ComCd comCd);

    void delete(String grpCd, String cd);
}
