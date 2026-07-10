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
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found."));
    }

    @Override
    public User register(String email, String password, String fullname, String phone, String address) {
        if ( userRepository.findByEmail(email) != null ) throw new RuntimeException("This email is already registered.");
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
        if ( user == null ) throw new RuntimeException("No account found with this email.");
        if ( !passwordEncoder.matches(password, user.getPasswordHash()) ) throw new RuntimeException("Incorrect password.");
        if ( user.getStatus().equals("LOCK") ) throw new RuntimeException("This account has been locked.");
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
        if ( auth == null || !auth.isAuthenticated() ) throw new RuntimeException("Your session has expired. Please sign in again.");
        return (User) auth.getPrincipal();
    }
}
