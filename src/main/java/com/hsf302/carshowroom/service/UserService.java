package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.User;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    User getUserByid(Integer id);
    User createUser(String email, String password, String fullName, String phone, String address, String role);
    User updateUser(Integer id, String fullName, String phone, String address, String role);
    void changeStatus(Integer id);
}
