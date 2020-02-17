# Dubbo-Shop
RocketMQ Zookeeper SpringBoot MyBatis

# Dubbo

## Dubbo依赖

```
        <!--    dubbo spring-->
        <dependency>
            <groupId>com.alibaba.boot</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
            <version>0.2.0</version>
        </dependency>
        
          <!--            zookeeper-->
        <dependency>
            <groupId>org.apache.zookeeper</groupId>
            <artifactId>zookeeper</artifactId>
            <version>3.4.10</version>
            <exclusions>
                <exclusion>
                    <groupId>log4j</groupId>
                    <artifactId>log4j</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-log4j12</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
```
## Dubbo配置文件

```
#Dubbo 配置
spring.application.name=dubbo-user-provider 
dubbo.application.id=dubbo-user-provider
dubbo.application.name=dubbo-user-provider
dubbo.registry.address=zookeeper://xxx.xxx.xxx.xxx:2181;zookeeper://xxx.xxx.xxx.xxx:2182; #注册中心地址
dubbo.server=true
dubbo.protocol.name=dubbo
dubbo.protocol.port=20883 #多个服务需要使用多个端口 不可重复
dubbo.provider.timeout=120000 #发送消息超时时间
```

## Dubbo 使用
### 主方法
```
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo //开启Dubbo

public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class,args);
    }
}
```

### 接口类
```
public interface IUserService {
}
```

### 实现类
```
import org.springframework.stereotype.Component;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.annotation.Reference;


@Component      //注入Spring容器
@Service(interfaceClass = IUserService.class)   //注册到Dubbo注册中心
public class UserServiceImpl implements IUserService{

    @Reference          //引用Dubbo服务
    private XXXService xxxService;
}
```
