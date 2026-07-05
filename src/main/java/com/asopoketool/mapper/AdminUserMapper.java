package com.asopoketool.mapper;

import com.asopoketool.model.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminUserMapper {
    @Select("SELECT * FROM admin_user WHERE username = #{username}")
    AdminUser findByUsername(String username);

    @org.apache.ibatis.annotations.Update("UPDATE admin_user SET password_hash = #{passwordHash} WHERE id = #{id}")
    void updatePasswordHash(AdminUser adminUser);

    @Select("SELECT * FROM admin_user ORDER BY created_at DESC")
    java.util.List<AdminUser> findAll();

    @org.apache.ibatis.annotations.Insert("INSERT INTO admin_user (username, password_hash, role, created_at) VALUES (#{username}, #{passwordHash}, #{role}, NOW())")
    void insert(AdminUser adminUser);
}
