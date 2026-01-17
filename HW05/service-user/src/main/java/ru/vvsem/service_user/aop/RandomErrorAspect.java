package ru.vvsem.service_user.aop;

import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import ru.vvsem.service_user.config.ErrorHandlingProperties;

@Aspect
@Component
@RequiredArgsConstructor
public class RandomErrorAspect {

    private final ErrorHandlingProperties errorHandlingProperties;
    private final Random random = new Random();

    // Для всех методов контроллера с аннотацией @RandomError
    @Before("@annotation(ru.vvsem.service_user.annotation.RandomError)")
    public void checkForRandomErrorWithAnnotation(JoinPoint joinPoint) {
        checkAndThrowError();
    }

    private void checkAndThrowError() {
        if (errorHandlingProperties.isEnabled() && random.nextDouble() < errorHandlingProperties.getProbability()) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format(
                            "Random error triggered (probability: %.2f%%)",
                            errorHandlingProperties.getProbability() * 100));
        }
    }
}
