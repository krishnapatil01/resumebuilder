package com.autoresume.controller;

import com.autoresume.model.JobApplication;
import com.autoresume.repository.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobApplicationRepository repository;

    @Autowired
    private com.autoresume.service.AnalysisService analysisService;

    @Autowired
    private com.autoresume.repository.ResumeRepository resumeRepository;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @PostMapping("/add")
    public ResponseEntity<?> addApplication(@RequestBody JobApplication application) {
        if (application.getAppliedDate() == null) {
            application.setAppliedDate(LocalDate.now());
        }

        // Calculate fit score if JD is provided and a resume exists
        if (application.getJobDescription() != null && !application.getJobDescription().isEmpty()) {
            Optional<com.autoresume.model.Resume> resume = resumeRepository.findFirstByOrderByIdDesc();
            if (resume.isPresent()) {
                Map<String, Object> analysis = analysisService.analyzeFit(resume.get().getFullText(), application.getJobDescription());
                application.setFitScore((Double) analysis.get("fitScore"));
                application.setMissingSkills(analysis.get("missingSkills").toString());
                application.setMatchedKeywords(analysis.get("matchedKeywords").toString());
                
                try {
                    application.setBreakdown(objectMapper.writeValueAsString(analysis.get("breakdown")));
                    application.setSuggestions(objectMapper.writeValueAsString(analysis.get("suggestions")));
                    application.setEvaluationSummary((String) analysis.get("summary"));
                } catch (Exception e) {
                    // Fallback if serialization fails
                    application.setEvaluationSummary("Analysis completed with errors.");
                }
            }
        }

        repository.save(application);
        return ResponseEntity.ok(application);
    }

    @GetMapping("/all")
    public ResponseEntity<List<JobApplication>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        List<JobApplication> all = repository.findAll();
        
        long total = all.size();
        double avgFit = all.stream()
                .filter(a -> a.getFitScore() != null)
                .mapToDouble(JobApplication::getFitScore)
                .average().orElse(0.0);
        
        long interviewCount = all.stream()
                .filter(a -> "Interview".equalsIgnoreCase(a.getStatus()))
                .count();
        
        double interviewRate = total == 0 ? 0 : (double) interviewCount / total * 100;

        // Weekly activity (last 7 days)
        Map<LocalDate, Long> weekly = all.stream()
                .filter(a -> a.getAppliedDate() != null && a.getAppliedDate().isAfter(LocalDate.now().minusDays(7)))
                .collect(Collectors.groupingBy(JobApplication::getAppliedDate, Collectors.counting()));

        // Streak tracking (simple gap analysis)
        // This calculates the longest sequence of days with at least 1 application
        // For simplicity, let's just return the last 7 days count
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalApplications", total);
        stats.put("avgFitScore", Math.round(avgFit * 10.0) / 10.0);
        stats.put("interviewRate", Math.round(interviewRate * 10.0) / 10.0);
        stats.put("weeklyActivity", weekly);
        
        return ResponseEntity.ok(stats);
    }
}
