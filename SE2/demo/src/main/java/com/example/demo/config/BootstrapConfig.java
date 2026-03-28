package com.example.demo.config;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Configuration
public class BootstrapConfig {

    private final Random random = new Random();

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               CourseRepository courseRepository,
                               EnrollmentRepository enrollmentRepository,
                               ErrorTypeRepository errorTypeRepository,
                               TestRepository testRepository,
                               QuestionRepository questionRepository,
                               StudentResultRepository studentResultRepository,
                               StudentErrorRepository studentErrorRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Seed Roles & Users
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@university.edu");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                admin.setStatus("ACTIVE");
                admin.setFullName("Lê Quang Quế (Admin)");
                admin.setPhone("0901234567");
                userRepository.save(admin);
                
                User staff = new User();
                staff.setUsername("staff");
                staff.setEmail("staff@university.edu");
                staff.setPassword(passwordEncoder.encode("staff123"));
                staff.setRole(Role.ACADEMIC_STAFF);
                staff.setStatus("ACTIVE");
                staff.setFullName("Nguyễn Văn Nhân");
                userRepository.save(staff);

                // Students
                String[] names = {"Trần Anh Tuấn", "Phạm Thị Mai", "Lê Gia Hưng", "Vũ Nhật Minh", "Hoàng Kim Chi", "Đỗ Bảo Ngọc"};
                for (int i = 1; i <= 6; i++) {
                    User student = new User();
                    student.setUsername("student" + i);
                    student.setEmail("student" + i + "@university.edu");
                    student.setPassword(passwordEncoder.encode("student123"));
                    student.setRole(Role.STUDENT);
                    student.setStatus("ACTIVE");
                    student.setFullName(names[i-1]);
                    userRepository.save(student);
                }
            }

            // 2. Seed Courses
            if (courseRepository.count() == 0) {
                Course c1 = new Course();
                c1.setName("IELTS Academic Writing Task 2");
                c1.setDescription("Học cách viết luận Academic IELTS đạt band 7.0+");
                c1.setStartDate(LocalDate.now().minusWeeks(2));
                c1.setEndDate(LocalDate.now().plusMonths(3));
                courseRepository.save(c1);

                Course c2 = new Course();
                c2.setName("Communicative Business English");
                c2.setDescription("Tiếng Anh giao tiếp chuyên sâu cho môi trường văn phòng quốc tế.");
                c2.setStartDate(LocalDate.now().plusWeeks(1));
                c2.setEndDate(LocalDate.now().plusMonths(6));
                courseRepository.save(c2);

                Course c3 = new Course();
                c3.setName("TOEIC Intensive 800+");
                c3.setDescription("Luyện đề TOEIC thần tốc, nắm vững mẹo thi đạt điểm cao.");
                c3.setStartDate(LocalDate.now().minusMonths(1));
                c3.setEndDate(LocalDate.now().plusMonths(2));
                courseRepository.save(c3);
                
                // 3. Seed Enrollments
                List<User> students = userRepository.findByRole(Role.STUDENT);
                for (User s : students) {
                    Enrollment e = new Enrollment();
                    e.setStudent(s);
                    e.setCourse(c1);
                    e.setStatus("APPROVED");
                    e.setEnrolledAt(LocalDateTime.now().minusDays(random.nextInt(10)));
                    enrollmentRepository.save(e);
                    
                    if (random.nextBoolean()) {
                        Enrollment e2 = new Enrollment();
                        e2.setStudent(s);
                        e2.setCourse(c3);
                        e2.setStatus(random.nextBoolean() ? "PENDING" : "APPROVED");
                        e2.setEnrolledAt(LocalDateTime.now().minusDays(random.nextInt(5)));
                        enrollmentRepository.save(e2);
                    }
                }
                
                // 4. Seed Error Types
                ErrorType et1 = new ErrorType();
                et1.setName("Grammar: Subject-Verb Agreement");
                et1.setDescription("Mắc lỗi chia động từ không khớp với chủ ngữ số ít/số nhiều.");
                errorTypeRepository.save(et1);
                
                ErrorType et2 = new ErrorType();
                et2.setName("Pronunciation: Final Sounds /s/ & /z/");
                et2.setDescription("Mất âm đuôi hoặc phát âm sai âm gió.");
                errorTypeRepository.save(et2);

                // 5. Seed Tests
                Test t1 = new Test();
                t1.setTitle("Midterm Writing Challenge");
                t1.setDescription("Kiểm tra kỹ năng viết luận học thuật.");
                t1.setDuration(60);
                t1.setCourse(c1);
                testRepository.save(t1);

                // Questions
                Question q1 = new Question();
                q1.setContent("Chia động từ: The team (be) _______ winning.");
                q1.setCorrectAnswer("is");
                q1.setTest(t1);
                questionRepository.save(q1);

                // Results
                for (int i = 0; i < 3; i++) {
                    StudentResult res = new StudentResult();
                    res.setStudent(students.get(i));
                    res.setTest(t1);
                    res.setScore(7.5 + i*0.5);
                    res.setSubmittedAt(LocalDateTime.now().minusHours(random.nextInt(48)));
                    studentResultRepository.save(res);
                    
                    // Add student errors
                    StudentError se = new StudentError();
                    se.setStudent(students.get(i));
                    se.setErrorType(et1);
                    se.setCreatedAt(LocalDateTime.now().minusHours(i*4));
                    studentErrorRepository.save(se);
                }
            }
        };
    }
}
