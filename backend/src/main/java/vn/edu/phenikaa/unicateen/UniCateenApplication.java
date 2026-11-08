package vn.edu.phenikaa.unicateen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** UniCateen - he thong quan ly canteen truong hoc (Nhom 10, Phan tich va thiet ke phan mem). */
@SpringBootApplication
@EnableScheduling
public class UniCateenApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniCateenApplication.class, args);
    }
}
