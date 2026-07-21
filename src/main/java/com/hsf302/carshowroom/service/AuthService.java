package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.User;

public interface AuthService {
    User getUserByid(Integer id);
    User register (String email, String password, String fullname, String phone, String address);
    User login (String email, String password);
    User updateProfile(Integer userId, String fullName, String phone, String address);
    void changePassword(Integer userId, String currentPassword, String newPassword);
    //    void logout();
    User getCurrentUser();
}
