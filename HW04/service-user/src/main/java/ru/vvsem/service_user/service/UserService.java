package ru.vvsem.service_user.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.vvsem.service_user.dto.UserFullDto;
import ru.vvsem.service_user.dto.UserNewDto;

public interface UserService {
    Page<UserFullDto> getAll(Pageable pageable);

    UserFullDto getOne(Long id);

    List<UserFullDto> getMany(List<Long> ids);

    UserFullDto create(UserNewDto dto);

    UserFullDto patch(Long id, JsonNode patchNode) throws IOException;

    List<Long> patchMany(List<Long> ids, JsonNode patchNode) throws IOException;

    UserFullDto delete(Long id);

    void deleteMany(List<Long> ids);
}
