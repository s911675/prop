package com.example.prop.code;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ComCdServiceImpl implements ComCdService {

    @Autowired
    private ComCdMapper comCdMapper;

    @Transactional(readOnly = true)
    @Override
    public List<ComCd> findAll(ComCd condition) {
        return comCdMapper.findAll(condition);
    }

    @Cacheable(value = "detail", key = "#grpCd")
    @Transactional(readOnly = true)
    @Override
    public List<ComCd> findByGrpCd(String grpCd) {
        return comCdMapper.findByGrpCd(grpCd);
    }

    @Cacheable(value = "detail", key = "#grpCd + ':' + #cd")
    @Transactional(readOnly = true)
    @Override
    public ComCd findById(String grpCd, String cd) {
        return comCdMapper.findById(grpCd, cd);
    }

    /**
     * Insert or update: try update first; if no row affected, insert.
     * Evicts the group's list cache (grpCd key) so the next read refreshes from DB.
     */
    @CacheEvict(value = "detail", key = "#comCd.grpCd")
    @Transactional
    @Override
    public void save(ComCd comCd) {
        int rows = comCdMapper.update(comCd);
        if (rows == 0) {
            comCdMapper.insert(comCd);
        }
    }

    @CacheEvict(value = "detail", key = "#grpCd")
    @Transactional
    @Override
    public void delete(String grpCd, String cd) {
        comCdMapper.delete(grpCd, cd);
    }
}
