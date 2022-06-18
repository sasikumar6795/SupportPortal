package com.supportportal.service;

import com.supportportal.domain.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

public interface UserService {

    User register(String firstName, String lastName, String userName, String email);

    List<User> getUsers();

    User findByUserName(String userName);

    User findUserByEmail(String email);

}
