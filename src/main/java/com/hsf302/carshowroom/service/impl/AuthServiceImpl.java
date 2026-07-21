package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.UserRepository;
import com.hsf302.carshowroom.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public User getUserByid(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    @Override
    public User register(String email, String password, String fullname, String phone, String address) {
        if ( userRepository.findByEmail(email) != null ) throw new RuntimeException("Email này đã được đăng ký.");
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullname)
                .phone(phone)
                .address(address)
                .role("CUSTOMER")
                .status("ACTIVE")
                .build();
        return userRepository.save(user);
    }

    @Override
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email);
        if ( user == null ) throw new RuntimeException("Không tìm thấy tài khoản với email này.");
        if ( !passwordEncoder.matches(password, user.getPasswordHash()) ) throw new RuntimeException("Mật khẩu không chính xác.");
        if ( user.getStatus().equals("LOCK") ) throw new RuntimeException("Tài khoản này đã bị khóa.");
        return user;
    }

    @Override
    public User updateProfile(Integer id, String fullName, String phone, String address) {
        User user = getUserByid(id);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);
        return userRepository.save(user);
    }

//    @Override
//    public void logout() {
//        SecurityContextHolder.clearContext();
//    }

    @Override
    public User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if ( auth == null || !auth.isAuthenticated() ) throw new RuntimeException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        return (User) auth.getPrincipal();
    }
}
