package com.sky.compilerbackend;

import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class TestDemo {
//    private static final Logger log = LoggerFactory.getLogger(TestDemo.class);
    @Test
    public void testdemo(){
        log.debug("fuck");
    }
}
