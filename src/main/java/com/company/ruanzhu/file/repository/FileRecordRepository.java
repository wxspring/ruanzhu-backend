package com.company.ruanzhu.file.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.ruanzhu.file.model.FileRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FileRecordRepository extends BaseMapper<FileRecord> {

    default List<FileRecord> findByProjectIdAndFileType(Long projectId, String fileType) {
        return selectList(
            new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getProjectId, projectId)
                .eq(FileRecord::getFileType, fileType)
                .orderByDesc(FileRecord::getVersion)
        );
    }

    default List<FileRecord> findByProjectId(Long projectId) {
        return selectList(
            new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getProjectId, projectId)
                .orderByDesc(FileRecord::getCreatedAt)
        );
    }
}
