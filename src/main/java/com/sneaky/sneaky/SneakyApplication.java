package com.sneaky.sneaky;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;

import com.sneaky.sneaky.repository.UsersRepository;

@SpringBootApplication
public class SneakyApplication implements CommandLineRunner {

	private final UsersRepository usersRepository;
	private final List<String> adminEmails;

	public SneakyApplication(
			UsersRepository usersRepository,
			@Value("${sneaky.admin.emails:}") List<String> adminEmails) {
		this.usersRepository = usersRepository;
		this.adminEmails = adminEmails;
	}

	public static void main(String[] args) {
		SpringApplication.run(SneakyApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Sneaky Application Started!");
		adminEmails.stream()
				.map(email -> email.trim().toLowerCase(Locale.ROOT))
				.filter(email -> !email.isBlank())
				.forEach(email -> usersRepository.findByEmail(email).ifPresent(user -> {
					if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
						user.setRole("ADMIN");
						usersRepository.save(user);
						System.out.println("Promoted admin user: " + email);
					}
				}));
	}
}
