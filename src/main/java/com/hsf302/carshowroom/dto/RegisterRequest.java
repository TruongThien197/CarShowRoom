package com.hsf302.carshowroom.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu phải dài từ 6 đến 50 ký tự")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 150, message = "Full name must be at most 150 characters")
    private String fullName;

    @Pattern(regexp = "^$|^[0-9]{9,15}$", message = "Số điện thoại chỉ được gồm 9 đến 15 chữ số")
    private String phone;

    @Size(max = 255, message = "Địa chỉ không được quá 255 ký tự")
    private String address;
}
