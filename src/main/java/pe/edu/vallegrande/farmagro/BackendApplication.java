package pe.edu.vallegrande.farmagro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@SpringBootApplication
@EnableReactiveMongoRepositories(basePackages = "pe.edu.vallegrande.farmagro.repository")
public class BackendApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
