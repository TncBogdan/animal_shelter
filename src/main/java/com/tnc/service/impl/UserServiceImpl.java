package com.tnc.service.impl;

import com.tnc.repository.entities.User;
import com.tnc.repository.repositories.UserRepository;
import com.tnc.service.domain.Role;
import com.tnc.service.domain.UserDomain;
import com.tnc.service.exception.EmailExistException;
import com.tnc.service.exception.UserNotFoundException;
import com.tnc.service.exception.UsernameExistException;
import com.tnc.service.interfaces.UserService;
import com.tnc.service.mapper.UserDomainMapper;
import com.tnc.service.security.PasswordEncoder;
import com.tnc.service.security.UserPrincipal;
import com.tnc.service.security.util.JWTTokenProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

import static com.tnc.service.security.constant.SecurityConstant.JWT_TOKEN_HEADER;
import static com.tnc.service.security.constant.UserImplConstant.*;
import static org.springframework.http.HttpStatus.OK;

@Service
@Transactional
@Qualifier("userDetailsService")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass()); //getClass = this class
    private final UserRepository userRepository;
    private final UserDomainMapper userDomainMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTTokenProvider jwtTokenProvider;

    public ResponseEntity<UserDomain> login(UserDomain userDomain) {
        authenticate(userDomain.getUsername(), userDomain.getPassword());
        UserDomain loginUser = findByUsername(userDomain.getUsername());
        UserPrincipal userPrincipal = new UserPrincipal(userDomainMapper.toEntity(loginUser));
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);
        return new ResponseEntity<>(loginUser, jwtHeader, OK);

//        return ResponseEntity.ok(userDTOMapper.toDTORegistration(userService.register(userDTOMapper.toDomainRegistration(userDTO))));
    }

    @Override
    public UserDomain register(UserDomain userDomain) throws UserNotFoundException, EmailExistException, UsernameExistException {

//        var currentUsername = userDomain.getUsername();
//        validateNewUsernameAndEmail(currentUsername, userDomain.getUsername(), userDomain.getEmail());
        userDomain.setUserId(generateUserId());
        String password = generatePassword();
        String encodedPassword = encodePassword(password);
//        userDomain.setFirstName(userDomain.getFirstName());
//        userDomain.setLastName(userDomain.getLastName());
//        userDomain.setUsername(userDomain.getUsername());
//        userDomain.setEmail(userDomain.getEmail());
        userDomain.setJoinDate(new Date());
        userDomain.setPassword(encodedPassword);
        userDomain.setActive(true);
        userDomain.setNotLocked(true);
        userDomain.setRole(Role.ROLE_USER.name());
        userDomain.setAuthorities(Role.ROLE_USER.getAuthorities());
        userDomain.setProfileImageUrl(getTemporaryProfileImageUrl());
        LOGGER.info("New userDomain password " + password);
        return userDomainMapper.toDomain(userRepository.save(userDomainMapper.toEntity(userDomain)));
    }

    private String getTemporaryProfileImageUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH).toUriString();
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    @Override
    public UserDomain findByUsername(String username) {
        return userDomainMapper.toDomain(userRepository.findUserByUsername(username));
    }

    @Override
    public UserDomain findByEmail(String email) {
        return userDomainMapper.toDomain(userRepository.findByEmail(email));
    }

    @Override
    public UserDomain get(Long id) {
        return userDomainMapper.toDomain(userRepository.getById(id));
    }

    @Override
    public List<UserDomain> getAll() {
        return userDomainMapper.toDomainList(userRepository.findAll());
    }

    @Override
    public UserDomain add(UserDomain userDomain) {
        return userDomainMapper.toDomain(userRepository.save(userDomainMapper.toEntity(userDomain)));
    }

    @Override
    public UserDomain update(UserDomain userDomain) {
        return userDomainMapper.toDomain(userRepository.save(userDomainMapper.toEntity(userDomain)));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsername(username);
        if (user == null) {
            LOGGER.error(NO_USER_FOUND_BY_USERNAME + username);
            throw new UsernameNotFoundException(NO_USER_FOUND_BY_USERNAME + username);
        } else {
            user.setLastLoginDateDisplay(user.getLastLoginDate());
            user.setLastLoginDate(new Date());
            userRepository.save(user);
            UserPrincipal userPrincipal = new UserPrincipal(user);
            LOGGER.info(FOUND_USER_BY_USERNAME + username);
            return userPrincipal;
        }
    }

    private HttpHeaders getJwtHeader(UserPrincipal userPrincipal) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(userPrincipal));
        return headers;
    }

    private void authenticate(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }

//    private UserDomain validateNewUsernameAndEmail(String currentUsername, String newUsername, String newEmail) throws UsernameExistException, EmailExistException, UserNotFoundException {
//        var userByNewUsername = findByUsername(newUsername);
//        var userByEmail = findByEmail(newEmail);
//        if (StringUtils.isNotBlank(currentUsername)) {
//            var currentUser = findByUsername(currentUsername);
//            if (currentUser == null) {
//                throw new UserNotFoundException(NO_USER_FOUND_BY_USERNAME + currentUsername);
//            }
//            if (userByNewUsername != null && !currentUser.getId().equals(userByNewUsername.getId())) {
//                throw new UsernameExistException(USERNAME_ALREADY_EXIST);
//            }
//            if (userByEmail != null && !currentUser.getId().equals(userByEmail.getId())) {
//                throw new EmailExistException(EMAIL_ALREADY_EXIST);
//            }
//            return currentUser;
//        } else {
//            if (userByNewUsername != null) {
//                throw new UsernameExistException(USERNAME_ALREADY_EXIST);
//            }
//            if (userByEmail != null) {
//                throw new EmailExistException(EMAIL_ALREADY_EXIST);
//            }
//            return null;
//        }
//    }
}
