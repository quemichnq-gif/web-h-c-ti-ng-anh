package com.example.demo.controller;

import com.example.demo.model.Course;
import com.example.demo.model.CourseStatus;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.TestRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TestRepository testRepository;

    public CourseController(CourseRepository courseRepository, EnrollmentRepository enrollmentRepository,
                            TestRepository testRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.testRepository = testRepository;
    }

    @GetMapping
    public String list(Model model, @RequestParam(required = false) String search,
                       @RequestParam(required = false) String status) {
        List<Course> courses = courseRepository.findAll();

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            courses = courses.stream()
                    .filter(c -> safe(c.getName()).contains(q) || safe(c.getCode()).contains(q))
                    .toList();
        }

        if (status != null && !status.isBlank() && !status.equals("ALL")) {
            courses = courses.stream()
                    .filter(c -> c.getStatus() != null && c.getStatus().name().equals(status))
                    .toList();
        }

        model.addAttribute("courses", courses);
        model.addAttribute("enrollmentCounts", courses.stream().collect(
                java.util.stream.Collectors.toMap(Course::getId, c -> enrollmentRepository.countByCourse(c))));
        model.addAttribute("search", search);
        model.addAttribute("filterStatus", status);
        model.addAttribute("statuses", CourseStatus.values());
        model.addAttribute("now", LocalDate.now());
        return "courses/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("statuses", List.of(CourseStatus.DRAFT, CourseStatus.OPEN, CourseStatus.ARCHIVED));
        return "courses/create";
    }

    @PostMapping("/create")
    public String create(@RequestParam String code,
                         @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam String status,
                         @RequestParam(required = false) String startDate,
                         @RequestParam(required = false) String endDate,
                         RedirectAttributes ra) {
        if (courseRepository.findAll().stream().anyMatch(course -> course.getCode() != null && course.getCode().equalsIgnoreCase(code))) {
            ra.addFlashAttribute("error", "Course code '" + code + "' da ton tai.");
            return "redirect:/courses/create";
        }

        Course course = new Course();
        course.setCode(code.trim().toUpperCase());
        course.setName(name);
        course.setDescription(description);
        course.setStatus(CourseStatus.valueOf(status));
        if (startDate != null && !startDate.isBlank()) course.setStartDate(LocalDate.parse(startDate));
        if (endDate != null && !endDate.isBlank()) course.setEndDate(LocalDate.parse(endDate));
        courseRepository.save(course);
        ra.addFlashAttribute("success", "Da tao course '" + name + "' thanh cong!");
        return "redirect:/courses";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<Course> opt = courseRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Course khong ton tai.");
            return "redirect:/courses";
        }
        model.addAttribute("course", opt.get());
        model.addAttribute("statuses", CourseStatus.values());
        return "courses/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam String code,
                         @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam String status,
                         @RequestParam(required = false) String startDate,
                         @RequestParam(required = false) String endDate,
                         RedirectAttributes ra) {
        Optional<Course> opt = courseRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Course khong ton tai.");
            return "redirect:/courses";
        }

        boolean codeUsedByAnother = courseRepository.findAll().stream()
                .anyMatch(course -> !course.getId().equals(id) && course.getCode() != null && course.getCode().equalsIgnoreCase(code));
        if (codeUsedByAnother) {
            ra.addFlashAttribute("error", "Course code '" + code + "' da duoc dung.");
            return "redirect:/courses/" + id + "/edit";
        }

        Course course = opt.get();
        course.setCode(code.trim().toUpperCase());
        course.setName(name);
        course.setDescription(description);
        course.setStatus(CourseStatus.valueOf(status));
        if (startDate != null && !startDate.isBlank()) course.setStartDate(LocalDate.parse(startDate));
        if (endDate != null && !endDate.isBlank()) course.setEndDate(LocalDate.parse(endDate));
        courseRepository.save(course);
        ra.addFlashAttribute("success", "Da cap nhat course thanh cong!");
        return "redirect:/courses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        long enrolled = courseRepository.findById(id)
                .map(enrollmentRepository::countByCourse).orElse(0L);
        long tests = courseRepository.findById(id)
                .map(testRepository::countByCourse).orElse(0L);
        courseRepository.deleteById(id);
        String msg = "Da xoa course.";
        if (enrolled > 0) msg += " Da xoa " + enrolled + " enrollment lien quan.";
        if (tests > 0) msg += " Co " + tests + " test lien quan.";
        ra.addFlashAttribute("success", msg);
        return "redirect:/courses";
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
