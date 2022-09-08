package com.supportportal.service.implementation;

import com.supportportal.domain.User;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.enumeration.Role;
import com.supportportal.exception.EmailExistException;
import com.supportportal.exception.EmailNotFoundException;
import com.supportportal.exception.NotAnImageFileException;
import com.supportportal.exception.UserNameExistException;
import com.supportportal.repository.UserRepository;
import com.supportportal.service.EmailService;
import com.supportportal.service.LoginAttempt;
import com.supportportal.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.supportportal.constant.FileConstant.*;
import static com.supportportal.constant.UserImplementationConstant.*;
import static com.supportportal.enumeration.Role.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.lang3.StringUtils.*;
import static org.springframework.http.MediaType.*;

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
                .profileImageUrl(getTemporaryImageUrl(userName))
                .build();
        userRepository.save(user);
        LOGGER.info("New user password: "+ password);
        //emailService.sendNewPasswordEmail(firstName,password,email);
        return user;
    }

    @Override
    public Optional<User> addNewUser(String firstName, String lastName, String userName, String email, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) {
        User addNewUserValidated = validateNewUserNameAndEmail(EMPTY, userName,email);
        if(addNewUserValidated!=null)
        {
            return Optional.empty();
        }
        String password=generatePassword();
        String encodedPassword=encodePasswordMethod(password);
        User user = User.builder()
                .userId(generateUserId())
                .userName(userName)
                .firstName(firstName)
                .lastName(lastName)
                .joinDate(new Date())
                .isNotLocked(isNonLocked)
                .isActive(isActive)
                .email(email)
                .password(encodedPassword)
                .role(getRoleEnumName(role).name())
                .authorities(getRoleEnumName(role).getAuthorities())
                .profileImageUrl(getTemporaryImageUrl(userName))
                .build();
        userRepository.save(user);
        saveProfileImage(user, profileImage);
        return Optional.of(user);
    }

    @Override
    public Optional<User> updateUser(String currentUserName, String newFirstName, String newLastName, String newUserName, String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) {
        User currentUser = validateNewUserNameAndEmail(currentUserName, newUserName,newEmail);
        if(currentUser==null)
        {
            return Optional.empty();
        }
        currentUser.builder()
                .firstName(newFirstName)
                .lastName(newLastName)
                .email(newEmail)
                .isActive(isActive)
                .isNotLocked(isNonLocked)
                .role(getRoleEnumName(role).name())
                .authorities(getRoleEnumName(role).getAuthorities())
                .build();
        userRepository.save(currentUser);
        saveProfileImage(currentUser,profileImage);
        return Optional.of(currentUser);
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

    @Override
    public void deleteUser(String userName) {
        User userByUserName = userRepository.findUserByUserName(userName);
        Path userFolder = Paths.get(USER_FOLDER + userByUserName.getUserName()).toAbsolutePath().normalize();
        try {
            FileUtils.deleteDirectory(new File(userFolder.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        userRepository.deleteById(userByUserName.getId());
    }

    @Override
    public void resetPassword(String email) {
        User user = userRepository.findUserByEmail(email);
        if(user==null)
        {
            throw new EmailNotFoundException(NO_USER_FOUND_BY_EMAIL + email);
        }
        String password = generatePassword();
        user.builder()
                .password(encodePasswordMethod(password))
                .build();
        userRepository.save(user);
        //emailService.sendNewPasswordEmail(user.getFirstName(),user.getPassword(), user.getEmail());
    }

    @Override
    public Optional<User> updateProfileImage(String userName, MultipartFile profileImage) {
        User user = validateNewUserNameAndEmail(userName, null, null);
        if(user==null)
        {
            return Optional.empty();
        }
        saveProfileImage(user,profileImage);
        return Optional.of(user);
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
                throw new UsernameNotFoundException(NO_USER_FOUND_BY_USER_NAME + " " + currentUserName);
            }
            if(userByNewUserName!=null&& !currentUser.getId().equals(userByNewUserName.getId()))
            {
                throw new UserNameExistException(USERNAME_ALREADY_EXITS + " " + userByNewUserName);
            }
            if(userByNewEmail!=null && !currentUser.getId().equals(userByNewEmail.getId()))
            {
                throw new EmailExistException(EMAIL_ALREADY_TAKEN + " " + userByNewEmail);
            }
            return currentUser;
        }
        else
        {
            if(userByNewUserName!=null)
                throw new UserNameExistException(USERNAME_ALREADY_EXITS + " " + userByNewUserName);
            if(userByNewEmail!=null)
                throw new EmailExistException(EMAIL_ALREADY_TAKEN + " " + userByNewEmail);
            return null;
        }
    }

    private void saveProfileImage(User user, MultipartFile profileImage) {
        if(profileImage!=null)
        {
            if(!Arrays.asList(IMAGE_JPEG_VALUE,IMAGE_PNG_VALUE,IMAGE_GIF_VALUE).contains(profileImage.getContentType())) {
                throw new NotAnImageFileException(profileImage.getOriginalFilename() + " is not an image file please upload an image");
            }
            Path userFolder = Paths.get(USER_FOLDER + user.getUserName()).toAbsolutePath().normalize();
            if(!Files.exists(userFolder))
            {
                Path directories = null;
                try {
                    Files.createDirectories(userFolder);
                    LOGGER.info(DIRECTORY_CREATED + " " + userFolder);
                    Files.deleteIfExists(Paths.get(userFolder+user.getUserName()+DOT+JPG_EXTENSION));
                    // double check to delete the file or replace the image with name already exists
                    Files.copy(profileImage.getInputStream(), userFolder.resolve(userFolder+user.getUserName()+DOT+JPG_EXTENSION), REPLACE_EXISTING);

                    user.setProfileImageUrl(setProfileImageUrl(user.getUserName()));
                    userRepository.save(user);

                    LOGGER.info(FILE_SAVED_IN_FILE_SYSTEM+ profileImage.getOriginalFilename());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private String setProfileImageUrl(String userName) {

        return ServletUriComponentsBuilder.fromCurrentContextPath().path(USER_IMAGE_PATH + userName + FORWARD_SLASH
        + userName + DOT + JPG_EXTENSION).toUriString();
    }

    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase());
    }

    private String getTemporaryImageUrl(String userName) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH + userName).toUriString();
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
