package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/errors")
public class ErrorManagementController {

    private final ErrorTypeRepository errorTypeRepository;
    private final ErrorTestMappingRepository mappingRepository;
    private final StudentErrorRepository studentErrorRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;

    public ErrorManagementController(ErrorTypeRepository errorTypeRepository,
                                     ErrorTestMappingRepository mappingRepository,
                                     StudentErrorRepository studentErrorRepository,
                                     TestRepository testRepository,
                                     UserRepository userRepository) {
        this.errorTypeRepository = errorTypeRepository;
        this.mappingRepository = mappingRepository;
        this.studentErrorRepository = studentErrorRepository;
        this.testRepository = testRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Model model, @RequestParam(required = false) String search) {
        List<ErrorType> types = errorTypeRepository.findAll();
        if (search != null && !search.isBlank()) {
            final String q = search.toLowerCase();
            types = types.stream().filter(e -> e.getName().toLowerCase().contains(q)).toList();
        }
        model.addAttribute("errorTypes", types);
        model.addAttribute("tests", testRepository.findAll());
        model.addAttribute("mappings", mappingRepository.findAll());
        
        List<StudentError> studentErrors = studentErrorRepository.findAllOrderByCreatedAtDesc();
        if (studentErrors.size() > 20) {
            studentErrors = studentErrors.subList(0, 20);
        }
        model.addAttribute("studentErrors", studentErrors);
        model.addAttribute("search", search);
        return "errors/list";
    }

    @GetMapping("/create")
    public String createForm() {
        return "errors/create";
    }

    @PostMapping("/create")
    public String createType(@RequestParam String name,
                             @RequestParam(required = false) String description,
                             RedirectAttributes ra) {
        ErrorType et = new ErrorType();
        et.setName(name);
        et.setDescription(description);
        errorTypeRepository.save(et);
        ra.addFlashAttribute("success", "Đã tạo Error Type '" + name + "' thành công!");
        return "redirect:/errors";
    }

    @PostMapping("/types/{id}/delete")
    public String deleteType(@PathVariable Long id, RedirectAttributes ra) {
        mappingRepository.findByErrorTypeId(id).ifPresent(mappingRepository::delete);
        List<StudentError> related = studentErrorRepository.findByErrorTypeId(id);
        studentErrorRepository.deleteAll(related);
        errorTypeRepository.deleteById(id);
        ra.addFlashAttribute("success", "Đã xóa Error Type.");
        return "redirect:/errors";
    }

    @PostMapping("/mappings/create")
    public String createMapping(@RequestParam Long errorTypeId, @RequestParam Long testId,
                                RedirectAttributes ra) {
        Optional<ErrorType> etOpt = errorTypeRepository.findById(errorTypeId);
        Optional<Test> testOpt = testRepository.findById(testId);
        if (etOpt.isEmpty() || testOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Dữ liệu không hợp lệ.");
            return "redirect:/errors";
        }
        mappingRepository.findByErrorTypeId(errorTypeId).ifPresent(mappingRepository::delete);

        ErrorTestMapping mapping = new ErrorTestMapping();
        mapping.setErrorType(etOpt.get());
        mapping.setTest(testOpt.get());
        mappingRepository.save(mapping);
        ra.addFlashAttribute("success", "Đã gán test cho Error Type thành công!");
        return "redirect:/errors";
    }

    @GetMapping("/students")
    public String studentErrors(Model model, @RequestParam(required = false) Long studentId) {
        model.addAttribute("students", userRepository.findByRole(Role.STUDENT));
        if (studentId != null) {
            Optional<User> student = userRepository.findById(studentId);
            student.ifPresent(s -> {
                model.addAttribute("selectedStudent", s);
                model.addAttribute("studentErrors", studentErrorRepository.findByStudentId(studentId));
            });
        }
        return "errors/students";
    }
}
