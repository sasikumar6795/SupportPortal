package com.supportportal.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttempt {

    private static final int MAXIMUM_NO_OF_ATTEMPTS = 5;
    public static final int ATTEMPT_INCREMENT = 1;
    private LoadingCache<String,Integer> loginAttemptCache;

    public LoginAttempt()
    {
        super();
        loginAttemptCache= CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(100).build(new CacheLoader<String, Integer>() {
                    @Override
                    public Integer load(String key) throws Exception {
                        return 0;
                    }
                });
    }

    public void evictUserFromLoginAttemptCache(String userName)
    {
        loginAttemptCache.invalidate(userName);
    }

    public void addUserToLoginAttempts(String userName)
    {
        Integer attempt = null;
        try {
            attempt = ATTEMPT_INCREMENT + loginAttemptCache.get(userName);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        loginAttemptCache.put(userName,attempt);
    }

    public boolean hasExceededMaxAttempts(String userName)
    {
        try {
            return loginAttemptCache.get(userName) >= MAXIMUM_NO_OF_ATTEMPTS;
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }
}
