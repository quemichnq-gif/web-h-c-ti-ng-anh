package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/assessments")
public class AssessmentController {

    private final TestRepository testRepository;
    private final CourseRepository courseRepository;
    private final StudentResultRepository resultRepository;
    private final QuestionRepository questionRepository;

    public AssessmentController(TestRepository testRepository, CourseRepository courseRepository,
                                StudentResultRepository resultRepository, QuestionRepository questionRepository) {
        this.testRepository = testRepository;
        this.courseRepository = courseRepository;
        this.resultRepository = resultRepository;
        this.questionRepository = questionRepository;
    }

    @GetMapping
    public String list(Model model, @RequestParam(required = false) Long courseId,
                       @RequestParam(required = false) String search) {
        List<Course> courses = courseRepository.findAll();
        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourseId", courseId);

        List<Test> tests;
        if (courseId != null) {
            tests = testRepository.findByCourseId(courseId);
        } else {
            tests = testRepository.findAll();
        }

        if (search != null && !search.isBlank()) {
            final String q = search.toLowerCase();
            tests = tests.stream().filter(t -> t.getTitle().toLowerCase().contains(q)).toList();
        }
        
        Map<Long, Long> qCounts = new HashMap<>();
        for (Test t : tests) {
            if (t.getId() != null) {
                qCounts.put(t.getId(), questionRepository.countByTest(t));
            }
        }
        
        model.addAttribute("tests", tests);
        model.addAttribute("questionCounts", qCounts);
        model.addAttribute("search", search);
        return "assessments/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("courses", courseRepository.findAll());
        return "assessments/create";
    }

    @PostMapping("/create")
    public String create(@RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam Integer duration,
                         @RequestParam Long courseId,
                         @RequestParam(required = false) List<String> questionTypes,
                         @RequestParam(required = false) List<String> questionContents,
                         @RequestParam(required = false) List<String> correctAnswers,
                         @RequestParam(required = false) List<String> optionAs,
                         @RequestParam(required = false) List<String> optionBs,
                         @RequestParam(required = false) List<String> optionCs,
                         @RequestParam(required = false) List<String> optionDs,
                         RedirectAttributes ra) {
        
        Optional<Course> course = courseRepository.findById(courseId);
        if (course.isEmpty()) { 
            ra.addFlashAttribute("error", "Course không tồn tại."); 
            return "redirect:/assessments/create"; 
        }

        Test test = new Test();
        test.setTitle(title);
        test.setDescription(description);
        test.setDuration(duration);
        test.setCourse(course.get());
        testRepository.save(test);

        if (questionContents != null) {
            for (int i = 0; i < questionContents.size(); i++) {
                if (!questionContents.get(i).isBlank()) {
                    Question q = new Question();
                    q.setTest(test);
                    q.setContent(questionContents.get(i));
                    
                    // Set type
                    String type = (questionTypes != null && i < questionTypes.size()) ? questionTypes.get(i) : "SHORT_ANSWER";
                    q.setQuestionType(QuestionType.valueOf(type));
                    
                    // Set correct answer
                    q.setCorrectAnswer(correctAnswers != null && i < correctAnswers.size() ? correctAnswers.get(i) : "");
                    
                    // Set options if it's multiple choice
                    if (QuestionType.MULTIPLE_CHOICE.name().equals(type)) {
                        q.setOptionA(getVal(optionAs, i));
                        q.setOptionB(getVal(optionBs, i));
                        q.setOptionC(getVal(optionCs, i));
                        q.setOptionD(getVal(optionDs, i));
                    }
                    questionRepository.save(q);
                }
            }
        }

        ra.addFlashAttribute("success", "Tạo assessment '" + title + "' thành công!");
        return "redirect:/assessments";
    }

    private String getVal(List<String> list, int i) {
        return (list != null && i < list.size()) ? list.get(i) : "";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<Test> opt = testRepository.findById(id);
        if (opt.isEmpty()) { ra.addFlashAttribute("error", "Test không tồn tại."); return "redirect:/assessments"; }
        model.addAttribute("test", opt.get());
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("questions", questionRepository.findByTestId(id));
        long resultCount = resultRepository.countByTest(opt.get());
        model.addAttribute("resultCount", resultCount);
        return "assessments/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam Integer duration,
                         @RequestParam Long courseId,
                         @RequestParam(required = false) List<String> questionTypes,
                         @RequestParam(required = false) List<String> questionContents,
                         @RequestParam(required = false) List<String> correctAnswers,
                         @RequestParam(required = false) List<String> optionAs,
                         @RequestParam(required = false) List<String> optionBs,
                         @RequestParam(required = false) List<String> optionCs,
                         @RequestParam(required = false) List<String> optionDs,
                         RedirectAttributes ra) {
        Optional<Test> opt = testRepository.findById(id);
        if (opt.isEmpty()) { ra.addFlashAttribute("error", "Test không tồn tại."); return "redirect:/assessments"; }

        Test test = opt.get();
        courseRepository.findById(courseId).ifPresent(test::setCourse);
        test.setTitle(title);
        test.setDescription(description);
        test.setDuration(duration);
        testRepository.save(test);

        // Rebuild questions
        questionRepository.deleteAll(questionRepository.findByTestId(id));
        if (questionContents != null) {
            for (int i = 0; i < questionContents.size(); i++) {
                if (!questionContents.get(i).isBlank()) {
                    Question q = new Question();
                    q.setTest(test);
                    q.setContent(questionContents.get(i));
                    
                    String type = (questionTypes != null && i < questionTypes.size()) ? questionTypes.get(i) : "SHORT_ANSWER";
                    q.setQuestionType(QuestionType.valueOf(type));
                    q.setCorrectAnswer(correctAnswers != null && i < correctAnswers.size() ? correctAnswers.get(i) : "");
                    
                    if (QuestionType.MULTIPLE_CHOICE.name().equals(type)) {
                        q.setOptionA(getVal(optionAs, i));
                        q.setOptionB(getVal(optionBs, i));
                        q.setOptionC(getVal(optionCs, i));
                        q.setOptionD(getVal(optionDs, i));
                    }
                    questionRepository.save(q);
                }
            }
        }

        ra.addFlashAttribute("success", "Cập nhật test thành công!");
        return "redirect:/assessments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        questionRepository.deleteAll(questionRepository.findByTestId(id));
        testRepository.deleteById(id);
        ra.addFlashAttribute("success", "Đã xóa test.");
        return "redirect:/assessments";
    }

    @GetMapping("/{id}/results")
    public String results(@PathVariable Long id, Model model,
                          @RequestParam(required = false) String bloomFilter,
                          @RequestParam(required = false) String passFilter,
                          RedirectAttributes ra) {
        Optional<Test> opt = testRepository.findById(id);
        if (opt.isEmpty()) { ra.addFlashAttribute("error", "Test không tồn tại."); return "redirect:/assessments"; }

        Test test = opt.get();
        List<StudentResult> results = resultRepository.findByTestId(id);

        if (bloomFilter != null && !bloomFilter.isBlank()) {
            results = results.stream().filter(r -> r.getBloomLevel().equalsIgnoreCase(bloomFilter)).toList();
        }
        if ("pass".equals(passFilter)) {
            results = results.stream().filter(StudentResult::isPassed).toList();
        } else if ("fail".equals(passFilter)) {
            results = results.stream().filter(r -> !r.isPassed()).toList();
        }

        Double avg = resultRepository.findAverageScoreByTestId(id);
        model.addAttribute("test", test);
        model.addAttribute("results", results);
        model.addAttribute("avgScore", avg != null ? String.format("%.1f", avg) : "N/A");
        model.addAttribute("totalCount", results.size());
        model.addAttribute("bloomFilter", bloomFilter);
        model.addAttribute("passFilter", passFilter);
        return "assessments/results";
    }
}
