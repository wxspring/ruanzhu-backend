package com.company.ruanzhu.user.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.ruanzhu.user.model.User;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;

@Mapper
public interface UserRepository extends BaseMapper<User> {
    default Optional<User> findByUsername(String username) {
        return Optional.ofNullable(selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        ));
    }

    default boolean existsByUsername(String username) {
        return selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        ) > 0;
    }
}
