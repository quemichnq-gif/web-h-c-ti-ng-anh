package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
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
            courses = courses.stream().filter(c -> c.getName().toLowerCase().contains(q)).toList();
        }

        if (status != null && !status.isBlank() && !status.equals("ALL")) {
            LocalDate now = LocalDate.now();
            courses = courses.stream().filter(c -> {
                String cs = getCourseStatus(c, now);
                return cs.equalsIgnoreCase(status);
            }).toList();
        }

        model.addAttribute("courses", courses);
        model.addAttribute("enrollmentCounts", courses.stream().collect(
                java.util.stream.Collectors.toMap(Course::getId, c -> enrollmentRepository.countByCourse(c))));
        model.addAttribute("search", search);
        model.addAttribute("filterStatus", status);
        model.addAttribute("now", LocalDate.now());
        return "courses/list";
    }

    @GetMapping("/create")
    public String createForm() {
        return "courses/create";
    }

    @PostMapping("/create")
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String startDate,
                         @RequestParam(required = false) String endDate,
                         RedirectAttributes ra) {
        Course course = new Course();
        course.setName(name);
        course.setDescription(description);
        if (startDate != null && !startDate.isBlank()) course.setStartDate(LocalDate.parse(startDate));
        if (endDate != null && !endDate.isBlank()) course.setEndDate(LocalDate.parse(endDate));
        courseRepository.save(course);
        ra.addFlashAttribute("success", "Đã tạo course '" + name + "' thành công!");
        return "redirect:/courses";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<Course> opt = courseRepository.findById(id);
        if (opt.isEmpty()) { ra.addFlashAttribute("error", "Course không tồn tại."); return "redirect:/courses"; }
        model.addAttribute("course", opt.get());
        return "courses/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String startDate,
                         @RequestParam(required = false) String endDate,
                         RedirectAttributes ra) {
        Optional<Course> opt = courseRepository.findById(id);
        if (opt.isEmpty()) { ra.addFlashAttribute("error", "Course không tồn tại."); return "redirect:/courses"; }

        Course course = opt.get();
        course.setName(name);
        course.setDescription(description);
        if (startDate != null && !startDate.isBlank()) course.setStartDate(LocalDate.parse(startDate));
        if (endDate != null && !endDate.isBlank()) course.setEndDate(LocalDate.parse(endDate));
        courseRepository.save(course);
        ra.addFlashAttribute("success", "Đã cập nhật course thành công!");
        return "redirect:/courses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        long enrolled = courseRepository.findById(id)
                .map(enrollmentRepository::countByCourse).orElse(0L);
        long tests = courseRepository.findById(id)
                .map(testRepository::countByCourse).orElse(0L);
        courseRepository.deleteById(id);
        String msg = "Đã xóa course.";
        if (enrolled > 0) msg += " Đã xóa " + enrolled + " enrollment liên quan.";
        if (tests > 0) msg += " Có " + tests + " test liên quan.";
        ra.addFlashAttribute("success", msg);
        return "redirect:/courses";
    }

    private String getCourseStatus(Course c, LocalDate now) {
        if (c.getStartDate() == null) return "Upcoming";
        if (now.isBefore(c.getStartDate())) return "Upcoming";
        if (c.getEndDate() != null && now.isAfter(c.getEndDate())) return "Ended";
        return "Ongoing";
    }
}
