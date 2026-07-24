package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.UserRepository;
import com.hsf302.carshowroom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    /** Lấy toàn bộ người dùng cho màn hình quản trị. */
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /** Tìm kiếm và phân trang người dùng theo từ khóa, vai trò và trạng thái. */
    @Override
    public Page<User> searchUsers(String keyword, String role, String status, Pageable pageable) {
        return userRepository.searchUsers(normalize(keyword), normalize(role), normalize(status), pageable);
    }

    /** Lấy người dùng theo mã, báo lỗi nếu không tìm thấy. */
    @Override
    public User getUserByid(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    /** Tạo tài khoản quản trị hoặc nhân viên với mật khẩu đã mã hóa. */
    @Override
    public User createUser(String email, String password, String fullName, String phone, String address, String role) {
        if ( userRepository.findByEmail(email) != null ) throw new RuntimeException("Email này đã được đăng ký.");
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

    /** Cập nhật thông tin hồ sơ và vai trò của người dùng. */
    @Override
    public User updateUser(Integer id, String fullName, String phone, String address, String role) {
        User user = getUserByid(id);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);
        user.setRole(role.toUpperCase());
        return userRepository.save(user);
    }

    /** Chuyển đổi trạng thái hoạt động/khóa của người dùng. */
    @Override
    public void changeStatus(Integer id) {
        User user = getUserByid(id);
        user.setStatus("ACTIVE".equals(user.getStatus()) ? "INACTIVE" : "ACTIVE");
        userRepository.save(user);
    }

    /** Chuẩn hóa chuỗi tìm kiếm; đổi chuỗi rỗng thành null để không áp dụng bộ lọc. */
    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
