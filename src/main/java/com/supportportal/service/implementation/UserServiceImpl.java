package com.supportportal.service.implementation;

import com.supportportal.domain.User;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.repository.UserRepository;
import com.supportportal.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@Transactional
@Qualifier("UserDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {

    private Logger LOGGER = LoggerFactory.getLogger(getClass());

    private UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        User user = userRepository.findUserByUserName(userName);
        if(user==null)
        {
            LOGGER.error("User not Found bu userName: "+userName);
            throw new UsernameNotFoundException("User not Found by userName: "+userName);
        }

        user.setLastLoginDateDisplay(user.getLastLoginDate());
        user.setLastLoginDate(new Date());
        userRepository.save(user);
        UserPrincipal userPrincipal = new UserPrincipal(user);
        LOGGER.info("Returning found user by userName: " + userName);
        return userPrincipal;
    }

}
