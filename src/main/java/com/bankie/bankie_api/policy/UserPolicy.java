package com.bankie.bankie_api.policy;

import com.bankie.bankie_api.exception.BsnAlreadyExistsException;
import com.bankie.bankie_api.exception.EmailAlreadyExistsException;
import com.bankie.bankie_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPolicy {

    private final UserRepository userRepository;

    public void requireEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
    }

    public void requireBsnAvailable(String bsn) {
        if (userRepository.existsByBsn(bsn)) {
            throw new BsnAlreadyExistsException();
        }
    }
}
