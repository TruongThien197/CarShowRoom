package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.UserRepository;
import com.hsf302.carshowroom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserByid(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User không tồn tại !"));
    }

    @Override
    public User createUser(String email, String password, String fullName, String phone, String address, String role) {
        if ( userRepository.findByEmail(email) != null ) throw new RuntimeException("Email đã tồn tại !");
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .phone(phone)
                .address(address)
                .role(role.toUpperCase())
                .status("ACTIVE")
                .build();
        return userRepository.save(user);
    }

    @Override
    public User updateUser(Integer id, String fullName, String phone, String address, String role) {
        User user = getUserByid(id);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);
        user.setRole(role.toUpperCase());
        return userRepository.save(user);
    }

    @Override
    public void changeStatus(Integer id) {
        User user = getUserByid(id);
        user.setStatus("ACTIVE".equals(user.getStatus()) ? "INACTIVE" : "ACTIVE");
        userRepository.save(user);
    }
}
