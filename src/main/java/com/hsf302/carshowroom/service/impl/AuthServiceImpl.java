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
        email = email == null ? "" : email.trim().toLowerCase();
        fullname = fullname == null ? "" : fullname.trim();
        phone = phone == null ? null : phone.trim();
        address = address == null ? null : address.trim();
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") || email.length() > 255) {
            throw new IllegalArgumentException("Địa chỉ email không hợp lệ.");
        }
        if (password == null || password.length() < 6 || password.length() > 50) {
            throw new IllegalArgumentException("Mật khẩu phải dài từ 6 đến 50 ký tự.");
        }
        if (fullname.isBlank() || fullname.length() > 150) {
            throw new IllegalArgumentException("Họ và tên không được để trống và tối đa 150 ký tự.");
        }
        if (phone != null && !phone.isBlank() && !phone.matches("^0[0-9]{9}$")) {
            throw new IllegalArgumentException("Số điện thoại phải bắt đầu bằng 0 và gồm 10 chữ số.");
        }
        if (address != null && address.length() > 500) {
            throw new IllegalArgumentException("Địa chỉ tối đa 500 ký tự.");
        }
        if (userRepository.findByEmail(email) != null) {
            throw new RuntimeException("Email này đã được đăng ký.");
        }
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
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Tài khoản này đã bị khóa hoặc ngừng hoạt động.");
        }
        return user;
    }

    @Override
    public User updateProfile(Integer id, String fullName, String phone, String address) {
        User user = getUserByid(id);
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }
        if (phone != null && !phone.isBlank() && !phone.matches("^0[0-9]{9}$")) {
            throw new IllegalArgumentException("Số điện thoại phải bắt đầu bằng 0 và gồm 10 chữ số.");
        }
        user.setFullName(fullName.trim());
        user.setPhone(phone == null ? null : phone.trim());
        user.setAddress(address == null ? null : address.trim());
        return userRepository.save(user);
    }

    @Override
    public void changePassword(Integer id, String currentPassword, String newPassword) {
        User user = getUserByid(id);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 50) {
            throw new IllegalArgumentException("Mật khẩu mới phải dài từ 6 đến 50 ký tự.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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
