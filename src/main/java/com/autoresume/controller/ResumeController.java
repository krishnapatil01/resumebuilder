package com.autoresume.controller;

import com.autoresume.model.Resume;
import com.autoresume.repository.ResumeRepository;
import com.autoresume.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "*")
public class ResumeController {

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private AnalysisService analysisService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            String text = analysisService.extractText(file);
            Resume resume = new Resume();
            resume.setFullText(text);
            // Basic extraction (can be improved)
            resumeRepository.save(resume);
            
            return ResponseEntity.ok(Map.of("message", "Resume uploaded", "text", text));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllResumes() {
        return ResponseEntity.ok(resumeRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getResumeById(@PathVariable Long id) {
        return resumeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResume(@PathVariable Long id) {
        resumeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Resume deleted"));
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveResume(@RequestBody Resume resume) {
        if (resume.getFullText() == null || resume.getFullText().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(resume.getFullName()).append(" ").append(resume.getTargetRole()).append(" ");
            if (resume.getProfileData() != null) {
                sb.append(resume.getProfileData()); // Basic fulltext from JSON
            }
            resume.setFullText(sb.toString().trim());
        }
        Resume saved = resumeRepository.save(resume);
        return ResponseEntity.ok(saved);
    }
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeWithJD(@RequestBody Map<String, String> request) {
        String jd = request.get("jobDescription");
        String resumeText = request.get("resumeText");
        
        if (resumeText == null || resumeText.isEmpty()) {
            resumeText = resumeRepository.findFirstByOrderByIdDesc()
                    .map(Resume::getFullText)
                    .orElse("");
        }

        Map<String, Object> analysis = analysisService.analyzeFit(resumeText, jd);
        return ResponseEntity.ok(analysis);
    }
}
