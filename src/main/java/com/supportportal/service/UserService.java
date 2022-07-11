package com.supportportal.service;

import com.supportportal.domain.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User register(String firstName, String lastName, String userName, String email);

    List<User> getUsers();

    User findByUserName(String userName);

    User findUserByEmail(String email);

    Optional<User> addNewUser(String firstName, String lastName, String userName, String email, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage);

    Optional<User> updateUser(String currentUserName, String newFirstName, String newLastName, String newUserName, String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage);

    void deleteUser(long id);

    void resetPassword(String email);

    Optional<User> updateProfileImage(String userName, MultipartFile profileImage);

}
