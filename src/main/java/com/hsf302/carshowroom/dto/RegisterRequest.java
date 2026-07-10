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

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String password;

    @NotBlank(message = "Full name must not be blank")
    @Size(max = 150, message = "Full name must be at most 150 characters")
    private String fullName;

    @Pattern(regexp = "^$|^[0-9]{9,15}$", message = "Phone number must contain only 9 to 15 digits")
    private String phone;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;
}