package com.quizapp.config;

import com.quizapp.entity.*;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Seed default Admin if not present
        User admin = userRepository.findByEmail("admin@quizapp.com").orElseGet(() -> {
            User newAdmin = User.builder()
                    .fullName("System Administrator")
                    .email("admin@quizapp.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            return userRepository.save(newAdmin);
        });

        // Seed default Student if not present
        userRepository.findByEmail("student@quizapp.com").orElseGet(() -> {
            User newStudent = User.builder()
                    .fullName("Alex Student")
                    .email("student@quizapp.com")
                    .password(passwordEncoder.encode("student123"))
                    .role(Role.STUDENT)
                    .enabled(true)
                    .build();
            return userRepository.save(newStudent);
        });

        // Seed sample quiz if no quizzes exist
        if (quizRepository.count() == 0) {
            Quiz quiz = Quiz.builder()
                    .title("Java Core & Spring Boot Basics")
                    .description("Test your knowledge on core Java fundamentals, OOP concepts, and Spring Boot framework essentials.")
                    .durationMinutes(10)
                    .totalMarks(30)
                    .active(true)
                    .createdBy(admin)
                    .questions(new ArrayList<>())
                    .build();

            // Question 1
            Question q1 = Question.builder()
                    .questionText("Which annotation is used to mark a class as a Spring REST Controller?")
                    .marks(10)
                    .quiz(quiz)
                    .options(new ArrayList<>())
                    .build();

            q1.getOptions().add(Option.builder().optionText("@RestController").correct(true).question(q1).build());
            q1.getOptions().add(Option.builder().optionText("@Controller").correct(false).question(q1).build());
            q1.getOptions().add(Option.builder().optionText("@Service").correct(false).question(q1).build());
            q1.getOptions().add(Option.builder().optionText("@Component").correct(false).question(q1).build());

            // Question 2
            Question q2 = Question.builder()
                    .questionText("What is the default scope of a Spring Bean?")
                    .marks(10)
                    .quiz(quiz)
                    .options(new ArrayList<>())
                    .build();

            q2.getOptions().add(Option.builder().optionText("Singleton").correct(true).question(q2).build());
            q2.getOptions().add(Option.builder().optionText("Prototype").correct(false).question(q2).build());
            q2.getOptions().add(Option.builder().optionText("Request").correct(false).question(q2).build());
            q2.getOptions().add(Option.builder().optionText("Session").correct(false).question(q2).build());

            // Question 3
            Question q3 = Question.builder()
                    .questionText("Which collection class in Java allows unique elements only?")
                    .marks(10)
                    .quiz(quiz)
                    .options(new ArrayList<>())
                    .build();

            q3.getOptions().add(Option.builder().optionText("HashSet").correct(true).question(q3).build());
            q3.getOptions().add(Option.builder().optionText("ArrayList").correct(false).question(q3).build());
            q3.getOptions().add(Option.builder().optionText("LinkedList").correct(false).question(q3).build());
            q3.getOptions().add(Option.builder().optionText("Vector").correct(false).question(q3).build());

            quiz.getQuestions().addAll(List.of(q1, q2, q3));
            quizRepository.save(quiz);
        }
    }
}
