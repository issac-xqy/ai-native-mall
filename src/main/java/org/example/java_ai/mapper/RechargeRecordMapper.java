package org.example.java_ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.java_ai.entity.RechargeRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 充值记录Mapper
 */
@Mapper
public interface RechargeRecordMapper extends BaseMapper<RechargeRecord> {
}
