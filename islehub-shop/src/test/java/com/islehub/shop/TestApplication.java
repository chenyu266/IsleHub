package com.islehub.shop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class
})
@ComponentScan("com.islehub")
@MapperScan("com.islehub.**.mapper")
public class TestApplication {
}
