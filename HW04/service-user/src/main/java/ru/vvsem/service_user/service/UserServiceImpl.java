package ru.vvsem.service_user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.vvsem.service_user.dto.UserFullDto;
import ru.vvsem.service_user.dto.UserNewDto;
import ru.vvsem.service_user.mapper.UserFullMapper;
import ru.vvsem.service_user.mapper.UserNewMapper;
import ru.vvsem.service_user.model.User;
import ru.vvsem.service_user.repository.UserRepository;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserFullMapper userFullMapper;

    private final UserNewMapper userNewMapper;

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;

    @Override
    public Page<UserFullDto> getAll(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userFullMapper::toUserFullDto);
    }

    @Override
    public UserFullDto getOne(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        return userFullMapper.toUserFullDto(userOptional.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity with id `%s` not found".formatted(id))));
    }

    @Override
    public List<UserFullDto> getMany(List<Long> ids) {
        List<User> users = userRepository.findAllById(ids);
        return users.stream().map(userFullMapper::toUserFullDto).toList();
    }

    @Override
    public UserFullDto create(UserNewDto dto) {
        User user = userNewMapper.toEntity(dto);
        User resultUser = userRepository.save(user);
        return userFullMapper.toUserFullDto(resultUser);
    }

    @Override
    public UserFullDto patch(Long id, JsonNode patchNode) throws IOException {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Entity with id `%s` not found".formatted(id)));

        UserFullDto userFullDto = userFullMapper.toUserFullDto(user);
        objectMapper.readerForUpdating(userFullDto).readValue(patchNode);
        userFullMapper.updateWithNull(userFullDto, user);

        User resultUser = userRepository.save(user);
        return userFullMapper.toUserFullDto(resultUser);
    }

    @Override
    public List<Long> patchMany(List<Long> ids, JsonNode patchNode) throws IOException {
        Collection<User> users = userRepository.findAllById(ids);

        for (User user : users) {
            UserFullDto userFullDto = userFullMapper.toUserFullDto(user);
            objectMapper.readerForUpdating(userFullDto).readValue(patchNode);
            userFullMapper.updateWithNull(userFullDto, user);
        }

        List<User> resultUsers = userRepository.saveAll(users);
        return resultUsers.stream().map(User::getId).toList();
    }

    @Override
    public UserFullDto delete(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            userRepository.delete(user);
        }
        return userFullMapper.toUserFullDto(user);
    }

    @Override
    public void deleteMany(List<Long> ids) {
        userRepository.deleteAllById(ids);
    }
}
