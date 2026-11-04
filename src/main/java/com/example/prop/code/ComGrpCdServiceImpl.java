package com.example.prop.code;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ComGrpCdServiceImpl implements ComGrpCdService {

    @Autowired
    private ComGrpCdMapper comGrpCdMapper;

    @Cacheable(value = "grp", key = "'all'")
    @Transactional(readOnly = true)
    @Override
    public List<ComGrpCd> findAll() {
        return comGrpCdMapper.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ComGrpCd> findAll(ComGrpCd condition) {
        return comGrpCdMapper.findAll(condition);
    }

    @Cacheable(value = "grp", key = "#grpCd")
    @Transactional(readOnly = true)
    @Override
    public ComGrpCd findById(String grpCd) {
        return comGrpCdMapper.findById(grpCd);
    }

    /**
     * Insert or update: try update first; if no row affected, insert.
     * Evicts entire grp-codes cache because group list may change.
     */
    @CacheEvict(value = "grp", allEntries = true)
    @Transactional
    @Override
    public void save(ComGrpCd comGrpCd) {
        int rows = comGrpCdMapper.update(comGrpCd);
        if (rows == 0) {
            comGrpCdMapper.insert(comGrpCd);
        }
    }

    @CacheEvict(value = "grp", allEntries = true)
    @Transactional
    @Override
    public void delete(String grpCd) {
        comGrpCdMapper.delete(grpCd);
    }
}
