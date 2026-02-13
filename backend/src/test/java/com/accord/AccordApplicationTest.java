package com.accord;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AccordApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @Test
    void testApplicationHasRequiredBeans() {
        assertNotNull(applicationContext.getBean("userService"));
        assertNotNull(applicationContext.getBean("chatService"));
        assertNotNull(applicationContext.getBean("userController"));
    }
}
