package org.example.java_ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.java_ai.entity.UserWallet;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 用户钱包Mapper
 */
@Mapper
public interface UserWalletMapper extends BaseMapper<UserWallet> {
}
