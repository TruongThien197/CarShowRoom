package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    Page<User> searchUsers(String keyword, String role, String status, Pageable pageable);
    User getUserByid(Integer id);
    User createUser(String email, String password, String fullName, String phone, String address, String role);
    User updateUser(Integer id, String fullName, String phone, String address, String role);
    void changeStatus(Integer id);
}
