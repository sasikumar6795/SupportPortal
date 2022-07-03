package com.supportportal.service.implementation;

import com.supportportal.domain.User;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.enumeration.Role;
import com.supportportal.exception.EmailExistException;
import com.supportportal.exception.UserNameExistException;
import com.supportportal.repository.UserRepository;
import com.supportportal.service.EmailService;
import com.supportportal.service.LoginAttempt;
import com.supportportal.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Date;
import java.util.List;

import static com.supportportal.enumeration.Role.*;
import static org.apache.commons.lang3.StringUtils.*;

@Slf4j
@Service
@Transactional
@Qualifier("UserDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {

    private Logger LOGGER = LoggerFactory.getLogger(getClass());

    private UserRepository userRepository;

    private BCryptPasswordEncoder bCryptPasswordEncoder;

    private LoginAttempt loginAttempt;

    private EmailService emailService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder,  LoginAttempt loginAttempt, EmailService emailService) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.loginAttempt = loginAttempt;
        this.emailService = emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        User user = userRepository.findUserByUserName(userName);
        if(user==null)
        {
            LOGGER.error("User not Found by userName: "+userName);
            throw new UsernameNotFoundException("User not Found by userName: "+userName);
        }
        validateLoginAttempt(user);
        user.setLastLoginDateDisplay(user.getLastLoginDate());
        user.setLastLoginDate(new Date());
        userRepository.save(user);
        //user prinicpal implements userdetails thats why we are returning this
        UserPrincipal userPrincipal = new UserPrincipal(user);
        LOGGER.info("Returning found user by userName: " + userName);
        return userPrincipal;
    }

    private void validateLoginAttempt(User user) {
        if(user.isNotLocked())
        {
            loginAttempt.addUserToLoginAttempts(user.getUserName());
            if(loginAttempt.hasExceededMaxAttempts(user.getUserName())){
                user.setNotLocked(false);
            }else {
                user.setNotLocked(true);
            }
        }
        else
        {
           loginAttempt.evictUserFromLoginAttemptCache(user.getUserName());
        }
    }

    @Override
    public User register(String firstName, String lastName, String userName, String email) {

        validateNewUserNameAndEmail(EMPTY, userName,email);
        String password=generatePassword();
        String encodedPassword=encodePasswordMethod(password);
        User user = User.builder()
                .userId(generateUserId())
                .firstName(firstName)
                .lastName(lastName)
                .userName(userName)
                .isActive(true)
                .email(email)
                .isNotLocked(true)
                .role(ROLE_USER.name())
                .authorities(ROLE_USER.getAuthorities())
                .joinDate(new Date())
                .password(encodedPassword)
                .profileImageUrl(getTemporaryImageUrl())
                .build();
        userRepository.save(user);
        LOGGER.info("New user password: "+ password);
        emailService.sendNewPasswordEmail(firstName,password,email);
        return user;
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public User findByUserName(String userName) {
        return userRepository.findUserByUserName(userName);
    }

    @Override
    public User findUserByEmail(String email) {

        return userRepository.findUserByEmail(email);
    }

    public User validateNewUserNameAndEmail(String currentUserName, String newUserName, String newEmail)
    {
        User userByNewUserName = findByUserName(newUserName);
        User userByNewEmail = findUserByEmail(newEmail);
        if(isNotBlank(currentUserName))
        {
            User currentUser = userRepository.findUserByUserName(currentUserName);
            if(currentUser==null)
            {
                throw new UsernameNotFoundException("No user found by userName "+ currentUserName);
            }
            if(userByNewUserName!=null&& !currentUser.getId().equals(userByNewUserName.getId()))
            {
                throw new UserNameExistException("Username already exits "+ userByNewUserName);
            }
            if(userByNewEmail!=null && !currentUser.getId().equals(userByNewEmail.getId()))
            {
                throw new EmailExistException("Email already taken "+ userByNewEmail);
            }
            return currentUser;
        }
        else
        {
            if(userByNewUserName!=null)
                throw new UserNameExistException("Username already exits "+ userByNewUserName);
            if(userByNewEmail!=null)
                throw new EmailExistException("Email already taken "+ userByNewEmail);
            return null;
        }
    }

    private String getTemporaryImageUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/image/profile/temp").toUriString();
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    private String encodePasswordMethod(String password) {
        return bCryptPasswordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }
}
